package net.tinkstav.brecher_dim.util;

import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Utility class for managing persistent dimension counters
 * Each dimension type (overworld, nether, end) gets its own counter
 * This ensures consistent naming: exploration_overworld_0, exploration_nether_0, etc.
 * Counters persist across server restarts to avoid naming conflicts
 */
public class DimensionCounterUtil {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String COUNTER_FILE = "brecher_dimension_counters.dat";
    private static final Map<String, AtomicLong> COUNTERS = new ConcurrentHashMap<>();
    private static Path counterPath = null;
    private static volatile boolean isDirty = false;
    
    /**
     * Initialize the counters from saved data
     */
    public static void initialize(MinecraftServer server) {
        try {
            // Store in data directory instead of world root
            counterPath = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                .resolve("data")
                .resolve(COUNTER_FILE);
            
            if (Files.exists(counterPath)) {
                // Read the file and parse counters
                String content = Files.readString(counterPath).trim();
                if (!content.isEmpty()) {
                    String[] lines = content.split("\n");
                    for (String line : lines) {
                        String[] parts = line.split("=");
                        if (parts.length == 2) {
                            String dimType = parts[0].trim();
                            long counter = Long.parseLong(parts[1].trim());
                            COUNTERS.put(dimType, new AtomicLong(counter));
                            LOGGER.info("Loaded counter for {}: {}", dimType, counter);
                        }
                    }
                }
            } else {
                LOGGER.info("No saved dimension counters found, starting from 0");
                // Ensure data directory exists
                Files.createDirectories(counterPath.getParent());
                saveCounters();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load dimension counters, starting from 0", e);
            COUNTERS.clear();
        }
    }
    
    /**
     * Get the next dimension ID for a specific dimension type
     */
    public static long getNextDimensionId(ResourceLocation baseDimension) {
        String dimType = baseDimension.getPath();
        AtomicLong counter = COUNTERS.computeIfAbsent(dimType, k -> new AtomicLong(0));
        long id = counter.getAndIncrement();
        isDirty = true; // Mark as dirty instead of saving immediately
        return id;
    }
    
    /**
     * Get the current counter value for a dimension type without incrementing
     */
    public static long getCurrentCounter(ResourceLocation baseDimension) {
        String dimType = baseDimension.getPath();
        AtomicLong counter = COUNTERS.get(dimType);
        return counter != null ? counter.get() : 0;
    }
    
    /**
     * Save all counters to disk
     */
    private static void saveCounters() {
        if (counterPath != null && isDirty) {
            try {
                // Ensure data directory exists before saving
                Files.createDirectories(counterPath.getParent());
                
                StringBuilder content = new StringBuilder();
                COUNTERS.forEach((dimType, counter) -> {
                    content.append(dimType).append("=").append(counter.get()).append("\n");
                });
                Files.writeString(counterPath, content.toString());
                isDirty = false;
                LOGGER.debug("Saved dimension counters to {}", counterPath);
            } catch (IOException e) {
                LOGGER.error("Failed to save dimension counters", e);
            }
        }
    }
    
    /**
     * Force save counters if dirty (call on server shutdown)
     */
    public static void saveIfDirty() {
        saveCounters();
    }
    
    /**
     * Reset all counters (for admin use)
     */
    public static void resetCounters() {
        COUNTERS.clear();
        isDirty = true;
        saveCounters(); // Save immediately for admin commands
        LOGGER.info("All dimension counters reset to 0");
    }
    
    /**
     * Reset counter for a specific dimension type
     */
    public static void resetCounter(ResourceLocation baseDimension) {
        String dimType = baseDimension.getPath();
        COUNTERS.remove(dimType);
        isDirty = true;
        saveCounters(); // Save immediately for admin commands
        LOGGER.info("Dimension counter for {} reset to 0", dimType);
    }
    
    /**
     * Get all current counters for display
     */
    public static Map<String, Long> getAllCounters() {
        Map<String, Long> result = new HashMap<>();
        COUNTERS.forEach((dimType, counter) -> result.put(dimType, counter.get()));
        return result;
    }
}