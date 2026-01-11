/*
 * Brecher's Dimensions - Temporary resettable dimensions for exploration
 * Copyright (C) 2025 Einbrecher. All rights reserved.
 */

package net.tinkstav.brecher_dim.config;

import com.mojang.logging.LogUtils;
import net.tinkstav.brecher_dim.platform.Services;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    private static String weeklyResetDay = BrecherConfigSpec.Defaults.WEEKLY_RESET_DAY;
    
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

    // Dimension locks settings (Progression Gating)
    private static boolean dimensionLocksEnabled = BrecherConfigSpec.Defaults.DIMENSION_LOCKS_ENABLED;
    private static Map<String, String> dimensionLocks = new HashMap<>(BrecherConfigSpec.Defaults.DIMENSION_LOCKS);

    // Performance settings
    private static int chunkUnloadDelay = BrecherConfigSpec.Defaults.CHUNK_UNLOAD_DELAY;
    private static int maxChunksPerPlayer = BrecherConfigSpec.Defaults.MAX_CHUNKS_PER_PLAYER;
    private static boolean aggressiveChunkUnloading = BrecherConfigSpec.Defaults.AGGRESSIVE_CHUNK_UNLOADING;
    private static int entityCleanupInterval = BrecherConfigSpec.Defaults.ENTITY_CLEANUP_INTERVAL;
    private static int chunkCleanupInterval = BrecherConfigSpec.Defaults.CHUNK_CLEANUP_INTERVAL;
    private static boolean preventDiskSaves = BrecherConfigSpec.Defaults.PREVENT_DISK_SAVES;
    private static int oldDimensionRetentionCount = BrecherConfigSpec.Defaults.OLD_DIMENSION_RETENTION_COUNT;
    
    // Chunk pre-generation settings (spawn)
    private static boolean preGenerateSpawnChunks = BrecherConfigSpec.Defaults.PRE_GENERATE_SPAWN_CHUNKS;
    private static int immediateSpawnRadius = BrecherConfigSpec.Defaults.IMMEDIATE_SPAWN_RADIUS;
    private static int extendedSpawnRadius = BrecherConfigSpec.Defaults.EXTENDED_SPAWN_RADIUS;
    
    // Chunk pre-generation settings (background)
    private static boolean pregenEnabled = BrecherConfigSpec.Defaults.PREGEN_ENABLED;
    private static int pregenChunksPerTick = BrecherConfigSpec.Defaults.PREGEN_CHUNKS_PER_TICK;
    private static int pregenTickInterval = BrecherConfigSpec.Defaults.PREGEN_TICK_INTERVAL;
    private static int pregenTicksPerChunk = BrecherConfigSpec.Defaults.PREGEN_TICKS_PER_CHUNK;
    private static int pregenTicketDuration = BrecherConfigSpec.Defaults.PREGEN_TICKET_DURATION;
    private static boolean pregenAutoStart = BrecherConfigSpec.Defaults.PREGEN_AUTO_START;
    private static boolean pregenAutoResume = BrecherConfigSpec.Defaults.PREGEN_AUTO_RESUME;
    private static int pregenMinTPS = BrecherConfigSpec.Defaults.PREGEN_MIN_TPS;
    private static int pregenMemoryThreshold = BrecherConfigSpec.Defaults.PREGEN_MEMORY_THRESHOLD;
    private static int pregenDefaultRadius = BrecherConfigSpec.Defaults.PREGEN_DEFAULT_RADIUS;
    private static boolean pregenPauseWithPlayers = BrecherConfigSpec.Defaults.PREGEN_PAUSE_WITH_PLAYERS;
    private static int pregenStaleHours = BrecherConfigSpec.Defaults.PREGEN_STALE_HOURS;
    private static int pregenMaxTickMs = BrecherConfigSpec.Defaults.PREGEN_MAX_TICK_MS;
    private static int pregenLogInterval = BrecherConfigSpec.Defaults.PREGEN_LOG_INTERVAL;

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
        if (!strategy.equals("random") && !strategy.equals("date-based") && 
            !strategy.equals("date") && !strategy.equals("weekly")) {
            LOGGER.error(
                "Invalid seed strategy '{}'. Using 'random' instead.", strategy
            );
            seedStrategy = "random";
        }
        
        // Validate weekly reset day if using weekly strategy
        if (strategy.equals("weekly")) {
            try {
                java.time.DayOfWeek.valueOf(weeklyResetDay.toUpperCase());
            } catch (IllegalArgumentException e) {
                LOGGER.error("Invalid weekly reset day '{}'. Using MONDAY.", weeklyResetDay);
                weeklyResetDay = "MONDAY";
            }
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
        
        // Validate background pre-generation settings
        if (pregenEnabled) {
            // Note: 0 is valid for pregenChunksPerTick - it enables fractional-rate mode
            // where ticksPerChunk controls the rate instead (e.g., 1 chunk per 2 ticks)
            if (pregenChunksPerTick < 0 || pregenChunksPerTick > 10) {
                LOGGER.warn("pregenChunksPerTick should be between 0 and 10 (0 = use ticksPerChunk). Current value: {}",
                    pregenChunksPerTick);
                pregenChunksPerTick = Math.max(0, Math.min(10, pregenChunksPerTick));
            }

            // Validate ticksPerChunk (used when chunksPerTick=0 for fractional rates)
            if (pregenTicksPerChunk < 1 || pregenTicksPerChunk > 100) {
                LOGGER.warn("pregenTicksPerChunk should be between 1 and 100. Current value: {}",
                    pregenTicksPerChunk);
                pregenTicksPerChunk = Math.max(1, Math.min(100, pregenTicksPerChunk));
            }

            if (pregenTickInterval < 1 || pregenTickInterval > 200) {
                LOGGER.warn("pregenTickInterval should be between 1 and 200. Current value: {}",
                    pregenTickInterval);
                pregenTickInterval = Math.max(1, Math.min(200, pregenTickInterval));
            }

            if (pregenMinTPS < 10 || pregenMinTPS > 20) {
                LOGGER.warn("pregenMinTPS should be between 10 and 20. Current value: {}",
                    pregenMinTPS);
                pregenMinTPS = Math.max(10, Math.min(20, pregenMinTPS));
            }

            if (pregenMemoryThreshold < 50 || pregenMemoryThreshold > 95) {
                LOGGER.warn("pregenMemoryThreshold should be between 50 and 95. Current value: {}",
                    pregenMemoryThreshold);
                pregenMemoryThreshold = Math.max(50, Math.min(95, pregenMemoryThreshold));
            }
        }
    }
    
    // Getters for config values
    public static int getExplorationBorder() { return explorationBorder; }
    public static String getSeedStrategy() { return seedStrategy; }
    public static long getDebugSeed() { return debugSeed; }
    public static String getWeeklyResetDay() { return weeklyResetDay; }
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
    public static boolean isDimensionLocksEnabled() { return dimensionLocksEnabled; }
    public static Map<String, String> getDimensionLocks() { return Collections.unmodifiableMap(dimensionLocks); }
    /**
     * Get the required advancement for a specific dimension.
     * @param dimension The dimension ID (e.g., "minecraft:the_end")
     * @return Optional containing the required advancement ID, or empty if no lock
     */
    public static Optional<String> getDimensionLock(String dimension) {
        return Optional.ofNullable(dimensionLocks.get(dimension));
    }
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
    public static boolean isPregenEnabled() { return pregenEnabled; }
    public static int getPregenChunksPerTick() { return pregenChunksPerTick; }
    public static int getPregenTickInterval() { return pregenTickInterval; }
    public static int getPregenTicksPerChunk() { return pregenTicksPerChunk; }
    public static int getPregenTicketDuration() { return pregenTicketDuration; }
    public static boolean isPregenAutoStart() { return pregenAutoStart; }
    public static boolean isPregenAutoResume() { return pregenAutoResume; }
    public static int getPregenMinTPS() { return pregenMinTPS; }
    public static int getPregenMemoryThreshold() { return pregenMemoryThreshold; }
    public static int getPregenDefaultRadius() { return pregenDefaultRadius; }
    public static boolean isPregenPauseWithPlayers() { return pregenPauseWithPlayers; }
    public static int getPregenStaleHours() { return pregenStaleHours; }
    public static int getPregenMaxTickMs() { return pregenMaxTickMs; }
    public static int getPregenLogInterval() { return pregenLogInterval; }
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
    public static void setWeeklyResetDay(String value) { weeklyResetDay = value; }
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
    public static void setDimensionLocksEnabled(boolean value) { dimensionLocksEnabled = value; }
    public static void setDimensionLocks(Map<String, String> value) { dimensionLocks = new HashMap<>(value); }

    /**
     * Validate and set dimension locks from config. Invalid entries are skipped with a warning.
     * @param rawLocks Raw map from config
     */
    public static void setDimensionLocksValidated(Map<String, String> rawLocks) {
        Map<String, String> validatedLocks = new HashMap<>();
        for (Map.Entry<String, String> entry : rawLocks.entrySet()) {
            String dimension = entry.getKey().trim();
            String advancement = entry.getValue().trim();

            if (!isValidResourceLocation(dimension)) {
                LOGGER.warn("Invalid dimension lock: dimension '{}' is not a valid ResourceLocation format", dimension);
                continue;
            }
            if (!isValidResourceLocation(advancement)) {
                LOGGER.warn("Invalid dimension lock: advancement '{}' for dimension '{}' is not a valid ResourceLocation format",
                    advancement, dimension);
                continue;
            }
            validatedLocks.put(dimension, advancement);
        }
        dimensionLocks = validatedLocks;
        LOGGER.debug("Loaded {} dimension locks", validatedLocks.size());
    }

    /**
     * Check if a string is a valid ResourceLocation format (namespace:path)
     */
    private static boolean isValidResourceLocation(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        int colonIndex = value.indexOf(':');
        if (colonIndex < 1) {
            return false; // No colon or colon at start
        }
        String namespace = value.substring(0, colonIndex);
        String path = value.substring(colonIndex + 1);
        // Namespace: [a-z0-9_.-]
        // Path: [a-z0-9/._-]
        return namespace.matches("[a-z0-9_.-]+") && path.matches("[a-z0-9/._-]+");
    }
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
    public static void setPregenEnabled(boolean value) { pregenEnabled = value; }
    public static void setPregenChunksPerTick(int value) { pregenChunksPerTick = value; }
    public static void setPregenTickInterval(int value) { pregenTickInterval = value; }
    public static void setPregenTicksPerChunk(int value) { pregenTicksPerChunk = Math.max(1, value); }
    public static void setPregenTicketDuration(int value) { pregenTicketDuration = value; }
    public static void setPregenAutoStart(boolean value) { pregenAutoStart = value; }
    public static void setPregenAutoResume(boolean value) { pregenAutoResume = value; }
    public static void setPregenMinTPS(int value) { pregenMinTPS = value; }
    public static void setPregenMemoryThreshold(int value) { pregenMemoryThreshold = value; }
    public static void setPregenDefaultRadius(int value) { pregenDefaultRadius = value; }
    public static void setPregenPauseWithPlayers(boolean value) { pregenPauseWithPlayers = value; }
    public static void setPregenStaleHours(int value) { pregenStaleHours = value; }
    public static void setPregenMaxTickMs(int value) { pregenMaxTickMs = Math.max(1, Math.min(50, value)); }
    public static void setPregenLogInterval(int value) { pregenLogInterval = Math.max(10, Math.min(10000, value)); }
    public static void setTeleportSafetyRadius(int value) { teleportSafetyRadius = value; }
    public static void setCreateEmergencyPlatforms(boolean value) { createEmergencyPlatforms = value; }
    public static void setPreferSurfaceSpawns(boolean value) { preferSurfaceSpawns = value; }
    public static void setExtendedSearchRadius(boolean value) { extendedSearchRadius = value; }
    public static void setWelcomeMessage(String value) { welcomeMessage = value; }
    public static void setReturnMessage(String value) { returnMessage = value; }
}