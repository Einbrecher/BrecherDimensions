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

package net.tinkstav.brecher_dim.generation;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.tinkstav.brecher_dim.config.BrecherConfig;
import net.tinkstav.brecher_dim.performance.MemoryMonitor;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Manages chunk generation for a single exploration dimension.
 * Runs on the main server thread during server ticks.
 *
 * <p>Key features:
 * <ul>
 *   <li>Time-based loop limiting to prevent lag spikes (5ms per tick budget)</li>
 *   <li>Correct chunk detection: loads chunk to EMPTY status first to check if already generated</li>
 *   <li>Ticket leak prevention: uses finally block to ensure tickets are always scheduled for removal</li>
 *   <li>Retry mechanism: failed chunks are retried up to MAX_CHUNK_RETRIES times before being skipped</li>
 * </ul>
 */
public class GenerationTask {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final TicketType<ChunkPos> PREGEN_TICKET = TicketType.create("brecher_pregen", (a, b) -> 0);
    private static final int MAX_CHUNK_RETRIES = 3;

    // TPS thresholds for batch size adjustment
    // - At FULL_SPEED or above: use full chunksPerTick from config
    // - At REDUCED_SPEED or above: reduce by 1 chunk per tick
    // - At MINIMUM_SPEED or above: use 1 chunk per tick
    // - Below MINIMUM_SPEED: pause generation entirely (return 0)
    private static final double TPS_FULL_SPEED = 19.5;
    private static final double TPS_REDUCED_SPEED = 19.0;
    private static final double TPS_MINIMUM_SPEED = 18.0;

    private final ResourceKey<Level> dimensionKey;
    private final GenerationProgress progress;
    private final Map<ChunkPos, Integer> ticketRemovalSchedule = new HashMap<>();
    // Note: failedChunkRetries, skippedChunksCount, and fractionalTickCounter are now stored
    // in GenerationProgress for persistence across server restarts
    private boolean throttled = false;
    
    /**
     * Create a new generation task
     */
    public GenerationTask(ResourceKey<Level> dimensionKey, long seed, int centerX, int centerZ, int radius) {
        this.dimensionKey = dimensionKey;
        this.progress = new GenerationProgress(dimensionKey.location(), seed, centerX, centerZ, radius);
    }
    
    /**
     * Restore a generation task from saved progress
     */
    public GenerationTask(ResourceKey<Level> dimensionKey, GenerationProgress progress) {
        this.dimensionKey = dimensionKey;
        this.progress = progress;
    }
    
    /**
     * Process the next batch of chunks
     * Called from the main server thread
     */
    public void processNextBatch(ServerLevel level) {
        // CRITICAL: Process ticket removals FIRST, even when throttled or paused
        // This prevents ticket leak "death spiral" where tickets accumulate during throttling,
        // keeping chunks loaded and preventing TPS recovery
        processTicketRemovals(level);

        // Check if paused or completed
        if (progress.getStatus() != GenerationProgress.Status.RUNNING) {
            return;
        }

        // Check for completion
        if (progress.isComplete()) {
            progress.setStatus(GenerationProgress.Status.COMPLETED);
            int skipped = progress.getSkippedChunksCount();
            LOGGER.info("Chunk generation completed for dimension {}: {} chunks visited, {} new chunks generated{}",
                dimensionKey.location(), progress.getChunksGenerated(), progress.getChunksActuallyGenerated(),
                skipped > 0 ? " (" + skipped + " skipped due to errors)" : "");
            return;
        }

        // Check performance throttling
        if (shouldThrottle(level)) {
            if (!throttled) {
                LOGGER.debug("Throttling chunk generation for {} due to performance", dimensionKey.location());
                throttled = true;
            }
            return;
        } else if (throttled) {
            LOGGER.debug("Resuming chunk generation for {}", dimensionKey.location());
            throttled = false;
        }
        
        // Calculate batch size based on performance
        int batchSize = calculateBatchSize(level);
        if (batchSize <= 0) {
            return;
        }

        // Generate chunks with time-based limiting
        // Use configurable time budget (converted from ms to ns)
        long maxTickNanos = BrecherConfig.getPregenMaxTickMs() * 1_000_000L;
        SpiralIterator iterator = progress.getSpiralIterator();
        int actuallyGenerated = 0;
        int chunksVisited = 0;
        long tickStartTime = System.nanoTime();

        while (actuallyGenerated < batchSize && iterator.hasNext()) {
            // Time check FIRST - exit if we've exceeded our tick budget
            // This prevents lag spikes when traversing many already-generated chunks
            if (System.nanoTime() - tickStartTime > maxTickNanos) {
                LOGGER.debug("Hit time budget for {}: visited {} chunks, generated {} new in {}ms",
                    dimensionKey.location(), chunksVisited, actuallyGenerated,
                    (System.nanoTime() - tickStartTime) / 1_000_000.0);
                break;
            }

            ChunkPos pos = iterator.next();
            if (pos == null) break;

            chunksVisited++;
            boolean ticketAdded = false;

            try {
                // 1. Add ticket FIRST to keep chunk loaded during processing
                level.getChunkSource().addRegionTicket(PREGEN_TICKET, pos, 0, pos);
                ticketAdded = true;

                // 2. Load chunk to EMPTY status - this forces a disk load if the chunk exists,
                // allowing us to check its actual persisted status rather than just whether
                // it's currently in memory. This fixes the bug where unloaded-but-generated
                // chunks were incorrectly counted as "new".
                ChunkAccess chunk = level.getChunk(pos.x, pos.z, ChunkStatus.EMPTY, true);

                // 3. Check if chunk was already fully generated
                boolean wasAlreadyGenerated = chunk.getPersistedStatus().isOrAfter(ChunkStatus.FULL);

                // 4. Generate to FULL status only if needed
                if (!wasAlreadyGenerated) {
                    level.getChunk(pos.x, pos.z, ChunkStatus.FULL, true);
                    progress.incrementChunksActuallyGenerated();
                    actuallyGenerated++;
                }

                // 5. Always count as visited (scanned) to track position in spiral
                progress.incrementChunksGenerated();

                // Clear retry counter on success (uses progress for persistence)
                progress.clearRetry(pos);

            } catch (Exception e) {
                // Handle retry: increment failure count using progress (persisted across restarts)
                progress.recordRetry(pos);
                int retries = progress.getRetryCount(pos);

                if (retries >= MAX_CHUNK_RETRIES) {
                    // Skip this chunk permanently after max retries
                    LOGGER.error("Permanently skipping chunk {} in {} after {} failed attempts: {}",
                        pos, dimensionKey.location(), MAX_CHUNK_RETRIES, e.getMessage());
                    progress.incrementChunksGenerated(); // Count as visited (skipped)
                    progress.clearRetry(pos);
                    progress.incrementSkippedChunks();
                    progress.failWithError("Skipped chunk " + pos + " after " + MAX_CHUNK_RETRIES + " failures");
                } else {
                    // Will retry on next tick - do NOT advance iterator/progress
                    LOGGER.warn("Chunk {} generation failed in {} (attempt {}/{}), will retry: {}",
                        pos, dimensionKey.location(), retries, MAX_CHUNK_RETRIES, e.getMessage());
                }
            } finally {
                // CRITICAL: Always schedule ticket removal to prevent memory leak
                // This must be in finally block to ensure cleanup even on exceptions
                if (ticketAdded) {
                    int removalTick = level.getServer().getTickCount() + BrecherConfig.getPregenTicketDuration();
                    ticketRemovalSchedule.put(pos, removalTick);
                }
            }
        }

        // Log progress periodically (interval is configurable via pregenLogInterval)
        int logInterval = BrecherConfig.getPregenLogInterval();
        if (logInterval > 0 && progress.getChunksGenerated() % logInterval == 0 && progress.getChunksGenerated() > 0) {
            LOGGER.info("Generation progress for {}: {}% (scanned {}/{}, generated {} new, {} chunks/min)",
                dimensionKey.location(),
                progress.getProgressPercent(),
                progress.getChunksGenerated(),
                iterator.getTotalChunks(),
                progress.getChunksActuallyGenerated(),
                String.format("%.1f", progress.getGenerationRate()));

            // Debug log ticket count to monitor chunk unloading health
            // A growing count indicates tickets aren't being cleaned up properly
            LOGGER.debug("Active pregen tickets for {}: {} (should stay bounded)",
                dimensionKey.location(), ticketRemovalSchedule.size());
        }
    }
    
    /**
     * Process scheduled ticket removals
     */
    private void processTicketRemovals(ServerLevel level) {
        if (ticketRemovalSchedule.isEmpty()) {
            return;
        }
        
        int currentTick = level.getServer().getTickCount();
        Iterator<Map.Entry<ChunkPos, Integer>> it = ticketRemovalSchedule.entrySet().iterator();
        
        while (it.hasNext()) {
            Map.Entry<ChunkPos, Integer> entry = it.next();
            if (currentTick >= entry.getValue()) {
                ChunkPos pos = entry.getKey();
                level.getChunkSource().removeRegionTicket(PREGEN_TICKET, pos, 0, pos);
                it.remove();
            }
        }
    }
    
    /**
     * Check if generation should be throttled based on server performance.
     * Note: Per-tick spike detection is not available due to API limitations.
     * The average TPS check provides gradual throttling instead.
     */
    private boolean shouldThrottle(ServerLevel level) {
        // Check average TPS
        double mspt = level.getServer().getAverageTickTimeNanos() / 1_000_000.0;
        double tps = Math.min(20.0, 1000.0 / mspt);
        if (tps < BrecherConfig.getPregenMinTPS()) {
            return true;
        }

        // Check memory
        int memoryPercent = MemoryMonitor.getMemoryUsagePercent();
        if (memoryPercent > BrecherConfig.getPregenMemoryThreshold()) {
            return true;
        }

        // Check if players are in the dimension
        if (!level.players().isEmpty() && BrecherConfig.isPregenPauseWithPlayers()) {
            return true;
        }

        return false;
    }
    
    /**
     * Calculate batch size based on current performance
     */
    private int calculateBatchSize(ServerLevel level) {
        int chunksPerTick = BrecherConfig.getPregenChunksPerTick();

        // Handle fractional generation rates (sub-1 chunk/tick)
        if (chunksPerTick == 0) {
            // Use ticksPerChunk for fractional rates
            // Counter is stored in progress for persistence across restarts
            int ticksPerChunk = BrecherConfig.getPregenTicksPerChunk();
            int counter = progress.incrementFractionalTickCounter();

            // Only generate a chunk when counter reaches the threshold
            if (counter >= ticksPerChunk) {
                progress.resetFractionalTickCounter();
                return 1; // Generate exactly 1 chunk
            }
            return 0; // Skip this tick
        }

        // Standard mode: multiple chunks per tick
        // Adjust based on TPS using defined thresholds
        double mspt = level.getServer().getAverageTickTimeNanos() / 1_000_000.0;
        double tps = Math.min(20.0, 1000.0 / mspt);

        if (tps >= TPS_FULL_SPEED) {
            return chunksPerTick;
        } else if (tps >= TPS_REDUCED_SPEED) {
            return Math.max(1, chunksPerTick - 1);
        } else if (tps >= TPS_MINIMUM_SPEED) {
            return 1;
        } else {
            return 0; // Pause generation - TPS too low
        }
    }
    
    /**
     * Pause the generation task
     */
    public void pause() {
        if (progress.getStatus() == GenerationProgress.Status.RUNNING) {
            progress.setStatus(GenerationProgress.Status.PAUSED);
            LOGGER.info("Paused chunk generation for {}", dimensionKey.location());
        }
    }
    
    /**
     * Resume the generation task
     */
    public void resume() {
        if (progress.getStatus() == GenerationProgress.Status.PAUSED) {
            progress.setStatus(GenerationProgress.Status.RUNNING);
            LOGGER.info("Resumed chunk generation for {}", dimensionKey.location());
        }
    }
    
    /**
     * Stop the generation task and clean up
     */
    public void stop(ServerLevel level) {
        // Remove all remaining tickets
        if (level != null) {
            for (ChunkPos pos : ticketRemovalSchedule.keySet()) {
                level.getChunkSource().removeRegionTicket(PREGEN_TICKET, pos, 0, pos);
            }
        }
        ticketRemovalSchedule.clear();
        
        progress.setStatus(GenerationProgress.Status.COMPLETED);
        LOGGER.info("Stopped chunk generation for {} at {} chunks visited ({} new generated)", 
            dimensionKey.location(), progress.getChunksGenerated(), progress.getChunksActuallyGenerated());
    }
    
    // Getters
    public ResourceKey<Level> getDimensionKey() { return dimensionKey; }
    public GenerationProgress getProgress() { return progress; }
    public boolean isComplete() { return progress.isComplete(); }
    public boolean isPaused() { return progress.getStatus() == GenerationProgress.Status.PAUSED; }
    public boolean isRunning() { return progress.getStatus() == GenerationProgress.Status.RUNNING; }

    /**
     * Check if generation is currently throttled due to performance issues.
     * Throttling occurs when TPS is too low, memory usage is too high,
     * or players are present in the dimension (if configured).
     */
    public boolean isThrottled() { return throttled; }

    /**
     * Get the current number of pending ticket removals.
     * Used for monitoring chunk unloading health during generation.
     * A growing number indicates tickets aren't being cleaned up properly.
     */
    public int getPendingTicketCount() { return ticketRemovalSchedule.size(); }
}