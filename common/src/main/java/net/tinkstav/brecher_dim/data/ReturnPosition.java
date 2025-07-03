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