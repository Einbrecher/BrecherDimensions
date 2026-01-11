/*
 * Brecher's Dimensions - Temporary resettable dimensions for exploration
 * Copyright (C) 2025 Einbrecher. All rights reserved.
 */

package net.tinkstav.brecher_dim.fabric.mixin;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.RandomSequences;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.ServerLevelData;
import net.tinkstav.brecher_dim.BrecherDimensions;
import net.tinkstav.brecher_dim.config.BrecherConfig;
import net.tinkstav.brecher_dim.dimension.DimensionRegistrar;
import net.tinkstav.brecher_dim.dimension.ExplorationSeedManager;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.concurrent.Executor;

@Mixin(ServerLevel.class)
public abstract class MixinServerLevel {
    @Unique
    private static final Logger LOGGER = LogUtils.getLogger();
    
    @Shadow @Final private PersistentEntitySectionManager<net.minecraft.world.entity.Entity> entityManager;
    @Shadow public abstract void save(net.minecraft.util.ProgressListener progressListener, boolean flush, boolean skipSave);
    
    @Unique
    private boolean brecher_dim$isRuntimeDimension = false;
    
    /**
     * Mark runtime dimensions after construction
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void brecher_dim$markRuntimeDimension(MinecraftServer server, Executor executor, 
            LevelStorageSource.LevelStorageAccess storageAccess, ServerLevelData serverLevelData, 
            ResourceKey<Level> dimension, LevelStem levelStem, ChunkProgressListener progressListener, 
            boolean isDebug, long seed, List<CustomSpawner> spawners, boolean shouldTickTime, 
            @Nullable RandomSequences randomSequences, CallbackInfo ci) {
        
        if (ExplorationSeedManager.isExplorationDimension(dimension)) {
            brecher_dim$isRuntimeDimension = true;
            ExplorationSeedManager.setCurrentDimension(dimension);
            LOGGER.info("Runtime exploration dimension created: {} with seed {}", dimension.location(), seed);
            BrecherDimensions.LOGGER.debug("Set dimension context for exploration dimension: {}", dimension.location());
        }
    }
    
    
    /**
     * Prevent saving runtime dimensions to disk if configured
     */
    @Inject(method = "saveLevelData", at = @At("HEAD"), cancellable = true)
    private void brecher_dim$preventRuntimeSave(CallbackInfo ci) {
        if (brecher_dim$isRuntimeDimension && BrecherConfig.isPreventDiskSaves()) {
            LOGGER.debug("Preventing disk save for runtime dimension");
            ci.cancel();
        }
    }
    
    /**
     * Prevent the overall save method for runtime dimensions
     */
    @Inject(method = "save", at = @At("HEAD"), cancellable = true)
    private void brecher_dim$preventOverallSave(net.minecraft.util.ProgressListener progressListener, boolean flush, boolean skipSave, CallbackInfo ci) {
        if (brecher_dim$isRuntimeDimension && BrecherConfig.isPreventDiskSaves()) {
            LOGGER.debug("Preventing overall save for runtime dimension");
            ci.cancel();
        }
    }
    
    /**
     * Log when runtime dimensions are being closed
     */
    @Inject(method = "close", at = @At("HEAD"))
    private void brecher_dim$logRuntimeCleanup(CallbackInfo ci) {
        if (brecher_dim$isRuntimeDimension) {
            ServerLevel self = (ServerLevel)(Object)this;
            LOGGER.info("Runtime dimension closing: {}", self.dimension().location());
            // Let the normal close process handle cleanup
            // We don't need to manually close chunk sources or entity managers
        }
    }
    
    /**
     * Optimize chunk loading for runtime dimensions
     */
    @Inject(method = "tickChunk", at = @At("HEAD"))
    private void brecher_dim$optimizeChunkTicking(CallbackInfo ci) {
        if (brecher_dim$isRuntimeDimension && BrecherConfig.isAggressiveChunkUnloading()) {
            // Additional optimization could be added here
            // For now, this serves as a hook for future performance improvements
        }
    }
}