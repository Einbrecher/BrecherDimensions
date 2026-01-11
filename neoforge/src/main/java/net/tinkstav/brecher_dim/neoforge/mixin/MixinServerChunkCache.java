/*
 * Brecher's Dimensions - Temporary resettable dimensions for exploration
 * Copyright (C) 2025 Einbrecher. All rights reserved.
 */

package net.tinkstav.brecher_dim.neoforge.mixin;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.storage.ServerLevelData;
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

import java.util.Optional;

/**
 * Mixin to intercept ServerChunkCache creation and potentially modify RandomState
 */
@Mixin(ServerChunkCache.class)
public class MixinServerChunkCache {
    @Unique
    private static final Logger LOGGER = LogUtils.getLogger();
    
    @Shadow @Final ServerLevel level;
    
    /**
     * Log when ServerChunkCache is created to understand the flow
     */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void brecher_dim$onInit(CallbackInfo ci) {
        if (ExplorationSeedManager.isExplorationDimension(level.dimension())) {
            LOGGER.info("ServerChunkCache created for exploration dimension: {}", level.dimension().location());
            
            // Try to get the custom seed for this dimension
            Optional<Long> customSeed = ExplorationSeedManager.getSeedForDimension(level.dimension());
            
            if (customSeed.isPresent()) {
                LOGGER.info("Exploration dimension {} should use custom seed: {}", 
                    level.dimension().location(), customSeed.get());
            }
        }
    }
    
}