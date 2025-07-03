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
        public static final String SEED_STRATEGY = "random";
        public static final long DEBUG_SEED = -1L;
        
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
        
        // Performance
        public static final int CHUNK_UNLOAD_DELAY = 60;
        public static final int MAX_CHUNKS_PER_PLAYER = 49;
        public static final boolean AGGRESSIVE_CHUNK_UNLOADING = true;
        public static final int ENTITY_CLEANUP_INTERVAL = 1200;
        public static final int CHUNK_CLEANUP_INTERVAL = 200;
        public static final boolean PREVENT_DISK_SAVES = false;
        public static final int OLD_DIMENSION_RETENTION_COUNT = 2;
        
        // Chunk Pre-generation
        public static final boolean PRE_GENERATE_SPAWN_CHUNKS = true;
        public static final int IMMEDIATE_SPAWN_RADIUS = 3;
        public static final int EXTENDED_SPAWN_RADIUS = 8;
        
        // Safety
        public static final int TELEPORT_SAFETY_RADIUS = 16;
        public static final boolean CREATE_EMERGENCY_PLATFORMS = true;
        public static final boolean PREFER_SURFACE_SPAWNS = true;
        public static final boolean EXTENDED_SEARCH_RADIUS = true;
        
        // Messages
        public static final String WELCOME_MESSAGE = "Welcome to the Exploration Dimension! This is a temporary dimension that resets with each server restart. If you're still here when the server restarts, you'll be returned to your departure point or the world spawn.";
        public static final String RETURN_MESSAGE = "Returned to the main world.";
    }
    
    // Configuration comments for documentation
    public static final class Comments {
        public static final String EXPLORATION_BORDER = "Border size (-1 for parent)";
        
        public static final String SEED_STRATEGY = "Seed generation strategy";
        public static final String DEBUG_SEED = "Fixed seed for debug mode";
        
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
        
        public static final String TELEPORT_SAFETY_RADIUS = "Safe teleport search radius";
        public static final String CREATE_EMERGENCY_PLATFORMS = "Create emergency platforms";
        public static final String PREFER_SURFACE_SPAWNS = "Prefer surface spawns over caves";
        public static final String EXTENDED_SEARCH_RADIUS = "Enable extended search radius (up to 48 blocks) for all dimensions";
        
        public static final String WELCOME_MESSAGE = "Welcome message";
        public static final String RETURN_MESSAGE = "Return message";
    }
}