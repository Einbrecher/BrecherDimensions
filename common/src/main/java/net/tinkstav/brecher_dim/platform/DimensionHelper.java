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

package net.tinkstav.brecher_dim.platform;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import java.util.Map;

/**
 * Platform-specific dimension handling.
 */
public interface DimensionHelper {
    /**
     * Creates and registers a new dimension at runtime.
     * Returns the created ServerLevel or null if creation failed.
     */
    ServerLevel createDimension(MinecraftServer server, ResourceKey<Level> dimensionKey, LevelStem levelStem, long seed);
    
    /**
     * Removes a dimension from the server.
     * Returns true if successful.
     */
    boolean removeDimension(MinecraftServer server, ResourceKey<Level> dimensionKey);
    
    /**
     * Gets all currently loaded dimensions.
     */
    Map<ResourceKey<Level>, ServerLevel> getLoadedDimensions(MinecraftServer server);
    
    /**
     * Checks if a dimension exists and is loaded.
     */
    boolean isDimensionLoaded(MinecraftServer server, ResourceKey<Level> dimensionKey);
    
    /**
     * Gets the registry for dimension types.
     */
    Registry<DimensionType> getDimensionTypeRegistry(MinecraftServer server);
    
    /**
     * Gets the registry for level stems.
     */
    Registry<LevelStem> getLevelStemRegistry(MinecraftServer server);
    
    /**
     * Forces a dimension to unload all chunks.
     */
    void unloadDimensionChunks(ServerLevel level);
    
    /**
     * Checks if the platform supports runtime dimension creation.
     */
    boolean supportsRuntimeDimensionCreation();
}