/*
 * Brecher's Dimensions - Temporary resettable dimensions for exploration
 * Copyright (C) 2025 Einbrecher. All rights reserved.
 */

package net.tinkstav.brecher_dim.generation;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks the progress of chunk generation for a dimension.
 * Supports serialization for persistence across server restarts.
 *
 * <p>Thread Safety: Progress counters use {@link AtomicLong} and status uses volatile
 * to ensure safe concurrent access between the server tick thread (which increments
 * counters) and command threads (which may read status).
 */
public class GenerationProgress {
    private static final Logger LOGGER = LogUtils.getLogger();

    public enum Status {
        RUNNING,
        PAUSED,
        COMPLETED,
        ERROR
    }

    private final ResourceLocation dimensionId;
    private final long dimensionSeed;
    private volatile Status status;  // volatile for safe cross-thread reads
    private SpiralIterator spiralIterator;

    // Thread-safe counters - may be read from command threads while updated on tick thread
    private final AtomicLong chunksGenerated = new AtomicLong(0);  // Chunks visited
    private final AtomicLong chunksActuallyGenerated = new AtomicLong(0);  // New chunks generated

    // Retry tracking - persisted across restarts
    // Thread-Safety: Uses ConcurrentHashMap because chunk generation may involve worker threads.
    // All compound operations (like incrementing retry count) use atomic methods (merge/compute).
    private final Map<ChunkPos, Integer> failedChunkRetries = new ConcurrentHashMap<>();
    private int skippedChunksCount = 0;

    // Fractional tick counter for sub-1 chunk/tick generation rates
    // Persisted to maintain consistent generation rate across server restarts
    // Added in v1.2.0 - backwards compatible (defaults to 0 for old saves)
    private int fractionalTickCounter = 0;

    private long startTime;
    private volatile long lastActivity;  // volatile since it's read for staleness checks
    private volatile String lastError;   // volatile since it's read from commands
    
    /**
     * Create new generation progress
     */
    public GenerationProgress(ResourceLocation dimensionId, long dimensionSeed,
                             int centerX, int centerZ, int radius) {
        this.dimensionId = dimensionId;
        this.dimensionSeed = dimensionSeed;
        this.status = Status.RUNNING;
        this.spiralIterator = new SpiralIterator(centerX, centerZ, radius);
        // AtomicLong fields are already initialized to 0
        this.startTime = System.currentTimeMillis();
        this.lastActivity = startTime;
        this.lastError = null;
    }
    
    /**
     * Load generation progress from NBT
     */
    public static GenerationProgress fromNbt(CompoundTag tag) {
        ResourceLocation dimId = ResourceLocation.parse(tag.getString("dimension"));
        long seed = tag.getLong("seed");
        
        // Restore spiral iterator state
        int centerX = tag.getInt("centerX");
        int centerZ = tag.getInt("centerZ");
        int maxRadius = tag.getInt("maxRadius");
        int currentX = tag.getInt("currentX");
        int currentZ = tag.getInt("currentZ");
        int dx = tag.getInt("dx");
        int dz = tag.getInt("dz");
        int stepsDir = tag.getInt("stepsInDirection");
        int stepsSide = tag.getInt("stepsInSide");
        int sidesCompleted = tag.getInt("sidesCompleted");
        
        SpiralIterator iterator = new SpiralIterator(
            centerX, centerZ, maxRadius,
            currentX, currentZ, dx, dz,
            stepsDir, stepsSide, sidesCompleted
        );
        
        GenerationProgress progress = new GenerationProgress(dimId, seed, centerX, centerZ, maxRadius);
        progress.spiralIterator = iterator;
        progress.status = Status.valueOf(tag.getString("status"));

        // Use AtomicLong.set() for thread-safe initialization
        progress.chunksGenerated.set(tag.getLong("chunksGenerated"));

        // Load chunksActuallyGenerated if it exists (backwards compatibility)
        if (tag.contains("chunksActuallyGenerated")) {
            progress.chunksActuallyGenerated.set(tag.getLong("chunksActuallyGenerated"));
        } else {
            // For old saves, assume all visited chunks were generated
            progress.chunksActuallyGenerated.set(progress.chunksGenerated.get());
        }

        progress.startTime = tag.getLong("startTime");
        progress.lastActivity = tag.getLong("lastActivity");

        if (tag.contains("lastError")) {
            progress.lastError = tag.getString("lastError");
        }

        // Load retry tracking data (Phase 4: persist across restarts)
        // Defaults to 0 if missing for backwards compatibility
        if (tag.contains("skippedChunksCount")) {
            progress.skippedChunksCount = tag.getInt("skippedChunksCount");
        }

        // Load failed retries map with robust error handling
        if (tag.contains("failedChunkRetries")) {
            CompoundTag retriesTag = tag.getCompound("failedChunkRetries");
            for (String key : retriesTag.getAllKeys()) {
                try {
                    String[] parts = key.split(",");
                    if (parts.length == 2) {
                        int x = Integer.parseInt(parts[0].trim());
                        int z = Integer.parseInt(parts[1].trim());
                        int count = retriesTag.getInt(key);
                        if (count > 0) {
                            progress.failedChunkRetries.put(new ChunkPos(x, z), count);
                        }
                    }
                } catch (NumberFormatException e) {
                    LOGGER.warn("Invalid chunk retry key in saved data: {}", key);
                    // Continue loading other entries - don't fail entire load
                }
            }
        }

        // Load fractional tick counter (added in v1.2.0)
        // Backwards compatibility: defaults to 0 for old saves (may cause one extra tick delay, not critical)
        if (tag.contains("fractionalTickCounter")) {
            progress.fractionalTickCounter = tag.getInt("fractionalTickCounter");
        }

        return progress;
    }
    
    /**
     * Save generation progress to NBT
     */
    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();

        tag.putString("dimension", dimensionId.toString());
        tag.putLong("seed", dimensionSeed);
        tag.putString("status", status.name());

        // Use AtomicLong.get() for thread-safe reads
        tag.putLong("chunksGenerated", chunksGenerated.get());
        tag.putLong("chunksActuallyGenerated", chunksActuallyGenerated.get());

        tag.putLong("startTime", startTime);
        tag.putLong("lastActivity", lastActivity);
        
        // Save spiral iterator state
        tag.putInt("centerX", spiralIterator.getCenterX());
        tag.putInt("centerZ", spiralIterator.getCenterZ());
        tag.putInt("maxRadius", spiralIterator.getMaxRadius());
        tag.putInt("currentX", spiralIterator.getCurrentX());
        tag.putInt("currentZ", spiralIterator.getCurrentZ());
        tag.putInt("dx", spiralIterator.getDx());
        tag.putInt("dz", spiralIterator.getDz());
        tag.putInt("stepsInDirection", spiralIterator.getStepsInCurrentDirection());
        tag.putInt("stepsInSide", spiralIterator.getStepsInCurrentSide());
        tag.putInt("sidesCompleted", spiralIterator.getSidesCompleted());
        
        if (lastError != null) {
            tag.putString("lastError", lastError);
        }

        // Save retry tracking data (persist across restarts)
        tag.putInt("skippedChunksCount", skippedChunksCount);

        // Save failed chunk retries map as "x,z" -> count
        CompoundTag retriesTag = new CompoundTag();
        failedChunkRetries.forEach((pos, count) ->
            retriesTag.putInt(pos.x + "," + pos.z, count)
        );
        tag.put("failedChunkRetries", retriesTag);

        // Save fractional tick counter (added in v1.2.0 for consistent sub-1 chunk/tick rates)
        tag.putInt("fractionalTickCounter", fractionalTickCounter);

        return tag;
    }

    /**
     * Update activity timestamp
     */
    public void updateActivity() {
        this.lastActivity = System.currentTimeMillis();
    }

    /**
     * Increment chunks generated counter (visited chunks).
     * Thread-safe via AtomicLong.
     */
    public void incrementChunksGenerated() {
        chunksGenerated.incrementAndGet();
        updateActivity();
    }

    /**
     * Increment chunks actually generated counter (new chunks only).
     * Thread-safe via AtomicLong.
     */
    public void incrementChunksActuallyGenerated() {
        chunksActuallyGenerated.incrementAndGet();
    }

    // ========== Retry Tracking Methods ==========
    // Thread-Safety: All methods are safe for concurrent access via ConcurrentHashMap.
    // - getRetryCount: Simple read, thread-safe
    // - recordRetry: Uses atomic merge() to avoid read-modify-write race conditions
    // - clearRetry: Simple remove, thread-safe

    /**
     * Get the retry count for a specific chunk position.
     * Thread-safe via ConcurrentHashMap.
     */
    public int getRetryCount(ChunkPos pos) {
        return failedChunkRetries.getOrDefault(pos, 0);
    }

    /**
     * Record a retry attempt for a chunk position.
     * Thread-safe via atomic merge() operation - avoids read-modify-write race conditions.
     */
    public void recordRetry(ChunkPos pos) {
        failedChunkRetries.merge(pos, 1, Integer::sum);
    }

    /**
     * Clear the retry count for a chunk position (called on success).
     * Thread-safe via ConcurrentHashMap.remove().
     */
    public void clearRetry(ChunkPos pos) {
        failedChunkRetries.remove(pos);
    }

    /**
     * Get the number of permanently skipped chunks.
     */
    public int getSkippedChunksCount() {
        return skippedChunksCount;
    }

    /**
     * Increment the skipped chunks counter.
     */
    public void incrementSkippedChunks() {
        skippedChunksCount++;
    }

    /**
     * Get a copy of the failed chunk retries map (for persistence).
     */
    public Map<ChunkPos, Integer> getFailedChunkRetries() {
        return new HashMap<>(failedChunkRetries);
    }

    // ========== Fractional Tick Counter Methods ==========
    // Used for sub-1 chunk/tick generation rates (e.g., 1 chunk every 2 ticks)
    // Persisted across restarts to maintain consistent generation timing

    /**
     * Get the fractional tick counter value.
     * Used for sub-1 chunk/tick generation rates.
     */
    public int getFractionalTickCounter() {
        return fractionalTickCounter;
    }

    /**
     * Set the fractional tick counter value.
     * Used for sub-1 chunk/tick generation rates.
     */
    public void setFractionalTickCounter(int value) {
        this.fractionalTickCounter = value;
    }

    /**
     * Increment the fractional tick counter and return the new value.
     * Used for sub-1 chunk/tick generation rates.
     */
    public int incrementFractionalTickCounter() {
        return ++fractionalTickCounter;
    }

    /**
     * Reset the fractional tick counter to 0.
     */
    public void resetFractionalTickCounter() {
        this.fractionalTickCounter = 0;
    }

    /**
     * Calculate generation rate (new chunks per minute).
     * Thread-safe read via AtomicLong.get().
     */
    public double getGenerationRate() {
        long elapsedMs = System.currentTimeMillis() - startTime;
        if (elapsedMs <= 0) return 0;
        double elapsedMinutes = elapsedMs / 60000.0;
        // Use actually generated chunks for rate calculation
        return chunksActuallyGenerated.get() / elapsedMinutes;
    }

    /**
     * Calculate visit rate (all chunks visited per minute, including already generated ones).
     * This is more accurate for ETA since it reflects actual traversal speed.
     * Thread-safe read via AtomicLong.get().
     */
    public double getVisitRate() {
        long elapsedMs = System.currentTimeMillis() - startTime;
        if (elapsedMs <= 0) return 0;
        double elapsedMinutes = elapsedMs / 60000.0;
        return chunksGenerated.get() / elapsedMinutes;
    }

    /**
     * Estimate time remaining in minutes.
     * Uses visit rate (not generation rate) for accurate estimates,
     * since remaining chunks may include already-generated chunks that only need to be checked.
     * Thread-safe read via AtomicLong.get().
     *
     * @return estimated minutes remaining, or -1 if unable to calculate
     */
    public long getEstimatedMinutesRemaining() {
        // Use visit rate, not generation rate, for accurate ETA
        // The generation rate only counts new chunks, but we traverse all chunks including existing ones
        double visitRate = getVisitRate();
        if (visitRate <= 0) return -1;

        // Use long arithmetic to prevent overflow with large radii
        long totalChunks = spiralIterator.getTotalChunks();
        long remaining = totalChunks - chunksGenerated.get();
        if (remaining <= 0) return 0;
        return (long) (remaining / visitRate);
    }

    /**
     * Get progress percentage (0-100).
     * Uses long arithmetic internally to prevent overflow with large radii.
     * Thread-safe read via AtomicLong.get().
     *
     * @return progress percentage
     */
    public int getProgressPercent() {
        return spiralIterator.getProgressPercent(chunksGenerated.get());
    }
    
    /**
     * Check if generation is complete
     */
    public boolean isComplete() {
        return status == Status.COMPLETED || !spiralIterator.hasNext();
    }
    
    /**
     * Check if task is stale (no activity for specified hours)
     */
    public boolean isStale(int hours) {
        long staleThreshold = hours * 60L * 60L * 1000L;
        return (System.currentTimeMillis() - lastActivity) > staleThreshold;
    }
    
    // Getters and setters
    public ResourceLocation getDimensionId() { return dimensionId; }
    public long getDimensionSeed() { return dimensionSeed; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public SpiralIterator getSpiralIterator() { return spiralIterator; }

    /** Get chunks visited (thread-safe). */
    public long getChunksGenerated() { return chunksGenerated.get(); }

    /** Get new chunks generated (thread-safe). */
    public long getChunksActuallyGenerated() { return chunksActuallyGenerated.get(); }

    public long getStartTime() { return startTime; }
    public long getLastActivity() { return lastActivity; }
    public String getLastError() { return lastError; }

    /**
     * Record an error and set the task status to ERROR.
     * This method both logs the error message and stops the task.
     *
     * @param error the error message to record
     */
    public void failWithError(String error) {
        this.lastError = error;
        this.status = Status.ERROR;
    }
}