/*
 * Brecher's Dimensions - Temporary resettable dimensions for exploration
 * Copyright (C) 2025 Einbrecher. All rights reserved.
 */

package net.tinkstav.brecher_dim.dimension;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.TheEndBiomeSource;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Holder;
import net.tinkstav.brecher_dim.BrecherDimensions;
import net.tinkstav.brecher_dim.platform.Services;
import net.tinkstav.brecher_dim.util.DimensionCounterUtil;
import org.slf4j.Logger;

/**
 * Factory for creating exploration dimensions at server startup
 * 
 * Uses platform-specific dimension creation to create dimensions without datapacks.
 * Each dimension gets a unique ID from per-dimension-type counters, resulting in
 * names like exploration_overworld_0, exploration_nether_0, exploration_the_end_0
 */
public class DynamicDimensionFactory {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    /**
     * Creates a new exploration dimension at server startup
     * 
     * @param server The Minecraft server instance
     * @param baseDimensionLocation The resource location of the base dimension to copy
     * @param seed The seed for the new dimension
     * @return The ServerLevel for exploration, or null if creation failed
     */
    public static ServerLevel createExplorationDimension(MinecraftServer server, 
                                                       ResourceLocation baseDimensionLocation, 
                                                       long seed) {
        try {
            // Check if runtime dimension creation is supported
            boolean isSupported = false;
            try {
                isSupported = Services.DIMENSIONS.supportsRuntimeDimensionCreation();
                LOGGER.info("Runtime dimension support check: {}", isSupported);
            } catch (Exception e) {
                LOGGER.error("Failed to check runtime dimension support", e);
            }
            
            if (!isSupported) {
                LOGGER.error("Runtime dimension creation is not supported on this platform");
                return null;
            }
            
            // We need to pass a valid dimension type even though it will be replaced
            // with the appropriate vanilla type in MixinMinecraftServer
            // This avoids "null parameters" errors in platform implementations
            String baseName = baseDimensionLocation.getPath();
            
            // Create a basic dimension type that will be replaced
            DimensionType dimensionType = new DimensionType(
                java.util.OptionalLong.empty(), // fixed time
                true,  // hasSkyLight
                false, // hasCeiling  
                false, // ultraWarm
                true,  // natural
                1.0D,  // coordinateScale
                true,  // bedWorks
                false, // respawnAnchorWorks
                -64,   // minY
                384,   // height
                384,   // logicalHeight
                net.minecraft.tags.BlockTags.INFINIBURN_OVERWORLD,
                ResourceLocation.parse("minecraft:overworld"),
                0.0F,  // ambientLight
                new DimensionType.MonsterSettings(false, true, 
                    net.minecraft.util.valueproviders.UniformInt.of(0, 7), 0)
            );
            
            // Generate unique dimension key using per-dimension-type counter
            long dimensionId = DimensionCounterUtil.getNextDimensionId(baseDimensionLocation);
            ResourceKey<Level> explorationKey = ResourceKey.create(
                Registries.DIMENSION,
                ResourceLocation.fromNamespaceAndPath(BrecherDimensions.MOD_ID, 
                    "exploration_" + baseDimensionLocation.getPath() + "_" + dimensionId)
            );
            
            // CRITICAL: Register the seed and set context BEFORE creating chunk generator
            // This ensures structure generation uses the correct seed
            ExplorationSeedManager.registerDimensionSeed(explorationKey, seed);
            ExplorationSeedManager.setCurrentDimension(explorationKey);
            
            try {
                // Create appropriate chunk generator based on dimension type
                ChunkGenerator chunkGenerator = createChunkGeneratorForDimension(server, baseName, seed);
                
                LOGGER.info("Using {} generator for exploration dimension with seed {}", 
                           chunkGenerator.getClass().getSimpleName(), seed);
                
                // Create LevelStem for the dimension with a direct holder
                var levelStem = new net.minecraft.world.level.dimension.LevelStem(
                    Holder.direct(dimensionType),
                    chunkGenerator
                );
                
                // Use platform-specific dimension creation
                ServerLevel explorationLevel = Services.DIMENSIONS.createDimension(
                    server,
                    explorationKey,
                    levelStem,
                    seed
                );
                
                if (explorationLevel != null) {
                    LOGGER.info("Successfully created runtime dimension: {} with seed: {}", explorationKey.location(), seed);
                    return explorationLevel;
                } else {
                    LOGGER.error("Platform failed to create runtime dimension");
                }
            } finally {
                // Always clear the context to prevent leaks
                ExplorationSeedManager.clearCurrentDimension();
            }
            
            return null;
            
        } catch (Exception e) {
            LOGGER.error("Failed to create exploration dimension for {}", baseDimensionLocation, e);
            return null;
        }
    }
    
    /**
     * Creates an appropriate chunk generator for the given dimension type
     */
    private static ChunkGenerator createChunkGeneratorForDimension(MinecraftServer server, String dimensionName, long seed) {
        var registries = server.registryAccess();
        
        // Get the appropriate noise settings based on dimension type
        var noiseSettingsRegistry = registries.registryOrThrow(Registries.NOISE_SETTINGS);
        Holder<NoiseGeneratorSettings> noiseSettings;
        BiomeSource biomeSource;
        
        // Get the multi-noise biome source parameters from the server
        var multiNoisePresets = registries.registryOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST);
        
        switch (dimensionName) {
            case "overworld" -> {
                noiseSettings = noiseSettingsRegistry.getHolderOrThrow(NoiseGeneratorSettings.OVERWORLD);
                // Get overworld preset using resource key
                var overworldPreset = ResourceKey.create(
                    Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST,
                    ResourceLocation.parse("minecraft:overworld")
                );
                biomeSource = MultiNoiseBiomeSource.createFromPreset(
                    multiNoisePresets.getHolderOrThrow(overworldPreset)
                );
            }
            case "the_nether" -> {
                noiseSettings = noiseSettingsRegistry.getHolderOrThrow(NoiseGeneratorSettings.NETHER);
                // Get nether preset using resource key
                var netherPreset = ResourceKey.create(
                    Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST,
                    ResourceLocation.parse("minecraft:nether")
                );
                biomeSource = MultiNoiseBiomeSource.createFromPreset(
                    multiNoisePresets.getHolderOrThrow(netherPreset)
                );
            }
            case "the_end" -> {
                noiseSettings = noiseSettingsRegistry.getHolderOrThrow(NoiseGeneratorSettings.END);
                // The End uses a special biome source
                var biomeRegistry = registries.registryOrThrow(Registries.BIOME);
                biomeSource = TheEndBiomeSource.create(biomeRegistry.asLookup());
            }
            default -> {
                // Fallback to overworld for unknown dimensions
                LOGGER.warn("Unknown dimension type {}, using overworld generator", dimensionName);
                noiseSettings = noiseSettingsRegistry.getHolderOrThrow(NoiseGeneratorSettings.OVERWORLD);
                var overworldPreset = ResourceKey.create(
                    Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST,
                    ResourceLocation.parse("minecraft:overworld")
                );
                biomeSource = MultiNoiseBiomeSource.createFromPreset(
                    multiNoisePresets.getHolderOrThrow(overworldPreset)
                );
            }
        }
        
        // Create the chunk generator with the appropriate settings
        return new NoiseBasedChunkGenerator(biomeSource, noiseSettings);
    }
    
    /**
     * Removes a runtime exploration dimension
     * Note: This method exists but is never called - dimensions persist until server shutdown
     */
    public static void removeExplorationDimension(MinecraftServer server, ResourceKey<Level> dimensionKey) {
        Services.DIMENSIONS.removeDimension(server, dimensionKey);
        LOGGER.info("Removed runtime dimension: {}", dimensionKey.location());
    }
}