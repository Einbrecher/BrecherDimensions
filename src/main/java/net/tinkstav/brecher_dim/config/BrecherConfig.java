package net.tinkstav.brecher_dim.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraft.resources.ResourceLocation;
import java.util.List;
import java.util.Arrays;

public class BrecherConfig {
    public static final ForgeConfigSpec SPEC;
    
    // General settings
    public static final ForgeConfigSpec.IntValue explorationBorder;
    
    // Seed settings
    public static final ForgeConfigSpec.ConfigValue<String> seedStrategy;
    public static final ForgeConfigSpec.LongValue debugSeed;
    
    // Dimension settings
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> enabledDimensions;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> blacklist;
    public static final ForgeConfigSpec.BooleanValue allowModdedDimensions;
    
    // Feature settings
    public static final ForgeConfigSpec.BooleanValue preventBedSpawn;
    public static final ForgeConfigSpec.BooleanValue disableEnderChests;
    public static final ForgeConfigSpec.BooleanValue clearInventoryOnReturn;
    public static final ForgeConfigSpec.BooleanValue disableModdedPortals;
    public static final ForgeConfigSpec.BooleanValue preventModdedTeleports;
    
    // Gameplay settings
    public static final ForgeConfigSpec.IntValue teleportCooldown;
    public static final ForgeConfigSpec.BooleanValue restrictToCurrentDimension;
    
    // Performance settings
    public static final ForgeConfigSpec.IntValue chunkUnloadDelay;
    public static final ForgeConfigSpec.IntValue maxChunksPerPlayer;
    public static final ForgeConfigSpec.BooleanValue aggressiveChunkUnloading;
    public static final ForgeConfigSpec.IntValue entityCleanupInterval;
    public static final ForgeConfigSpec.BooleanValue preventDiskSaves;
    public static final ForgeConfigSpec.IntValue oldDimensionRetentionCount;
    
    // Chunk pre-generation settings
    public static final ForgeConfigSpec.BooleanValue preGenerateSpawnChunks;
    public static final ForgeConfigSpec.IntValue immediateSpawnRadius;
    public static final ForgeConfigSpec.IntValue extendedSpawnRadius;
    
    // Safety settings
    public static final ForgeConfigSpec.IntValue teleportSafetyRadius;
    public static final ForgeConfigSpec.BooleanValue createEmergencyPlatforms;
    
    // Messages
    public static final ForgeConfigSpec.ConfigValue<String> welcomeMessage;
    public static final ForgeConfigSpec.ConfigValue<String> returnMessage;
    
    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        
        // General Settings
        builder.push("general");
        
        explorationBorder = builder
            .comment("World border size for exploration dimensions (-1 for same as parent)")
            .defineInRange("exploration_border", -1, -1, Integer.MAX_VALUE);
            
        builder.pop();
        
        // Seed Settings
        builder.push("seeds");
        
        seedStrategy = builder
            .comment("Seed generation strategy for exploration dimensions",
                    "Options: 'random' (new random seed each restart), 'date-based' (same seed for entire day)")
            .define("seed_strategy", "random", o -> {
                if (!(o instanceof String)) return false;
                String s = ((String) o).toLowerCase();
                return s.equals("random") || s.equals("date-based") || s.equals("date");
            });
            
        debugSeed = builder
            .comment("Debug seed for testing (-1 to disable, any other value to use as fixed seed)")
            .defineInRange("debug_seed", -1L, Long.MIN_VALUE, Long.MAX_VALUE);
            
        builder.pop();
        
        // Dimension Settings
        builder.push("dimensions");
        
        enabledDimensions = builder
            .comment("Which dimensions to create exploration copies for",
                    "These dimensions will have exploration versions created at server startup",
                    "Each restart creates new dimensions with new seeds")
            .defineList("enabled_dimensions", 
                Arrays.asList("minecraft:overworld", "minecraft:the_nether", "minecraft:the_end"),
                o -> o instanceof String && ResourceLocation.isValidResourceLocation((String)o));
                
        blacklist = builder
            .comment("Dimensions that should never have exploration copies")
            .defineList("blacklist", Arrays.asList(), 
                o -> o instanceof String && ResourceLocation.isValidResourceLocation((String)o));
            
        allowModdedDimensions = builder
            .comment("Allow exploration of modded dimensions")
            .define("allow_modded_dimensions", true);
            
        builder.pop();
        
        // Feature Settings
        builder.push("features");
        
        preventBedSpawn = builder
            .comment("Prevent players from setting spawn in exploration dimensions")
            .define("prevent_bed_spawn", true);
            
        disableEnderChests = builder
            .comment("Disable ender chest access in exploration dimensions")
            .define("disable_ender_chests", false);
            
        clearInventoryOnReturn = builder
            .comment("Clear player inventory when returning from exploration")
            .define("clear_inventory_on_return", false);
            
        disableModdedPortals = builder
            .comment("Disable modded portals in exploration dimensions")
            .define("disable_modded_portals", true);
            
        preventModdedTeleports = builder
            .comment("Prevent modded teleportation methods")
            .define("prevent_modded_teleports", false);
            
        builder.pop();
        
        // Gameplay Settings
        builder.push("gameplay");
        
        teleportCooldown = builder
            .comment("Cooldown in ticks between teleports (20 ticks = 1 second)")
            .defineInRange("teleport_cooldown", 100, 0, 6000);
            
        restrictToCurrentDimension = builder
            .comment("Restrict teleportation to only the exploration version of the current dimension",
                    "When true: players in overworld can only teleport to exploration_overworld",
                    "When true: players cannot teleport between exploration dimensions",
                    "When false: players can teleport to any enabled exploration dimension",
                    "When false: players can hop between exploration dimensions without returning first")
            .define("restrict_to_current_dimension", false);
            
        builder.pop();
        
        // Performance Settings
        builder.push("performance");
        
        chunkUnloadDelay = builder
            .comment("Ticks before unloading chunks in exploration dimensions")
            .defineInRange("chunk_unload_delay", 60, 20, 600);
            
        maxChunksPerPlayer = builder
            .comment("Maximum loaded chunks per player in exploration dimensions")
            .defineInRange("max_chunks_per_player", 49, 9, 121);
            
        aggressiveChunkUnloading = builder
            .comment("Use aggressive chunk unloading in exploration dimensions")
            .define("aggressive_chunk_unloading", true);
            
        entityCleanupInterval = builder
            .comment("Ticks between entity cleanup runs")
            .defineInRange("entity_cleanup_interval", 1200, 200, 6000);
            
        preventDiskSaves = builder
            .comment("Prevent exploration dimensions from saving to disk (improves performance)")
            .define("prevent_disk_saves", true);
            
        oldDimensionRetentionCount = builder
            .comment("Number of old exploration dimension folders to keep on disk (older ones are deleted)")
            .defineInRange("old_dimension_retention_count", 3, 0, 10);
            
        builder.pop();
        
        // Chunk Pre-generation Settings
        builder.push("chunk_pregeneration");
        
        preGenerateSpawnChunks = builder
            .comment("Pre-generate chunks around spawn points to reduce lag for first visitor",
                    "This increases server startup time but provides smoother experience")
            .define("enable_spawn_chunk_pregeneration", true);
            
        immediateSpawnRadius = builder
            .comment("Radius in chunks to pre-generate immediately at startup (synchronous)",
                    "Recommended: 3 chunks (48 blocks). Set to 0 to disable immediate generation")
            .defineInRange("immediate_spawn_radius", 3, 0, 8);
            
        extendedSpawnRadius = builder
            .comment("Extended radius in chunks to pre-generate asynchronously after startup",
                    "Recommended: 8 chunks (128 blocks). Must be >= immediate_spawn_radius")
            .defineInRange("extended_spawn_radius", 8, 0, 16);
            
        builder.pop();
        
        // Safety Settings
        builder.push("safety");
        
        teleportSafetyRadius = builder
            .comment("Radius in blocks to search for safe teleport positions")
            .defineInRange("teleport_safety_radius", 16, 4, 64);
            
        createEmergencyPlatforms = builder
            .comment("Create emergency obsidian platforms when no safe position is found")
            .define("create_emergency_platforms", true);
            
        builder.pop();
        
        // Messages
        builder.push("messages");
        
        welcomeMessage = builder
            .comment("Message shown when entering exploration dimension")
            .define("welcome_message", 
                "Welcome to the exploration dimension! This world will reset with a new seed on server restart.");
            
        returnMessage = builder
            .comment("Message shown when returning to normal dimension")
            .define("return_message", 
                "Returned to the main world.");
            
        builder.pop();
        
        SPEC = builder.build();
    }
    
    /**
     * Validate configuration values and log warnings for potentially problematic settings
     */
    public static void validateConfig() {
        // Warn about extremely large world borders
        int border = explorationBorder.get();
        if (border > 10000000 && border != -1) {
            com.mojang.logging.LogUtils.getLogger().warn(
                "Exploration border is set to {} which is extremely large and may cause performance issues", 
                border
            );
        }
        
        // Warn if no dimensions are enabled
        if (enabledDimensions.get().isEmpty()) {
            com.mojang.logging.LogUtils.getLogger().warn(
                "No dimensions are enabled for exploration! The mod will not create any exploration dimensions."
            );
        }
        
        // Warn about performance settings
        if (!aggressiveChunkUnloading.get() && maxChunksPerPlayer.get() > 81) {
            com.mojang.logging.LogUtils.getLogger().warn(
                "Aggressive chunk unloading is disabled but max chunks per player is high ({}). " +
                "This may cause memory issues with many players.", 
                maxChunksPerPlayer.get()
            );
        }
        
        // Validate seed strategy
        String strategy = seedStrategy.get().toLowerCase();
        if (!strategy.equals("random") && !strategy.equals("date-based") && !strategy.equals("date")) {
            com.mojang.logging.LogUtils.getLogger().error(
                "Invalid seed strategy '{}'. Using 'random' instead.", strategy
            );
        }
        
        // Validate chunk pre-generation settings
        if (immediateSpawnRadius.get() > extendedSpawnRadius.get()) {
            com.mojang.logging.LogUtils.getLogger().warn(
                "Immediate spawn radius ({}) is larger than extended spawn radius ({}). " +
                "This configuration doesn't make sense.", 
                immediateSpawnRadius.get(), extendedSpawnRadius.get()
            );
        }
        
        if (preGenerateSpawnChunks.get() && enabledDimensions.get().size() > 10) {
            com.mojang.logging.LogUtils.getLogger().warn(
                "Chunk pre-generation is enabled with {} dimensions. " +
                "This may significantly increase server startup time.", 
                enabledDimensions.get().size()
            );
        }
    }
}