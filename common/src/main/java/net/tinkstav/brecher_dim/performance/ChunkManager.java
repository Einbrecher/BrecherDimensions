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
import net.tinkstav.brecher_dim.config.BrecherConfig;
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
     * Clear all cached data - must be called on server shutdown to prevent memory leaks
     * in singleplayer/integrated server scenarios where static maps persist across restarts
     */
    public static void shutdown() {
        LOGGER.info("ChunkManager shutting down - clearing {} dimensions tracked", loadedChunks.size());
        loadedChunks.clear();
        chunkLoadCounts.clear();
        chunkUnloadCounts.clear();
    }

    /**
     * Configure a dimension for exploration mode.
     * Initializes chunk tracking data structures for the dimension.
     * Actual chunk unloading behavior is controlled via config settings
     * (aggressiveChunkUnloading, chunkUnloadDelay, etc.) and enforced
     * during tickChunkCleanup() calls.
     *
     * @param level the server level to configure for exploration
     */
    public static void configureForExploration(ServerLevel level) {
        ResourceLocation dimId = level.dimension().location();

        // Initialize tracking data structures for this dimension
        loadedChunks.computeIfAbsent(dimId, k -> ConcurrentHashMap.newKeySet());
        chunkLoadCounts.putIfAbsent(dimId, 0);
        chunkUnloadCounts.putIfAbsent(dimId, 0);

        LOGGER.debug("Initialized chunk tracking for exploration dimension: {}", dimId);
    }
    
    /**
     * Force unload all chunks in a dimension
     * Only performs forced unloading if aggressive mode is enabled or memory pressure is high
     */
    public static void forceUnloadAllChunks(ServerLevel level) {
        // Check if we should actually force unload
        boolean aggressiveMode = BrecherConfig.isAggressiveChunkUnloading();
        boolean isHighPressure = MemoryMonitor.isMemoryPressureHigh();

        if (aggressiveMode || isHighPressure) {
            LOGGER.info("Force unloading chunks in {} (aggressive: {}, high memory: {})",
                level.dimension().location(), aggressiveMode, isHighPressure);
            // Actual unloading through vanilla chunk system
            var chunkSource = level.getChunkSource();
            chunkSource.tick(() -> true, true);  // Force process all chunks
        } else {
            LOGGER.debug("Skipping force unload in {} - aggressive mode disabled", level.dimension().location());
        }
    }
    
    /**
     * Perform periodic cleanup of chunks
     */
    public static void performCleanup(MinecraftServer server) {
        BrecherDimensionManager manager = BrecherDimensions.getDimensionManager();
        if (manager == null) return;

        // Check if aggressive chunk unloading is enabled
        boolean aggressiveMode = BrecherConfig.isAggressiveChunkUnloading();
        boolean isHighPressure = MemoryMonitor.isMemoryPressureHigh();

        // Only perform aggressive cleanup if the config is enabled OR we're under high memory pressure
        if (!aggressiveMode && !isHighPressure) {
            LOGGER.trace("Skipping chunk cleanup - aggressive mode disabled and memory pressure normal");
            return;
        }

        if (isHighPressure) {
            LOGGER.warn("High memory pressure detected, performing {} chunk cleanup",
                aggressiveMode ? "aggressive" : "emergency");
            MemoryMonitor.logMemoryUsage("Before cleanup");
        }

        // Cleanup chunks in exploration dimensions
        for (ServerLevel level : server.getAllLevels()) {
            if (manager.isExplorationDimension(level.dimension().location())) {
                // Only perform cleanup if there are no players in the dimension
                if (level.players().isEmpty()) {
                    if (aggressiveMode) {
                        // Force unload all chunks when dimension is empty (aggressive mode)
                        var chunkSource = level.getChunkSource();
                        chunkSource.tick(() -> true, true);
                        LOGGER.debug("Aggressively unloaded all chunks in empty dimension: {}", level.dimension().location());
                    } else if (isHighPressure) {
                        // Emergency cleanup under memory pressure even without aggressive mode
                        var chunkSource = level.getChunkSource();
                        chunkSource.tick(() -> false, false);  // Normal tick, don't force process all chunks
                        LOGGER.debug("Emergency chunk cleanup in empty dimension {} due to memory pressure", level.dimension().location());
                    }
                } else if (isHighPressure) {
                    // Under memory pressure with players present, do a limited cleanup
                    var chunkSource = level.getChunkSource();
                    chunkSource.tick(() -> false, false);  // Normal tick, don't force process all chunks
                    LOGGER.debug("Limited chunk cleanup in {} due to memory pressure (players present)", level.dimension().location());
                }
                // Otherwise, skip cleanup - let vanilla handle it during normal operations
            }
        }

        if (isHighPressure) {
            MemoryMonitor.logMemoryUsage("After cleanup");
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