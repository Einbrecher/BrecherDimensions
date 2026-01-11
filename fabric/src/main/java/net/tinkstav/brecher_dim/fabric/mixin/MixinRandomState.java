/*
 * Brecher's Dimensions - Temporary resettable dimensions for exploration
 * Copyright (C) 2025 Einbrecher. All rights reserved.
 */

package net.tinkstav.brecher_dim.fabric.mixin;

import com.mojang.logging.LogUtils;
import net.minecraft.core.HolderGetter;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.tinkstav.brecher_dim.dimension.ExplorationSeedManager;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Mixin to intercept RandomState creation and modify the seed for exploration dimensions
 */
@Mixin(RandomState.class)
public class MixinRandomState {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    /**
     * Modify the seed variable before it's used in RandomState.create
     */
    @ModifyVariable(
        method = "create(Lnet/minecraft/world/level/levelgen/NoiseGeneratorSettings;Lnet/minecraft/core/HolderGetter;J)Lnet/minecraft/world/level/levelgen/RandomState;",
        at = @At("HEAD"),
        argsOnly = true,
        index = 2
    )
    private static long brecher_dim$modifySeedForExploration(long seed) {
        // First check if we're creating RandomState for an exploration dimension via context
        ResourceKey<?> currentDimension = ExplorationSeedManager.getCurrentDimension();
        
        // Add debug logging to track all RandomState creation
        if (currentDimension != null) {
            LOGGER.debug("RandomState.create() called with dimension context: {} (seed: {})", 
                        currentDimension.location(), seed);
        } else {
            LOGGER.debug("RandomState.create() called with no dimension context (seed: {})", seed);
        }
        
        if (currentDimension != null && currentDimension instanceof ResourceKey<?>) {
            @SuppressWarnings("unchecked")
            ResourceKey<Level> levelKey = (ResourceKey<Level>) currentDimension;
            
            // Double-check this is actually an exploration dimension
            if (ExplorationSeedManager.isExplorationDimension(levelKey)) {
                // Get the registered custom seed directly, ignoring the input seed
                java.util.Optional<Long> customSeed = ExplorationSeedManager.getSeedForDimension(levelKey);
                if (customSeed.isPresent()) {
                    long registeredSeed = customSeed.get();
                    LOGGER.info("Using registered seed for exploration dimension {}: {} (ignoring input seed {})", 
                               currentDimension.location(), registeredSeed, seed);
                    return registeredSeed;
                }
                // Fallback to modification if no seed is registered
                long modifiedSeed = ExplorationSeedManager.modifySeed(levelKey, seed);
                LOGGER.info("Modifying seed for exploration dimension {}: {} -> {}", 
                           currentDimension.location(), seed, modifiedSeed);
                return modifiedSeed;
            } else {
                LOGGER.warn("RandomState.create() called with non-exploration dimension context: {} - this may cause issues!", 
                           currentDimension.location());
            }
        }
        
        return seed;
    }
    
    /**
     * Modify the seed variable for the Provider-based create method
     */
    @ModifyVariable(
        method = "create(Lnet/minecraft/core/HolderGetter$Provider;Lnet/minecraft/resources/ResourceKey;J)Lnet/minecraft/world/level/levelgen/RandomState;",
        at = @At("HEAD"),
        argsOnly = true,
        index = 2
    )
    private static long brecher_dim$modifySeedForExplorationProvider(long seed) {
        // First check if we're creating RandomState for an exploration dimension via context
        ResourceKey<?> currentDimension = ExplorationSeedManager.getCurrentDimension();
        
        // Add debug logging to track all RandomState creation
        if (currentDimension != null) {
            LOGGER.debug("RandomState.create(Provider) called with dimension context: {} (seed: {})", 
                        currentDimension.location(), seed);
        } else {
            LOGGER.debug("RandomState.create(Provider) called with no dimension context (seed: {})", seed);
        }
        
        if (currentDimension != null && currentDimension instanceof ResourceKey<?>) {
            @SuppressWarnings("unchecked")
            ResourceKey<Level> levelKey = (ResourceKey<Level>) currentDimension;
            
            // Double-check this is actually an exploration dimension
            if (ExplorationSeedManager.isExplorationDimension(levelKey)) {
                // Get the registered custom seed directly, ignoring the input seed
                java.util.Optional<Long> customSeed = ExplorationSeedManager.getSeedForDimension(levelKey);
                if (customSeed.isPresent()) {
                    long registeredSeed = customSeed.get();
                    LOGGER.info("Using registered seed for exploration dimension {}: {} (ignoring input seed {})", 
                               currentDimension.location(), registeredSeed, seed);
                    return registeredSeed;
                }
                // Fallback to modification if no seed is registered
                long modifiedSeed = ExplorationSeedManager.modifySeed(levelKey, seed);
                LOGGER.info("Modifying seed for exploration dimension {}: {} -> {}", 
                           currentDimension.location(), seed, modifiedSeed);
                return modifiedSeed;
            } else {
                LOGGER.warn("RandomState.create(Provider) called with non-exploration dimension context: {} - this may cause issues!", 
                           currentDimension.location());
            }
        }
        
        return seed;
    }
}