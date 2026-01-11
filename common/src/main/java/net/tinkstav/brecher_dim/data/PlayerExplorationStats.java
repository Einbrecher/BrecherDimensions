/*
 * Brecher's Dimensions - Temporary resettable dimensions for exploration
 * Copyright (C) 2025 Einbrecher. All rights reserved.
 */

package net.tinkstav.brecher_dim.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import java.util.*;

public class PlayerExplorationStats {
    private final UUID playerId;
    private final Map<ResourceLocation, Integer> dimensionVisits = new HashMap<>();
    private final Set<ChunkPos> loadedChunks = new HashSet<>();
    private long totalExplorationTime = 0;
    private long lastVisitStart = 0;
    
    public PlayerExplorationStats(UUID playerId) {
        this.playerId = playerId;
    }
    
    public void recordVisit(ResourceLocation dimension) {
        dimensionVisits.merge(dimension, 1, Integer::sum);
        lastVisitStart = System.currentTimeMillis();
    }
    
    public void recordChunkLoad(ChunkPos pos) {
        loadedChunks.add(pos);
    }
    
    public void recordChunkUnload(ChunkPos pos) {
        loadedChunks.remove(pos);
    }
    
    public void endVisit() {
        if (lastVisitStart > 0) {
            totalExplorationTime += (System.currentTimeMillis() - lastVisitStart);
            lastVisitStart = 0;
        }
    }
    
    public UUID getPlayerId() {
        return playerId;
    }
    
    public Map<ResourceLocation, Integer> getDimensionVisits() {
        return new HashMap<>(dimensionVisits);
    }
    
    public int getLoadedChunkCount() {
        return loadedChunks.size();
    }
    
    public long getTotalExplorationTime() {
        return totalExplorationTime;
    }
    
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("playerId", playerId);
        tag.putLong("totalExplorationTime", totalExplorationTime);
        tag.putLong("lastVisitStart", lastVisitStart);
        
        ListTag visitsList = new ListTag();
        dimensionVisits.forEach((dim, count) -> {
            CompoundTag visitTag = new CompoundTag();
            visitTag.putString("dimension", dim.toString());
            visitTag.putInt("count", count);
            visitsList.add(visitTag);
        });
        tag.put("dimensionVisits", visitsList);
        
        // For performance reasons, we don't save all loaded chunks
        tag.putInt("totalChunksLoaded", loadedChunks.size());
        
        return tag;
    }
    
    public static PlayerExplorationStats fromNBT(CompoundTag tag) {
        PlayerExplorationStats stats = new PlayerExplorationStats(tag.getUUID("playerId"));
        stats.totalExplorationTime = tag.getLong("totalExplorationTime");
        stats.lastVisitStart = tag.getLong("lastVisitStart");
        
        ListTag visitsList = tag.getList("dimensionVisits", Tag.TAG_COMPOUND);
        for (int i = 0; i < visitsList.size(); i++) {
            CompoundTag visitTag = visitsList.getCompound(i);
            stats.dimensionVisits.put(
                ResourceLocation.parse(visitTag.getString("dimension")),
                visitTag.getInt("count")
            );
        }
        
        // Note: We don't restore loaded chunks as they're transient
        
        return stats;
    }
}