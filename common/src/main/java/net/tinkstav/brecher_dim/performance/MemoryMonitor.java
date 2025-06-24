package net.tinkstav.brecher_dim.performance;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * Monitors memory usage for the mod
 * Platform-agnostic implementation
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
}