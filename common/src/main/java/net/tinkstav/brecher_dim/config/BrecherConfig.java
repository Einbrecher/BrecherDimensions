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

package net.tinkstav.brecher_dim.config;

import com.mojang.logging.LogUtils;
import net.tinkstav.brecher_dim.platform.Services;
import org.slf4j.Logger;

import java.util.List;

/**
 * Platform-agnostic config holder.
 * Values are populated by platform-specific implementations.
 */
public class BrecherConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // General settings
    private static int explorationBorder = BrecherConfigSpec.Defaults.EXPLORATION_BORDER;
    
    // Seed settings
    private static String seedStrategy = BrecherConfigSpec.Defaults.SEED_STRATEGY;
    private static long debugSeed = BrecherConfigSpec.Defaults.DEBUG_SEED;
    
    // Dimension settings
    private static List<String> enabledDimensions = BrecherConfigSpec.Defaults.ENABLED_DIMENSIONS;
    private static List<String> blacklist = BrecherConfigSpec.Defaults.BLACKLIST;
    private static boolean allowModdedDimensions = BrecherConfigSpec.Defaults.ALLOW_MODDED_DIMENSIONS;
    
    // Feature settings
    private static boolean preventExplorationSpawnSetting = BrecherConfigSpec.Defaults.PREVENT_EXPLORATION_SPAWN_SETTING;
    private static boolean disableEnderChests = BrecherConfigSpec.Defaults.DISABLE_ENDER_CHESTS;
    private static boolean clearInventoryOnReturn = BrecherConfigSpec.Defaults.CLEAR_INVENTORY_ON_RETURN;
    private static boolean keepInventoryInExploration = BrecherConfigSpec.Defaults.KEEP_INVENTORY_IN_EXPLORATION;
    private static boolean deferToCorpseMods = BrecherConfigSpec.Defaults.DEFER_TO_CORPSE_MODS;
    private static boolean disableModdedPortals = BrecherConfigSpec.Defaults.DISABLE_MODDED_PORTALS;
    private static boolean preventModdedTeleports = BrecherConfigSpec.Defaults.PREVENT_MODDED_TELEPORTS;
    private static boolean cleanupXaeroMapData = BrecherConfigSpec.Defaults.CLEANUP_XAERO_MAP_DATA;
    private static List<String> xaeroCleanupTargets = BrecherConfigSpec.Defaults.XAERO_CLEANUP_TARGETS;
    private static boolean disableEndGateways = BrecherConfigSpec.Defaults.DISABLE_END_GATEWAYS;
    
    // Gameplay settings
    private static int teleportCooldown = BrecherConfigSpec.Defaults.TELEPORT_COOLDOWN;
    private static boolean restrictToCurrentDimension = BrecherConfigSpec.Defaults.RESTRICT_TO_CURRENT_DIMENSION;
    
    // Performance settings
    private static int chunkUnloadDelay = BrecherConfigSpec.Defaults.CHUNK_UNLOAD_DELAY;
    private static int maxChunksPerPlayer = BrecherConfigSpec.Defaults.MAX_CHUNKS_PER_PLAYER;
    private static boolean aggressiveChunkUnloading = BrecherConfigSpec.Defaults.AGGRESSIVE_CHUNK_UNLOADING;
    private static int entityCleanupInterval = BrecherConfigSpec.Defaults.ENTITY_CLEANUP_INTERVAL;
    private static int chunkCleanupInterval = BrecherConfigSpec.Defaults.CHUNK_CLEANUP_INTERVAL;
    private static boolean preventDiskSaves = BrecherConfigSpec.Defaults.PREVENT_DISK_SAVES;
    private static int oldDimensionRetentionCount = BrecherConfigSpec.Defaults.OLD_DIMENSION_RETENTION_COUNT;
    
    // Chunk pre-generation settings
    private static boolean preGenerateSpawnChunks = BrecherConfigSpec.Defaults.PRE_GENERATE_SPAWN_CHUNKS;
    private static int immediateSpawnRadius = BrecherConfigSpec.Defaults.IMMEDIATE_SPAWN_RADIUS;
    private static int extendedSpawnRadius = BrecherConfigSpec.Defaults.EXTENDED_SPAWN_RADIUS;
    
    // Safety settings
    private static int teleportSafetyRadius = BrecherConfigSpec.Defaults.TELEPORT_SAFETY_RADIUS;
    private static boolean createEmergencyPlatforms = BrecherConfigSpec.Defaults.CREATE_EMERGENCY_PLATFORMS;
    private static boolean preferSurfaceSpawns = BrecherConfigSpec.Defaults.PREFER_SURFACE_SPAWNS;
    private static boolean extendedSearchRadius = BrecherConfigSpec.Defaults.EXTENDED_SEARCH_RADIUS;
    
    // Messages
    private static String welcomeMessage = BrecherConfigSpec.Defaults.WELCOME_MESSAGE;
    private static String returnMessage = BrecherConfigSpec.Defaults.RETURN_MESSAGE;
    
    /**
     * Initialize config - delegates to platform-specific implementation
     */
    public static void init() {
        Services.CONFIG.init();
        validateConfig();
    }
    
    /**
     * Reload config values - delegates to platform-specific implementation
     */
    public static void reload() {
        Services.CONFIG.reload();
        validateConfig();
    }
    
    /**
     * Validate configuration values and log warnings for potentially problematic settings
     */
    public static void validateConfig() {
        // Warn about extremely large world borders
        if (explorationBorder > 10000000 && explorationBorder != -1) {
            LOGGER.warn(
                "Exploration border is set to {} which is extremely large and may cause performance issues", 
                explorationBorder
            );
        }
        
        // Warn if no dimensions are enabled
        if (enabledDimensions.isEmpty()) {
            LOGGER.warn(
                "No dimensions are enabled for exploration! The mod will not create any exploration dimensions."
            );
        }
        
        // Warn about performance settings
        if (!aggressiveChunkUnloading && maxChunksPerPlayer > 81) {
            LOGGER.warn(
                "Aggressive chunk unloading is disabled but max chunks per player is high ({}). " +
                "This may cause memory issues with many players.", 
                maxChunksPerPlayer
            );
        }
        
        // Validate seed strategy
        String strategy = seedStrategy.toLowerCase();
        if (!strategy.equals("random") && !strategy.equals("date-based") && !strategy.equals("date")) {
            LOGGER.error(
                "Invalid seed strategy '{}'. Using 'random' instead.", strategy
            );
            seedStrategy = "random";
        }
        
        // Validate chunk pre-generation settings
        if (immediateSpawnRadius > extendedSpawnRadius) {
            LOGGER.warn(
                "Immediate spawn radius ({}) is larger than extended spawn radius ({}). " +
                "This configuration doesn't make sense.", 
                immediateSpawnRadius, extendedSpawnRadius
            );
        }
        
        if (preGenerateSpawnChunks && enabledDimensions.size() > 10) {
            LOGGER.warn(
                "Chunk pre-generation is enabled with {} dimensions. " +
                "This may significantly increase server startup time.", 
                enabledDimensions.size()
            );
        }
    }
    
    // Getters for config values
    public static int getExplorationBorder() { return explorationBorder; }
    public static String getSeedStrategy() { return seedStrategy; }
    public static long getDebugSeed() { return debugSeed; }
    public static List<String> getEnabledDimensions() { return enabledDimensions; }
    public static List<String> getBlacklist() { return blacklist; }
    public static boolean isAllowModdedDimensions() { return allowModdedDimensions; }
    public static boolean isPreventExplorationSpawnSetting() { return preventExplorationSpawnSetting; }
    public static boolean isDisableEnderChests() { return disableEnderChests; }
    public static boolean isClearInventoryOnReturn() { return clearInventoryOnReturn; }
    public static boolean isKeepInventoryInExploration() { return keepInventoryInExploration; }
    public static boolean isDeferToCorpseMods() { return deferToCorpseMods; }
    public static boolean isDisableModdedPortals() { return disableModdedPortals; }
    public static boolean isPreventModdedTeleports() { return preventModdedTeleports; }
    public static boolean isCleanupXaeroMapData() { return cleanupXaeroMapData; }
    public static List<String> getXaeroCleanupTargets() { return xaeroCleanupTargets; }
    public static boolean isDisableEndGateways() { return disableEndGateways; }
    public static int getTeleportCooldown() { return teleportCooldown; }
    public static boolean isRestrictToCurrentDimension() { return restrictToCurrentDimension; }
    public static int getChunkUnloadDelay() { return chunkUnloadDelay; }
    public static int getMaxChunksPerPlayer() { return maxChunksPerPlayer; }
    public static boolean isAggressiveChunkUnloading() { return aggressiveChunkUnloading; }
    public static int getEntityCleanupInterval() { return entityCleanupInterval; }
    public static int getChunkCleanupInterval() { return chunkCleanupInterval; }
    public static boolean isPreventDiskSaves() { return preventDiskSaves; }
    public static int getOldDimensionRetentionCount() { return oldDimensionRetentionCount; }
    public static boolean isPreGenerateSpawnChunks() { return preGenerateSpawnChunks; }
    public static int getImmediateSpawnRadius() { return immediateSpawnRadius; }
    public static int getExtendedSpawnRadius() { return extendedSpawnRadius; }
    public static int getTeleportSafetyRadius() { return teleportSafetyRadius; }
    public static boolean isCreateEmergencyPlatforms() { return createEmergencyPlatforms; }
    public static boolean isPreferSurfaceSpawns() { return preferSurfaceSpawns; }
    public static boolean isExtendedSearchRadius() { return extendedSearchRadius; }
    public static String getWelcomeMessage() { return welcomeMessage; }
    public static String getReturnMessage() { return returnMessage; }
    
    // Public setters for platform-specific implementations
    public static void setExplorationBorder(int value) { explorationBorder = value; }
    public static void setSeedStrategy(String value) { seedStrategy = value; }
    public static void setDebugSeed(long value) { debugSeed = value; }
    public static void setEnabledDimensions(List<String> value) { enabledDimensions = value; }
    public static void setBlacklist(List<String> value) { blacklist = value; }
    public static void setAllowModdedDimensions(boolean value) { allowModdedDimensions = value; }
    public static void setPreventExplorationSpawnSetting(boolean value) { preventExplorationSpawnSetting = value; }
    public static void setDisableEnderChests(boolean value) { disableEnderChests = value; }
    public static void setClearInventoryOnReturn(boolean value) { clearInventoryOnReturn = value; }
    public static void setKeepInventoryInExploration(boolean value) { keepInventoryInExploration = value; }
    public static void setDeferToCorpseMods(boolean value) { deferToCorpseMods = value; }
    public static void setDisableModdedPortals(boolean value) { disableModdedPortals = value; }
    public static void setPreventModdedTeleports(boolean value) { preventModdedTeleports = value; }
    public static void setCleanupXaeroMapData(boolean value) { cleanupXaeroMapData = value; }
    public static void setXaeroCleanupTargets(List<String> value) { xaeroCleanupTargets = value; }
    public static void setDisableEndGateways(boolean value) { disableEndGateways = value; }
    public static void setTeleportCooldown(int value) { teleportCooldown = value; }
    public static void setRestrictToCurrentDimension(boolean value) { restrictToCurrentDimension = value; }
    public static void setChunkUnloadDelay(int value) { chunkUnloadDelay = value; }
    public static void setMaxChunksPerPlayer(int value) { maxChunksPerPlayer = value; }
    public static void setAggressiveChunkUnloading(boolean value) { aggressiveChunkUnloading = value; }
    public static void setEntityCleanupInterval(int value) { entityCleanupInterval = value; }
    public static void setChunkCleanupInterval(int value) { chunkCleanupInterval = value; }
    public static void setPreventDiskSaves(boolean value) { preventDiskSaves = value; }
    public static void setOldDimensionRetentionCount(int value) { oldDimensionRetentionCount = value; }
    public static void setPreGenerateSpawnChunks(boolean value) { preGenerateSpawnChunks = value; }
    public static void setImmediateSpawnRadius(int value) { immediateSpawnRadius = value; }
    public static void setExtendedSpawnRadius(int value) { extendedSpawnRadius = value; }
    public static void setTeleportSafetyRadius(int value) { teleportSafetyRadius = value; }
    public static void setCreateEmergencyPlatforms(boolean value) { createEmergencyPlatforms = value; }
    public static void setPreferSurfaceSpawns(boolean value) { preferSurfaceSpawns = value; }
    public static void setExtendedSearchRadius(boolean value) { extendedSearchRadius = value; }
    public static void setWelcomeMessage(String value) { welcomeMessage = value; }
    public static void setReturnMessage(String value) { returnMessage = value; }
}