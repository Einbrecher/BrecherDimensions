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

package net.tinkstav.brecher_dim.neoforge.mixin;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.Lifecycle;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.tags.TagKey;
import net.minecraft.util.valueproviders.UniformInt;
import java.util.OptionalLong;
import java.lang.reflect.Method;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.level.storage.WorldData;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.tinkstav.brecher_dim.BrecherDimensions;
import net.tinkstav.brecher_dim.config.BrecherConfig;
import net.tinkstav.brecher_dim.dimension.DimensionRegistrar;
import net.tinkstav.brecher_dim.dimension.ExplorationSeedManager;
import net.tinkstav.brecher_dim.network.BrecherNetworking;
import net.tinkstav.brecher_dim.teleport.TeleportHandler;
import net.tinkstav.brecher_dim.accessor.IServerDimensionAccessor;
import net.tinkstav.brecher_dim.accessor.IRegistryAccessor;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer implements IServerDimensionAccessor {
    @Unique
    private static final Logger LOGGER = LogUtils.getLogger();
    
    @Shadow @Final private Map<ResourceKey<Level>, ServerLevel> levels;
    @Shadow @Final protected LevelStorageSource.LevelStorageAccess storageSource;
    @Shadow @Final private Executor executor;
    // Avoid using @Shadow for registryAccess() due to mapping conflicts between Fabric/NeoForge
    // We'll access it via the server instance instead
    // Removed @Shadow abstract methods to avoid cross-platform mapping issues
    // We'll access these through the server instance instead
    @Shadow @Final private WorldData worldData;
    
    @Unique
    private final Map<ResourceKey<Level>, ServerLevel> brecher_dim$runtimeLevels = new ConcurrentHashMap<>();
    
    @Unique
    private final List<BorderChangeListener> brecher_dim$borderListeners = new ArrayList<>();
    
    /**
     * Create a new dimension during server startup with proper registry manipulation
     * Called by DynamicDimensionFactory to create exploration dimensions
     */
    @Unique
    public ServerLevel brecher_dim$createRuntimeDimension(
            ResourceKey<Level> dimensionKey,
            DimensionType dimensionType,
            ChunkGenerator chunkGenerator,
            long seed) {
        
        MinecraftServer server = (MinecraftServer)(Object)this;
        
        // Validate inputs
        if (dimensionKey == null || dimensionType == null || chunkGenerator == null) {
            LOGGER.error("Cannot create dimension with null parameters");
            return null;
        }
        
        // Check if dimension already exists
        if (levels.containsKey(dimensionKey)) {
            LOGGER.warn("Dimension {} already exists", dimensionKey.location());
            return levels.get(dimensionKey);
        }
        
        try {
            // Register dimension type if needed
            Registry<DimensionType> dimTypeRegistry = server.registryAccess().registryOrThrow(Registries.DIMENSION_TYPE);
            ResourceKey<DimensionType> dimTypeKey = ResourceKey.create(
                Registries.DIMENSION_TYPE, 
                dimensionKey.location()
            );
            
            // Check if dimension type is already registered
            Holder<DimensionType> dimTypeHolder;
            if (dimTypeRegistry.containsKey(dimTypeKey)) {
                dimTypeHolder = dimTypeRegistry.getHolderOrThrow(dimTypeKey);
                LOGGER.debug("Using existing dimension type: {}", dimTypeKey.location());
            } else {
                // Use vanilla dimension types instead of registering custom ones
                // This avoids registry manipulation issues at runtime
                String dimensionName = dimensionKey.location().getPath();
                ResourceKey<DimensionType> vanillaTypeKey;
                
                if (dimensionName.contains("nether")) {
                    vanillaTypeKey = ResourceKey.create(
                        Registries.DIMENSION_TYPE, 
                        ResourceLocation.fromNamespaceAndPath("minecraft", "the_nether")
                    );
                    LOGGER.info("Using vanilla nether dimension type for {}", dimensionKey.location());
                } else if (dimensionName.contains("the_end")) {
                    vanillaTypeKey = ResourceKey.create(
                        Registries.DIMENSION_TYPE, 
                        ResourceLocation.fromNamespaceAndPath("minecraft", "the_end")
                    );
                    LOGGER.info("Using vanilla end dimension type for {}", dimensionKey.location());
                } else {
                    vanillaTypeKey = ResourceKey.create(
                        Registries.DIMENSION_TYPE, 
                        ResourceLocation.fromNamespaceAndPath("minecraft", "overworld")
                    );
                    LOGGER.info("Using vanilla overworld dimension type for {}", dimensionKey.location());
                }
                
                try {
                    dimTypeHolder = dimTypeRegistry.getHolderOrThrow(vanillaTypeKey);
                } catch (Exception e) {
                    LOGGER.error("Failed to get vanilla dimension type {}, using overworld as ultimate fallback", 
                        vanillaTypeKey.location(), e);
                    // Ultimate fallback to overworld
                    ResourceKey<DimensionType> overworldKey = ResourceKey.create(
                        Registries.DIMENSION_TYPE, 
                        ResourceLocation.fromNamespaceAndPath("minecraft", "overworld")
                    );
                    dimTypeHolder = dimTypeRegistry.getHolderOrThrow(overworldKey);
                }
            }
            
            // Create level stem with proper holder
            LevelStem levelStem = new LevelStem(
                dimTypeHolder,
                chunkGenerator
            );
            
            // Register level stem
            Registry<LevelStem> stemRegistry = server.registryAccess().registryOrThrow(Registries.LEVEL_STEM);
            ResourceKey<LevelStem> stemKey = ResourceKey.create(
                Registries.LEVEL_STEM, 
                dimensionKey.location()
            );
            
            // Register level stem using enhanced registry
            if (!stemRegistry.containsKey(stemKey)) {
                if (stemRegistry instanceof IRegistryAccessor) {
                    @SuppressWarnings("unchecked")
                    IRegistryAccessor<LevelStem> stemAccessor = (IRegistryAccessor<LevelStem>) stemRegistry;
                    
                    try {
                        stemAccessor.brecher_dim$registerRuntime(stemKey, levelStem);
                        if (stemRegistry.containsKey(stemKey)) {
                            LOGGER.info("Successfully registered level stem: {}", stemKey.location());
                        } else {
                            LOGGER.warn("Level stem registration appeared to succeed but key not found: {}", stemKey.location());
                        }
                    } catch (Exception e) {
                        LOGGER.warn("Failed to register level stem {}: {}", stemKey.location(), e.getMessage());
                        // Continue without registration - dimension can still be created
                    }
                } else {
                    LOGGER.warn("Level stem registry is not enhanced with mixin - skipping registration for {}", stemKey.location());
                }
            } else {
                LOGGER.debug("Level stem {} already registered", stemKey.location());
            }
            
            // Create level data
            ServerLevelData levelData = brecher_dim$createRuntimeLevelData(dimensionKey, seed);
            
            // Create a no-op progress listener for runtime dimensions
            net.minecraft.server.level.progress.ChunkProgressListener progressListener = 
                new net.minecraft.server.level.progress.ChunkProgressListener() {
                    @Override
                    public void updateSpawnPos(net.minecraft.world.level.ChunkPos chunkPos) {
                        // No-op for exploration dimensions
                    }
                    
                    @Override
                    public void onStatusChange(net.minecraft.world.level.ChunkPos chunkPos, 
                                             net.minecraft.world.level.chunk.status.ChunkStatus chunkStatus) {
                        // No-op for exploration dimensions
                    }
                    
                    @Override
                    public void start() {
                        // No-op
                    }
                    
                    @Override
                    public void stop() {
                        // No-op
                    }
                };
            
            // Set dimension context for RandomState creation
            ExplorationSeedManager.setCurrentDimension(dimensionKey);
            // Register the seed BEFORE creating the ServerLevel so RandomState can use it
            ExplorationSeedManager.registerDimensionSeed(dimensionKey, seed);
            LOGGER.info("Setting dimension context for {} with seed {}", dimensionKey.location(), seed);
            
            // IMPORTANT: Create a fresh chunk generator with the modified seed
            // This ensures each exploration dimension gets its own RandomState
            ChunkGenerator freshChunkGenerator = chunkGenerator;
            if (chunkGenerator instanceof NoiseBasedChunkGenerator) {
                // For noise-based generators, we need to ensure a fresh RandomState will be created
                // The seed parameter in ServerLevel constructor will be used to create the RandomState
                LOGGER.info("Creating ServerLevel with fresh RandomState for seed {}", seed);
            }
            
            // Create the ServerLevel
            ServerLevel newLevel = new ServerLevel(
                server,
                executor,
                storageSource,
                levelData,
                dimensionKey,
                levelStem,
                progressListener, // Use proper progress listener for runtime dimensions
                false, // not debug
                seed,  // This seed will be used to create the RandomState
                List.of(), // no special spawn
                true, // should tick time
                null  // random sequence source
            );
            
            // Clear the dimension context immediately after ServerLevel creation
            // This prevents the context from affecting other dimension processing
            ExplorationSeedManager.clearCurrentDimension();
            LOGGER.info("Immediately cleared dimension context for {} after creation", dimensionKey.location());
            
            // Also schedule a cleanup on next tick as a safety measure
            server.execute(() -> {
                ExplorationSeedManager.clearCurrentDimension();
                LOGGER.debug("Safety cleanup of dimension context completed");
            });
            
            // Initialize world border
            try {
                BorderChangeListener listener = new BorderChangeListener.DelegateBorderChangeListener(newLevel.getWorldBorder());
                ServerLevel overworld = server.overworld();
                if (overworld != null) {
                    overworld.getWorldBorder().addListener(listener);
                    brecher_dim$borderListeners.add(listener);
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to setup world border listener for {}", dimensionKey.location(), e);
            }
            
            // Add to server's level map
            levels.put(dimensionKey, newLevel);
            brecher_dim$runtimeLevels.put(dimensionKey, newLevel);
            
            // Validate registry state after creation
            if (dimTypeRegistry instanceof IRegistryAccessor) {
                @SuppressWarnings("unchecked")
                IRegistryAccessor<DimensionType> accessor = (IRegistryAccessor<DimensionType>) dimTypeRegistry;
                
                if (!accessor.brecher_dim$validateRegistryState()) {
                    LOGGER.warn("Registry state validation failed after dimension creation: {}", dimensionKey.location());
                }
            }
            
            // Notify all players with proper registry sync
            brecher_dim$syncDimensionToAllPlayers(server, dimensionKey, dimTypeKey, levelStem);
            
            LOGGER.info("Successfully created runtime dimension: {}", dimensionKey.location());
            return newLevel;
            
        } catch (Exception e) {
            LOGGER.error("Failed to create runtime dimension: {}", dimensionKey.location(), e);
            // Attempt cleanup on failure
            brecher_dim$cleanupFailedDimension(dimensionKey);
            return null;
        }
    }
    
    @Unique
    private void brecher_dim$cleanupFailedDimension(ResourceKey<Level> dimensionKey) {
        try {
            // Remove from runtime levels map
            brecher_dim$runtimeLevels.remove(dimensionKey);
            
            // Remove from server levels map
            levels.remove(dimensionKey);
            
            // Clean up any partial registry entries
            brecher_dim$cleanupRegistryEntries(dimensionKey);
            
            LOGGER.debug("Cleaned up failed dimension creation: {}", dimensionKey.location());
        } catch (Exception e) {
            LOGGER.error("Failed to cleanup failed dimension: {}", dimensionKey.location(), e);
        }
    }
    
    /**
     * Remove a runtime dimension (unused in practice - dimensions persist until server shutdown)
     * This method exists for completeness but exploration dimensions are never removed during runtime
     */
    @Unique
    public void brecher_dim$removeRuntimeDimension(ResourceKey<Level> dimensionKey) {
        ServerLevel level = brecher_dim$runtimeLevels.remove(dimensionKey);
        if (level != null) {
            MinecraftServer server = (MinecraftServer)(Object)this;
            
            // Evacuate all players first
            ServerLevel overworld = server.overworld();
            if (overworld != null) {
                level.players().forEach(player -> {
                    try {
                        Vec3 spawnPos = Vec3.atCenterOf(overworld.getSharedSpawnPos());
                        DimensionTransition transition = new DimensionTransition(
                            overworld, spawnPos, Vec3.ZERO, 0.0f, 0.0f, 
                            DimensionTransition.PLAY_PORTAL_SOUND
                        );
                        player.changeDimension(transition);
                    } catch (Exception e) {
                        LOGGER.error("Failed to evacuate player {} from dimension {}", 
                            player.getName().getString(), dimensionKey.location(), e);
                        // Force safe teleport as fallback
                        try {
                            TeleportHandler.forceReturnToSpawn(player);
                        } catch (Exception fallbackException) {
                            LOGGER.error("Failed to use safe fallback teleport for player {}", player.getName().getString(), fallbackException);
                            // Ultimate fallback - at least try to find safe Y position
                            BlockPos spawnPos = overworld.getSharedSpawnPos();
                            while (spawnPos.getY() > overworld.getMinBuildHeight() && !overworld.getBlockState(spawnPos.below()).isSolid()) {
                                spawnPos = spawnPos.below();
                            }
                            while (spawnPos.getY() < overworld.getMaxBuildHeight() && overworld.getBlockState(spawnPos).isSolid()) {
                                spawnPos = spawnPos.above();
                            }
                            player.teleportTo(overworld, 
                                spawnPos.getX() + 0.5,
                                spawnPos.getY(),
                                spawnPos.getZ() + 0.5,
                                0, 0);
                        }
                    }
                });
            }
            
            // Remove border listeners
            brecher_dim$borderListeners.forEach(listener -> {
                try {
                    ServerLevel overworldLevel = server.overworld();
                    if (overworldLevel != null) {
                        overworldLevel.getWorldBorder().removeListener(listener);
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to remove border listener", e);
                }
            });
            brecher_dim$borderListeners.clear();
            
            // Save and close level
            try {
                if (!BrecherConfig.isPreventDiskSaves()) {
                    level.save(null, true, false);
                }
                level.close();
            } catch (Exception e) {
                LOGGER.error("Error closing runtime dimension: {}", dimensionKey.location(), e);
            }
            
            // Remove from server
            levels.remove(dimensionKey);
            
            // Clean up registry entries
            brecher_dim$cleanupRegistryEntries(dimensionKey);
            
            LOGGER.info("Removed runtime dimension: {}", dimensionKey.location());
        }
    }
    
    /**
     * Get all runtime levels
     */
    @Unique
    public Map<ResourceKey<Level>, ServerLevel> brecher_dim$getRuntimeLevels() {
        return new HashMap<>(brecher_dim$runtimeLevels);
    }
    
    @Unique
    private ServerLevelData brecher_dim$createRuntimeLevelData(ResourceKey<Level> dimension, long seed) {
        MinecraftServer server = (MinecraftServer)(Object)this;
        ServerLevel overworld = server.overworld();
        ServerLevelData overworldData = ((IServerLevelAccessor) overworld).getServerLevelData();
        
        // Create derived level data
        // Note: In 1.20.1, the seed is provided through the ServerLevel constructor,
        // not through the ServerLevelData. The seed parameter in ServerLevel constructor
        // will be used to initialize the RandomState for world generation.
        return new DerivedLevelData(worldData, overworldData) {
            @Override
            public String getLevelName() {
                return dimension.location().toString();
            }
        };
    }
    
    @Unique
    private int brecher_dim$findAvailableId(Registry<?> registry) {
        // Start from registry size to avoid conflicts with existing entries
        int baseId = registry.size();
        
        // For safety, add some padding to avoid conflicts
        // Use mod ID hash to ensure uniqueness across different mod dimensions
        int modOffset = Math.abs(BrecherDimensions.MOD_ID.hashCode() % 1000);
        
        return baseId + modOffset;
    }
    
    @Unique
    private void brecher_dim$syncDimensionToAllPlayers(MinecraftServer server, 
                                                       ResourceKey<Level> dimensionKey,
                                                       ResourceKey<DimensionType> dimTypeKey,
                                                       LevelStem levelStem) {
        try {
            // Send sync packets to all players
            server.getPlayerList().getPlayers().forEach(player -> {
                try {
                    BrecherNetworking.sendDimensionSync(player, dimensionKey.location(), true);
                } catch (Exception e) {
                    LOGGER.error("Failed to sync dimension {} to player {}", 
                        dimensionKey.location(), player.getName().getString(), e);
                }
            });
            
            LOGGER.debug("Synced dimension {} to {} players", 
                dimensionKey.location(), server.getPlayerList().getPlayerCount());
                
        } catch (Exception e) {
            LOGGER.error("Failed to sync dimension to players: {}", dimensionKey.location(), e);
        }
    }
    
    @Unique
    private void brecher_dim$cleanupRegistryEntries(ResourceKey<Level> dimensionKey) {
        MinecraftServer server = (MinecraftServer)(Object)this;
        try {
            // Clean up dimension type registry
            Registry<DimensionType> dimTypeRegistry = server.registryAccess().registryOrThrow(Registries.DIMENSION_TYPE);
            ResourceKey<DimensionType> dimTypeKey = ResourceKey.create(
                Registries.DIMENSION_TYPE, 
                dimensionKey.location()
            );
            
            if (dimTypeRegistry instanceof IRegistryAccessor) {
                @SuppressWarnings("unchecked")
                IRegistryAccessor<DimensionType> accessor = (IRegistryAccessor<DimensionType>) dimTypeRegistry;
                
                try {
                    accessor.brecher_dim$removeRuntimeEntry(dimTypeKey);
                    LOGGER.debug("Cleaned up dimension type registry entry: {}", dimTypeKey.location());
                } catch (Exception e) {
                    LOGGER.debug("Could not clean up dimension type registry entry: {}", dimTypeKey.location());
                }
            } else {
                LOGGER.debug("Dimension type registry is not enhanced - skipping cleanup for {}", dimTypeKey.location());
            }
            
            // Clean up level stem registry
            Registry<LevelStem> stemRegistry = server.registryAccess().registryOrThrow(Registries.LEVEL_STEM);
            ResourceKey<LevelStem> stemKey = ResourceKey.create(
                Registries.LEVEL_STEM, 
                dimensionKey.location()
            );
            
            if (stemRegistry instanceof IRegistryAccessor) {
                @SuppressWarnings("unchecked")
                IRegistryAccessor<LevelStem> stemAccessor = (IRegistryAccessor<LevelStem>) stemRegistry;
                
                try {
                    stemAccessor.brecher_dim$removeRuntimeEntry(stemKey);
                    LOGGER.debug("Cleaned up level stem registry entry: {}", stemKey.location());
                } catch (Exception e) {
                    LOGGER.debug("Could not clean up level stem registry entry: {}", stemKey.location());
                }
            } else {
                LOGGER.debug("Level stem registry is not enhanced - skipping cleanup for {}", stemKey.location());
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to cleanup registry entries for dimension: {}", dimensionKey.location(), e);
        }
    }
    
    @Unique
    public void brecher_dim$cleanupAllRuntimeDimensions() {
        MinecraftServer server = (MinecraftServer)(Object)this;
        // Get a copy of the keys to avoid concurrent modification
        List<ResourceKey<Level>> dimensionsToRemove = new ArrayList<>(brecher_dim$runtimeLevels.keySet());
        
        for (ResourceKey<Level> dimensionKey : dimensionsToRemove) {
            brecher_dim$removeRuntimeDimension(dimensionKey);
        }
        
        // Clean up all runtime registry entries
        try {
            Registry<DimensionType> dimTypeRegistry = server.registryAccess().registryOrThrow(Registries.DIMENSION_TYPE);
            if (dimTypeRegistry instanceof IRegistryAccessor) {
                @SuppressWarnings("unchecked")
                IRegistryAccessor<DimensionType> accessor = (IRegistryAccessor<DimensionType>) dimTypeRegistry;
                accessor.brecher_dim$cleanupAllRuntimeEntries();
                LOGGER.debug("Cleaned up all dimension type registry entries");
            }
            
            Registry<LevelStem> stemRegistry = server.registryAccess().registryOrThrow(Registries.LEVEL_STEM);
            if (stemRegistry instanceof IRegistryAccessor) {
                @SuppressWarnings("unchecked")
                IRegistryAccessor<LevelStem> stemAccessor = (IRegistryAccessor<LevelStem>) stemRegistry;
                stemAccessor.brecher_dim$cleanupAllRuntimeEntries();
                LOGGER.debug("Cleaned up all level stem registry entries");
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to cleanup all registry entries", e);
        }
        
        LOGGER.info("Cleaned up all runtime dimensions and registry entries");
    }
}