/*
 * Brecher's Dimensions - Temporary resettable dimensions for exploration
 * Copyright (C) 2025 Einbrecher. All rights reserved.
 */

package net.tinkstav.brecher_dim.data;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;

public record ReturnPosition(
    BlockPos pos,
    ResourceLocation dimension,
    float yRot,
    float xRot,
    long timestamp
) {
    private static final long EXPIRY_TIME = 7L * 24 * 60 * 60 * 1000; // 7 days in milliseconds
    
    // Constructor without timestamp for backward compatibility
    public ReturnPosition(BlockPos pos, ResourceLocation dimension, float yRot, float xRot) {
        this(pos, dimension, yRot, xRot, System.currentTimeMillis());
    }
    
    public boolean isExpired() {
        return System.currentTimeMillis() - timestamp > EXPIRY_TIME;
    }
    
    public long getTimeSinceCreation() {
        return System.currentTimeMillis() - timestamp;
    }
    
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("pos", pos.asLong());
        tag.putString("dimension", dimension.toString());
        tag.putFloat("yRot", yRot);
        tag.putFloat("xRot", xRot);
        tag.putLong("timestamp", timestamp);
        return tag;
    }
    
    public static ReturnPosition fromNBT(CompoundTag tag) {
        return new ReturnPosition(
            BlockPos.of(tag.getLong("pos")),
            ResourceLocation.parse(tag.getString("dimension")),
            tag.getFloat("yRot"),
            tag.getFloat("xRot"),
            tag.contains("timestamp") ? tag.getLong("timestamp") : System.currentTimeMillis()
        );
    }
}