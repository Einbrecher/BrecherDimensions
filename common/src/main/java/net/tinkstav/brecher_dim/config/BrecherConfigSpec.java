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

import java.util.List;
import java.util.Map;
import java.util.Arrays;

/**
 * Platform-agnostic configuration specification.
 * Platform-specific implementations will handle the actual config loading.
 */
public class BrecherConfigSpec {
    // Default values
    public static final class Defaults {
        // General
        public static final int EXPLORATION_BORDER = -1;
        
        // Seeds
        public static final String SEED_STRATEGY = "weekly";
        public static final long DEBUG_SEED = -1L;
        public static final String WEEKLY_RESET_DAY = "THURSDAY";
        
        // Dimensions
        public static final List<String> ENABLED_DIMENSIONS = Arrays.asList(
            "minecraft:overworld",
            "minecraft:the_nether", 
            "minecraft:the_end"
        );
        public static final List<String> BLACKLIST = Arrays.asList();
        public static final boolean ALLOW_MODDED_DIMENSIONS = true;
        
        // Features
        public static final boolean PREVENT_EXPLORATION_SPAWN_SETTING = true;
        public static final boolean DISABLE_ENDER_CHESTS = false;
        public static final boolean CLEAR_INVENTORY_ON_RETURN = false;
        public static final boolean KEEP_INVENTORY_IN_EXPLORATION = true;
        public static final boolean DEFER_TO_CORPSE_MODS = true;
        public static final boolean DISABLE_MODDED_PORTALS = true;
        public static final boolean PREVENT_MODDED_TELEPORTS = false;
        public static final boolean CLEANUP_XAERO_MAP_DATA = false;
        public static final List<String> XAERO_CLEANUP_TARGETS = Arrays.asList();
        public static final boolean DISABLE_END_GATEWAYS = false;
        
        // Gameplay
        public static final int TELEPORT_COOLDOWN = 5;
        public static final boolean RESTRICT_TO_CURRENT_DIMENSION = false;

        // Dimension Locks (Progression Gating)
        public static final boolean DIMENSION_LOCKS_ENABLED = true;
        public static final Map<String, String> DIMENSION_LOCKS = Map.of(
            "minecraft:the_nether", "minecraft:story/enter_the_nether",
            "minecraft:the_end", "minecraft:story/enter_the_end"
        );
        
        // Performance
        public static final int CHUNK_UNLOAD_DELAY = 60;
        public static final int MAX_CHUNKS_PER_PLAYER = 49;
        public static final boolean AGGRESSIVE_CHUNK_UNLOADING = true;
        public static final int ENTITY_CLEANUP_INTERVAL = 1200;
        public static final int CHUNK_CLEANUP_INTERVAL = 200;
        public static final boolean PREVENT_DISK_SAVES = false;
        public static final int OLD_DIMENSION_RETENTION_COUNT = 2;
        
        // Chunk Pre-generation (Spawn)
        public static final boolean PRE_GENERATE_SPAWN_CHUNKS = true;
        public static final int IMMEDIATE_SPAWN_RADIUS = 3;
        public static final int EXTENDED_SPAWN_RADIUS = 8;
        
        // Chunk Pre-generation (Background)
        public static final boolean PREGEN_ENABLED = true;  // Enable by default for convenience
        public static final int PREGEN_CHUNKS_PER_TICK = 0;  // 0 = use ticksPerChunk for fractional rates
        public static final int PREGEN_TICK_INTERVAL = 1;   // Process every tick for smooth generation
        public static final int PREGEN_TICKS_PER_CHUNK = 2;  // Generate 1 chunk every 2 ticks (10 chunks/second)
        public static final int PREGEN_TICKET_DURATION = 60;
        public static final boolean PREGEN_AUTO_START = true;  // Auto-start by default
        public static final boolean PREGEN_AUTO_RESUME = true;
        public static final int PREGEN_MIN_TPS = 18;
        public static final int PREGEN_MEMORY_THRESHOLD = 85;
        public static final int PREGEN_DEFAULT_RADIUS = 100;
        public static final boolean PREGEN_PAUSE_WITH_PLAYERS = false;
        public static final int PREGEN_STALE_HOURS = 168;
        public static final int PREGEN_MAX_TICK_MS = 5;  // Max milliseconds per tick for generation loop
        public static final int PREGEN_LOG_INTERVAL = 1000;  // Log progress every N chunks (1000 = less spam)

        // Safety
        public static final int TELEPORT_SAFETY_RADIUS = 16;
        public static final boolean CREATE_EMERGENCY_PLATFORMS = true;
        public static final boolean PREFER_SURFACE_SPAWNS = true;
        public static final boolean EXTENDED_SEARCH_RADIUS = true;
        
        // Messages
        public static final String WELCOME_MESSAGE = "Welcome to the Exploration Dimension! This dimension will be replaced with a new world on the next server restart. If you're still here when the server restarts, you'll be returned to your departure point or the world spawn.";
        public static final String RETURN_MESSAGE = "Returned to the main world.";
    }
    
    // Configuration comments for documentation
    public static final class Comments {
        public static final String EXPLORATION_BORDER = "Border size (-1 for parent)";
        
        public static final String SEED_STRATEGY = "Seed generation strategy";
        public static final String DEBUG_SEED = "Fixed seed for debug mode";
        public static final String WEEKLY_RESET_DAY = "Day of week for weekly seed reset (MONDAY-SUNDAY, only used with weekly strategy)";
        
        public static final String ENABLED_DIMENSIONS = "Dimensions to create exploration copies for";
        public static final String BLACKLIST = "Excluded dimensions";
        public static final String ALLOW_MODDED_DIMENSIONS = "Allow modded dimensions";
        
        public static final String PREVENT_EXPLORATION_SPAWN_SETTING = "Prevent spawn point setting in exploration dimensions";
        public static final String DISABLE_ENDER_CHESTS = "Disable ender chests";
        public static final String CLEAR_INVENTORY_ON_RETURN = "Clear inventory on return";
        public static final String DISABLE_MODDED_PORTALS = "Disable modded portals";
        public static final String PREVENT_MODDED_TELEPORTS = "Block modded teleports";
        public static final String CLEANUP_XAERO_MAP_DATA = "Auto-clean Xaero map data";
        public static final String XAERO_CLEANUP_TARGETS = "Specific Xaero server directories to clean (empty = all)";
        public static final String DISABLE_END_GATEWAYS = "Disable End Gateways";
        
        public static final String TELEPORT_COOLDOWN = "Teleport cooldown (seconds)";
        public static final String RESTRICT_TO_CURRENT_DIMENSION = "Restrict Exploration teleports to like dimensions only (e.g., Overworld -> Exploration Overworld, Nether -> Exploration Nether)";

        public static final String DIMENSION_LOCKS_ENABLED = "Enable dimension locks (require advancements to access exploration dimensions)";
        public static final String DIMENSION_LOCKS = "Map of dimension to required advancement. Players must complete the advancement to access the exploration version";
        
        public static final String CHUNK_UNLOAD_DELAY = "Chunk unload delay (ticks)";
        public static final String MAX_CHUNKS_PER_PLAYER = "Max chunks per player";
        public static final String AGGRESSIVE_CHUNK_UNLOADING = "Aggressive chunk unloading";
        public static final String ENTITY_CLEANUP_INTERVAL = "Entity cleanup interval (ticks)";
        public static final String CHUNK_CLEANUP_INTERVAL = "Chunk cleanup interval (ticks)";
        public static final String PREVENT_DISK_SAVES = "Skip saving chunks to disk. (If true, Exploration chunks will NOT persist once unloaded.)";
        public static final String OLD_DIMENSION_RETENTION_COUNT = "Old dimension folders to keep per dimension type";
        
        public static final String PRE_GENERATE_SPAWN_CHUNKS = "Pre-generate spawn chunks";
        public static final String IMMEDIATE_SPAWN_RADIUS = "Immediate spawn radius (chunks)";
        public static final String EXTENDED_SPAWN_RADIUS = "Extended spawn radius (chunks)";
        
        public static final String PREGEN_ENABLED = "Enable background chunk pre-generation";
        public static final String PREGEN_CHUNKS_PER_TICK = "Chunks to generate per processing tick (0 = use ticksPerChunk, 1-10 = fast mode)";
        public static final String PREGEN_TICK_INTERVAL = "Ticks between generation batches (20 = 1 second)";
        public static final String PREGEN_TICKS_PER_CHUNK = "Ticks per chunk when chunksPerTick=0 (2 = 1 chunk/2 ticks, 3 = 1 chunk/3 ticks)";
        public static final String PREGEN_TICKET_DURATION = "Ticks to keep generated chunks loaded";
        public static final String PREGEN_AUTO_START = "Auto-start generation when dimensions are created";
        public static final String PREGEN_AUTO_RESUME = "Resume generation tasks after server restart";
        public static final String PREGEN_MIN_TPS = "Minimum TPS before pausing generation";
        public static final String PREGEN_MEMORY_THRESHOLD = "Memory usage % threshold for pausing";
        public static final String PREGEN_DEFAULT_RADIUS = "Default generation radius in chunks";
        public static final String PREGEN_PAUSE_WITH_PLAYERS = "Pause generation when players are in dimension";
        public static final String PREGEN_STALE_HOURS = "Hours before considering a task stale";
        public static final String PREGEN_MAX_TICK_MS = "Max milliseconds per tick for generation loop (prevents lag spikes)";
        public static final String PREGEN_LOG_INTERVAL = "Log progress every N chunks (100 = frequent, 1000 = less spam)";

        public static final String TELEPORT_SAFETY_RADIUS = "Safe teleport search radius";
        public static final String CREATE_EMERGENCY_PLATFORMS = "Create emergency platforms";
        public static final String PREFER_SURFACE_SPAWNS = "Prefer surface spawns over caves";
        public static final String EXTENDED_SEARCH_RADIUS = "Enable extended search radius (up to 48 blocks) for all dimensions";
        
        public static final String WELCOME_MESSAGE = "Welcome message";
        public static final String RETURN_MESSAGE = "Return message";
    }
}