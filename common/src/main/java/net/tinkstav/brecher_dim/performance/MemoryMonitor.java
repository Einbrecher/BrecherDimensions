/*
 * Brecher's Dimensions - Temporary resettable dimensions for exploration
 * Copyright (C) 2025 Einbrecher
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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