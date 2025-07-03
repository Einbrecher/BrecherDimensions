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

package net.tinkstav.brecher_dim.platform.neoforge;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.tinkstav.brecher_dim.config.BrecherConfig;
import net.tinkstav.brecher_dim.platform.TeleportHelper;
import net.tinkstav.brecher_dim.teleport.TeleportHandler;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TeleportHelperImpl implements TeleportHelper {
    private static final Map<UUID, Long> lastTeleportTime = new ConcurrentHashMap<>();
    
    @Override
    public boolean teleportPlayer(ServerPlayer player, ServerLevel targetLevel, Vec3 targetPos) {
        return teleportPlayer(player, targetLevel, targetPos, player.getYRot(), player.getXRot());
    }
    
    @Override
    public boolean teleportPlayer(ServerPlayer player, ServerLevel targetLevel, Vec3 targetPos, float yaw, float pitch) {
        if (player.level() == targetLevel) {
            player.teleportTo(targetPos.x, targetPos.y, targetPos.z);
            player.setYRot(yaw);
            player.setXRot(pitch);
            return true;
        } else {
            DimensionTransition transition = new DimensionTransition(targetLevel, targetPos, Vec3.ZERO, yaw, pitch, DimensionTransition.DO_NOTHING);
            player.changeDimension(transition);
            return player.level() == targetLevel;
        }
    }
    
    @Override
    public Vec3 findSafeSpawnPosition(ServerLevel level, Vec3 targetPos) {
        BlockPos blockPos = BlockPos.containing(targetPos);
        
        // Simple safe position finder
        for (int y = 0; y < 10; y++) {
            for (int x = -5; x <= 5; x++) {
                for (int z = -5; z <= 5; z++) {
                    BlockPos checkPos = blockPos.offset(x, y, z);
                    if (isSafeLocation(level, checkPos)) {
                        return Vec3.atCenterOf(checkPos);
                    }
                }
            }
        }
        
        return targetPos; // Return original if no safe spot found
    }
    
    private boolean isSafeLocation(ServerLevel level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isSolid() &&
               !level.getBlockState(below).is(Blocks.LAVA) &&
               level.getBlockState(pos).isAir() &&
               level.getBlockState(pos.above()).isAir() &&
               level.getFluidState(pos).isEmpty() &&
               level.getFluidState(pos.above()).isEmpty();
    }
    
    @Override
    public void createPlatformIfNeeded(ServerLevel level, Vec3 pos) {
        BlockPos blockPos = BlockPos.containing(pos);
        if (!level.getBlockState(blockPos.below()).isSolid()) {
            // Create a 3x3 obsidian platform
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    level.setBlock(blockPos.below().offset(x, 0, z), Blocks.OBSIDIAN.defaultBlockState(), 3);
                    // Clear blocks above platform
                    for (int y = 0; y <= 2; y++) {
                        level.setBlock(blockPos.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
    }
    
    @Override
    public void preTeleport(ServerPlayer player, ServerLevel targetLevel) {
        // Record last teleport time
        lastTeleportTime.put(player.getUUID(), System.currentTimeMillis());
    }
    
    @Override
    public void postTeleport(ServerPlayer player, ServerLevel fromLevel) {
        // Apply temporary invulnerability
        player.setInvulnerable(true);
        player.getServer().tell(new net.minecraft.server.TickTask(player.getServer().getTickCount() + 100, () -> {
            player.setInvulnerable(false);
        }));
    }
    
    @Override
    public boolean canTeleport(ServerPlayer player) {
        Long lastTime = lastTeleportTime.get(player.getUUID());
        if (lastTime == null) {
            return true;
        }
        
        long cooldownMillis = BrecherConfig.getTeleportCooldown() * 1000L; // Convert seconds to millis
        return System.currentTimeMillis() - lastTime >= cooldownMillis;
    }
    
    @Override
    public int getRemainingCooldown(ServerPlayer player) {
        Long lastTime = lastTeleportTime.get(player.getUUID());
        if (lastTime == null) {
            return 0;
        }
        
        long cooldownMillis = BrecherConfig.getTeleportCooldown() * 1000L; // Convert seconds to millis
        long elapsed = System.currentTimeMillis() - lastTime;
        if (elapsed >= cooldownMillis) {
            return 0;
        }
        
        return (int) ((cooldownMillis - elapsed) / 1000); // Convert back to seconds
    }
}