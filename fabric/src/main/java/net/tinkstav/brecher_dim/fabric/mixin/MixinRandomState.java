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
        if (currentDimension != null && currentDimension instanceof ResourceKey<?>) {
            @SuppressWarnings("unchecked")
            ResourceKey<Level> levelKey = (ResourceKey<Level>) currentDimension;
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
            }
        }
        
        // Fallback: Check if this seed matches any registered exploration dimension seed
        // This handles cases where the context might have been cleared too early
        for (var entry : ExplorationSeedManager.getAllDimensionSeeds().entrySet()) {
            if (entry.getValue() == seed && ExplorationSeedManager.isExplorationDimension(entry.getKey())) {
                LOGGER.info("Seed {} matches registered exploration dimension {}, using it directly", 
                           seed, entry.getKey().location());
                return seed; // Already the correct seed
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
        if (currentDimension != null && currentDimension instanceof ResourceKey<?>) {
            @SuppressWarnings("unchecked")
            ResourceKey<Level> levelKey = (ResourceKey<Level>) currentDimension;
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
            }
        }
        
        // Fallback: Check if this seed matches any registered exploration dimension seed
        // This handles cases where the context might have been cleared too early
        for (var entry : ExplorationSeedManager.getAllDimensionSeeds().entrySet()) {
            if (entry.getValue() == seed && ExplorationSeedManager.isExplorationDimension(entry.getKey())) {
                LOGGER.info("Seed {} matches registered exploration dimension {}, using it directly", 
                           seed, entry.getKey().location());
                return seed; // Already the correct seed
            }
        }
        
        return seed;
    }
}