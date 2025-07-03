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

package net.tinkstav.brecher_dim.platform.fabric;

import net.tinkstav.brecher_dim.platform.TeleportHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.portal.DimensionTransition;

public class TeleportHelperImpl implements TeleportHelper {
    @Override
    public boolean teleportPlayer(ServerPlayer player, ServerLevel targetLevel, Vec3 targetPos) {
        return teleportPlayer(player, targetLevel, targetPos, player.getYRot(), player.getXRot());
    }
    
    @Override
    public boolean teleportPlayer(ServerPlayer player, ServerLevel targetLevel, Vec3 targetPos, float yaw, float pitch) {
        // Use Fabric's teleportation
        player.teleportTo(targetLevel, targetPos.x, targetPos.y, targetPos.z, yaw, pitch);
        return true;
    }
    
    @Override
    public Vec3 findSafeSpawnPosition(ServerLevel level, Vec3 targetPos) {
        // Simple implementation - delegate to more complex logic if needed
        // For now, just return the target position
        // In a real implementation, this would check for solid ground, avoid hazards, etc.
        return targetPos;
    }
    
    @Override
    public void createPlatformIfNeeded(ServerLevel level, Vec3 pos) {
        // Create a simple obsidian platform if needed
        // This is typically handled by the teleport logic in TeleportHandler
    }
    
    @Override
    public void preTeleport(ServerPlayer player, ServerLevel targetLevel) {
        // Pre-teleport setup if needed
        // Fabric doesn't require special pre-teleport handling
    }
    
    @Override
    public void postTeleport(ServerPlayer player, ServerLevel fromLevel) {
        // Post-teleport cleanup if needed
        // Ensure player data is synced
        if (player != null && player.serverLevel() != null) {
            player.connection.teleport(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        }
    }
    
    @Override
    public boolean canTeleport(ServerPlayer player) {
        // Check if teleportation is allowed
        return player != null && !player.isRemoved();
    }
    
    @Override
    public int getRemainingCooldown(ServerPlayer player) {
        // Cooldown is handled by TeleportHandler, not platform-specific
        return 0;
    }
}