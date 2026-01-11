/*
 * Brecher's Dimensions - Temporary resettable dimensions for exploration
 * Copyright (C) 2025 Einbrecher. All rights reserved.
 */

package net.tinkstav.brecher_dim.generation;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;
import net.tinkstav.brecher_dim.BrecherDimensions;
import net.tinkstav.brecher_dim.config.BrecherConfig;
import net.tinkstav.brecher_dim.data.BrecherSavedData;
import net.tinkstav.brecher_dim.dimension.BrecherDimensionManager;
import net.tinkstav.brecher_dim.dimension.DimensionRegistrar;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main orchestrator for chunk pre-generation across exploration dimensions.
 * Manages all generation tasks and coordinates with the server tick system.
 */
public class ChunkPreGenerator {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<ResourceKey<Level>, GenerationTask> activeTasks = new ConcurrentHashMap<>();
    private static int tickCounter = 0;
    
    /**
     * Called every server tick to process chunk generation
     */
    public static void tick(MinecraftServer server) {
        if (!BrecherConfig.isPregenEnabled()) {
            return;
        }
        
        // Only process on configured interval
        tickCounter++;
        if (tickCounter < BrecherConfig.getPregenTickInterval()) {
            return;
        }
        tickCounter = 0;
        
        // Process each active task
        Iterator<Map.Entry<ResourceKey<Level>, GenerationTask>> it = activeTasks.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<ResourceKey<Level>, GenerationTask> entry = it.next();
            ResourceKey<Level> dimKey = entry.getKey();
            GenerationTask task = entry.getValue();
            
            // Get the server level
            ServerLevel level = server.getLevel(dimKey);
            if (level == null) {
                LOGGER.warn("Dimension {} no longer exists, removing generation task", dimKey.location());
                it.remove();
                continue;
            }
            
            // Process the task
            task.processNextBatch(level);
            
            // Remove completed tasks
            if (task.isComplete()) {
                it.remove();
                saveProgress(server);
            }
        }
    }
    
    /**
     * Start generation for a dimension
     */
    public static Component startGeneration(MinecraftServer server, ResourceKey<Level> dimensionKey, int radius) {
        // Check if dimension exists
        ServerLevel level = server.getLevel(dimensionKey);
        if (level == null) {
            return Component.literal("Dimension not found: " + dimensionKey.location())
                .withStyle(style -> style.withColor(0xFF5555));
        }
        
        // Check if it's an exploration dimension
        BrecherDimensionManager manager = BrecherDimensions.getDimensionManager();
        if (manager == null || !manager.isExplorationDimension(dimensionKey.location())) {
            return Component.literal("Not an exploration dimension: " + dimensionKey.location())
                .withStyle(style -> style.withColor(0xFF5555));
        }
        
        // Check for existing task
        if (activeTasks.containsKey(dimensionKey)) {
            return Component.literal("Generation already in progress for " + dimensionKey.location())
                .withStyle(style -> style.withColor(0xFFAA00));
        }
        
        // Get dimension seed
        DimensionRegistrar registrar = DimensionRegistrar.getInstance();
        Optional<Long> seedOpt = registrar.getDimensionSeed(dimensionKey);
        if (seedOpt.isEmpty()) {
            return Component.literal("Could not determine seed for " + dimensionKey.location())
                .withStyle(style -> style.withColor(0xFF5555));
        }
        long seed = seedOpt.get();
        
        // Determine radius
        if (radius <= 0) {
            WorldBorder border = level.getWorldBorder();
            double borderSize = border.getSize();
            radius = (int)(borderSize / 32); // Convert blocks to chunks, divide by 2 for radius
            
            if (radius <= 0 || radius > 10000) {
                radius = BrecherConfig.getPregenDefaultRadius();
            }
        }
        
        // Get spawn position
        BlockPos spawn = level.getSharedSpawnPos();
        int centerX = spawn.getX() >> 4; // Convert to chunk coordinates
        int centerZ = spawn.getZ() >> 4;
        
        // Create and start task
        GenerationTask task = new GenerationTask(dimensionKey, seed, centerX, centerZ, radius);
        activeTasks.put(dimensionKey, task);
        saveProgress(server);

        // Use long arithmetic to prevent overflow for large radii
        long side = 2L * radius + 1;
        long totalChunks = side * side;
        return Component.literal("Started chunk generation for " + dimensionKey.location())
            .append("\n  Center: [" + centerX + ", " + centerZ + "]")
            .append("\n  Radius: " + String.format("%,d", radius) + " chunks")
            .append("\n  Total chunks: " + String.format("%,d", totalChunks))
            .append("\n  Seed: " + seed)
            .withStyle(style -> style.withColor(0x55FF55));
    }
    
    /**
     * Stop generation for a dimension
     */
    public static Component stopGeneration(MinecraftServer server, ResourceKey<Level> dimensionKey) {
        GenerationTask task = activeTasks.remove(dimensionKey);
        if (task == null) {
            return Component.literal("No active generation for " + dimensionKey.location())
                .withStyle(style -> style.withColor(0xFFAA00));
        }
        
        ServerLevel level = server.getLevel(dimensionKey);
        task.stop(level);
        saveProgress(server);
        
        GenerationProgress progress = task.getProgress();
        return Component.literal("Stopped generation for " + dimensionKey.location() + 
            " at " + progress.getChunksGenerated() + " chunks visited (" + 
            progress.getChunksActuallyGenerated() + " new generated)")
            .withStyle(style -> style.withColor(0x55FF55));
    }
    
    /**
     * Pause generation for a dimension
     */
    public static Component pauseGeneration(ResourceKey<Level> dimensionKey) {
        GenerationTask task = activeTasks.get(dimensionKey);
        if (task == null) {
            return Component.literal("No active generation for " + dimensionKey.location())
                .withStyle(style -> style.withColor(0xFFAA00));
        }
        
        task.pause();
        return Component.literal("Paused generation for " + dimensionKey.location())
            .withStyle(style -> style.withColor(0x55FF55));
    }
    
    /**
     * Resume generation for a dimension
     */
    public static Component resumeGeneration(ResourceKey<Level> dimensionKey) {
        GenerationTask task = activeTasks.get(dimensionKey);
        if (task == null) {
            return Component.literal("No active generation for " + dimensionKey.location())
                .withStyle(style -> style.withColor(0xFFAA00));
        }
        
        task.resume();
        return Component.literal("Resumed generation for " + dimensionKey.location())
            .withStyle(style -> style.withColor(0x55FF55));
    }
    
    /**
     * Stop all generation tasks
     */
    public static Component stopAll(MinecraftServer server) {
        int count = activeTasks.size();
        for (Map.Entry<ResourceKey<Level>, GenerationTask> entry : activeTasks.entrySet()) {
            ServerLevel level = server.getLevel(entry.getKey());
            entry.getValue().stop(level);
        }
        activeTasks.clear();
        saveProgress(server);
        
        return Component.literal("Stopped " + count + " generation tasks")
            .withStyle(style -> style.withColor(0x55FF55));
    }
    
    /**
     * Get status of a specific dimension or all dimensions
     */
    public static Component getStatus(ResourceKey<Level> dimensionKey) {
        if (dimensionKey != null) {
            GenerationTask task = activeTasks.get(dimensionKey);
            if (task == null) {
                return Component.literal("No active generation for " + dimensionKey.location())
                    .withStyle(style -> style.withColor(0xFFAA00));
            }
            return formatTaskStatus(dimensionKey, task);
        } else {
            // Show all tasks
            if (activeTasks.isEmpty()) {
                return Component.literal("No active generation tasks")
                    .withStyle(style -> style.withColor(0xAAAAAA));
            }
            
            Component result = Component.literal("Active generation tasks:")
                .withStyle(style -> style.withColor(0x55FFFF));
            
            for (Map.Entry<ResourceKey<Level>, GenerationTask> entry : activeTasks.entrySet()) {
                result = result.copy().append("\n").append(formatTaskStatus(entry.getKey(), entry.getValue()));
            }
            
            return result;
        }
    }
    
    /**
     * Format task status for display.
     * Uses thousands separators for large numbers to improve readability.
     */
    private static Component formatTaskStatus(ResourceKey<Level> dimKey, GenerationTask task) {
        GenerationProgress progress = task.getProgress();

        // Null check for safety (shouldn't happen but defensive)
        if (progress == null) {
            return Component.literal(dimKey.location() + ": No progress data")
                .withStyle(style -> style.withColor(0xFF5555));
        }

        // Null check for spiral iterator (corrupted progress data)
        SpiralIterator iterator = progress.getSpiralIterator();
        if (iterator == null) {
            return Component.literal(dimKey.location() + ": Corrupted progress data (no iterator)")
                .withStyle(style -> style.withColor(0xFF5555));
        }

        String status = progress.getStatus().name();
        int percent = progress.getProgressPercent();
        long chunksScanned = progress.getChunksGenerated();  // renamed for clarity
        long chunksGenerated = progress.getChunksActuallyGenerated();
        long totalChunks = iterator.getTotalChunks();
        double rate = progress.getGenerationRate();
        long remaining = progress.getEstimatedMinutesRemaining();
        int skippedCount = progress.getSkippedChunksCount();

        // Build status using MutableComponent for efficiency
        Component result = Component.literal(dimKey.location().toString() + ":")
            .withStyle(style -> style.withColor(0x55FFFF));

        // Use String.format for thousands separators on large numbers
        result = result.copy()
            .append("\n  Status: " + status)
            .append(String.format("\n  Progress: %d%% (scanned %,d/%,d, generated %,d new)",
                percent, chunksScanned, totalChunks, chunksGenerated))
            .append(String.format("\n  Generation rate: %.1f new chunks/min", rate));

        if (remaining > 0) {
            result = result.copy().append("\n  Est. time: " + remaining + " minutes");
        } else if (remaining == 0 && percent >= 100) {
            result = result.copy().append("\n  Est. time: Complete!");
        }

        if (skippedCount > 0) {
            result = result.copy().append(String.format("\n  Skipped chunks: %,d (due to errors)", skippedCount));
        }

        // Add throttle and ticket count indicators
        if (task.isThrottled()) {
            result = result.copy().append("\n  [THROTTLED - low TPS or high memory]")
                .withStyle(style -> style.withColor(0xFFAA00));
        }

        int ticketCount = task.getPendingTicketCount();
        if (ticketCount > 0) {
            result = result.copy().append(String.format("\n  Pending tickets: %d", ticketCount));
        }

        if (progress.getLastError() != null) {
            result = result.copy().append("\n  Last error: " + progress.getLastError())
                .withStyle(style -> style.withColor(0xFFAA00));
        }

        return result;
    }
    
    /**
     * Resume saved tasks on server start
     */
    public static void resumeSavedTasks(MinecraftServer server) {
        if (!BrecherConfig.isPregenAutoResume()) {
            return;
        }
        
        BrecherSavedData data = BrecherSavedData.get(server);
        Map<ResourceLocation, GenerationProgress> savedTasks = data.getPregenTasks();
        
        if (savedTasks.isEmpty()) {
            return;
        }
        
        LOGGER.info("Resuming {} saved chunk generation tasks", savedTasks.size());
        
        for (Map.Entry<ResourceLocation, GenerationProgress> entry : savedTasks.entrySet()) {
            ResourceLocation dimLoc = entry.getKey();
            GenerationProgress progress = entry.getValue();
            
            // Check if dimension still exists
            ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, dimLoc);
            ServerLevel level = server.getLevel(dimKey);
            
            if (level == null) {
                LOGGER.warn("Dimension {} no longer exists, skipping saved generation task", dimLoc);
                continue;
            }
            
            // Check if task was running
            if (progress.getStatus() != GenerationProgress.Status.RUNNING) {
                LOGGER.info("Skipping paused/completed task for {}", dimLoc);
                continue;
            }
            
            // Check for stale tasks
            if (progress.isStale(BrecherConfig.getPregenStaleHours())) {
                LOGGER.warn("Generation task for {} is stale (no activity for {} hours), skipping", 
                    dimLoc, BrecherConfig.getPregenStaleHours());
                continue;
            }
            
            // Resume the task
            GenerationTask task = new GenerationTask(dimKey, progress);
            activeTasks.put(dimKey, task);
            LOGGER.info("Resumed generation for {} at {}% ({} chunks visited, {} new generated)", 
                dimLoc, progress.getProgressPercent(), progress.getChunksGenerated(), 
                progress.getChunksActuallyGenerated());
        }
    }
    
    /**
     * Save current progress to persistent storage
     */
    public static void saveProgress(MinecraftServer server) {
        BrecherSavedData data = BrecherSavedData.get(server);
        Map<ResourceLocation, GenerationProgress> toSave = new HashMap<>();
        
        for (Map.Entry<ResourceKey<Level>, GenerationTask> entry : activeTasks.entrySet()) {
            ResourceLocation dimLoc = entry.getKey().location();
            GenerationProgress progress = entry.getValue().getProgress();
            toSave.put(dimLoc, progress);
        }
        
        data.setPregenTasks(toSave);
        data.setDirty();
    }
    
    /**
     * Clean up completed or stale tasks from saved data
     */
    public static void cleanupSavedTasks(MinecraftServer server) {
        BrecherSavedData data = BrecherSavedData.get(server);
        Map<ResourceLocation, GenerationProgress> savedTasks = data.getPregenTasks();
        
        if (savedTasks.isEmpty()) {
            return;
        }
        
        boolean changed = false;
        Iterator<Map.Entry<ResourceLocation, GenerationProgress>> it = savedTasks.entrySet().iterator();
        
        while (it.hasNext()) {
            Map.Entry<ResourceLocation, GenerationProgress> entry = it.next();
            GenerationProgress progress = entry.getValue();
            
            // Remove completed tasks
            if (progress.getStatus() == GenerationProgress.Status.COMPLETED) {
                it.remove();
                changed = true;
                continue;
            }
            
            // Remove stale tasks
            if (progress.isStale(BrecherConfig.getPregenStaleHours() * 7)) { // Week old
                LOGGER.info("Removing stale generation task for {}", entry.getKey());
                it.remove();
                changed = true;
            }
        }
        
        if (changed) {
            data.setDirty();
        }
    }
    
    /**
     * Check if a dimension has an active generation task
     */
    public static boolean hasActiveTask(ResourceKey<Level> dimensionKey) {
        return activeTasks.containsKey(dimensionKey);
    }
    
    /**
     * Get all active tasks (for monitoring)
     */
    public static Map<ResourceKey<Level>, GenerationTask> getActiveTasks() {
        return Collections.unmodifiableMap(activeTasks);
    }

    /**
     * Shutdown the chunk pre-generator and clear all state.
     * Called when the server is stopping to prevent stale references.
     *
     * @param server the MinecraftServer instance (may be null in edge cases)
     */
    public static void shutdown(MinecraftServer server) {
        LOGGER.debug("ChunkPreGenerator.shutdown() called from server lifecycle event");

        int taskCount = activeTasks.size();

        // Save progress before clearing (Task 1.4: preserve progress across restarts)
        if (server != null && taskCount > 0) {
            LOGGER.info("Saving progress for {} active pregen tasks before shutdown", taskCount);
            try {
                saveProgress(server);
            } catch (Exception e) {
                LOGGER.error("Failed to save pregen progress during shutdown", e);
            }
        } else if (server == null && taskCount > 0) {
            LOGGER.warn("Cannot save pregen progress: server is null (progress will be lost)");
        }

        if (taskCount > 0) {
            LOGGER.info("ChunkPreGenerator shutting down, clearing {} active tasks", taskCount);
        }
        activeTasks.clear();
        tickCounter = 0;
    }

    /**
     * Shutdown without saving (legacy compatibility, prefer shutdown(server) when possible).
     * @deprecated Use {@link #shutdown(MinecraftServer)} instead to preserve progress.
     */
    @Deprecated
    public static void shutdown() {
        shutdown(null);
    }
}