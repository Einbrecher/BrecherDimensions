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

package net.tinkstav.brecher_dim.util;

import com.mojang.logging.LogUtils;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.tinkstav.brecher_dim.config.BrecherConfig;
import net.tinkstav.brecher_dim.data.BrecherSavedData;
import org.slf4j.Logger;

import java.util.Optional;

/**
 * Utility class for checking dimension lock status based on advancements and manual unlocks.
 * Centralizes all logic for determining if a player can access an exploration dimension.
 */
public class AdvancementLockChecker {
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Represents the lock status for a dimension
     */
    public enum LockStatus {
        /** Dimension is unlocked (player has advancement or manual unlock) */
        UNLOCKED,
        /** Dimension is locked (player lacks required advancement) */
        LOCKED,
        /** No lock is configured for this dimension */
        NO_LOCK_REQUIRED
    }

    /**
     * Result of a lock check, containing status and display information
     */
    public record LockCheckResult(
        LockStatus status,
        Optional<String> requiredAdvancementId,
        Optional<Component> advancementDisplayName
    ) {
        public static LockCheckResult unlocked() {
            return new LockCheckResult(LockStatus.UNLOCKED, Optional.empty(), Optional.empty());
        }

        public static LockCheckResult noLockRequired() {
            return new LockCheckResult(LockStatus.NO_LOCK_REQUIRED, Optional.empty(), Optional.empty());
        }

        public static LockCheckResult locked(String advancementId, Component displayName) {
            return new LockCheckResult(LockStatus.LOCKED, Optional.of(advancementId), Optional.of(displayName));
        }

        public static LockCheckResult lockedWithId(String advancementId) {
            return new LockCheckResult(LockStatus.LOCKED, Optional.of(advancementId), Optional.empty());
        }
    }

    /**
     * Get the AdvancementHolder for a given advancement ID.
     * @param server The Minecraft server
     * @param advancementId The advancement ResourceLocation
     * @return Optional containing the advancement holder, or empty if not found
     */
    public static Optional<AdvancementHolder> getAdvancementHolder(MinecraftServer server, ResourceLocation advancementId) {
        if (server == null || advancementId == null) {
            return Optional.empty();
        }
        AdvancementHolder holder = server.getAdvancements().get(advancementId);
        if (holder == null) {
            LOGGER.warn("Advancement '{}' not found in server advancement registry. " +
                "This may be due to a typo in the config or a missing datapack.", advancementId);
        }
        return Optional.ofNullable(holder);
    }

    /**
     * Check if a player has completed a specific advancement.
     * @param player The server player
     * @param advancementId The advancement ResourceLocation to check
     * @return true if the player has completed the advancement, false otherwise
     */
    public static boolean hasAdvancement(ServerPlayer player, ResourceLocation advancementId) {
        if (player == null || advancementId == null) {
            return false;
        }

        Optional<AdvancementHolder> holderOpt = getAdvancementHolder(player.getServer(), advancementId);
        if (holderOpt.isEmpty()) {
            // Advancement not found - for safety, return false to require manual unlock
            return false;
        }

        AdvancementHolder holder = holderOpt.get();
        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(holder);
        return progress.isDone();
    }

    /**
     * Check if a player is unlocked for a specific base dimension.
     * Checks manual unlock first, then advancement.
     * @param player The server player
     * @param baseDimension The base dimension ResourceLocation (e.g., minecraft:the_end)
     * @param data The BrecherSavedData for manual unlock checks (can be null)
     * @return true if the player can access the exploration version of this dimension
     */
    public static boolean isUnlocked(ServerPlayer player, ResourceLocation baseDimension, BrecherSavedData data) {
        if (player == null || baseDimension == null) {
            return false;
        }

        // Check if dimension locks are enabled
        if (!BrecherConfig.isDimensionLocksEnabled()) {
            return true;
        }

        // Check for manual unlock first
        if (data != null && data.hasManualUnlock(player.getUUID(), baseDimension)) {
            LOGGER.debug("Player {} has manual unlock for dimension {}", player.getName().getString(), baseDimension);
            return true;
        }

        // Check for required advancement
        Optional<String> requiredAdvancementOpt = BrecherConfig.getDimensionLock(baseDimension.toString());
        if (requiredAdvancementOpt.isEmpty()) {
            // No lock configured for this dimension
            return true;
        }

        String advancementId = requiredAdvancementOpt.get();
        try {
            ResourceLocation advancementLoc = ResourceLocation.parse(advancementId);
            return hasAdvancement(player, advancementLoc);
        } catch (Exception e) {
            LOGGER.error("Invalid advancement ID '{}' for dimension lock: {}", advancementId, e.getMessage());
            // On error, fail open to avoid softlocking players due to config issues
            return true;
        }
    }

    /**
     * Get the detailed lock status for a player and dimension.
     * @param player The server player
     * @param baseDimension The base dimension ResourceLocation
     * @param data The BrecherSavedData for manual unlock checks (can be null)
     * @return LockCheckResult containing status and advancement info if locked
     */
    public static LockCheckResult getDimensionLockStatus(ServerPlayer player, ResourceLocation baseDimension, BrecherSavedData data) {
        if (player == null || baseDimension == null) {
            return LockCheckResult.noLockRequired();
        }

        // Check if dimension locks are enabled
        if (!BrecherConfig.isDimensionLocksEnabled()) {
            return LockCheckResult.noLockRequired();
        }

        // Check for manual unlock first
        if (data != null && data.hasManualUnlock(player.getUUID(), baseDimension)) {
            return LockCheckResult.unlocked();
        }

        // Check for required advancement
        Optional<String> requiredAdvancementOpt = BrecherConfig.getDimensionLock(baseDimension.toString());
        if (requiredAdvancementOpt.isEmpty()) {
            // No lock configured for this dimension
            return LockCheckResult.noLockRequired();
        }

        String advancementId = requiredAdvancementOpt.get();
        try {
            ResourceLocation advancementLoc = ResourceLocation.parse(advancementId);

            // Check if player has the advancement
            if (hasAdvancement(player, advancementLoc)) {
                return LockCheckResult.unlocked();
            }

            // Player is locked - get advancement display name for better UX
            Optional<AdvancementHolder> holderOpt = getAdvancementHolder(player.getServer(), advancementLoc);
            if (holderOpt.isPresent()) {
                AdvancementHolder holder = holderOpt.get();
                Component displayName = holder.value().display()
                    .map(display -> display.getTitle())
                    .orElse(Component.literal(advancementId));
                return LockCheckResult.locked(advancementId, displayName);
            } else {
                // Advancement not found - return with just ID
                return LockCheckResult.lockedWithId(advancementId);
            }
        } catch (Exception e) {
            LOGGER.error("Error checking dimension lock for {}: {}", baseDimension, e.getMessage());
            // On error, fail open
            return LockCheckResult.noLockRequired();
        }
    }

    /**
     * Convenience method to get lock status without manual unlock data.
     * @param player The server player
     * @param baseDimension The base dimension ResourceLocation
     * @return LockCheckResult containing status and advancement info if locked
     */
    public static LockCheckResult getDimensionLockStatus(ServerPlayer player, ResourceLocation baseDimension) {
        BrecherSavedData data = null;
        if (player != null && player.getServer() != null) {
            try {
                data = BrecherSavedData.get(player.getServer());
            } catch (Exception e) {
                LOGGER.debug("Could not get BrecherSavedData for lock check: {}", e.getMessage());
            }
        }
        return getDimensionLockStatus(player, baseDimension, data);
    }
}
