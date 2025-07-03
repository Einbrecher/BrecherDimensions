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

package net.tinkstav.brecher_dim.config.neoforge;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.tinkstav.brecher_dim.config.BrecherConfig;
import org.slf4j.Logger;

import java.util.List;

/**
 * NeoForge implementation using TOML configuration
 */
public class BrecherConfigImpl {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    /**
     * Initialize config system
     */
    public static void init() {
        LOGGER.info("Initializing Brecher's Exploration Dimensions config for NeoForge");
        
        // Config registration and event listener registration are handled in the mod constructor
        // This method is just for any additional initialization if needed
    }
    
    /**
     * Register config with mod container (called from mod constructor)
     */
    public static void registerConfig(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, BrecherConfigSpec.SPEC, "brecher_exploration.toml");
    }
    
    /**
     * Handle config loading/reloading
     */
    @SubscribeEvent
    public static void onConfigLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == BrecherConfigSpec.SPEC) {
            LOGGER.info("Loading Brecher's Exploration Dimensions config");
            syncConfigValues();
        }
    }
    
    /**
     * Handle config reloading
     */
    @SubscribeEvent
    public static void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == BrecherConfigSpec.SPEC) {
            LOGGER.info("Reloading Brecher's Exploration Dimensions config");
            syncConfigValues();
        }
    }
    
    /**
     * Sync values from ForgeConfigSpec to BrecherConfig
     */
    private static void syncConfigValues() {
        try {
            // General settings
            BrecherConfig.setExplorationBorder(BrecherConfigSpec.EXPLORATION_BORDER.get());
            
            // Seed settings
            BrecherConfig.setSeedStrategy(BrecherConfigSpec.SEED_STRATEGY.get().name());
            BrecherConfig.setDebugSeed(BrecherConfigSpec.DEBUG_SEED.get());
            
            // Dimension settings
            BrecherConfig.setEnabledDimensions(new ArrayList<>(BrecherConfigSpec.ENABLED_DIMENSIONS.get()));
            BrecherConfig.setBlacklist(new ArrayList<>(BrecherConfigSpec.BLACKLIST.get()));
            BrecherConfig.setAllowModdedDimensions(BrecherConfigSpec.ALLOW_MODDED_DIMENSIONS.get());
            
            // Feature settings
            BrecherConfig.setPreventExplorationSpawnSetting(BrecherConfigSpec.PREVENT_EXPLORATION_SPAWN_SETTING.get());
            BrecherConfig.setDisableEnderChests(BrecherConfigSpec.DISABLE_ENDER_CHESTS.get());
            BrecherConfig.setClearInventoryOnReturn(BrecherConfigSpec.CLEAR_INVENTORY_ON_RETURN.get());
            BrecherConfig.setKeepInventoryInExploration(BrecherConfigSpec.KEEP_INVENTORY_IN_EXPLORATION.get());
            BrecherConfig.setDeferToCorpseMods(BrecherConfigSpec.DEFER_TO_CORPSE_MODS.get());
            BrecherConfig.setDisableModdedPortals(BrecherConfigSpec.DISABLE_MODDED_PORTALS.get());
            BrecherConfig.setPreventModdedTeleports(BrecherConfigSpec.PREVENT_MODDED_TELEPORTS.get());
            BrecherConfig.setDisableEndGateways(BrecherConfigSpec.DISABLE_END_GATEWAYS.get());
            
            // Client settings
            BrecherConfig.setCleanupXaeroMapData(BrecherConfigSpec.CLEANUP_XAERO_MAP_DATA.get());
            BrecherConfig.setXaeroCleanupTargets(new ArrayList<>(BrecherConfigSpec.XAERO_CLEANUP_TARGETS.get()));
            
            // Gameplay settings
            BrecherConfig.setTeleportCooldown(BrecherConfigSpec.TELEPORT_COOLDOWN.get());
            BrecherConfig.setRestrictToCurrentDimension(BrecherConfigSpec.RESTRICT_TO_CURRENT_DIMENSION.get());
            
            // Performance settings
            BrecherConfig.setChunkUnloadDelay(BrecherConfigSpec.CHUNK_UNLOAD_DELAY.get());
            BrecherConfig.setMaxChunksPerPlayer(BrecherConfigSpec.MAX_CHUNKS_PER_PLAYER.get());
            BrecherConfig.setAggressiveChunkUnloading(BrecherConfigSpec.AGGRESSIVE_CHUNK_UNLOADING.get());
            BrecherConfig.setEntityCleanupInterval(BrecherConfigSpec.ENTITY_CLEANUP_INTERVAL.get());
            BrecherConfig.setChunkCleanupInterval(BrecherConfigSpec.CHUNK_CLEANUP_INTERVAL.get());
            BrecherConfig.setPreventDiskSaves(BrecherConfigSpec.PREVENT_DISK_SAVES.get());
            BrecherConfig.setOldDimensionRetentionCount(BrecherConfigSpec.OLD_DIMENSION_RETENTION_COUNT.get());
            
            // Chunk pre-generation settings
            BrecherConfig.setPreGenerateSpawnChunks(BrecherConfigSpec.PRE_GENERATE_SPAWN_CHUNKS.get());
            BrecherConfig.setImmediateSpawnRadius(BrecherConfigSpec.IMMEDIATE_SPAWN_RADIUS.get());
            BrecherConfig.setExtendedSpawnRadius(BrecherConfigSpec.EXTENDED_SPAWN_RADIUS.get());
            
            // Safety settings
            BrecherConfig.setTeleportSafetyRadius(BrecherConfigSpec.TELEPORT_SAFETY_RADIUS.get());
            BrecherConfig.setCreateEmergencyPlatforms(BrecherConfigSpec.CREATE_EMERGENCY_PLATFORMS.get());
            BrecherConfig.setPreferSurfaceSpawns(BrecherConfigSpec.PREFER_SURFACE_SPAWNS.get());
            
            // Messages
            BrecherConfig.setWelcomeMessage(BrecherConfigSpec.WELCOME_MESSAGE.get());
            BrecherConfig.setReturnMessage(BrecherConfigSpec.RETURN_MESSAGE.get());
            
            LOGGER.debug("Config values synced successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to sync config values", e);
        }
    }
    
    /**
     * Reload config values
     */
    public static void reload() {
        LOGGER.info("Manually reloading Brecher's Exploration Dimensions config");
        syncConfigValues();
    }
}