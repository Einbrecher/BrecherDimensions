/*
 * Brecher's Dimensions - Temporary resettable dimensions for exploration
 * Copyright (C) 2025 Einbrecher. All rights reserved.
 */

package net.tinkstav.brecher_dim.accessor;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import java.util.Map;

/**
 * Accessor interface for MinecraftServer mixin methods
 * Provides safe access to runtime dimension creation/removal functionality
 */
public interface IServerDimensionAccessor {
    
    /**
     * Create a new runtime dimension
     */
    ServerLevel brecher_dim$createRuntimeDimension(ResourceKey<Level> dimensionKey, 
                                                  DimensionType dimensionType, 
                                                  ChunkGenerator chunkGenerator, 
                                                  long seed);
    
    /**
     * Remove a runtime dimension
     */
    void brecher_dim$removeRuntimeDimension(ResourceKey<Level> dimensionKey);
    
    /**
     * Get map of all runtime levels
     */
    Map<ResourceKey<Level>, ServerLevel> brecher_dim$getRuntimeLevels();
    
    /**
     * Cleanup all runtime dimensions during shutdown
     */
    void brecher_dim$cleanupAllRuntimeDimensions();
}