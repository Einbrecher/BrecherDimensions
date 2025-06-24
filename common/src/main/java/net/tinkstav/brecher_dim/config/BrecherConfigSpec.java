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
        public static final boolean PREVENT_BED_SPAWN = true;
        public static final boolean DISABLE_ENDER_CHESTS = false;
        public static final boolean CLEAR_INVENTORY_ON_RETURN = false;
        public static final boolean DISABLE_MODDED_PORTALS = true;
        public static final boolean PREVENT_MODDED_TELEPORTS = false;
        
        // Gameplay
        public static final int TELEPORT_COOLDOWN = 100;
        public static final boolean RESTRICT_TO_CURRENT_DIMENSION = false;
        
        // Performance
        public static final int CHUNK_UNLOAD_DELAY = 60;
        public static final int MAX_CHUNKS_PER_PLAYER = 49;
        public static final boolean AGGRESSIVE_CHUNK_UNLOADING = true;
        public static final int ENTITY_CLEANUP_INTERVAL = 1200;
        public static final boolean PREVENT_DISK_SAVES = true;
        public static final int OLD_DIMENSION_RETENTION_COUNT = 3;
        
        // Chunk Pre-generation
        public static final boolean PRE_GENERATE_SPAWN_CHUNKS = true;
        public static final int IMMEDIATE_SPAWN_RADIUS = 3;
        public static final int EXTENDED_SPAWN_RADIUS = 8;
        
        // Safety
        public static final int TELEPORT_SAFETY_RADIUS = 16;
        public static final boolean CREATE_EMERGENCY_PLATFORMS = true;
        
        // Messages
        public static final String WELCOME_MESSAGE = "Welcome to the exploration dimension! This world will reset with a new seed on server restart.";
        public static final String RETURN_MESSAGE = "Returned to the main world.";
    }
    
    // Configuration comments for documentation
    public static final class Comments {
        public static final String EXPLORATION_BORDER = "World border size for exploration dimensions (-1 for same as parent)";
        
        public static final String SEED_STRATEGY = "Seed generation strategy for exploration dimensions. Options: 'random' (new random seed each restart), 'date-based' (same seed for entire day)";
        public static final String DEBUG_SEED = "Debug seed for testing (-1 to disable, any other value to use as fixed seed)";
        
        public static final String ENABLED_DIMENSIONS = "Which dimensions to create exploration copies for. These dimensions will have exploration versions created at server startup. Each restart creates new dimensions with new seeds";
        public static final String BLACKLIST = "Dimensions that should never have exploration copies";
        public static final String ALLOW_MODDED_DIMENSIONS = "Allow exploration of modded dimensions";
        
        public static final String PREVENT_BED_SPAWN = "Prevent players from setting spawn in exploration dimensions";
        public static final String DISABLE_ENDER_CHESTS = "Disable ender chest access in exploration dimensions";
        public static final String CLEAR_INVENTORY_ON_RETURN = "Clear player inventory when returning from exploration";
        public static final String DISABLE_MODDED_PORTALS = "Disable modded portals in exploration dimensions";
        public static final String PREVENT_MODDED_TELEPORTS = "Prevent modded teleportation methods";
        
        public static final String TELEPORT_COOLDOWN = "Cooldown in ticks between teleports (20 ticks = 1 second)";
        public static final String RESTRICT_TO_CURRENT_DIMENSION = "Restrict teleportation to only the exploration version of the current dimension. When true: players in overworld can only teleport to exploration_overworld. When true: players cannot teleport between exploration dimensions. When false: players can teleport to any enabled exploration dimension. When false: players can hop between exploration dimensions without returning first";
        
        public static final String CHUNK_UNLOAD_DELAY = "Ticks before unloading chunks in exploration dimensions";
        public static final String MAX_CHUNKS_PER_PLAYER = "Maximum loaded chunks per player in exploration dimensions";
        public static final String AGGRESSIVE_CHUNK_UNLOADING = "Use aggressive chunk unloading in exploration dimensions";
        public static final String ENTITY_CLEANUP_INTERVAL = "Ticks between entity cleanup runs";
        public static final String PREVENT_DISK_SAVES = "Prevent exploration dimensions from saving to disk (improves performance)";
        public static final String OLD_DIMENSION_RETENTION_COUNT = "Number of old exploration dimension folders to keep on disk (older ones are deleted)";
        
        public static final String PRE_GENERATE_SPAWN_CHUNKS = "Pre-generate chunks around spawn points to reduce lag for first visitor. This increases server startup time but provides smoother experience";
        public static final String IMMEDIATE_SPAWN_RADIUS = "Radius in chunks to pre-generate immediately at startup (synchronous). Recommended: 3 chunks (48 blocks). Set to 0 to disable immediate generation";
        public static final String EXTENDED_SPAWN_RADIUS = "Extended radius in chunks to pre-generate asynchronously after startup. Recommended: 8 chunks (128 blocks). Must be >= immediate_spawn_radius";
        
        public static final String TELEPORT_SAFETY_RADIUS = "Radius in blocks to search for safe teleport positions";
        public static final String CREATE_EMERGENCY_PLATFORMS = "Create emergency obsidian platforms when no safe position is found";
        
        public static final String WELCOME_MESSAGE = "Message shown when entering exploration dimension";
        public static final String RETURN_MESSAGE = "Message shown when returning to normal dimension";
    }
    
    // Validation ranges
    public static final class Ranges {
        public static final int EXPLORATION_BORDER_MIN = -1;
        public static final int EXPLORATION_BORDER_MAX = Integer.MAX_VALUE;
        
        public static final int TELEPORT_COOLDOWN_MIN = 0;
        public static final int TELEPORT_COOLDOWN_MAX = 6000;
        
        public static final int CHUNK_UNLOAD_DELAY_MIN = 20;
        public static final int CHUNK_UNLOAD_DELAY_MAX = 600;
        
        public static final int MAX_CHUNKS_PER_PLAYER_MIN = 9;
        public static final int MAX_CHUNKS_PER_PLAYER_MAX = 121;
        
        public static final int ENTITY_CLEANUP_INTERVAL_MIN = 200;
        public static final int ENTITY_CLEANUP_INTERVAL_MAX = 6000;
        
        public static final int OLD_DIMENSION_RETENTION_COUNT_MIN = 0;
        public static final int OLD_DIMENSION_RETENTION_COUNT_MAX = 10;
        
        public static final int IMMEDIATE_SPAWN_RADIUS_MIN = 0;
        public static final int IMMEDIATE_SPAWN_RADIUS_MAX = 8;
        
        public static final int EXTENDED_SPAWN_RADIUS_MIN = 0;
        public static final int EXTENDED_SPAWN_RADIUS_MAX = 16;
        
        public static final int TELEPORT_SAFETY_RADIUS_MIN = 4;
        public static final int TELEPORT_SAFETY_RADIUS_MAX = 64;
    }
}