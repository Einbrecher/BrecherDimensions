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

import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.List;

/**
 * NeoForge configuration specification using TOML format
 */
public class BrecherConfigSpec {
    public static final ModConfigSpec SPEC;
    
    // Server-side options
    public static final ModConfigSpec.IntValue EXPLORATION_BORDER;
    public static final ModConfigSpec.EnumValue<SeedStrategy> SEED_STRATEGY;
    public static final ModConfigSpec.LongValue DEBUG_SEED;
    public static final ModConfigSpec.ConfigValue<String> WEEKLY_RESET_DAY;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ENABLED_DIMENSIONS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> BLACKLIST;
    public static final ModConfigSpec.BooleanValue ALLOW_MODDED_DIMENSIONS;
    public static final ModConfigSpec.BooleanValue PREVENT_EXPLORATION_SPAWN_SETTING;
    public static final ModConfigSpec.BooleanValue DISABLE_ENDER_CHESTS;
    public static final ModConfigSpec.BooleanValue CLEAR_INVENTORY_ON_RETURN;
    public static final ModConfigSpec.BooleanValue KEEP_INVENTORY_IN_EXPLORATION;
    public static final ModConfigSpec.BooleanValue DEFER_TO_CORPSE_MODS;
    public static final ModConfigSpec.BooleanValue DISABLE_MODDED_PORTALS;
    public static final ModConfigSpec.BooleanValue PREVENT_MODDED_TELEPORTS;
    public static final ModConfigSpec.BooleanValue DISABLE_END_GATEWAYS;
    public static final ModConfigSpec.IntValue TELEPORT_COOLDOWN;
    public static final ModConfigSpec.BooleanValue RESTRICT_TO_CURRENT_DIMENSION;
    public static final ModConfigSpec.IntValue CHUNK_UNLOAD_DELAY;
    public static final ModConfigSpec.IntValue MAX_CHUNKS_PER_PLAYER;
    public static final ModConfigSpec.BooleanValue AGGRESSIVE_CHUNK_UNLOADING;
    public static final ModConfigSpec.IntValue ENTITY_CLEANUP_INTERVAL;
    public static final ModConfigSpec.IntValue CHUNK_CLEANUP_INTERVAL;
    public static final ModConfigSpec.BooleanValue PREVENT_DISK_SAVES;
    public static final ModConfigSpec.IntValue OLD_DIMENSION_RETENTION_COUNT;
    public static final ModConfigSpec.BooleanValue PRE_GENERATE_SPAWN_CHUNKS;
    public static final ModConfigSpec.IntValue IMMEDIATE_SPAWN_RADIUS;
    public static final ModConfigSpec.IntValue EXTENDED_SPAWN_RADIUS;
    
    // Background pre-generation
    public static final ModConfigSpec.BooleanValue PREGEN_ENABLED;
    public static final ModConfigSpec.IntValue PREGEN_CHUNKS_PER_TICK;
    public static final ModConfigSpec.IntValue PREGEN_TICK_INTERVAL;
    public static final ModConfigSpec.IntValue PREGEN_TICKS_PER_CHUNK;
    public static final ModConfigSpec.IntValue PREGEN_TICKET_DURATION;
    public static final ModConfigSpec.BooleanValue PREGEN_AUTO_START;
    public static final ModConfigSpec.BooleanValue PREGEN_AUTO_RESUME;
    public static final ModConfigSpec.IntValue PREGEN_MIN_TPS;
    public static final ModConfigSpec.IntValue PREGEN_MEMORY_THRESHOLD;
    public static final ModConfigSpec.IntValue PREGEN_DEFAULT_RADIUS;
    public static final ModConfigSpec.BooleanValue PREGEN_PAUSE_WITH_PLAYERS;
    public static final ModConfigSpec.IntValue PREGEN_STALE_HOURS;
    
    public static final ModConfigSpec.IntValue TELEPORT_SAFETY_RADIUS;
    public static final ModConfigSpec.BooleanValue CREATE_EMERGENCY_PLATFORMS;
    public static final ModConfigSpec.BooleanValue PREFER_SURFACE_SPAWNS;
    public static final ModConfigSpec.ConfigValue<String> WELCOME_MESSAGE;
    public static final ModConfigSpec.ConfigValue<String> RETURN_MESSAGE;
    
    // Client-side options
    public static final ModConfigSpec.BooleanValue CLEANUP_XAERO_MAP_DATA;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> XAERO_CLEANUP_TARGETS;
    
    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        
        builder.comment("Brecher's Exploration Dimensions")
               .push("server");
        
        builder.push("general");
        
        EXPLORATION_BORDER = builder
            .comment("Border size (-1 for parent)")
            .defineInRange("explorationBorder", -1, -1, Integer.MAX_VALUE);
        
        builder.pop();
        
        builder.push("seeds");
        
        SEED_STRATEGY = builder
            .comment("Seed generation strategy")
            .defineEnum("seedStrategy", SeedStrategy.WEEKLY);
        
        DEBUG_SEED = builder
            .comment("Fixed seed for DEBUG mode")
            .defineInRange("debugSeed", -1L, Long.MIN_VALUE, Long.MAX_VALUE);
        
        WEEKLY_RESET_DAY = builder
            .comment("Day of week for weekly seed reset (MONDAY-SUNDAY, only used with WEEKLY strategy)")
            .define("weeklyResetDay", "THURSDAY");
        
        builder.pop();
        
        builder.push("dimensions");
        
        ENABLED_DIMENSIONS = builder
            .comment("Dimensions to create exploration copies for")
            .defineList("enabledDimensions", 
                List.of("minecraft:overworld", "minecraft:the_nether", "minecraft:the_end"),
                o -> o instanceof String);
        
        BLACKLIST = builder
            .comment("Excluded dimensions")
            .defineList("blacklist", List.of(), o -> o instanceof String);
        
        ALLOW_MODDED_DIMENSIONS = builder
            .comment("Allow modded dimensions")
            .define("allowModdedDimensions", false);
        
        builder.pop();
        
        builder.push("features");
        
        PREVENT_EXPLORATION_SPAWN_SETTING = builder
            .comment("Prevent spawn point setting in exploration dimensions")
            .define("preventExplorationSpawnSetting", true);
        
        DISABLE_ENDER_CHESTS = builder
            .comment("Disable ender chests")
            .define("disableEnderChests", false);
        
        CLEAR_INVENTORY_ON_RETURN = builder
            .comment("Clear inventory on return")
            .define("clearInventoryOnReturn", false);
        
        KEEP_INVENTORY_IN_EXPLORATION = builder
            .comment("Keep inventory on death in exploration dimensions")
            .define("keepInventoryInExploration", true);
        
        DEFER_TO_CORPSE_MODS = builder
            .comment("Let corpse/gravestone mods handle deaths in exploration dimensions")
            .define("deferToCorpseMods", true);
        
        DISABLE_MODDED_PORTALS = builder
            .comment("Disable modded portals")
            .define("disableModdedPortals", true);
        
        PREVENT_MODDED_TELEPORTS = builder
            .comment("Block modded teleports")
            .define("preventModdedTeleports", false);
        
        DISABLE_END_GATEWAYS = builder
            .comment("Disable End Gateways")
            .define("disableEndGateways", false);
        
        builder.pop();
        
        builder.push("gameplay");
        
        TELEPORT_COOLDOWN = builder
            .comment("Teleport cooldown (seconds)")
            .defineInRange("teleportCooldown", 10, 0, 1200);
        
        RESTRICT_TO_CURRENT_DIMENSION = builder
            .comment("Restrict Exploration teleports to like dimensions only (e.g., Overworld -> Exploration Overworld, Nether -> Exploration Nether)")
            .define("restrictToCurrentDimension", false);
        
        builder.pop();
        
        builder.push("performance");
        
        CHUNK_UNLOAD_DELAY = builder
            .comment("Chunk unload delay (ticks)")
            .defineInRange("chunkUnloadDelay", 300, 20, 6000);
        
        MAX_CHUNKS_PER_PLAYER = builder
            .comment("Max chunks per player")
            .defineInRange("maxChunksPerPlayer", 100, 9, 500);
        
        AGGRESSIVE_CHUNK_UNLOADING = builder
            .comment("Aggressive chunk unloading")
            .define("aggressiveChunkUnloading", true);
        
        ENTITY_CLEANUP_INTERVAL = builder
            .comment("Entity cleanup interval (ticks)")
            .defineInRange("entityCleanupInterval", 1200, 200, 12000);
        
        CHUNK_CLEANUP_INTERVAL = builder
            .comment("Chunk cleanup interval (ticks)")
            .defineInRange("chunkCleanupInterval", 200, 20, 6000);
        
        PREVENT_DISK_SAVES = builder
            .comment("Skip saving chunks to disk. (If true, Exploration chunks will NOT persist once unloaded.)")
            .define("preventDiskSaves", false);
        
        OLD_DIMENSION_RETENTION_COUNT = builder
            .comment("Old dimension folders to keep per dimension type (e.g., 2 means keep last 2 overworld, last 2 nether, last 2 end)")
            .defineInRange("oldDimensionRetentionCount", 2, 0, 10);
        
        builder.pop();
        
        builder.push("chunkPreGeneration");
        
        PRE_GENERATE_SPAWN_CHUNKS = builder
            .comment("Pre-generate spawn chunks")
            .define("preGenerateSpawnChunks", true);
        
        IMMEDIATE_SPAWN_RADIUS = builder
            .comment("Immediate spawn radius (chunks)")
            .defineInRange("immediateSpawnRadius", 3, 0, 10);
        
        EXTENDED_SPAWN_RADIUS = builder
            .comment("Extended spawn radius (chunks)")
            .defineInRange("extendedSpawnRadius", 5, 0, 20);
        
        builder.pop();
        
        builder.push("backgroundPreGeneration");
        
        PREGEN_ENABLED = builder
            .comment("Enable background chunk pre-generation")
            .define("pregenEnabled", true);
        
        PREGEN_CHUNKS_PER_TICK = builder
            .comment("Chunks to generate per processing tick (0 = use ticksPerChunk for fractional rates)")
            .defineInRange("pregenChunksPerTick", 0, 0, 10);
        
        PREGEN_TICK_INTERVAL = builder
            .comment("Ticks between generation batches")
            .defineInRange("pregenTickInterval", 1, 1, 200);
        
        PREGEN_TICKS_PER_CHUNK = builder
            .comment("Ticks per chunk when chunksPerTick=0 (2 = 1 chunk/2 ticks, 3 = 1 chunk/3 ticks)")
            .defineInRange("pregenTicksPerChunk", 2, 1, 100);
        
        PREGEN_TICKET_DURATION = builder
            .comment("Ticks to keep generated chunks loaded")
            .defineInRange("pregenTicketDuration", 60, 20, 300);
        
        PREGEN_AUTO_START = builder
            .comment("Auto-start generation when dimensions are created")
            .define("pregenAutoStart", true);
        
        PREGEN_AUTO_RESUME = builder
            .comment("Resume generation tasks after server restart")
            .define("pregenAutoResume", true);
        
        PREGEN_MIN_TPS = builder
            .comment("Minimum TPS before pausing generation")
            .defineInRange("pregenMinTPS", 18, 10, 20);
        
        PREGEN_MEMORY_THRESHOLD = builder
            .comment("Memory usage % threshold for pausing")
            .defineInRange("pregenMemoryThreshold", 85, 50, 95);
        
        PREGEN_DEFAULT_RADIUS = builder
            .comment("Default generation radius in chunks")
            .defineInRange("pregenDefaultRadius", 100, 10, 1000);
        
        PREGEN_PAUSE_WITH_PLAYERS = builder
            .comment("Pause generation when players are in dimension")
            .define("pregenPauseWithPlayers", false);
        
        PREGEN_STALE_HOURS = builder
            .comment("Hours before considering a task stale")
            .defineInRange("pregenStaleHours", 168, 24, 720);
        
        builder.pop();
        
        builder.push("safety");
        
        TELEPORT_SAFETY_RADIUS = builder
            .comment("Safe teleport search radius")
            .defineInRange("teleportSafetyRadius", 16, 1, 64);
        
        CREATE_EMERGENCY_PLATFORMS = builder
            .comment("Create emergency platforms")
            .define("createEmergencyPlatforms", true);
        
        PREFER_SURFACE_SPAWNS = builder
            .comment("Prefer surface spawns over caves")
            .define("preferSurfaceSpawns", true);
        
        builder.pop();
        
        builder.push("messages");
        
        WELCOME_MESSAGE = builder
            .comment("Welcome message")
            .define("welcomeMessage", "Welcome to the Exploration Dimension! This dimension will be replaced with a new world on the next server restart. If you're still here when the server restarts, you'll be returned to your departure point or the world spawn.");
        
        RETURN_MESSAGE = builder
            .comment("Return message")
            .define("returnMessage", "Welcome back to the persistent world!");
        
        builder.pop(); // messages
        builder.pop(); // server
        
        builder.push("client");
        
        CLEANUP_XAERO_MAP_DATA = builder
            .comment("Auto-clean Xaero map data")
            .define("cleanupXaeroMapData", false);
        
        XAERO_CLEANUP_TARGETS = builder
            .comment("Specific Xaero server directories to clean (empty = all). Example: [\"Multiplayer_192.168.1.16\", \"Singleplayer_MyWorld\"]")
            .defineList("xaeroCleanupTargets", List.of(), o -> o instanceof String);
        
        builder.pop(); // client
        
        SPEC = builder.build();
    }
    
    public enum SeedStrategy {
        RANDOM,
        DATE_BASED,
        WEEKLY,
        DEBUG
    }
}