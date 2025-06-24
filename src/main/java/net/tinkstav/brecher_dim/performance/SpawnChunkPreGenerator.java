package net.tinkstav.brecher_dim.performance;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.tinkstav.brecher_dim.config.BrecherConfig;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Handles pre-generation of chunks around spawn points in exploration dimensions
 * to provide a smoother experience for the first player to visit each day
 */
public class SpawnChunkPreGenerator {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // Dedicated thread pool for async chunk generation
    private static final ScheduledExecutorService CHUNK_GEN_EXECUTOR = Executors.newScheduledThreadPool(2, r -> {
        Thread t = new Thread(r, "BrecherDim-ChunkPreGen");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY); // Low priority to not impact server
        return t;
    });
    
    /**
     * Pre-generates chunks around the spawn point of an exploration dimension
     * Uses a hybrid approach: small radius sync, larger radius async
     * 
     * @param explorationLevel The exploration dimension to pre-generate for
     */
    public static void preGenerateSpawnArea(ServerLevel explorationLevel) {
        if (!BrecherConfig.preGenerateSpawnChunks.get()) {
            return;
        }
        
        long startTime = System.currentTimeMillis();
        String dimName = explorationLevel.dimension().location().toString();
        
        // Get spawn position
        BlockPos spawnPos = explorationLevel.getSharedSpawnPos();
        int spawnChunkX = spawnPos.getX() >> 4;
        int spawnChunkZ = spawnPos.getZ() >> 4;
        
        // Phase 1: Generate immediate spawn area synchronously (3x3 chunks)
        int immediateRadius = BrecherConfig.immediateSpawnRadius.get();
        if (immediateRadius > 0) {
            LOGGER.info("Pre-generating immediate spawn area ({} chunk radius) for {}", 
                immediateRadius, dimName);
            
            int chunksGenerated = generateChunksInRadius(
                explorationLevel, spawnChunkX, spawnChunkZ, immediateRadius
            );
            
            long syncTime = System.currentTimeMillis() - startTime;
            LOGGER.info("Generated {} immediate spawn chunks for {} in {}ms", 
                chunksGenerated, dimName, syncTime);
        }
        
        // Phase 2: Schedule async generation of extended area
        int extendedRadius = BrecherConfig.extendedSpawnRadius.get();
        if (extendedRadius > immediateRadius) {
            // Delay to let server finish startup
            CHUNK_GEN_EXECUTOR.schedule(() -> {
                preGenerateExtendedAreaAsync(
                    explorationLevel, spawnChunkX, spawnChunkZ, 
                    immediateRadius, extendedRadius
                );
            }, 10, TimeUnit.SECONDS);
        }
    }
    
    /**
     * Synchronously generates chunks in a square radius around a center point
     */
    private static int generateChunksInRadius(ServerLevel level, int centerX, int centerZ, int radius) {
        int generated = 0;
        
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                try {
                    level.getChunk(centerX + x, centerZ + z, ChunkStatus.FULL, true);
                    generated++;
                } catch (Exception e) {
                    LOGGER.warn("Failed to pre-generate chunk at {}, {} in {}", 
                        centerX + x, centerZ + z, level.dimension().location(), e);
                }
            }
        }
        
        return generated;
    }
    
    /**
     * Asynchronously generates chunks in the extended area (between immediate and extended radius)
     */
    private static void preGenerateExtendedAreaAsync(ServerLevel level, int centerX, int centerZ, 
                                                   int innerRadius, int outerRadius) {
        CompletableFuture.runAsync(() -> {
            long startTime = System.currentTimeMillis();
            String dimName = level.dimension().location().toString();
            int generated = 0;
            
            LOGGER.info("Starting async pre-generation of extended spawn area ({} to {} chunk radius) for {}", 
                innerRadius, outerRadius, dimName);
            
            // Generate in a spiral pattern from inner to outer radius
            for (int radius = innerRadius + 1; radius <= outerRadius; radius++) {
                // Top and bottom edges
                for (int x = -radius; x <= radius; x++) {
                    generated += tryGenerateChunk(level, centerX + x, centerZ - radius);
                    generated += tryGenerateChunk(level, centerX + x, centerZ + radius);
                }
                
                // Left and right edges (excluding corners to avoid duplicates)
                for (int z = -radius + 1; z < radius; z++) {
                    generated += tryGenerateChunk(level, centerX - radius, centerZ + z);
                    generated += tryGenerateChunk(level, centerX + radius, centerZ + z);
                }
                
                // Yield periodically to avoid hogging resources
                if (radius % 2 == 0) {
                    try {
                        Thread.sleep(100); // Small pause every 2 radius levels
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
            
            long asyncTime = System.currentTimeMillis() - startTime;
            LOGGER.info("Async pre-generated {} extended spawn chunks for {} in {}ms", 
                generated, dimName, asyncTime);
            
        }, CHUNK_GEN_EXECUTOR).exceptionally(throwable -> {
            LOGGER.error("Failed to complete async chunk pre-generation", throwable);
            return null;
        });
    }
    
    /**
     * Attempts to generate a single chunk, returns 1 if successful, 0 if failed
     */
    private static int tryGenerateChunk(ServerLevel level, int chunkX, int chunkZ) {
        try {
            level.getChunk(chunkX, chunkZ, ChunkStatus.FULL, true);
            return 1;
        } catch (Exception e) {
            // Don't spam logs for individual chunk failures during async generation
            return 0;
        }
    }
    
    /**
     * Shuts down the chunk generation executor
     */
    public static void shutdown() {
        CHUNK_GEN_EXECUTOR.shutdown();
        try {
            if (!CHUNK_GEN_EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
                CHUNK_GEN_EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            CHUNK_GEN_EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}