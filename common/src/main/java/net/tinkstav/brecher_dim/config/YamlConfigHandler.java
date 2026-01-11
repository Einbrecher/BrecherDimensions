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
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.representer.Representer;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * YAML-based configuration handler for Brecher's Dimensions mod.
 * Provides a more user-friendly YAML format with comments.
 */
public class YamlConfigHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String CONFIG_FILE_NAME = "brecher_dimensions.yml";
    
    private final Path configPath;
    private Yaml yaml;
    private Map<String, Object> config;
    
    public YamlConfigHandler(Path configDir) {
        this.configPath = configDir.resolve(CONFIG_FILE_NAME);
        initYaml();
    }
    
    private void initYaml() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);
        
        Representer representer = new Representer(options);
        representer.getPropertyUtils().setSkipMissingProperties(true);
        
        yaml = new Yaml(representer, options);
    }
    
    public void init() {
        try {
            if (!Files.exists(configPath.getParent())) {
                Files.createDirectories(configPath.getParent());
            }
            
            if (!Files.exists(configPath)) {
                createDefaultConfig();
            } else {
                loadConfig();
            }
        } catch (IOException e) {
            LOGGER.error("Failed to initialize YAML configuration", e);
            config = new LinkedHashMap<>();
            loadDefaults();
        }
    }
    
    private void createDefaultConfig() {
        StringBuilder yamlContent = new StringBuilder();
        
        // Header
        yamlContent.append("# Brecher's Dimensions Configuration File\n");
        yamlContent.append("# Version: 1.0.0\n");
        yamlContent.append("# Configure exploration dimensions, teleportation, and performance settings\n\n");
        
        // General Settings
        yamlContent.append("general:\n");
        yamlContent.append("  # World border size for exploration dimensions\n");
        yamlContent.append("  # Set to -1 to use the same border as the parent dimension\n");
        yamlContent.append("  exploration_border: ").append(BrecherConfigSpec.Defaults.EXPLORATION_BORDER).append("\n\n");
        
        // Seed Settings
        yamlContent.append("seeds:\n");
        yamlContent.append("  # Strategy for generating dimension seeds\n");
        yamlContent.append("  # Options: random, date, weekly, debug\n");
        yamlContent.append("  strategy: \"").append(BrecherConfigSpec.Defaults.SEED_STRATEGY).append("\"\n");
        yamlContent.append("  \n");
        yamlContent.append("  # Fixed seed to use when strategy is 'debug'\n");
        yamlContent.append("  debug_seed: ").append(BrecherConfigSpec.Defaults.DEBUG_SEED).append("\n");
        yamlContent.append("  \n");
        yamlContent.append("  # Day of week for reset when using 'weekly' strategy\n");
        yamlContent.append("  # Options: MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY\n");
        yamlContent.append("  weekly_reset_day: \"").append(BrecherConfigSpec.Defaults.WEEKLY_RESET_DAY).append("\"\n\n");
        
        // Dimension Settings
        yamlContent.append("dimensions:\n");
        yamlContent.append("  # List of dimensions to create exploration copies for\n");
        yamlContent.append("  # Each dimension listed here will have an exploration version created\n");
        yamlContent.append("  enabled:\n");
        for (String dim : BrecherConfigSpec.Defaults.ENABLED_DIMENSIONS) {
            yamlContent.append("    - \"").append(dim).append("\"\n");
        }
        yamlContent.append("  \n");
        yamlContent.append("  # Dimensions that should never have exploration copies\n");
        yamlContent.append("  blacklist:\n");
        for (String dim : BrecherConfigSpec.Defaults.BLACKLIST) {
            yamlContent.append("    - \"").append(dim).append("\"\n");
        }
        yamlContent.append("  \n");
        yamlContent.append("  # Allow exploration versions of modded dimensions\n");
        yamlContent.append("  allow_modded: ").append(BrecherConfigSpec.Defaults.ALLOW_MODDED_DIMENSIONS).append("\n\n");
        
        // Feature Settings
        yamlContent.append("features:\n");
        yamlContent.append("  # Prevent players from setting spawn in exploration dimensions\n");
        yamlContent.append("  prevent_spawn_setting: ").append(BrecherConfigSpec.Defaults.PREVENT_EXPLORATION_SPAWN_SETTING).append("\n");
        yamlContent.append("  \n");
        yamlContent.append("  # Disable ender chest access in exploration dimensions\n");
        yamlContent.append("  disable_ender_chests: ").append(BrecherConfigSpec.Defaults.DISABLE_ENDER_CHESTS).append("\n");
        yamlContent.append("  \n");
        yamlContent.append("  # Clear player inventory when returning from exploration\n");
        yamlContent.append("  clear_inventory_on_return: ").append(BrecherConfigSpec.Defaults.CLEAR_INVENTORY_ON_RETURN).append("\n");
        yamlContent.append("  \n");
        yamlContent.append("  # Keep inventory when dying in exploration dimensions\n");
        yamlContent.append("  keep_inventory_in_exploration: ").append(BrecherConfigSpec.Defaults.KEEP_INVENTORY_IN_EXPLORATION).append("\n");
        yamlContent.append("  \n");
        yamlContent.append("  # Let corpse mods handle death instead of keeping inventory\n");
        yamlContent.append("  defer_to_corpse_mods: ").append(BrecherConfigSpec.Defaults.DEFER_TO_CORPSE_MODS).append("\n");
        yamlContent.append("  \n");
        yamlContent.append("  # Prevent modded portal usage in exploration dimensions\n");
        yamlContent.append("  disable_modded_portals: ").append(BrecherConfigSpec.Defaults.DISABLE_MODDED_PORTALS).append("\n");
        yamlContent.append("  \n");
        yamlContent.append("  # Block modded teleportation methods\n");
        yamlContent.append("  prevent_modded_teleports: ").append(BrecherConfigSpec.Defaults.PREVENT_MODDED_TELEPORTS).append("\n");
        yamlContent.append("  \n");
        yamlContent.append("  # Auto-cleanup Xaero minimap data when dimensions rotate\n");
        yamlContent.append("  cleanup_xaero_map_data: ").append(BrecherConfigSpec.Defaults.CLEANUP_XAERO_MAP_DATA).append("\n");
        yamlContent.append("  \n");
        yamlContent.append("  # List of dimension patterns to clean up Xaero data for\n");
        yamlContent.append("  xaero_cleanup_targets:\n");
        for (String target : BrecherConfigSpec.Defaults.XAERO_CLEANUP_TARGETS) {
            yamlContent.append("    - \"").append(target).append("\"\n");
        }
        yamlContent.append("  \n");
        yamlContent.append("  # Prevent end gateway usage in exploration dimensions\n");
        yamlContent.append("  disable_end_gateways: ").append(BrecherConfigSpec.Defaults.DISABLE_END_GATEWAYS).append("\n\n");
        
        // Gameplay Settings
        yamlContent.append("gameplay:\n");
        yamlContent.append("  # Cooldown in seconds between teleports\n");
        yamlContent.append("  teleport_cooldown: ").append(BrecherConfigSpec.Defaults.TELEPORT_COOLDOWN).append("\n");
        yamlContent.append("  \n");
        yamlContent.append("  # Only allow teleport to exploration version of current dimension\n");
        yamlContent.append("  restrict_to_current_dimension: ").append(BrecherConfigSpec.Defaults.RESTRICT_TO_CURRENT_DIMENSION).append("\n\n");

        // Dimension Locks (Progression Gating)
        yamlContent.append("dimension_locks:\n");
        yamlContent.append("  # Require players to complete advancements before accessing exploration dimensions\n");
        yamlContent.append("  enabled: ").append(BrecherConfigSpec.Defaults.DIMENSION_LOCKS_ENABLED).append("\n");
        yamlContent.append("  \n");
        yamlContent.append("  # Map of dimension -> required advancement\n");
        yamlContent.append("  # Players must complete the advancement to use /exploration tp for that dimension\n");
        yamlContent.append("  # Format: \"dimension_id\": \"advancement_id\"\n");
        yamlContent.append("  locks:\n");
        for (Map.Entry<String, String> entry : BrecherConfigSpec.Defaults.DIMENSION_LOCKS.entrySet()) {
            yamlContent.append("    \"").append(entry.getKey()).append("\": \"").append(entry.getValue()).append("\"\n");
        }
        yamlContent.append("\n");
        
        // Performance Settings
        yamlContent.append("performance:\n");
        yamlContent.append("  # Chunk management\n");
        yamlContent.append("  chunks:\n");
        yamlContent.append("    # Delay before unloading chunks (ticks)\n");
        yamlContent.append("    unload_delay: ").append(BrecherConfigSpec.Defaults.CHUNK_UNLOAD_DELAY).append("\n");
        yamlContent.append("    # Maximum chunks loaded per player\n");
        yamlContent.append("    max_per_player: ").append(BrecherConfigSpec.Defaults.MAX_CHUNKS_PER_PLAYER).append("\n");
        yamlContent.append("    # Enable aggressive chunk unloading\n");
        yamlContent.append("    aggressive_unloading: ").append(BrecherConfigSpec.Defaults.AGGRESSIVE_CHUNK_UNLOADING).append("\n");
        yamlContent.append("    # Interval for chunk cleanup operations (ticks)\n");
        yamlContent.append("    cleanup_interval: ").append(BrecherConfigSpec.Defaults.CHUNK_CLEANUP_INTERVAL).append("\n");
        yamlContent.append("  \n");
        yamlContent.append("  # Entity cleanup interval in exploration dimensions (ticks)\n");
        yamlContent.append("  entity_cleanup_interval: ").append(BrecherConfigSpec.Defaults.ENTITY_CLEANUP_INTERVAL).append("\n");
        yamlContent.append("  \n");
        yamlContent.append("  # Skip saving exploration dimensions to disk\n");
        yamlContent.append("  prevent_disk_saves: ").append(BrecherConfigSpec.Defaults.PREVENT_DISK_SAVES).append("\n");
        yamlContent.append("  \n");
        yamlContent.append("  # Number of old dimension folders to keep on disk\n");
        yamlContent.append("  old_dimension_retention_count: ").append(BrecherConfigSpec.Defaults.OLD_DIMENSION_RETENTION_COUNT).append("\n\n");
        
        // Spawn Pre-generation Settings
        yamlContent.append("spawn_generation:\n");
        yamlContent.append("  # Pre-generate spawn chunks on dimension creation\n");
        yamlContent.append("  enabled: ").append(BrecherConfigSpec.Defaults.PRE_GENERATE_SPAWN_CHUNKS).append("\n");
        yamlContent.append("  \n");
        yamlContent.append("  # Radius of chunks to generate immediately (in chunks)\n");
        yamlContent.append("  immediate_radius: ").append(BrecherConfigSpec.Defaults.IMMEDIATE_SPAWN_RADIUS).append("\n");
        yamlContent.append("  \n");
        yamlContent.append("  # Radius of chunks to generate in background (in chunks)\n");
        yamlContent.append("  extended_radius: ").append(BrecherConfigSpec.Defaults.EXTENDED_SPAWN_RADIUS).append("\n\n");
        
        // Background Pre-generation Settings
        yamlContent.append("background_pregen:\n");
        yamlContent.append("  # Enable background chunk pre-generation\n");
        yamlContent.append("  enabled: ").append(BrecherConfigSpec.Defaults.PREGEN_ENABLED).append("\n");
        yamlContent.append("  \n");
        yamlContent.append("  # Generation settings\n");
        yamlContent.append("  generation:\n");
        yamlContent.append("    # Chunks to generate per tick\n");
        yamlContent.append("    chunks_per_tick: ").append(BrecherConfigSpec.Defaults.PREGEN_CHUNKS_PER_TICK).append("\n");
        yamlContent.append("    # Interval between generation ticks\n");
        yamlContent.append("    tick_interval: ").append(BrecherConfigSpec.Defaults.PREGEN_TICK_INTERVAL).append("\n");
        yamlContent.append("    # Ticks to wait per chunk generation\n");
        yamlContent.append("    ticks_per_chunk: ").append(BrecherConfigSpec.Defaults.PREGEN_TICKS_PER_CHUNK).append("\n");
        yamlContent.append("    # Chunk ticket duration (ticks)\n");
        yamlContent.append("    ticket_duration: ").append(BrecherConfigSpec.Defaults.PREGEN_TICKET_DURATION).append("\n");
        yamlContent.append("    # Default radius for pre-generation (chunks)\n");
        yamlContent.append("    default_radius: ").append(BrecherConfigSpec.Defaults.PREGEN_DEFAULT_RADIUS).append("\n");
        yamlContent.append("  \n");
        yamlContent.append("  # Automation settings\n");
        yamlContent.append("  automation:\n");
        yamlContent.append("    # Start pre-generation automatically\n");
        yamlContent.append("    auto_start: ").append(BrecherConfigSpec.Defaults.PREGEN_AUTO_START).append("\n");
        yamlContent.append("    # Resume interrupted pre-generation\n");
        yamlContent.append("    auto_resume: ").append(BrecherConfigSpec.Defaults.PREGEN_AUTO_RESUME).append("\n");
        yamlContent.append("    # Pause when players are online\n");
        yamlContent.append("    pause_with_players: ").append(BrecherConfigSpec.Defaults.PREGEN_PAUSE_WITH_PLAYERS).append("\n");
        yamlContent.append("  \n");
        yamlContent.append("  # Performance limits\n");
        yamlContent.append("  limits:\n");
        yamlContent.append("    # Minimum TPS to continue generation\n");
        yamlContent.append("    min_tps: ").append(BrecherConfigSpec.Defaults.PREGEN_MIN_TPS).append("\n");
        yamlContent.append("    # Memory usage threshold percentage\n");
        yamlContent.append("    memory_threshold: ").append(BrecherConfigSpec.Defaults.PREGEN_MEMORY_THRESHOLD).append("\n");
        yamlContent.append("    # Hours before pre-generation data is considered stale\n");
        yamlContent.append("    stale_hours: ").append(BrecherConfigSpec.Defaults.PREGEN_STALE_HOURS).append("\n\n");
        
        // Safety Settings
        yamlContent.append("safety:\n");
        yamlContent.append("  # Search radius for safe teleport locations (blocks)\n");
        yamlContent.append("  teleport_search_radius: ").append(BrecherConfigSpec.Defaults.TELEPORT_SAFETY_RADIUS).append("\n");
        yamlContent.append("  \n");
        yamlContent.append("  # Create obsidian platforms when no safe location found\n");
        yamlContent.append("  create_emergency_platforms: ").append(BrecherConfigSpec.Defaults.CREATE_EMERGENCY_PLATFORMS).append("\n");
        yamlContent.append("  \n");
        yamlContent.append("  # Prefer surface locations for teleportation\n");
        yamlContent.append("  prefer_surface_spawns: ").append(BrecherConfigSpec.Defaults.PREFER_SURFACE_SPAWNS).append("\n");
        yamlContent.append("  \n");
        yamlContent.append("  # Use extended search radius for safe locations\n");
        yamlContent.append("  extended_search_radius: ").append(BrecherConfigSpec.Defaults.EXTENDED_SEARCH_RADIUS).append("\n\n");
        
        // Messages
        yamlContent.append("messages:\n");
        yamlContent.append("  # Message shown when entering exploration dimension\n");
        yamlContent.append("  welcome: \"").append(BrecherConfigSpec.Defaults.WELCOME_MESSAGE).append("\"\n");
        yamlContent.append("  \n");
        yamlContent.append("  # Message shown when returning from exploration\n");
        yamlContent.append("  return: \"").append(BrecherConfigSpec.Defaults.RETURN_MESSAGE).append("\"\n");
        
        try {
            Files.writeString(configPath, yamlContent.toString(), StandardCharsets.UTF_8);
            loadConfig();
            LOGGER.info("Created default YAML configuration at: {}", configPath);
        } catch (IOException e) {
            LOGGER.error("Failed to create default configuration", e);
            config = new LinkedHashMap<>();
            loadDefaults();
        }
    }
    
    @SuppressWarnings("unchecked")
    private void loadConfig() {
        try {
            String content = Files.readString(configPath, StandardCharsets.UTF_8);
            config = yaml.load(content);
            if (config == null) {
                config = new LinkedHashMap<>();
            }
            applyConfig();
            LOGGER.info("Loaded YAML configuration from: {}", configPath);
        } catch (IOException e) {
            LOGGER.error("Failed to load configuration", e);
            config = new LinkedHashMap<>();
            loadDefaults();
        }
    }
    
    private void loadDefaults() {
        // Apply all default values when config can't be loaded
        BrecherConfig.setExplorationBorder(BrecherConfigSpec.Defaults.EXPLORATION_BORDER);
        BrecherConfig.setSeedStrategy(BrecherConfigSpec.Defaults.SEED_STRATEGY);
        BrecherConfig.setDebugSeed(BrecherConfigSpec.Defaults.DEBUG_SEED);
        BrecherConfig.setWeeklyResetDay(BrecherConfigSpec.Defaults.WEEKLY_RESET_DAY);
        BrecherConfig.setEnabledDimensions(BrecherConfigSpec.Defaults.ENABLED_DIMENSIONS);
        BrecherConfig.setBlacklist(BrecherConfigSpec.Defaults.BLACKLIST);
        BrecherConfig.setAllowModdedDimensions(BrecherConfigSpec.Defaults.ALLOW_MODDED_DIMENSIONS);
        BrecherConfig.setPreventExplorationSpawnSetting(BrecherConfigSpec.Defaults.PREVENT_EXPLORATION_SPAWN_SETTING);
        BrecherConfig.setDisableEnderChests(BrecherConfigSpec.Defaults.DISABLE_ENDER_CHESTS);
        BrecherConfig.setClearInventoryOnReturn(BrecherConfigSpec.Defaults.CLEAR_INVENTORY_ON_RETURN);
        BrecherConfig.setKeepInventoryInExploration(BrecherConfigSpec.Defaults.KEEP_INVENTORY_IN_EXPLORATION);
        BrecherConfig.setDeferToCorpseMods(BrecherConfigSpec.Defaults.DEFER_TO_CORPSE_MODS);
        BrecherConfig.setDisableModdedPortals(BrecherConfigSpec.Defaults.DISABLE_MODDED_PORTALS);
        BrecherConfig.setPreventModdedTeleports(BrecherConfigSpec.Defaults.PREVENT_MODDED_TELEPORTS);
        BrecherConfig.setCleanupXaeroMapData(BrecherConfigSpec.Defaults.CLEANUP_XAERO_MAP_DATA);
        BrecherConfig.setXaeroCleanupTargets(BrecherConfigSpec.Defaults.XAERO_CLEANUP_TARGETS);
        BrecherConfig.setDisableEndGateways(BrecherConfigSpec.Defaults.DISABLE_END_GATEWAYS);
        BrecherConfig.setTeleportCooldown(BrecherConfigSpec.Defaults.TELEPORT_COOLDOWN);
        BrecherConfig.setRestrictToCurrentDimension(BrecherConfigSpec.Defaults.RESTRICT_TO_CURRENT_DIMENSION);
        BrecherConfig.setDimensionLocksEnabled(BrecherConfigSpec.Defaults.DIMENSION_LOCKS_ENABLED);
        BrecherConfig.setDimensionLocks(BrecherConfigSpec.Defaults.DIMENSION_LOCKS);
        BrecherConfig.setChunkUnloadDelay(BrecherConfigSpec.Defaults.CHUNK_UNLOAD_DELAY);
        BrecherConfig.setMaxChunksPerPlayer(BrecherConfigSpec.Defaults.MAX_CHUNKS_PER_PLAYER);
        BrecherConfig.setAggressiveChunkUnloading(BrecherConfigSpec.Defaults.AGGRESSIVE_CHUNK_UNLOADING);
        BrecherConfig.setEntityCleanupInterval(BrecherConfigSpec.Defaults.ENTITY_CLEANUP_INTERVAL);
        BrecherConfig.setChunkCleanupInterval(BrecherConfigSpec.Defaults.CHUNK_CLEANUP_INTERVAL);
        BrecherConfig.setPreventDiskSaves(BrecherConfigSpec.Defaults.PREVENT_DISK_SAVES);
        BrecherConfig.setOldDimensionRetentionCount(BrecherConfigSpec.Defaults.OLD_DIMENSION_RETENTION_COUNT);
        BrecherConfig.setPreGenerateSpawnChunks(BrecherConfigSpec.Defaults.PRE_GENERATE_SPAWN_CHUNKS);
        BrecherConfig.setImmediateSpawnRadius(BrecherConfigSpec.Defaults.IMMEDIATE_SPAWN_RADIUS);
        BrecherConfig.setExtendedSpawnRadius(BrecherConfigSpec.Defaults.EXTENDED_SPAWN_RADIUS);
        BrecherConfig.setPregenEnabled(BrecherConfigSpec.Defaults.PREGEN_ENABLED);
        BrecherConfig.setPregenChunksPerTick(BrecherConfigSpec.Defaults.PREGEN_CHUNKS_PER_TICK);
        BrecherConfig.setPregenTickInterval(BrecherConfigSpec.Defaults.PREGEN_TICK_INTERVAL);
        BrecherConfig.setPregenTicksPerChunk(BrecherConfigSpec.Defaults.PREGEN_TICKS_PER_CHUNK);
        BrecherConfig.setPregenTicketDuration(BrecherConfigSpec.Defaults.PREGEN_TICKET_DURATION);
        BrecherConfig.setPregenAutoStart(BrecherConfigSpec.Defaults.PREGEN_AUTO_START);
        BrecherConfig.setPregenAutoResume(BrecherConfigSpec.Defaults.PREGEN_AUTO_RESUME);
        BrecherConfig.setPregenMinTPS(BrecherConfigSpec.Defaults.PREGEN_MIN_TPS);
        BrecherConfig.setPregenMemoryThreshold(BrecherConfigSpec.Defaults.PREGEN_MEMORY_THRESHOLD);
        BrecherConfig.setPregenDefaultRadius(BrecherConfigSpec.Defaults.PREGEN_DEFAULT_RADIUS);
        BrecherConfig.setPregenPauseWithPlayers(BrecherConfigSpec.Defaults.PREGEN_PAUSE_WITH_PLAYERS);
        BrecherConfig.setPregenStaleHours(BrecherConfigSpec.Defaults.PREGEN_STALE_HOURS);
        BrecherConfig.setTeleportSafetyRadius(BrecherConfigSpec.Defaults.TELEPORT_SAFETY_RADIUS);
        BrecherConfig.setCreateEmergencyPlatforms(BrecherConfigSpec.Defaults.CREATE_EMERGENCY_PLATFORMS);
        BrecherConfig.setPreferSurfaceSpawns(BrecherConfigSpec.Defaults.PREFER_SURFACE_SPAWNS);
        BrecherConfig.setExtendedSearchRadius(BrecherConfigSpec.Defaults.EXTENDED_SEARCH_RADIUS);
        BrecherConfig.setWelcomeMessage(BrecherConfigSpec.Defaults.WELCOME_MESSAGE);
        BrecherConfig.setReturnMessage(BrecherConfigSpec.Defaults.RETURN_MESSAGE);
    }
    
    @SuppressWarnings("unchecked")
    private void applyConfig() {
        try {
            // General settings
            Map<String, Object> general = getSection("general");
            BrecherConfig.setExplorationBorder(getInt(general, "exploration_border", BrecherConfigSpec.Defaults.EXPLORATION_BORDER));
            
            // Seed settings
            Map<String, Object> seeds = getSection("seeds");
            BrecherConfig.setSeedStrategy(getString(seeds, "strategy", BrecherConfigSpec.Defaults.SEED_STRATEGY));
            BrecherConfig.setDebugSeed(getLong(seeds, "debug_seed", BrecherConfigSpec.Defaults.DEBUG_SEED));
            BrecherConfig.setWeeklyResetDay(getString(seeds, "weekly_reset_day", BrecherConfigSpec.Defaults.WEEKLY_RESET_DAY));
            
            // Dimension settings
            Map<String, Object> dimensions = getSection("dimensions");
            BrecherConfig.setEnabledDimensions(getStringList(dimensions, "enabled", BrecherConfigSpec.Defaults.ENABLED_DIMENSIONS));
            BrecherConfig.setBlacklist(getStringList(dimensions, "blacklist", BrecherConfigSpec.Defaults.BLACKLIST));
            BrecherConfig.setAllowModdedDimensions(getBoolean(dimensions, "allow_modded", BrecherConfigSpec.Defaults.ALLOW_MODDED_DIMENSIONS));
            
            // Feature settings
            Map<String, Object> features = getSection("features");
            BrecherConfig.setPreventExplorationSpawnSetting(getBoolean(features, "prevent_spawn_setting", BrecherConfigSpec.Defaults.PREVENT_EXPLORATION_SPAWN_SETTING));
            BrecherConfig.setDisableEnderChests(getBoolean(features, "disable_ender_chests", BrecherConfigSpec.Defaults.DISABLE_ENDER_CHESTS));
            BrecherConfig.setClearInventoryOnReturn(getBoolean(features, "clear_inventory_on_return", BrecherConfigSpec.Defaults.CLEAR_INVENTORY_ON_RETURN));
            BrecherConfig.setKeepInventoryInExploration(getBoolean(features, "keep_inventory_in_exploration", BrecherConfigSpec.Defaults.KEEP_INVENTORY_IN_EXPLORATION));
            BrecherConfig.setDeferToCorpseMods(getBoolean(features, "defer_to_corpse_mods", BrecherConfigSpec.Defaults.DEFER_TO_CORPSE_MODS));
            BrecherConfig.setDisableModdedPortals(getBoolean(features, "disable_modded_portals", BrecherConfigSpec.Defaults.DISABLE_MODDED_PORTALS));
            BrecherConfig.setPreventModdedTeleports(getBoolean(features, "prevent_modded_teleports", BrecherConfigSpec.Defaults.PREVENT_MODDED_TELEPORTS));
            BrecherConfig.setCleanupXaeroMapData(getBoolean(features, "cleanup_xaero_map_data", BrecherConfigSpec.Defaults.CLEANUP_XAERO_MAP_DATA));
            BrecherConfig.setXaeroCleanupTargets(getStringList(features, "xaero_cleanup_targets", BrecherConfigSpec.Defaults.XAERO_CLEANUP_TARGETS));
            BrecherConfig.setDisableEndGateways(getBoolean(features, "disable_end_gateways", BrecherConfigSpec.Defaults.DISABLE_END_GATEWAYS));
            
            // Gameplay settings
            Map<String, Object> gameplay = getSection("gameplay");
            BrecherConfig.setTeleportCooldown(getInt(gameplay, "teleport_cooldown", BrecherConfigSpec.Defaults.TELEPORT_COOLDOWN));
            BrecherConfig.setRestrictToCurrentDimension(getBoolean(gameplay, "restrict_to_current_dimension", BrecherConfigSpec.Defaults.RESTRICT_TO_CURRENT_DIMENSION));

            // Dimension locks (Progression Gating)
            Map<String, Object> dimensionLocks = getSection("dimension_locks");
            BrecherConfig.setDimensionLocksEnabled(getBoolean(dimensionLocks, "enabled", BrecherConfigSpec.Defaults.DIMENSION_LOCKS_ENABLED));
            Map<String, String> locks = getStringMap(dimensionLocks, "locks", BrecherConfigSpec.Defaults.DIMENSION_LOCKS);
            BrecherConfig.setDimensionLocksValidated(locks);

            // Performance settings
            Map<String, Object> performance = getSection("performance");
            Map<String, Object> chunks = getSection(performance, "chunks");
            BrecherConfig.setChunkUnloadDelay(getInt(chunks, "unload_delay", BrecherConfigSpec.Defaults.CHUNK_UNLOAD_DELAY));
            BrecherConfig.setMaxChunksPerPlayer(getInt(chunks, "max_per_player", BrecherConfigSpec.Defaults.MAX_CHUNKS_PER_PLAYER));
            BrecherConfig.setAggressiveChunkUnloading(getBoolean(chunks, "aggressive_unloading", BrecherConfigSpec.Defaults.AGGRESSIVE_CHUNK_UNLOADING));
            BrecherConfig.setChunkCleanupInterval(getInt(chunks, "cleanup_interval", BrecherConfigSpec.Defaults.CHUNK_CLEANUP_INTERVAL));
            BrecherConfig.setEntityCleanupInterval(getInt(performance, "entity_cleanup_interval", BrecherConfigSpec.Defaults.ENTITY_CLEANUP_INTERVAL));
            BrecherConfig.setPreventDiskSaves(getBoolean(performance, "prevent_disk_saves", BrecherConfigSpec.Defaults.PREVENT_DISK_SAVES));
            BrecherConfig.setOldDimensionRetentionCount(getInt(performance, "old_dimension_retention_count", BrecherConfigSpec.Defaults.OLD_DIMENSION_RETENTION_COUNT));
            
            // Spawn pre-generation settings
            Map<String, Object> spawnGen = getSection("spawn_generation");
            BrecherConfig.setPreGenerateSpawnChunks(getBoolean(spawnGen, "enabled", BrecherConfigSpec.Defaults.PRE_GENERATE_SPAWN_CHUNKS));
            BrecherConfig.setImmediateSpawnRadius(getInt(spawnGen, "immediate_radius", BrecherConfigSpec.Defaults.IMMEDIATE_SPAWN_RADIUS));
            BrecherConfig.setExtendedSpawnRadius(getInt(spawnGen, "extended_radius", BrecherConfigSpec.Defaults.EXTENDED_SPAWN_RADIUS));
            
            // Background pre-generation settings
            Map<String, Object> bgPregen = getSection("background_pregen");
            BrecherConfig.setPregenEnabled(getBoolean(bgPregen, "enabled", BrecherConfigSpec.Defaults.PREGEN_ENABLED));
            
            Map<String, Object> generation = getSection(bgPregen, "generation");
            BrecherConfig.setPregenChunksPerTick(getInt(generation, "chunks_per_tick", BrecherConfigSpec.Defaults.PREGEN_CHUNKS_PER_TICK));
            BrecherConfig.setPregenTickInterval(getInt(generation, "tick_interval", BrecherConfigSpec.Defaults.PREGEN_TICK_INTERVAL));
            BrecherConfig.setPregenTicksPerChunk(getInt(generation, "ticks_per_chunk", BrecherConfigSpec.Defaults.PREGEN_TICKS_PER_CHUNK));
            BrecherConfig.setPregenTicketDuration(getInt(generation, "ticket_duration", BrecherConfigSpec.Defaults.PREGEN_TICKET_DURATION));
            BrecherConfig.setPregenDefaultRadius(getInt(generation, "default_radius", BrecherConfigSpec.Defaults.PREGEN_DEFAULT_RADIUS));
            
            Map<String, Object> automation = getSection(bgPregen, "automation");
            BrecherConfig.setPregenAutoStart(getBoolean(automation, "auto_start", BrecherConfigSpec.Defaults.PREGEN_AUTO_START));
            BrecherConfig.setPregenAutoResume(getBoolean(automation, "auto_resume", BrecherConfigSpec.Defaults.PREGEN_AUTO_RESUME));
            BrecherConfig.setPregenPauseWithPlayers(getBoolean(automation, "pause_with_players", BrecherConfigSpec.Defaults.PREGEN_PAUSE_WITH_PLAYERS));
            
            Map<String, Object> limits = getSection(bgPregen, "limits");
            BrecherConfig.setPregenMinTPS(getInt(limits, "min_tps", BrecherConfigSpec.Defaults.PREGEN_MIN_TPS));
            BrecherConfig.setPregenMemoryThreshold(getInt(limits, "memory_threshold", BrecherConfigSpec.Defaults.PREGEN_MEMORY_THRESHOLD));
            BrecherConfig.setPregenStaleHours(getInt(limits, "stale_hours", BrecherConfigSpec.Defaults.PREGEN_STALE_HOURS));
            
            // Safety settings
            Map<String, Object> safety = getSection("safety");
            BrecherConfig.setTeleportSafetyRadius(getInt(safety, "teleport_search_radius", BrecherConfigSpec.Defaults.TELEPORT_SAFETY_RADIUS));
            BrecherConfig.setCreateEmergencyPlatforms(getBoolean(safety, "create_emergency_platforms", BrecherConfigSpec.Defaults.CREATE_EMERGENCY_PLATFORMS));
            BrecherConfig.setPreferSurfaceSpawns(getBoolean(safety, "prefer_surface_spawns", BrecherConfigSpec.Defaults.PREFER_SURFACE_SPAWNS));
            BrecherConfig.setExtendedSearchRadius(getBoolean(safety, "extended_search_radius", BrecherConfigSpec.Defaults.EXTENDED_SEARCH_RADIUS));
            
            // Messages
            Map<String, Object> messages = getSection("messages");
            BrecherConfig.setWelcomeMessage(getString(messages, "welcome", BrecherConfigSpec.Defaults.WELCOME_MESSAGE));
            BrecherConfig.setReturnMessage(getString(messages, "return", BrecherConfigSpec.Defaults.RETURN_MESSAGE));
            
        } catch (Exception e) {
            LOGGER.error("Error parsing YAML configuration, using defaults", e);
            loadDefaults();
        }
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> getSection(String key) {
        Object value = config.get(key);
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return new HashMap<>();
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> getSection(Map<String, Object> parent, String key) {
        Object value = parent.get(key);
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return new HashMap<>();
    }
    
    private int getInt(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }
    
    private long getLong(Map<String, Object> map, String key, long defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return defaultValue;
    }
    
    private boolean getBoolean(Map<String, Object> map, String key, boolean defaultValue) {
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }
    
    private String getString(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        if (value instanceof String) {
            return (String) value;
        }
        return defaultValue;
    }
    
    @SuppressWarnings("unchecked")
    private List<String> getStringList(Map<String, Object> map, String key, List<String> defaultValue) {
        Object value = map.get(key);
        if (value instanceof List) {
            List<String> result = new ArrayList<>();
            for (Object item : (List<?>) value) {
                if (item instanceof String) {
                    result.add((String) item);
                }
            }
            return result;
        }
        return defaultValue;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> getStringMap(Map<String, Object> map, String key, Map<String, String> defaultValue) {
        Object value = map.get(key);
        if (value instanceof Map) {
            Map<String, String> result = new HashMap<>();
            Map<?, ?> rawMap = (Map<?, ?>) value;
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                if (entry.getKey() instanceof String && entry.getValue() instanceof String) {
                    result.put((String) entry.getKey(), (String) entry.getValue());
                }
            }
            return result;
        }
        return new HashMap<>(defaultValue);
    }

    public void reload() {
        try {
            if (Files.exists(configPath)) {
                loadConfig();
            } else {
                init();
            }
            LOGGER.info("Reloaded YAML configuration");
        } catch (Exception e) {
            LOGGER.error("Failed to reload YAML configuration", e);
        }
    }
}