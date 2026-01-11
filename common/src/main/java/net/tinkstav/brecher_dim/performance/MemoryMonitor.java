/*
 * Brecher's Dimensions - Temporary resettable dimensions for exploration
 * Copyright (C) 2025 Einbrecher. All rights reserved.
 */

package net.tinkstav.brecher_dim.performance;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * Monitors JVM memory usage for performance throttling decisions.
 *
 * <p><b>Memory Calculation Explanation:</b>
 * <pre>
 * JVM Heap Memory Model:
 * ┌─────────────────────────────────────────────────────┐
 * │                   maxMemory (-Xmx)                   │  Maximum heap limit
 * │ ┌─────────────────────────────────────┐             │
 * │ │         totalMemory                  │             │  Currently allocated
 * │ │ ┌───────────────┬──────────────────┐│             │
 * │ │ │   usedMemory  │    freeMemory    ││             │
 * │ │ └───────────────┴──────────────────┘│             │
 * │ └─────────────────────────────────────┘             │
 * └─────────────────────────────────────────────────────┘
 *
 * - maxMemory:   Maximum heap the JVM will allocate (set by -Xmx flag)
 * - totalMemory: Currently allocated heap (grows up to maxMemory as needed)
 * - freeMemory:  Unused portion of totalMemory
 * - usedMemory:  totalMemory - freeMemory (actual memory in use)
 * </pre>
 *
 * <p><b>Why use maxMemory as denominator:</b>
 * <ul>
 *   <li>Provides consistent percentage regardless of heap expansion state</li>
 *   <li>Reflects the actual OOM threshold the JVM will hit</li>
 *   <li>Using totalMemory would give inconsistent readings as heap grows</li>
 * </ul>
 *
 * <p><b>Usage in chunk generation:</b> The {@link #getMemoryUsagePercent()} method
 * is used by {@link net.tinkstav.brecher_dim.generation.GenerationTask} to pause
 * generation when memory exceeds the configured threshold (default: 85%).
 */
public class MemoryMonitor {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    /**
     * Log current memory usage with a context message
     */
    public static void logMemoryUsage(String context) {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / 1024 / 1024;
        long totalMemory = runtime.totalMemory() / 1024 / 1024;
        long freeMemory = runtime.freeMemory() / 1024 / 1024;
        long usedMemory = totalMemory - freeMemory;
        
        LOGGER.info("[{}] Memory usage: {} MB / {} MB (max: {} MB)", 
                   context, usedMemory, totalMemory, maxMemory);
    }
    
    /**
     * Check if memory usage is above a threshold
     */
    public static boolean isMemoryPressureHigh() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        // Consider high pressure if used memory is over 80% of max
        return (double) usedMemory / maxMemory > 0.8;
    }
    
    /**
     * Get current memory usage as a percentage (0-100).
     *
     * <p>Calculation: {@code (totalMemory - freeMemory) / maxMemory * 100}
     *
     * <p>This uses {@code maxMemory} (the -Xmx limit) as the denominator rather than
     * {@code totalMemory} (currently allocated heap) because:
     * <ul>
     *   <li>The JVM expands the heap dynamically up to maxMemory</li>
     *   <li>Using totalMemory would give inconsistent readings (e.g., 90% at 1GB, then 45% after expansion to 2GB)</li>
     *   <li>maxMemory represents the actual OOM threshold we need to avoid</li>
     * </ul>
     *
     * @return memory usage percentage (0-100), where 100 means at the -Xmx limit
     */
    public static int getMemoryUsagePercent() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        return (int) ((double) usedMemory / maxMemory * 100);
    }
}