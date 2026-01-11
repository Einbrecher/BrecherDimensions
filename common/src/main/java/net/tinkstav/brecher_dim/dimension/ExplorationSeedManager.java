/*
 * Brecher's Dimensions - Temporary resettable dimensions for exploration
 * Copyright (C) 2025 Einbrecher. All rights reserved.
 */

package net.tinkstav.brecher_dim.dimension;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.tinkstav.brecher_dim.util.DimensionUtils;
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
     * @deprecated Use {@link DimensionUtils#isExplorationDimension(ResourceKey)} instead
     */
    @Deprecated
    public static boolean isExplorationDimension(ResourceKey<Level> dimension) {
        return DimensionUtils.isExplorationDimension(dimension);
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
        LOGGER.info("Set current dimension context: {}", dimension != null ? dimension.location() : "null");
        
        // Log the registered seed for this dimension if it exists
        if (dimension != null) {
            Long seed = DIMENSION_SEEDS.get(dimension);
            if (seed != null) {
                LOGGER.info("Dimension {} has registered seed: {}", dimension.location(), seed);
            }
        }
    }
    
    /**
     * Get the current dimension being processed
     */
    public static ResourceKey<Level> getCurrentDimension() {
        ResourceKey<Level> current = CURRENT_DIMENSION.get();
        if (current != null) {
            LOGGER.debug("Retrieved current dimension context: {}", current.location());
        }
        return current;
    }
    
    /**
     * Clear the current dimension context
     */
    public static void clearCurrentDimension() {
        ResourceKey<Level> current = CURRENT_DIMENSION.get();
        CURRENT_DIMENSION.remove();
        LOGGER.info("Cleared dimension context (was: {})", current != null ? current.location() : "null");
    }
    
    /**
     * Get the seed for a dimension if registered
     */
    public static java.util.Optional<Long> getSeedForDimension(ResourceKey<Level> dimension) {
        return java.util.Optional.ofNullable(DIMENSION_SEEDS.get(dimension));
    }
    
    /**
     * Get all registered dimension seeds
     */
    public static Map<ResourceKey<Level>, Long> getAllDimensionSeeds() {
        return new ConcurrentHashMap<>(DIMENSION_SEEDS);
    }
    
    /**
     * Clear all registered seeds (for cleanup)
     */
    public static void clearAll() {
        DIMENSION_SEEDS.clear();
        CURRENT_DIMENSION.remove();
    }
}