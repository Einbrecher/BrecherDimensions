package net.tinkstav.brecher_dim.config;

import com.mojang.logging.LogUtils;
import dev.architectury.injectables.annotations.ExpectPlatform;
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
    private static boolean preventBedSpawn = BrecherConfigSpec.Defaults.PREVENT_BED_SPAWN;
    private static boolean disableEnderChests = BrecherConfigSpec.Defaults.DISABLE_ENDER_CHESTS;
    private static boolean clearInventoryOnReturn = BrecherConfigSpec.Defaults.CLEAR_INVENTORY_ON_RETURN;
    private static boolean disableModdedPortals = BrecherConfigSpec.Defaults.DISABLE_MODDED_PORTALS;
    private static boolean preventModdedTeleports = BrecherConfigSpec.Defaults.PREVENT_MODDED_TELEPORTS;
    
    // Gameplay settings
    private static int teleportCooldown = BrecherConfigSpec.Defaults.TELEPORT_COOLDOWN;
    private static boolean restrictToCurrentDimension = BrecherConfigSpec.Defaults.RESTRICT_TO_CURRENT_DIMENSION;
    
    // Performance settings
    private static int chunkUnloadDelay = BrecherConfigSpec.Defaults.CHUNK_UNLOAD_DELAY;
    private static int maxChunksPerPlayer = BrecherConfigSpec.Defaults.MAX_CHUNKS_PER_PLAYER;
    private static boolean aggressiveChunkUnloading = BrecherConfigSpec.Defaults.AGGRESSIVE_CHUNK_UNLOADING;
    private static int entityCleanupInterval = BrecherConfigSpec.Defaults.ENTITY_CLEANUP_INTERVAL;
    private static boolean preventDiskSaves = BrecherConfigSpec.Defaults.PREVENT_DISK_SAVES;
    private static int oldDimensionRetentionCount = BrecherConfigSpec.Defaults.OLD_DIMENSION_RETENTION_COUNT;
    
    // Chunk pre-generation settings
    private static boolean preGenerateSpawnChunks = BrecherConfigSpec.Defaults.PRE_GENERATE_SPAWN_CHUNKS;
    private static int immediateSpawnRadius = BrecherConfigSpec.Defaults.IMMEDIATE_SPAWN_RADIUS;
    private static int extendedSpawnRadius = BrecherConfigSpec.Defaults.EXTENDED_SPAWN_RADIUS;
    
    // Safety settings
    private static int teleportSafetyRadius = BrecherConfigSpec.Defaults.TELEPORT_SAFETY_RADIUS;
    private static boolean createEmergencyPlatforms = BrecherConfigSpec.Defaults.CREATE_EMERGENCY_PLATFORMS;
    
    // Messages
    private static String welcomeMessage = BrecherConfigSpec.Defaults.WELCOME_MESSAGE;
    private static String returnMessage = BrecherConfigSpec.Defaults.RETURN_MESSAGE;
    
    /**
     * Initialize config - called by platform-specific implementations
     */
    @ExpectPlatform
    public static void init() {
        throw new AssertionError("Platform implementation missing");
    }
    
    /**
     * Reload config values - called by platform-specific implementations
     */
    @ExpectPlatform
    public static void reload() {
        throw new AssertionError("Platform implementation missing");
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
    public static boolean isPreventBedSpawn() { return preventBedSpawn; }
    public static boolean isDisableEnderChests() { return disableEnderChests; }
    public static boolean isClearInventoryOnReturn() { return clearInventoryOnReturn; }
    public static boolean isDisableModdedPortals() { return disableModdedPortals; }
    public static boolean isPreventModdedTeleports() { return preventModdedTeleports; }
    public static int getTeleportCooldown() { return teleportCooldown; }
    public static boolean isRestrictToCurrentDimension() { return restrictToCurrentDimension; }
    public static int getChunkUnloadDelay() { return chunkUnloadDelay; }
    public static int getMaxChunksPerPlayer() { return maxChunksPerPlayer; }
    public static boolean isAggressiveChunkUnloading() { return aggressiveChunkUnloading; }
    public static int getEntityCleanupInterval() { return entityCleanupInterval; }
    public static boolean isPreventDiskSaves() { return preventDiskSaves; }
    public static int getOldDimensionRetentionCount() { return oldDimensionRetentionCount; }
    public static boolean isPreGenerateSpawnChunks() { return preGenerateSpawnChunks; }
    public static int getImmediateSpawnRadius() { return immediateSpawnRadius; }
    public static int getExtendedSpawnRadius() { return extendedSpawnRadius; }
    public static int getTeleportSafetyRadius() { return teleportSafetyRadius; }
    public static boolean isCreateEmergencyPlatforms() { return createEmergencyPlatforms; }
    public static String getWelcomeMessage() { return welcomeMessage; }
    public static String getReturnMessage() { return returnMessage; }
    
    // Package-private setters for platform-specific implementations
    static void setExplorationBorder(int value) { explorationBorder = value; }
    static void setSeedStrategy(String value) { seedStrategy = value; }
    static void setDebugSeed(long value) { debugSeed = value; }
    static void setEnabledDimensions(List<String> value) { enabledDimensions = value; }
    static void setBlacklist(List<String> value) { blacklist = value; }
    static void setAllowModdedDimensions(boolean value) { allowModdedDimensions = value; }
    static void setPreventBedSpawn(boolean value) { preventBedSpawn = value; }
    static void setDisableEnderChests(boolean value) { disableEnderChests = value; }
    static void setClearInventoryOnReturn(boolean value) { clearInventoryOnReturn = value; }
    static void setDisableModdedPortals(boolean value) { disableModdedPortals = value; }
    static void setPreventModdedTeleports(boolean value) { preventModdedTeleports = value; }
    static void setTeleportCooldown(int value) { teleportCooldown = value; }
    static void setRestrictToCurrentDimension(boolean value) { restrictToCurrentDimension = value; }
    static void setChunkUnloadDelay(int value) { chunkUnloadDelay = value; }
    static void setMaxChunksPerPlayer(int value) { maxChunksPerPlayer = value; }
    static void setAggressiveChunkUnloading(boolean value) { aggressiveChunkUnloading = value; }
    static void setEntityCleanupInterval(int value) { entityCleanupInterval = value; }
    static void setPreventDiskSaves(boolean value) { preventDiskSaves = value; }
    static void setOldDimensionRetentionCount(int value) { oldDimensionRetentionCount = value; }
    static void setPreGenerateSpawnChunks(boolean value) { preGenerateSpawnChunks = value; }
    static void setImmediateSpawnRadius(int value) { immediateSpawnRadius = value; }
    static void setExtendedSpawnRadius(int value) { extendedSpawnRadius = value; }
    static void setTeleportSafetyRadius(int value) { teleportSafetyRadius = value; }
    static void setCreateEmergencyPlatforms(boolean value) { createEmergencyPlatforms = value; }
    static void setWelcomeMessage(String value) { welcomeMessage = value; }
    static void setReturnMessage(String value) { returnMessage = value; }
}