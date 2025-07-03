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

package net.tinkstav.brecher_dim.platform;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Platform-specific teleportation handling.
 */
public interface TeleportHelper {
    /**
     * Teleports a player to a specific position in a dimension.
     * Handles portal creation and safety checks.
     */
    boolean teleportPlayer(ServerPlayer player, ServerLevel targetLevel, Vec3 targetPos);
    
    /**
     * Teleports a player to a specific position with rotation.
     */
    boolean teleportPlayer(ServerPlayer player, ServerLevel targetLevel, Vec3 targetPos, float yaw, float pitch);
    
    /**
     * Finds a safe spawn position in the target dimension.
     * Returns null if no safe position found.
     */
    Vec3 findSafeSpawnPosition(ServerLevel level, Vec3 targetPos);
    
    /**
     * Creates a platform at the specified position if needed.
     */
    void createPlatformIfNeeded(ServerLevel level, Vec3 pos);
    
    /**
     * Handles pre-teleport setup (e.g., saving position).
     */
    void preTeleport(ServerPlayer player, ServerLevel targetLevel);
    
    /**
     * Handles post-teleport cleanup (e.g., applying effects).
     */
    void postTeleport(ServerPlayer player, ServerLevel fromLevel);
    
    /**
     * Checks if a player can teleport (cooldowns, permissions, etc.).
     */
    boolean canTeleport(ServerPlayer player);
    
    /**
     * Gets the remaining cooldown time in ticks.
     */
    int getRemainingCooldown(ServerPlayer player);
}