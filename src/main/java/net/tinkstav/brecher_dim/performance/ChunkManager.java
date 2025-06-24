package net.tinkstav.brecher_dim.performance;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.tinkstav.brecher_dim.config.BrecherConfig;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import java.util.*;

public class ChunkManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    /**
     * Configure chunk management for exploration dimension
     */
    public static void configureForExploration(ServerLevel level) {
        ServerChunkCache chunkSource = level.getChunkSource();
        
        // Configure aggressive unloading
        if (BrecherConfig.aggressiveChunkUnloading.get()) {
            // This would require AccessTransformer or reflection to modify
            // For now, we'll use game rules and other available methods
            
            // Reduce spawn chunks
            level.setSpawnSettings(false, false);
            
            // Set shorter unload delay through gamerules if possible
            // (Custom implementation would go here)
        }
        
        LOGGER.info("Configured chunk management for exploration dimension: {}", 
            level.dimension().location());
    }
    
    /**
     * Force unload chunks in a dimension
     */
    public static void forceUnloadChunks(ServerLevel level) {
        ServerChunkCache chunkSource = level.getChunkSource();
        
        // Get loaded chunks count before
        int loadedBefore = chunkSource.getLoadedChunksCount();
        
        // Force chunk unloading by ticking the chunk system
        // This will process chunk unloading naturally
        chunkSource.tick(() -> true, true);
        
        // Save and flush chunks to trigger unloading
        chunkSource.save(true);
        
        // Force a second tick to process any pending unloads
        if (BrecherConfig.aggressiveChunkUnloading.get()) {
            // Trigger multiple ticks to force more aggressive unloading
            for (int i = 0; i < 5; i++) {
                chunkSource.tick(() -> true, false);
            }
        }
        
        // Get loaded chunks count after
        int loadedAfter = chunkSource.getLoadedChunksCount();
        int unloadedCount = Math.max(0, loadedBefore - loadedAfter);
        
        if (unloadedCount > 0) {
            LOGGER.info("Force unloaded {} chunks in {}", unloadedCount, 
                level.dimension().location());
        }
    }
    
    /**
     * Check if players are near a chunk
     */
    private static boolean hasNearbyPlayers(ServerLevel level, ChunkPos chunkPos) {
        int chunkX = chunkPos.x << 4;
        int chunkZ = chunkPos.z << 4;
        
        return level.players().stream().anyMatch(player -> {
            double dx = player.getX() - chunkX;
            double dz = player.getZ() - chunkZ;
            double distanceSq = dx * dx + dz * dz;
            return distanceSq < (128 * 128); // 8 chunk radius
        });
    }
}