package net.tinkstav.brecher_dim.dimension;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.tinkstav.brecher_dim.BrecherDimensions;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages seed modifications for exploration dimensions
 * This is the single source of truth for determining if a dimension
 * should use a modified seed and what that seed should be
 */
public class ExplorationSeedManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<ResourceKey<Level>, Long> DIMENSION_SEEDS = new ConcurrentHashMap<>();
    private static final ThreadLocal<ResourceKey<Level>> CURRENT_DIMENSION = new ThreadLocal<>();
    
    /**
     * Register a custom seed for an exploration dimension
     */
    public static void registerDimensionSeed(ResourceKey<Level> dimension, long seed) {
        DIMENSION_SEEDS.put(dimension, seed);
        LOGGER.info("Registered exploration dimension {} with custom seed {}", dimension.location(), seed);
    }
    
    /**
     * Check if a dimension is an exploration dimension
     */
    public static boolean isExplorationDimension(ResourceKey<Level> dimension) {
        if (dimension == null) return false;
        ResourceLocation location = dimension.location();
        return location.getNamespace().equals(BrecherDimensions.MOD_ID) && 
               location.getPath().startsWith("exploration_");
    }
    
    /**
     * Get the modified seed for a dimension
     */
    public static long modifySeed(ResourceKey<Level> dimension, long originalSeed) {
        if (!isExplorationDimension(dimension)) {
            return originalSeed;
        }
        
        Long customSeed = DIMENSION_SEEDS.get(dimension);
        if (customSeed != null) {
            LOGGER.info("Using registered custom seed {} for dimension {}", customSeed, dimension.location());
            return customSeed;
        }
        
        // If no specific seed registered, generate one based on dimension name
        // This ensures consistent but different generation
        long modifier = dimension.location().hashCode() * 0x123456789L;
        long modifiedSeed = originalSeed ^ modifier;
        
        LOGGER.debug("Generated seed {} for dimension {} (original: {})", 
                    modifiedSeed, dimension.location(), originalSeed);
        
        return modifiedSeed;
    }
    
    /**
     * Set the current dimension being processed
     * Used by mixins to track context
     */
    public static void setCurrentDimension(ResourceKey<Level> dimension) {
        CURRENT_DIMENSION.set(dimension);
        LOGGER.trace("Set current dimension context: {}", dimension != null ? dimension.location() : "null");
    }
    
    /**
     * Get the current dimension being processed
     */
    public static ResourceKey<Level> getCurrentDimension() {
        return CURRENT_DIMENSION.get();
    }
    
    /**
     * Clear the current dimension context
     */
    public static void clearCurrentDimension() {
        CURRENT_DIMENSION.remove();
        LOGGER.trace("Cleared dimension context");
    }
    
    /**
     * Get the seed for a dimension if registered
     */
    public static java.util.Optional<Long> getSeedForDimension(ResourceKey<Level> dimension) {
        return java.util.Optional.ofNullable(DIMENSION_SEEDS.get(dimension));
    }
    
    /**
     * Clear all registered seeds (for cleanup)
     */
    public static void clearAll() {
        DIMENSION_SEEDS.clear();
        CURRENT_DIMENSION.remove();
    }
}