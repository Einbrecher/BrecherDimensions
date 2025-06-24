package net.tinkstav.brecher_dim.dimension;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.core.registries.Registries;
import net.tinkstav.brecher_dim.BrecherDimensions;
import net.tinkstav.brecher_dim.platform.DimensionPlatform;
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
            if (!DimensionPlatform.isRuntimeDimensionSupported()) {
                LOGGER.error("Runtime dimension creation is not supported on this platform");
                return null;
            }
            
            // Get the base dimension
            ResourceKey<Level> baseDimensionKey = ResourceKey.create(Registries.DIMENSION, baseDimensionLocation);
            ServerLevel baseLevel = server.getLevel(baseDimensionKey);
            
            if (baseLevel == null) {
                LOGGER.error("Base dimension {} not found", baseDimensionLocation);
                return null;
            }
            
            // Generate unique dimension key using per-dimension-type counter
            long dimensionId = DimensionCounterUtil.getNextDimensionId(baseDimensionLocation);
            ResourceKey<Level> explorationKey = ResourceKey.create(
                Registries.DIMENSION,
                ResourceLocation.fromNamespaceAndPath(BrecherDimensions.MOD_ID, 
                    "exploration_" + baseDimensionLocation.getPath() + "_" + dimensionId)
            );
            
            // Get proper dimension type for exploration
            DimensionType dimensionType;
            if (baseDimensionLocation.equals(Level.OVERWORLD.location())) {
                dimensionType = ExplorationDimensionTypes.createOverworldExploration();
            } else if (baseDimensionLocation.equals(Level.NETHER.location())) {
                dimensionType = ExplorationDimensionTypes.createNetherExploration();
            } else if (baseDimensionLocation.equals(Level.END.location())) {
                dimensionType = ExplorationDimensionTypes.createEndExploration();
            } else {
                // For custom dimensions, use the base dimension type
                dimensionType = baseLevel.dimensionType();
            }
            
            // Use the base dimension's chunk generator
            ChunkGenerator chunkGenerator = baseLevel.getChunkSource().getGenerator();
            
            LOGGER.info("Using {} generator for exploration dimension with seed {}", 
                       chunkGenerator.getClass().getSimpleName(), seed);
            
            // Use platform-specific dimension creation
            ServerLevel explorationLevel = DimensionPlatform.createRuntimeDimension(
                server,
                explorationKey,
                dimensionType,
                chunkGenerator,
                seed
            );
            
            if (explorationLevel != null) {
                // Register the seed with ExplorationSeedManager for world generation modification
                ExplorationSeedManager.registerDimensionSeed(explorationKey, seed);
                
                LOGGER.info("Successfully created runtime dimension: {} with seed: {}", explorationKey.location(), seed);
                return explorationLevel;
            } else {
                LOGGER.error("Platform failed to create runtime dimension");
            }
            
            return null;
            
        } catch (Exception e) {
            LOGGER.error("Failed to create exploration dimension for {}", baseDimensionLocation, e);
            return null;
        }
    }
    
    /**
     * Removes a runtime exploration dimension
     * Note: This method exists but is never called - dimensions persist until server shutdown
     */
    public static void removeExplorationDimension(MinecraftServer server, ResourceKey<Level> dimensionKey) {
        DimensionPlatform.removeRuntimeDimension(server, dimensionKey);
        LOGGER.info("Removed runtime dimension: {}", dimensionKey.location());
    }
}