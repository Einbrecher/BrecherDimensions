package net.tinkstav.brecher_dim.data;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import java.util.*;

public class DimensionMetadata {
    private final ResourceLocation dimension;
    private long createdTime;
    private long lastAccessTime;
    private final Set<UUID> accessedBy = new HashSet<>();
    private int totalVisits = 0;
    
    public DimensionMetadata(ResourceLocation dimension) {
        this.dimension = dimension;
        this.createdTime = System.currentTimeMillis();
        this.lastAccessTime = createdTime;
    }
    
    public void recordAccess(UUID playerId) {
        lastAccessTime = System.currentTimeMillis();
        accessedBy.add(playerId);
        totalVisits++;
    }
    
    public void reset() {
        createdTime = System.currentTimeMillis();
        lastAccessTime = createdTime;
        accessedBy.clear();
        totalVisits = 0;
    }
    
    public ResourceLocation getDimension() {
        return dimension;
    }
    
    public long getCreatedTime() {
        return createdTime;
    }
    
    public long getLastAccessTime() {
        return lastAccessTime;
    }
    
    public Set<UUID> getAccessedBy() {
        return new HashSet<>(accessedBy);
    }
    
    public int getTotalVisits() {
        return totalVisits;
    }
    
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("dimension", dimension.toString());
        tag.putLong("createdTime", createdTime);
        tag.putLong("lastAccessTime", lastAccessTime);
        tag.putInt("totalVisits", totalVisits);
        
        ListTag playerList = new ListTag();
        accessedBy.forEach(uuid -> {
            CompoundTag playerTag = new CompoundTag();
            playerTag.putUUID("uuid", uuid);
            playerList.add(playerTag);
        });
        tag.put("accessedBy", playerList);
        
        return tag;
    }
    
    public static DimensionMetadata fromNBT(CompoundTag tag) {
        DimensionMetadata metadata = new DimensionMetadata(
            ResourceLocation.parse(tag.getString("dimension"))
        );
        metadata.createdTime = tag.getLong("createdTime");
        metadata.lastAccessTime = tag.getLong("lastAccessTime");
        metadata.totalVisits = tag.getInt("totalVisits");
        
        ListTag playerList = tag.getList("accessedBy", Tag.TAG_COMPOUND);
        for (int i = 0; i < playerList.size(); i++) {
            metadata.accessedBy.add(playerList.getCompound(i).getUUID("uuid"));
        }
        
        return metadata;
    }
}