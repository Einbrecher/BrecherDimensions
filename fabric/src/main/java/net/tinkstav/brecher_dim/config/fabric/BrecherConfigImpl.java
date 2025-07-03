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

package net.tinkstav.brecher_dim.config.fabric;

import com.mojang.logging.LogUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.tinkstav.brecher_dim.config.BrecherConfig;
import net.tinkstav.brecher_dim.config.BrecherConfigSpec;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Fabric implementation using properties file with comments
 */
public class BrecherConfigImpl {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String CONFIG_FILE = "brecher_exploration.properties";
    private static Path configPath;
    
    /**
     * Initialize config system
     */
    public static void init() {
        LOGGER.info("Initializing Brecher's Exploration Dimensions config for Fabric");
        
        // Get config directory
        Path configDir = FabricLoader.getInstance().getConfigDir();
        configPath = configDir.resolve(CONFIG_FILE);
        
        // Create config if it doesn't exist
        if (!Files.exists(configPath)) {
            createDefaultConfig();
        }
        
        // Load config
        loadConfig();
    }
    
    /**
     * Reload config values
     */
    public static void reload() {
        LOGGER.info("Reloading Brecher's Exploration Dimensions config for Fabric");
        loadConfig();
    }
    
    /**
     * Create default config file with comments
     */
    private static void createDefaultConfig() {
        try {
            StringWriter writer = new StringWriter();
            
            // Header
            writer.write("# Brecher's Exploration Dimensions\n");
            writer.write("# Created: " + new Date() + "\n\n");
            
            // Server Settings  
            writer.write("#=== SERVER SETTINGS ===\n\n");
            
            writer.write("# General\n");
            writer.write("explorationBorder=-1  # Border size (-1 for parent)\n\n");
            
            writer.write("# Seeds\n");
            writer.write("seedStrategy=RANDOM      # Seed generation strategy\n");
            writer.write("debugSeed=-1            # Fixed seed for DEBUG mode\n\n");
            
            writer.write("# Dimensions\n");
            writer.write("enabledDimensions=minecraft:overworld,minecraft:the_nether,minecraft:the_end\n");
            writer.write("blacklist=              # Excluded dimensions\n");
            writer.write("allowModdedDimensions=false\n\n");
            
            writer.write("# Features\n");
            writer.write("preventExplorationSpawnSetting=true # Prevent spawn point setting in exploration dimensions\n");
            writer.write("disableEnderChests=false     # Disable ender chests\n");
            writer.write("clearInventoryOnReturn=false # Clear inventory on return\n");
            writer.write("keepInventoryInExploration=true # Keep inventory on death in exploration dimensions\n");
            writer.write("deferToCorpseMods=true       # Let corpse/gravestone mods handle deaths\n");
            writer.write("disableModdedPortals=true    # Disable modded portals\n");
            writer.write("preventModdedTeleports=false # Block modded teleports\n");
            writer.write("disableEndGateways=false     # Disable End Gateways\n\n");
            
            writer.write("# Gameplay\n");
            writer.write("teleportCooldown=10          # Teleport cooldown (seconds)\n");
            writer.write("restrictToCurrentDimension=false  # Restrict Exploration teleports to like dimensions only (e.g., Overworld -> Exploration Overworld, Nether -> Exploration Nether)\n\n");
            
            writer.write("# Performance\n");
            writer.write("chunkUnloadDelay=300          # Chunk unload delay (ticks)\n");
            writer.write("maxChunksPerPlayer=100        # Max chunks per player\n");
            writer.write("aggressiveChunkUnloading=true # Aggressive chunk unloading\n");
            writer.write("entityCleanupInterval=1200    # Entity cleanup interval (ticks)\n");
            writer.write("chunkCleanupInterval=200      # Chunk cleanup interval (ticks)\n");
            writer.write("preventDiskSaves=false        # Skip saving chunks to disk. (If true, Exploration chunks will NOT persist once unloaded.)\n");
            writer.write("oldDimensionRetentionCount=2  # Old dimension folders to keep per dimension type (e.g., 2 = keep last 2 overworld, last 2 nether, last 2 end)\n\n");
            
            writer.write("# Chunk Pre-generation\n");
            writer.write("preGenerateSpawnChunks=true   # Pre-generate spawn chunks\n");
            writer.write("immediateSpawnRadius=3        # Immediate spawn radius (chunks)\n");
            writer.write("extendedSpawnRadius=5         # Extended spawn radius (chunks)\n\n");
            
            writer.write("# Safety\n");
            writer.write("teleportSafetyRadius=16       # Safe teleport search radius\n");
            writer.write("createEmergencyPlatforms=true # Create emergency platforms\n");
            writer.write("preferSurfaceSpawns=true      # Prefer surface spawns over caves\n\n");
            
            writer.write("# Messages\n");
            writer.write("welcomeMessage=Welcome to the Exploration Dimension! This is a temporary dimension that resets with each server restart. If you're still here when the server restarts, you'll be returned to your departure point or the world spawn.\n");
            writer.write("returnMessage=Welcome back to the persistent world!\n\n");
            
            writer.write("#=== CLIENT SETTINGS ===\n\n");
            
            writer.write("cleanupXaeroMapData=false     # Auto-clean Xaero map data\n");
            writer.write("xaeroCleanupTargets=          # Specific Xaero server directories to clean (empty = all). Example: Multiplayer_192.168.1.16,Singleplayer_MyWorld\n");
            
            // Write to file
            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, writer.toString());
            
            LOGGER.info("Created default config file at {}", configPath);
        } catch (IOException e) {
            LOGGER.error("Failed to create default config file", e);
        }
    }
    
    /**
     * Load config from properties file
     */
    private static void loadConfig() {
        try {
            if (!Files.exists(configPath)) {
                LOGGER.warn("Config file not found, using defaults");
                return;
            }
            
            Properties props = new Properties();
            props.load(Files.newBufferedReader(configPath));
            
            // General settings
            loadInt(props, "explorationBorder", BrecherConfig::setExplorationBorder);
            
            // Seed settings
            loadString(props, "seedStrategy", BrecherConfig::setSeedStrategy);
            loadLong(props, "debugSeed", BrecherConfig::setDebugSeed);
            
            // Dimension settings
            loadStringList(props, "enabledDimensions", BrecherConfig::setEnabledDimensions);
            loadStringList(props, "blacklist", BrecherConfig::setBlacklist);
            loadBoolean(props, "allowModdedDimensions", BrecherConfig::setAllowModdedDimensions);
            
            // Feature settings
            loadBoolean(props, "preventExplorationSpawnSetting", BrecherConfig::setPreventExplorationSpawnSetting);
            loadBoolean(props, "disableEnderChests", BrecherConfig::setDisableEnderChests);
            loadBoolean(props, "clearInventoryOnReturn", BrecherConfig::setClearInventoryOnReturn);
            loadBoolean(props, "keepInventoryInExploration", BrecherConfig::setKeepInventoryInExploration);
            loadBoolean(props, "deferToCorpseMods", BrecherConfig::setDeferToCorpseMods);
            loadBoolean(props, "disableModdedPortals", BrecherConfig::setDisableModdedPortals);
            loadBoolean(props, "preventModdedTeleports", BrecherConfig::setPreventModdedTeleports);
            loadBoolean(props, "cleanupXaeroMapData", BrecherConfig::setCleanupXaeroMapData);
            loadStringList(props, "xaeroCleanupTargets", BrecherConfig::setXaeroCleanupTargets);
            loadBoolean(props, "disableEndGateways", BrecherConfig::setDisableEndGateways);
            
            // Gameplay settings
            loadInt(props, "teleportCooldown", BrecherConfig::setTeleportCooldown);
            loadBoolean(props, "restrictToCurrentDimension", BrecherConfig::setRestrictToCurrentDimension);
            
            // Performance settings
            loadInt(props, "chunkUnloadDelay", BrecherConfig::setChunkUnloadDelay);
            loadInt(props, "maxChunksPerPlayer", BrecherConfig::setMaxChunksPerPlayer);
            loadBoolean(props, "aggressiveChunkUnloading", BrecherConfig::setAggressiveChunkUnloading);
            loadInt(props, "entityCleanupInterval", BrecherConfig::setEntityCleanupInterval);
            loadInt(props, "chunkCleanupInterval", BrecherConfig::setChunkCleanupInterval);
            loadBoolean(props, "preventDiskSaves", BrecherConfig::setPreventDiskSaves);
            loadInt(props, "oldDimensionRetentionCount", BrecherConfig::setOldDimensionRetentionCount);
            
            // Chunk pre-generation settings
            loadBoolean(props, "preGenerateSpawnChunks", BrecherConfig::setPreGenerateSpawnChunks);
            loadInt(props, "immediateSpawnRadius", BrecherConfig::setImmediateSpawnRadius);
            loadInt(props, "extendedSpawnRadius", BrecherConfig::setExtendedSpawnRadius);
            
            // Safety settings
            loadInt(props, "teleportSafetyRadius", BrecherConfig::setTeleportSafetyRadius);
            loadBoolean(props, "createEmergencyPlatforms", BrecherConfig::setCreateEmergencyPlatforms);
            loadBoolean(props, "preferSurfaceSpawns", BrecherConfig::setPreferSurfaceSpawns);
            
            // Messages
            loadString(props, "welcomeMessage", BrecherConfig::setWelcomeMessage);
            loadString(props, "returnMessage", BrecherConfig::setReturnMessage);
            
            LOGGER.info("Loaded config from file");
        } catch (Exception e) {
            LOGGER.error("Failed to load config file, using defaults", e);
        }
    }
    
    private static void loadString(Properties props, String key, java.util.function.Consumer<String> setter) {
        String value = props.getProperty(key);
        if (value != null && !value.isEmpty()) {
            setter.accept(value);
        }
    }
    
    private static void loadInt(Properties props, String key, java.util.function.Consumer<Integer> setter) {
        String value = props.getProperty(key);
        if (value != null) {
            try {
                setter.accept(Integer.parseInt(value));
            } catch (NumberFormatException e) {
                LOGGER.warn("Invalid integer value for {}: {}", key, value);
            }
        }
    }
    
    private static void loadLong(Properties props, String key, java.util.function.Consumer<Long> setter) {
        String value = props.getProperty(key);
        if (value != null) {
            try {
                setter.accept(Long.parseLong(value));
            } catch (NumberFormatException e) {
                LOGGER.warn("Invalid long value for {}: {}", key, value);
            }
        }
    }
    
    private static void loadBoolean(Properties props, String key, java.util.function.Consumer<Boolean> setter) {
        String value = props.getProperty(key);
        if (value != null) {
            setter.accept(Boolean.parseBoolean(value));
        }
    }
    
    private static void loadStringList(Properties props, String key, java.util.function.Consumer<List<String>> setter) {
        String value = props.getProperty(key);
        if (value != null && !value.isEmpty()) {
            List<String> list = Arrays.asList(value.split(","));
            list = list.stream().map(String::trim).filter(s -> !s.isEmpty()).toList();
            setter.accept(list);
        }
    }
}