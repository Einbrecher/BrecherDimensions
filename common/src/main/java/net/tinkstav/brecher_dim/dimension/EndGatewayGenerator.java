/*
 * Brecher's Dimensions - Temporary resettable dimensions for exploration
 * Copyright (C) 2025 Einbrecher. All rights reserved.
 */

package net.tinkstav.brecher_dim.dimension;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EndGatewayBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.tinkstav.brecher_dim.config.BrecherConfig;
import org.slf4j.Logger;

/**
 * Generates End gateways for exploration End dimensions.
 * Creates all 20 gateways that would normally appear after defeating the Ender Dragon.
 */
public class EndGatewayGenerator {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // Gateway ring configuration
    private static final int GATEWAY_COUNT = 20;
    private static final int GATEWAY_DISTANCE = 96; // Distance from origin
    private static final int GATEWAY_HEIGHT = 75;   // Y level for gateways
    private static final double ANGLE_INCREMENT = 360.0 / GATEWAY_COUNT;
    
    /**
     * Generates all End gateways for an exploration End dimension
     * @param level The End dimension to generate gateways in
     */
    public static void generateEndGateways(ServerLevel level) {
        if (!level.dimension().location().getPath().contains("the_end")) {
            LOGGER.warn("Attempted to generate End gateways in non-End dimension: {}", 
                level.dimension().location());
            return;
        }
        
        // Check if End Gateways are disabled in config
        if (BrecherConfig.isDisableEndGateways()) {
            LOGGER.info("End Gateways are disabled in exploration dimensions - skipping generation for {}", 
                level.dimension().location());
            return;
        }
        
        LOGGER.info("Generating {} End gateways for exploration dimension: {}", 
            GATEWAY_COUNT, level.dimension().location());
        
        // Generate gateways in a circle around the origin
        for (int i = 0; i < GATEWAY_COUNT; i++) {
            double angle = Math.toRadians(i * ANGLE_INCREMENT);
            int x = (int) Math.round(GATEWAY_DISTANCE * Math.cos(angle));
            int z = (int) Math.round(GATEWAY_DISTANCE * Math.sin(angle));
            
            BlockPos gatewayPos = new BlockPos(x, GATEWAY_HEIGHT, z);
            
            // Find the actual surface position for the gateway
            BlockPos actualPos = findGatewayPosition(level, gatewayPos);
            if (actualPos != null) {
                generateGatewayStructure(level, actualPos);
                LOGGER.debug("Generated gateway {} at position {}", i + 1, actualPos);
            } else {
                LOGGER.warn("Failed to find suitable position for gateway {} near {}", i + 1, gatewayPos);
            }
        }
        
        LOGGER.info("Completed End gateway generation for {}", level.dimension().location());
    }
    
    /**
     * Find a suitable position for the gateway near the target position
     */
    private static BlockPos findGatewayPosition(ServerLevel level, BlockPos targetPos) {
        // First check if target position is suitable
        if (isValidGatewayPosition(level, targetPos)) {
            return targetPos;
        }
        
        // Search in expanding circles
        for (int radius = 1; radius <= 16; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) == radius || Math.abs(dz) == radius) {
                        BlockPos checkPos = targetPos.offset(dx, 0, dz);
                        
                        // Search vertically for a suitable position
                        for (int dy = -10; dy <= 10; dy++) {
                            BlockPos pos = checkPos.offset(0, dy, 0);
                            if (isValidGatewayPosition(level, pos)) {
                                return pos;
                            }
                        }
                    }
                }
            }
        }
        
        // If no suitable position found, create one at the target position
        return createGatewayPlatform(level, targetPos);
    }
    
    /**
     * Check if a position is valid for placing a gateway
     */
    private static boolean isValidGatewayPosition(ServerLevel level, BlockPos pos) {
        // Check if position is within build limits
        if (pos.getY() < level.getMinBuildHeight() + 5 || 
            pos.getY() > level.getMaxBuildHeight() - 5) {
            return false;
        }
        
        // Check if there's solid ground below
        BlockPos below = pos.below();
        return level.getBlockState(below).isSolid() && 
               level.getBlockState(pos).isAir();
    }
    
    /**
     * Create a platform for the gateway if no suitable position exists
     */
    private static BlockPos createGatewayPlatform(ServerLevel level, BlockPos targetPos) {
        BlockPos platformPos = new BlockPos(targetPos.getX(), GATEWAY_HEIGHT, targetPos.getZ());
        
        // Clear space for the gateway structure (no platform needed)
        // End gateways in vanilla float in the air with just bedrock structure
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                // Clear space where the gateway will be
                for (int y = -2; y < 5; y++) {
                    BlockPos clearPos = platformPos.offset(x, y, z);
                    if (!level.getBlockState(clearPos).isAir()) {
                        level.setBlockAndUpdate(clearPos, Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }
        
        return platformPos;
    }
    
    /**
     * Generate the gateway structure at the specified position
     */
    private static void generateGatewayStructure(ServerLevel level, BlockPos centerPos) {
        // Place the gateway block
        level.setBlockAndUpdate(centerPos, Blocks.END_GATEWAY.defaultBlockState());
        
        // Configure the gateway block entity
        BlockEntity blockEntity = level.getBlockEntity(centerPos);
        if (blockEntity instanceof TheEndGatewayBlockEntity gateway) {
            // Calculate exit position for this gateway
            // In vanilla, gateways are arranged in a ring and teleport outward
            // We'll calculate an exit position based on the gateway's position relative to spawn
            
            // Calculate direction from spawn (0,0) to this gateway
            double dx = centerPos.getX();
            double dz = centerPos.getZ();
            double distance = Math.sqrt(dx * dx + dz * dz);
            
            if (distance > 0) {
                // Normalize direction vector
                double dirX = dx / distance;
                double dirZ = dz / distance;
                
                // Exit portals are typically 1024+ blocks away from the main island
                // We'll place them in a ring at distance 1024-1536 blocks
                double exitDistance = 1024 + (centerPos.hashCode() & 0x1FF); // Add some variation
                
                // Calculate exit position
                int exitX = (int) Math.round(dirX * exitDistance);
                int exitZ = (int) Math.round(dirZ * exitDistance);
                
                // Use a reasonable height for the exit
                BlockPos exitPos = new BlockPos(exitX, 75, exitZ);
                
                // Set the exit position for this gateway
                // The false parameter means it's not an exact teleport - vanilla will find safe ground near this position
                gateway.setExitPosition(exitPos, false);
                
                LOGGER.info("Created gateway at {} with exit position at {}", centerPos, exitPos);
            } else {
                // Gateway at spawn - set a random exit position
                int angle = centerPos.hashCode() & 0xFF;
                double radians = Math.toRadians(angle * 360.0 / 256.0);
                int exitX = (int) Math.round(1024 * Math.cos(radians));
                int exitZ = (int) Math.round(1024 * Math.sin(radians));
                BlockPos exitPos = new BlockPos(exitX, 75, exitZ);
                
                gateway.setExitPosition(exitPos, false);
                LOGGER.info("Created gateway at spawn {} with exit position at {}", centerPos, exitPos);
            }
            
            gateway.setChanged();
        }
        
        // Generate bedrock structure around the gateway
        generateBedrockStructure(level, centerPos);
    }
    
    
    /**
     * Generate the bedrock structure around the gateway
     */
    private static void generateBedrockStructure(ServerLevel level, BlockPos gatewayPos) {
        // Vanilla End gateway structure is 5 blocks tall
        
        // Layer 1 (Y-2): Single center bedrock
        level.setBlockAndUpdate(gatewayPos.below(2), Blocks.BEDROCK.defaultBlockState());
        
        // Layer 2 (Y-1): + shape (5 blocks)
        level.setBlockAndUpdate(gatewayPos.below(1), Blocks.BEDROCK.defaultBlockState()); // Center
        level.setBlockAndUpdate(gatewayPos.below(1).north(), Blocks.BEDROCK.defaultBlockState());
        level.setBlockAndUpdate(gatewayPos.below(1).south(), Blocks.BEDROCK.defaultBlockState());
        level.setBlockAndUpdate(gatewayPos.below(1).east(), Blocks.BEDROCK.defaultBlockState());
        level.setBlockAndUpdate(gatewayPos.below(1).west(), Blocks.BEDROCK.defaultBlockState());
        
        // Layer 3 (Y+0): Gateway block only - no bedrock
        // The gateway block itself is placed elsewhere
        
        // Layer 4 (Y+1): + shape (5 blocks)
        level.setBlockAndUpdate(gatewayPos.above(1), Blocks.BEDROCK.defaultBlockState()); // Center
        level.setBlockAndUpdate(gatewayPos.above(1).north(), Blocks.BEDROCK.defaultBlockState());
        level.setBlockAndUpdate(gatewayPos.above(1).south(), Blocks.BEDROCK.defaultBlockState());
        level.setBlockAndUpdate(gatewayPos.above(1).east(), Blocks.BEDROCK.defaultBlockState());
        level.setBlockAndUpdate(gatewayPos.above(1).west(), Blocks.BEDROCK.defaultBlockState());
        
        // Layer 5 (Y+2): Single center bedrock
        level.setBlockAndUpdate(gatewayPos.above(2), Blocks.BEDROCK.defaultBlockState());
    }
}