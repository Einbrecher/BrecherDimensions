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