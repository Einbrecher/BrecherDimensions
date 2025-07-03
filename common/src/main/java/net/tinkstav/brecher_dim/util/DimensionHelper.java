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

package net.tinkstav.brecher_dim.util;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.core.registries.Registries;
import net.tinkstav.brecher_dim.BrecherDimensions;
import net.tinkstav.brecher_dim.dimension.ExplorationSeedManager;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper methods for working with exploration dimensions
 */
public class DimensionHelper {
    // Pattern to match exploration dimension names: exploration_{base}_[id]
    private static final Pattern EXPLORATION_PATTERN = Pattern.compile("exploration_(.+)_(\\d+)");
    
    /**
     * Get the parent dimension key from an exploration dimension key.
     * For example, exploration_overworld_0 -> minecraft:overworld
     * 
     * @param explorationKey The exploration dimension key
     * @return The parent dimension key, or empty if not an exploration dimension
     */
    public static Optional<ResourceKey<Level>> getParentDimension(ResourceKey<Level> explorationKey) {
        if (!ExplorationSeedManager.isExplorationDimension(explorationKey)) {
            return Optional.empty();
        }
        
        String path = explorationKey.location().getPath();
        Matcher matcher = EXPLORATION_PATTERN.matcher(path);
        
        if (matcher.matches()) {
            String baseDimension = matcher.group(1);
            
            // Map base dimension names to their vanilla resource keys
            ResourceLocation parentLocation = switch(baseDimension) {
                case "overworld" -> ResourceLocation.parse("minecraft:overworld");
                case "the_nether", "nether" -> ResourceLocation.parse("minecraft:the_nether");
                case "the_end", "end" -> ResourceLocation.parse("minecraft:the_end");
                default -> {
                    // For modded dimensions, assume the namespace is the same as the base dimension name
                    // This may need adjustment based on how modded dimensions are named
                    if (baseDimension.contains(":")) {
                        yield ResourceLocation.parse(baseDimension);
                    } else {
                        // If no namespace, assume it's a minecraft dimension
                        yield ResourceLocation.parse("minecraft:" + baseDimension);
                    }
                }
            };
            
            return Optional.of(ResourceKey.create(Registries.DIMENSION, parentLocation));
        }
        
        return Optional.empty();
    }
    
    /**
     * Get the seed for a dimension, considering parent dimension for structure location.
     * 
     * @param dimensionKey The dimension key
     * @param isLocating Whether we're locating structures (vs generating)
     * @return The appropriate seed to use
     */
    public static Optional<Long> getDimensionSeed(ResourceKey<Level> dimensionKey, boolean isLocating) {
        if (!ExplorationSeedManager.isExplorationDimension(dimensionKey)) {
            // Not an exploration dimension, use normal seed
            return Optional.empty();
        }
        
        if (isLocating) {
            // When locating structures, use the parent dimension's seed
            // This ensures structures appear in the same locations as the parent
            Optional<ResourceKey<Level>> parentKey = getParentDimension(dimensionKey);
            if (parentKey.isPresent()) {
                // For vanilla dimensions, we would ideally get the actual world seed
                // Since we can't easily access that here, we'll return empty to use default behavior
                // The structure location will use the world's base seed
                return Optional.empty();
            }
        }
        
        // When generating structures, use the exploration dimension's seed
        return ExplorationSeedManager.getSeedForDimension(dimensionKey);
    }
}