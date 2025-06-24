package net.tinkstav.brecher_dim.fabric;

import net.fabricmc.fabric.api.dimension.v1.FabricDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.level.storage.WorldData;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * Fabric implementation of DimensionPlatform
 * Handles runtime dimension creation and removal for Fabric
 */
public class FabricDimensionPlatform {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public static ServerLevel createRuntimeDimension(MinecraftServer server,
                                                   ResourceKey<Level> dimensionKey,
                                                   DimensionType dimensionType,
                                                   ChunkGenerator chunkGenerator,
                                                   long seed) {
        try {
            LOGGER.info("Creating runtime dimension: {} with seed: {}", dimensionKey.location(), seed);
            
            // Get the overworld as a template
            ServerLevel overworld = server.getLevel(Level.OVERWORLD);
            if (overworld == null) {
                LOGGER.error("Overworld not found, cannot create runtime dimension");
                return null;
            }
            
            // Create dimension configuration
            LevelStem levelStem = new LevelStem(
                server.registryAccess().registryOrThrow(Registries.DIMENSION_TYPE).getHolderOrThrow(
                    ResourceKey.create(Registries.DIMENSION_TYPE, dimensionKey.location())
                ),
                chunkGenerator
            );
            
            // Create derived level data from overworld
            WorldData worldData = server.getWorldData();
            ServerLevelData overworldData = overworld.serverLevelData;
            DerivedLevelData derivedData = new DerivedLevelData(worldData, overworldData);
            
            // Get executor and chunk progress listener
            Executor executor = server.executor;
            ChunkProgressListener chunkProgressListener = server.progressListenerFactory.create(11);
            
            // Create the new level
            ServerLevel newLevel = new ServerLevel(
                server,
                executor,
                server.storageSource,
                derivedData,
                dimensionKey,
                levelStem,
                chunkProgressListener,
                worldData.worldGenOptions().isDebug(),
                BiomeManager.obfuscateSeed(seed),
                List.of(), // Custom spawners
                false, // Should tick time  
                null  // Random sequences
            );
            
            // Set up world border
            WorldBorder worldBorder = newLevel.getWorldBorder();
            if (overworldData.getWorldBorder() != null) {
                worldBorder.applySettings(overworldData.getWorldBorder());
            }
            
            // Add border listener to sync with overworld
            overworld.getWorldBorder().addListener(new BorderChangeListener.DelegateBorderChangeListener(worldBorder));
            
            // Register the level with the server
            server.levels.put(dimensionKey, newLevel);
            
            // Initialize spawn chunks if needed
            if (!server.isSingleplayer()) {
                BlockPos spawnPos = newLevel.getSharedSpawnPos();
                int spawnRadius = server.getGameRules().getInt(net.minecraft.world.level.GameRules.RULE_SPAWN_RADIUS);
                if (spawnRadius > 0) {
                    // Pre-generate spawn chunks
                    net.minecraft.server.level.ChunkMap chunkMap = newLevel.getChunkSource().chunkMap;
                    chunkMap.addRegionTicket(
                        net.minecraft.server.level.TicketType.START,
                        new net.minecraft.world.level.ChunkPos(spawnPos),
                        11,
                        net.minecraft.util.Unit.INSTANCE
                    );
                }
            }
            
            LOGGER.info("Successfully created runtime dimension: {}", dimensionKey.location());
            return newLevel;
            
        } catch (Exception e) {
            LOGGER.error("Failed to create runtime dimension: {}", dimensionKey.location(), e);
            return null;
        }
    }
    
    public static void removeRuntimeDimension(MinecraftServer server, ResourceKey<Level> dimensionKey) {
        try {
            LOGGER.info("Removing runtime dimension: {}", dimensionKey.location());
            
            ServerLevel level = server.getLevel(dimensionKey);
            if (level == null) {
                LOGGER.warn("Dimension {} not found, cannot remove", dimensionKey.location());
                return;
            }
            
            // Ensure no players are in the dimension
            if (!level.players().isEmpty()) {
                LOGGER.error("Cannot remove dimension {} - players still present", dimensionKey.location());
                return;
            }
            
            // Save any pending changes
            level.save(null, true, false);
            
            // Close the level
            try {
                level.close();
            } catch (Exception e) {
                LOGGER.error("Error closing level {}", dimensionKey.location(), e);
            }
            
            // Remove from server's level map
            server.levels.remove(dimensionKey);
            
            LOGGER.info("Successfully removed runtime dimension: {}", dimensionKey.location());
            
        } catch (Exception e) {
            LOGGER.error("Failed to remove runtime dimension: {}", dimensionKey.location(), e);
        }
    }
    
    public static boolean isRuntimeDimensionSupported() {
        // Fabric supports runtime dimensions in 1.21.1
        return true;
    }
}