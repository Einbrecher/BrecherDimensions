/*
 * Brecher's Dimensions - Temporary resettable dimensions for exploration
 * Copyright (C) 2025 Einbrecher. All rights reserved.
 */

package net.tinkstav.brecher_dim.neoforge.mixin;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.tinkstav.brecher_dim.config.BrecherConfig;
import net.tinkstav.brecher_dim.dimension.ExplorationSeedManager;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

/**
 * Mixin to prevent chunk saving for exploration dimensions
 */
@Mixin(ChunkMap.class)
public class MixinChunkMap {
    @Unique
    private static final Logger LOGGER = LogUtils.getLogger();
    
    @Shadow @Final ServerLevel level;
    
    /**
     * Prevent chunk saves for exploration dimensions
     */
    @Inject(method = "save(Lnet/minecraft/world/level/chunk/ChunkAccess;)Z", at = @At("HEAD"), cancellable = true)
    private void brecher_dim$preventChunkSave(ChunkAccess chunkAccess, CallbackInfoReturnable<Boolean> cir) {
        if (BrecherConfig.isPreventDiskSaves() && ExplorationSeedManager.isExplorationDimension(level.dimension())) {
            LOGGER.debug("Preventing chunk save for exploration dimension: {}", level.dimension().location());
            // Return false to indicate the chunk was not saved
            cir.setReturnValue(false);
        }
    }
}