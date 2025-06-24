package net.tinkstav.brecher_dim.performance;

import net.minecraft.server.MinecraftServer;
import net.tinkstav.brecher_dim.Brecher_Dim;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class MemoryMonitor {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long MB = 1024 * 1024;
    
    /**
     * Log current memory usage
     */
    public static void logMemoryUsage(String context) {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / MB;
        long totalMemory = runtime.totalMemory() / MB;
        long freeMemory = runtime.freeMemory() / MB;
        long usedMemory = totalMemory - freeMemory;
        
        LOGGER.info("[{}] Memory - Used: {}MB, Free: {}MB, Total: {}MB, Max: {}MB",
            context, usedMemory, freeMemory, totalMemory, maxMemory);
    }
    
    /**
     * Check if memory usage is high
     */
    public static boolean isMemoryPressureHigh() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        // If we're using more than 80% of max memory
        return (double) usedMemory / maxMemory > 0.8;
    }
    
    /**
     * Suggest garbage collection if needed
     */
    public static void suggestGarbageCollection() {
        if (isMemoryPressureHigh()) {
            LOGGER.info("High memory pressure detected, suggesting garbage collection");
            System.gc();
        }
    }
}