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

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.resources.ResourceLocation;
import net.tinkstav.brecher_dim.BrecherDimensions;
import net.tinkstav.brecher_dim.dimension.BrecherDimensionManager;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages chunk loading and unloading for exploration dimensions
 * Platform-agnostic implementation
 */
public class ChunkManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // Track loaded chunks per dimension
    private static final Map<ResourceLocation, Set<ChunkPos>> loadedChunks = new ConcurrentHashMap<>();
    
    // Track chunk load counts for monitoring
    private static final Map<ResourceLocation, Integer> chunkLoadCounts = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, Integer> chunkUnloadCounts = new ConcurrentHashMap<>();
    
    /**
     * Configure a dimension for exploration mode with aggressive chunk unloading
     */
    public static void configureForExploration(ServerLevel level) {
        // This will be implemented with platform-specific optimizations
        // For now, just log the configuration
        LOGGER.info("Configured {} for exploration mode", level.dimension().location());
    }
    
    /**
     * Force unload all chunks in a dimension
     */
    public static void forceUnloadAllChunks(ServerLevel level) {
        LOGGER.info("Force unloading chunks in {}", level.dimension().location());
        // Platform-specific implementation will handle actual unloading
    }
    
    /**
     * Perform periodic cleanup of chunks
     */
    public static void performCleanup(MinecraftServer server) {
        BrecherDimensionManager manager = BrecherDimensions.getDimensionManager();
        if (manager == null) return;
        
        boolean isHighPressure = MemoryMonitor.isMemoryPressureHigh();
        
        if (isHighPressure) {
            LOGGER.warn("High memory pressure detected, performing aggressive chunk cleanup");
            MemoryMonitor.logMemoryUsage("Before aggressive cleanup");
        }
        
        // Cleanup chunks in exploration dimensions
        for (ServerLevel level : server.getAllLevels()) {
            if (manager.isExplorationDimension(level.dimension().location())) {
                // Only perform cleanup if there are no players in the dimension
                if (level.players().isEmpty()) {
                    // Force unload all chunks when dimension is empty
                    var chunkSource = level.getChunkSource();
                    chunkSource.tick(() -> true, true);
                    LOGGER.debug("Unloaded all chunks in empty dimension: {}", level.dimension().location());
                } else if (isHighPressure) {
                    // Under memory pressure, do a limited cleanup
                    // Don't process all chunks at once - let vanilla handle it normally
                    var chunkSource = level.getChunkSource();
                    chunkSource.tick(() -> false, false);  // Normal tick, don't force process all chunks
                    LOGGER.debug("Performed normal chunk cleanup in {} due to memory pressure", level.dimension().location());
                }
                // Otherwise, skip cleanup - let vanilla handle it during normal operations
            }
        }
        
        if (isHighPressure) {
            MemoryMonitor.logMemoryUsage("After aggressive cleanup");
        }
    }
    
    /**
     * Handle chunk load event
     */
    public static void onChunkLoad(Level level, int chunkX, int chunkZ) {
        ResourceLocation dimId = level.dimension().location();
        ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
        
        // Track loaded chunk
        Set<ChunkPos> chunks = loadedChunks.computeIfAbsent(dimId, k -> ConcurrentHashMap.newKeySet());
        chunks.add(chunkPos);
        
        // Update load count
        chunkLoadCounts.compute(dimId, (k, v) -> v == null ? 1 : v + 1);
        
        LOGGER.trace("Chunk loaded at {}, {} in {} (total loaded: {})", 
            chunkX, chunkZ, dimId, chunks.size());
    }
    
    /**
     * Handle chunk unload event
     */
    public static void onChunkUnload(Level level, int chunkX, int chunkZ) {
        ResourceLocation dimId = level.dimension().location();
        ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
        
        // Remove from loaded chunks
        Set<ChunkPos> chunks = loadedChunks.get(dimId);
        if (chunks != null) {
            chunks.remove(chunkPos);
            
            // Update unload count
            chunkUnloadCounts.compute(dimId, (k, v) -> v == null ? 1 : v + 1);
            
            LOGGER.trace("Chunk unloaded at {}, {} in {} (remaining loaded: {})", 
                chunkX, chunkZ, dimId, chunks.size());
                
            // Clean up empty sets
            if (chunks.isEmpty()) {
                loadedChunks.remove(dimId);
            }
        }
    }
    
    /**
     * Get chunk statistics for a dimension
     */
    public static Map<String, Object> getChunkStatistics(ResourceLocation dimensionId) {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        
        Set<ChunkPos> chunks = loadedChunks.get(dimensionId);
        stats.put("currentlyLoaded", chunks != null ? chunks.size() : 0);
        stats.put("totalLoaded", chunkLoadCounts.getOrDefault(dimensionId, 0));
        stats.put("totalUnloaded", chunkUnloadCounts.getOrDefault(dimensionId, 0));
        
        return stats;
    }
    
    /**
     * Clear chunk tracking data for a dimension
     */
    public static void clearDimensionData(ResourceLocation dimensionId) {
        loadedChunks.remove(dimensionId);
        chunkLoadCounts.remove(dimensionId);
        chunkUnloadCounts.remove(dimensionId);
        LOGGER.debug("Cleared chunk tracking data for dimension: {}", dimensionId);
    }
}