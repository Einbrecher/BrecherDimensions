/*
 * Brecher's Dimensions - Temporary resettable dimensions for exploration
 * Copyright (C) 2025 Einbrecher. All rights reserved.
 */

package net.tinkstav.brecher_dim.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.tinkstav.brecher_dim.generation.GenerationProgress;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BrecherSavedData extends SavedData {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String NAME = "brecher_dimensions";
    
    private final Set<ResourceLocation> activeDimensions = ConcurrentHashMap.newKeySet();
    private final Map<ResourceLocation, Long> dimensionResetTimes = new ConcurrentHashMap<>();
    private final Map<UUID, ResourceLocation> playerLastPositions = new ConcurrentHashMap<>();
    private final Map<ResourceLocation, Set<UUID>> dimensionAccessHistory = new ConcurrentHashMap<>();
    private final Map<UUID, ReturnPosition> playerReturnPositions = new ConcurrentHashMap<>();
    private final Map<ResourceLocation, DimensionMetadata> dimensionMetadata = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerExplorationStats> playerStats = new ConcurrentHashMap<>();
    private final Map<UUID, ResourceLocation> playerLastKnownDimensions = new ConcurrentHashMap<>();
    private final Map<ResourceLocation, GenerationProgress> pregenTasks = new ConcurrentHashMap<>();
    private long nextResetTime = 0;
    
    public static BrecherSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
            new SavedData.Factory<>(
                BrecherSavedData::new,
                (tag, provider) -> load(tag),
                null
            ),
            NAME
        );
    }
    
    public static BrecherSavedData load(CompoundTag tag) {
        BrecherSavedData data = new BrecherSavedData();
        
        try {
            // Load active dimensions
            ListTag activeList = tag.getList("activeDimensions", Tag.TAG_STRING);
            for (int i = 0; i < activeList.size(); i++) {
                try {
                    data.activeDimensions.add(ResourceLocation.parse(activeList.getString(i)));
                } catch (Exception e) {
                    LOGGER.warn("Failed to load active dimension at index {}: {}", i, e.getMessage());
                }
            }
            
            // Load reset times
            CompoundTag resetTimes = tag.getCompound("resetTimes");
            for (String key : resetTimes.getAllKeys()) {
                try {
                    data.dimensionResetTimes.put(ResourceLocation.parse(key), resetTimes.getLong(key));
                } catch (Exception e) {
                    LOGGER.warn("Failed to load reset time for dimension {}: {}", key, e.getMessage());
                }
            }
            
            // Load player positions
            CompoundTag positions = tag.getCompound("playerPositions");
            for (String key : positions.getAllKeys()) {
                try {
                    data.playerLastPositions.put(
                        UUID.fromString(key), 
                        ResourceLocation.parse(positions.getString(key))
                    );
                } catch (Exception e) {
                    LOGGER.warn("Failed to load player position for {}: {}", key, e.getMessage());
                }
            }
            
            // Load access history
            CompoundTag accessHistory = tag.getCompound("accessHistory");
            for (String dimKey : accessHistory.getAllKeys()) {
                try {
                    ResourceLocation dimLoc = ResourceLocation.parse(dimKey);
                    Set<UUID> players = ConcurrentHashMap.newKeySet();
                    
                    ListTag playerList = accessHistory.getList(dimKey, Tag.TAG_STRING);
                    for (int i = 0; i < playerList.size(); i++) {
                        try {
                            players.add(UUID.fromString(playerList.getString(i)));
                        } catch (Exception e) {
                            LOGGER.warn("Failed to load player UUID in access history: {}", e.getMessage());
                        }
                    }
                    
                    data.dimensionAccessHistory.put(dimLoc, players);
                } catch (Exception e) {
                    LOGGER.warn("Failed to load access history for dimension {}: {}", dimKey, e.getMessage());
                }
            }
            
            // Load return positions
            CompoundTag returnPositions = tag.getCompound("returnPositions");
            for (String key : returnPositions.getAllKeys()) {
                try {
                    UUID playerId = UUID.fromString(key);
                    CompoundTag posTag = returnPositions.getCompound(key);
                    ReturnPosition pos = ReturnPosition.fromNBT(posTag);
                    // Only load non-expired positions
                    if (pos != null && !pos.isExpired()) {
                        data.playerReturnPositions.put(playerId, pos);
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to load return position for player {}: {}", key, e.getMessage());
                }
            }
            
            // Load dimension metadata
            CompoundTag metadataTag = tag.getCompound("dimensionMetadata");
            for (String key : metadataTag.getAllKeys()) {
                try {
                    ResourceLocation dimLoc = ResourceLocation.parse(key);
                    DimensionMetadata metadata = DimensionMetadata.fromNBT(metadataTag.getCompound(key));
                    if (metadata != null) {
                        data.dimensionMetadata.put(dimLoc, metadata);
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to load dimension metadata for {}: {}", key, e.getMessage());
                }
            }
            
            // Load player stats
            CompoundTag statsTag = tag.getCompound("playerStats");
            for (String key : statsTag.getAllKeys()) {
                try {
                    UUID playerId = UUID.fromString(key);
                    PlayerExplorationStats stats = PlayerExplorationStats.fromNBT(statsTag.getCompound(key));
                    if (stats != null) {
                        data.playerStats.put(playerId, stats);
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to load player stats for {}: {}", key, e.getMessage());
                }
            }
            
            // Load player last known dimensions
            CompoundTag lastDimensionsTag = tag.getCompound("playerLastKnownDimensions");
            for (String key : lastDimensionsTag.getAllKeys()) {
                try {
                    UUID playerId = UUID.fromString(key);
                    ResourceLocation dim = ResourceLocation.parse(lastDimensionsTag.getString(key));
                    data.playerLastKnownDimensions.put(playerId, dim);
                } catch (Exception e) {
                    LOGGER.warn("Failed to load last known dimension for {}: {}", key, e.getMessage());
                }
            }
            
            // Load next reset time
            data.nextResetTime = tag.getLong("nextResetTime");
            
            // Load pregen tasks
            CompoundTag pregenTag = tag.getCompound("pregenTasks");
            for (String key : pregenTag.getAllKeys()) {
                try {
                    ResourceLocation dimLoc = ResourceLocation.parse(key);
                    GenerationProgress progress = GenerationProgress.fromNbt(pregenTag.getCompound(key));
                    if (progress != null) {
                        data.pregenTasks.put(dimLoc, progress);
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to load pregen task for {}: {}", key, e.getMessage());
                }
            }

            // Load manual unlocks (progression gating)
            CompoundTag manualUnlocksTag = tag.getCompound("manualUnlocks");
            for (String playerKey : manualUnlocksTag.getAllKeys()) {
                try {
                    UUID playerId = UUID.fromString(playerKey);
                    Set<ResourceLocation> unlocks = ConcurrentHashMap.newKeySet();

                    ListTag dimensionList = manualUnlocksTag.getList(playerKey, Tag.TAG_STRING);
                    for (int i = 0; i < dimensionList.size(); i++) {
                        try {
                            unlocks.add(ResourceLocation.parse(dimensionList.getString(i)));
                        } catch (Exception e) {
                            LOGGER.warn("Failed to load manual unlock dimension: {}", e.getMessage());
                        }
                    }

                    if (!unlocks.isEmpty()) {
                        data.manualUnlocks.put(playerId, unlocks);
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to load manual unlocks for player {}: {}", playerKey, e.getMessage());
                }
            }

        } catch (Exception e) {
            LOGGER.error("Critical error loading BrecherSavedData, returning partial data", e);
        }
        
        return data;
    }
    
    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        // Save active dimensions
        ListTag activeList = new ListTag();
        for (ResourceLocation dim : activeDimensions) {
            activeList.add(StringTag.valueOf(dim.toString()));
        }
        tag.put("activeDimensions", activeList);
        
        // Save reset times
        CompoundTag resetTimes = new CompoundTag();
        dimensionResetTimes.forEach((dim, time) -> 
            resetTimes.putLong(dim.toString(), time)
        );
        tag.put("resetTimes", resetTimes);
        
        // Save player positions
        CompoundTag positions = new CompoundTag();
        playerLastPositions.forEach((uuid, dim) -> 
            positions.putString(uuid.toString(), dim.toString())
        );
        tag.put("playerPositions", positions);
        
        // Save access history
        CompoundTag accessHistory = new CompoundTag();
        dimensionAccessHistory.forEach((dim, players) -> {
            ListTag playerList = new ListTag();
            for (UUID player : players) {
                playerList.add(StringTag.valueOf(player.toString()));
            }
            accessHistory.put(dim.toString(), playerList);
        });
        tag.put("accessHistory", accessHistory);
        
        // Save return positions
        CompoundTag returnPositions = new CompoundTag();
        playerReturnPositions.forEach((uuid, pos) -> {
            // Only save non-expired positions
            if (!pos.isExpired()) {
                returnPositions.put(uuid.toString(), pos.toNBT());
            }
        });
        tag.put("returnPositions", returnPositions);
        
        // Save dimension metadata
        CompoundTag metadataTag = new CompoundTag();
        dimensionMetadata.forEach((dim, metadata) -> 
            metadataTag.put(dim.toString(), metadata.toNBT())
        );
        tag.put("dimensionMetadata", metadataTag);
        
        // Save player stats
        CompoundTag statsTag = new CompoundTag();
        playerStats.forEach((uuid, stats) -> 
            statsTag.put(uuid.toString(), stats.toNBT())
        );
        tag.put("playerStats", statsTag);
        
        // Save player last known dimensions
        CompoundTag lastDimensionsTag = new CompoundTag();
        playerLastKnownDimensions.forEach((uuid, dim) -> 
            lastDimensionsTag.putString(uuid.toString(), dim.toString())
        );
        tag.put("playerLastKnownDimensions", lastDimensionsTag);
        
        // Save next reset time
        tag.putLong("nextResetTime", nextResetTime);
        
        // Save pregen tasks
        CompoundTag pregenTag = new CompoundTag();
        pregenTasks.forEach((dim, progress) ->
            pregenTag.put(dim.toString(), progress.toNbt())
        );
        tag.put("pregenTasks", pregenTag);

        // Save manual unlocks (progression gating)
        CompoundTag manualUnlocksTag = new CompoundTag();
        manualUnlocks.forEach((playerId, dimensions) -> {
            ListTag dimensionList = new ListTag();
            for (ResourceLocation dim : dimensions) {
                dimensionList.add(StringTag.valueOf(dim.toString()));
            }
            manualUnlocksTag.put(playerId.toString(), dimensionList);
        });
        tag.put("manualUnlocks", manualUnlocksTag);

        return tag;
    }
    
    public Set<ResourceLocation> getActiveDimensions() {
        return new HashSet<>(activeDimensions);
    }
    
    public void recordDimensionAccess(ResourceLocation dimension, UUID player) {
        activeDimensions.add(dimension);
        dimensionAccessHistory.computeIfAbsent(dimension, k -> ConcurrentHashMap.newKeySet()).add(player);
        
        // Update dimension metadata
        DimensionMetadata metadata = dimensionMetadata.computeIfAbsent(dimension, 
            k -> new DimensionMetadata(dimension));
        metadata.recordAccess(player);
        
        // Update player stats
        PlayerExplorationStats stats = playerStats.computeIfAbsent(player,
            k -> new PlayerExplorationStats(player));
        stats.recordVisit(dimension);
        
        setDirty();
    }
    
    public void recordDimensionReset(ResourceKey<Level> dimension) {
        ResourceLocation dimLoc = dimension.location();
        dimensionResetTimes.put(dimLoc, System.currentTimeMillis());
        dimensionAccessHistory.remove(dimLoc);
        activeDimensions.remove(dimLoc);
        
        // Reset dimension metadata
        DimensionMetadata metadata = dimensionMetadata.get(dimLoc);
        if (metadata != null) {
            metadata.reset();
        }
        
        setDirty();
    }
    
    public void savePlayerPosition(UUID player, ResourceLocation dimension) {
        playerLastPositions.put(player, dimension);
        setDirty();
    }
    
    public Optional<ResourceLocation> getPlayerLastPosition(UUID player) {
        return Optional.ofNullable(playerLastPositions.get(player));
    }
    
    public long getLastResetTime(ResourceLocation dimension) {
        return dimensionResetTimes.getOrDefault(dimension, 0L);
    }
    
    public Set<UUID> getDimensionAccessHistory(ResourceLocation dimension) {
        return new HashSet<>(dimensionAccessHistory.getOrDefault(dimension, Collections.emptySet()));
    }
    
    public void saveReturnPosition(UUID player, BlockPos pos, ResourceLocation dimension) {
        // Get player's current rotation from server if available
        playerReturnPositions.put(player, new ReturnPosition(pos, dimension, 0, 0));
        setDirty();
    }
    
    public void saveReturnPosition(UUID player, BlockPos pos, ResourceLocation dimension, float yRot, float xRot) {
        playerReturnPositions.put(player, new ReturnPosition(pos, dimension, yRot, xRot));
        LOGGER.debug("Saved return position for player {}: {} in dimension {} (rotation: {}/{})", 
            player, pos, dimension, yRot, xRot);
        setDirty();
    }
    
    public Optional<ReturnPosition> getReturnPosition(UUID player) {
        ReturnPosition pos = playerReturnPositions.get(player);
        LOGGER.debug("Getting return position for player {}: {}", player, 
            pos != null ? pos.toString() : "null");
        return Optional.ofNullable(pos);
    }
    
    public void clearReturnPosition(UUID player) {
        playerReturnPositions.remove(player);
        setDirty();
    }
    
    /**
     * Track player's last known dimension
     */
    public void updatePlayerDimension(UUID player, ResourceLocation dimension) {
        playerLastKnownDimensions.put(player, dimension);
        setDirty();
    }
    
    /**
     * Get player's last known dimension
     */
    public Optional<ResourceLocation> getPlayerLastKnownDimension(UUID player) {
        return Optional.ofNullable(playerLastKnownDimensions.get(player));
    }
    
    /**
     * Check if player was in an exploration dimension that no longer exists
     */
    public boolean wasPlayerInExpiredExplorationDimension(UUID player, Set<ResourceLocation> currentExplorationDimensions) {
        ResourceLocation lastDim = playerLastKnownDimensions.get(player);
        if (lastDim == null) {
            return false;
        }
        
        // Check if it was an exploration dimension
        if (!lastDim.getNamespace().equals("brecher_dim") || !lastDim.getPath().startsWith("exploration_")) {
            return false;
        }
        
        // Check if it still exists
        return !currentExplorationDimensions.contains(lastDim);
    }
    
    /**
     * Clear player's dimension tracking (called after evacuation)
     */
    public void clearPlayerDimensionTracking(UUID player) {
        playerLastKnownDimensions.remove(player);
        setDirty();
    }
    
    public void setNextResetTime(long time) {
        this.nextResetTime = time;
        setDirty();
    }
    
    public long getNextResetTime() {
        return nextResetTime;
    }
    
    public Optional<DimensionMetadata> getDimensionMetadata(ResourceLocation dimension) {
        return Optional.ofNullable(dimensionMetadata.get(dimension));
    }
    
    public Optional<PlayerExplorationStats> getPlayerStats(UUID player) {
        return Optional.ofNullable(playerStats.get(player));
    }
    
    public void trackChunkLoad(UUID playerId, net.minecraft.world.level.ChunkPos chunkPos) {
        PlayerExplorationStats stats = playerStats.get(playerId);
        if (stats != null) {
            stats.recordChunkLoad(chunkPos);
            setDirty();
        }
    }
    
    public void trackChunkUnload(UUID playerId, net.minecraft.world.level.ChunkPos chunkPos) {
        PlayerExplorationStats stats = playerStats.get(playerId);
        if (stats != null) {
            stats.recordChunkUnload(chunkPos);
            setDirty();
        }
    }
    
    // Manual unlock management (Progression Gating)
    // Full implementation in Phase 4 - these are stubs for Phase 2 compatibility
    private final Map<UUID, Set<ResourceLocation>> manualUnlocks = new ConcurrentHashMap<>();

    /**
     * Check if a player has a manual unlock for a dimension.
     * Manual unlocks bypass advancement requirements.
     * @param player The player's UUID
     * @param dimension The base dimension ResourceLocation
     * @return true if the player has a manual unlock
     */
    public boolean hasManualUnlock(UUID player, ResourceLocation dimension) {
        Set<ResourceLocation> unlocks = manualUnlocks.get(player);
        return unlocks != null && unlocks.contains(dimension);
    }

    /**
     * Grant a manual unlock for a dimension to a player.
     * @param player The player's UUID
     * @param dimension The base dimension ResourceLocation
     */
    public void grantManualUnlock(UUID player, ResourceLocation dimension) {
        manualUnlocks.computeIfAbsent(player, k -> ConcurrentHashMap.newKeySet()).add(dimension);
        setDirty();
        LOGGER.info("Granted manual unlock for dimension {} to player {}", dimension, player);
    }

    /**
     * Revoke a manual unlock for a dimension from a player.
     * @param player The player's UUID
     * @param dimension The base dimension ResourceLocation
     * @return true if the unlock was removed, false if it didn't exist
     */
    public boolean revokeManualUnlock(UUID player, ResourceLocation dimension) {
        Set<ResourceLocation> unlocks = manualUnlocks.get(player);
        if (unlocks != null && unlocks.remove(dimension)) {
            setDirty();
            LOGGER.info("Revoked manual unlock for dimension {} from player {}", dimension, player);
            return true;
        }
        return false;
    }

    /**
     * Get all manual unlocks for a player.
     * @param player The player's UUID
     * @return Set of dimension ResourceLocations the player has manually unlocked
     */
    public Set<ResourceLocation> getManualUnlocks(UUID player) {
        Set<ResourceLocation> unlocks = manualUnlocks.get(player);
        return unlocks != null ? new HashSet<>(unlocks) : Collections.emptySet();
    }

    // Pregen task management
    public Map<ResourceLocation, GenerationProgress> getPregenTasks() {
        return new HashMap<>(pregenTasks);
    }
    
    public void setPregenTasks(Map<ResourceLocation, GenerationProgress> tasks) {
        pregenTasks.clear();
        pregenTasks.putAll(tasks);
        setDirty();
    }
    
    public void addPregenTask(ResourceLocation dimension, GenerationProgress progress) {
        pregenTasks.put(dimension, progress);
        setDirty();
    }
    
    public void removePregenTask(ResourceLocation dimension) {
        pregenTasks.remove(dimension);
        setDirty();
    }
    
    public Optional<GenerationProgress> getPregenTask(ResourceLocation dimension) {
        return Optional.ofNullable(pregenTasks.get(dimension));
    }
}