/*
 * Brecher's Dimensions - Temporary resettable dimensions for exploration
 * Copyright (C) 2025 Einbrecher. All rights reserved.
 */

package net.tinkstav.brecher_dim.fabric.mixin;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EndGatewayBlock;
import net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.tinkstav.brecher_dim.BrecherDimensions;
import net.tinkstav.brecher_dim.config.BrecherConfig;
import net.tinkstav.brecher_dim.dimension.BrecherDimensionManager;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EndGatewayBlock.class)
public class MixinEndGatewayBlock {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    @Inject(method = "getPortalDestination", at = @At("HEAD"), cancellable = true)
    private void onGetPortalDestination(ServerLevel world, Entity entity, BlockPos pos, CallbackInfoReturnable<DimensionTransition> cir) {
        // Only intercept for exploration dimensions
        BrecherDimensionManager manager = BrecherDimensions.getDimensionManager();
        if (manager == null || !manager.isExplorationDimension(world.dimension().location())) {
            return;
        }
        
        // Check if End Gateways are disabled
        if (BrecherConfig.isDisableEndGateways()) {
            LOGGER.debug("End Gateway usage blocked in exploration dimension - config disabled");
            cir.setReturnValue(null); // This will prevent teleportation
            return;
        }
        
        LOGGER.debug("Intercepting End Gateway teleportation in exploration dimension for entity {}", entity);
        
        // Get the gateway block entity
        if (!(world.getBlockEntity(pos) instanceof TheEndGatewayBlockEntity gateway)) {
            return;
        }
        
        // Get the vanilla exit position
        Vec3 exitPos = gateway.getPortalPosition(world, pos);
        if (exitPos == null) {
            LOGGER.warn("End Gateway has no exit position set");
            return;
        }
        
        // Check if the exit position is safe
        BlockPos exitBlockPos = BlockPos.containing(exitPos);
        if (!isSafeEndGatewayDestination(world, exitBlockPos)) {
            LOGGER.warn("End Gateway exit position {} is not safe - finding alternative", exitBlockPos);
            
            // Find a safe position near the exit
            BlockPos safePos = findSafeEndGatewayPosition(world, exitBlockPos);
            if (safePos != null) {
                LOGGER.info("Found safe alternative position: {}", safePos);
                Vec3 safeVec = new Vec3(safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5);
                
                // Create custom teleport target with safety
                DimensionTransition transition = new DimensionTransition(
                    world,
                    safeVec,
                    Vec3.ZERO, // velocity
                    entity.getYRot(),
                    entity.getXRot(),
                    DimensionTransition.PLACE_PORTAL_TICKET // Ensure chunks stay loaded
                );
                
                cir.setReturnValue(transition);
            } else {
                // Create emergency platform
                LOGGER.warn("No safe position found near {} - creating emergency platform", exitBlockPos);
                BlockPos platformPos = createEmergencyEndPlatform(world, exitBlockPos);
                Vec3 platformVec = new Vec3(platformPos.getX() + 0.5, platformPos.getY() + 1, platformPos.getZ() + 0.5);
                
                DimensionTransition transition = new DimensionTransition(
                    world,
                    platformVec,
                    Vec3.ZERO,
                    entity.getYRot(),
                    entity.getXRot(),
                    DimensionTransition.PLACE_PORTAL_TICKET
                );
                
                cir.setReturnValue(transition);
            }
        }
        
        // If the position is safe, let vanilla handle it
    }
    
    private boolean isSafeEndGatewayDestination(ServerLevel level, BlockPos pos) {
        // Check if position is within build limits
        if (pos.getY() < level.getMinBuildHeight() + 2 || pos.getY() > level.getMaxBuildHeight() - 2) {
            return false;
        }
        
        // Check if chunk is loaded
        if (!level.hasChunkAt(pos)) {
            // Force load the chunk to check
            level.getChunk(pos);
        }
        
        // Check the blocks at and around the position
        BlockState below = level.getBlockState(pos.below());
        BlockState feet = level.getBlockState(pos);
        BlockState head = level.getBlockState(pos.above());
        
        // Must have solid ground
        if (!below.isSolid() || below.isAir()) {
            // Special case: Check if we're over the void
            if (pos.getY() <= 5) {
                return false;
            }
            
            // Check further down for solid ground
            boolean foundGround = false;
            for (int y = 2; y <= 10; y++) {
                BlockPos checkPos = pos.below(y);
                if (checkPos.getY() < level.getMinBuildHeight()) {
                    break;
                }
                if (level.getBlockState(checkPos).isSolid()) {
                    foundGround = true;
                    break;
                }
            }
            if (!foundGround) {
                return false;
            }
        }
        
        // Must have air to spawn in
        if (!feet.isAir() || !head.isAir()) {
            return false;
        }
        
        // Don't spawn on bedrock at Y=0 (void floor)
        if (pos.getY() <= 1 && below.is(Blocks.BEDROCK)) {
            return false;
        }
        
        // Additional check for End-specific dangers
        if (below.is(Blocks.END_PORTAL) || below.is(Blocks.END_PORTAL_FRAME)) {
            return false;
        }
        
        return true;
    }
    
    private BlockPos findSafeEndGatewayPosition(ServerLevel level, BlockPos center) {
        // Search in expanding circles for a safe position
        // Increased range to 64 blocks horizontally (from 16)
        for (int radius = 0; radius <= 64; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) == radius || Math.abs(dz) == radius) {
                        BlockPos checkPos = center.offset(dx, 0, dz);
                        
                        // Search vertically - increased to 32 blocks up/down (from 16)
                        for (int dy = -32; dy <= 32; dy++) {
                            BlockPos pos = checkPos.offset(0, dy, 0);
                            if (isSafeEndGatewayDestination(level, pos)) {
                                return pos;
                            }
                        }
                    }
                }
            }
        }
        
        return null;
    }
    
    private BlockPos createEmergencyEndPlatform(ServerLevel level, BlockPos target) {
        // Create platform at a safe height
        int safeY = Math.max(target.getY(), 64);
        if (safeY < level.getMinBuildHeight() + 5) {
            safeY = level.getMinBuildHeight() + 5;
        }
        if (safeY > level.getMaxBuildHeight() - 5) {
            safeY = level.getMaxBuildHeight() - 5;
        }
        
        BlockPos platformCenter = new BlockPos(target.getX(), safeY, target.getZ());
        
        // Create 5x5 obsidian platform (standard End gateway exit size)
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                BlockPos platformPos = platformCenter.offset(x, 0, z);
                level.setBlockAndUpdate(platformPos, Blocks.OBSIDIAN.defaultBlockState());
                
                // Clear space above
                for (int y = 1; y <= 3; y++) {
                    BlockPos clearPos = platformPos.above(y);
                    if (!level.getBlockState(clearPos).isAir()) {
                        level.setBlockAndUpdate(clearPos, Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }
        
        LOGGER.info("Created emergency End platform at {} for gateway teleportation", platformCenter);
        return platformCenter;
    }
}