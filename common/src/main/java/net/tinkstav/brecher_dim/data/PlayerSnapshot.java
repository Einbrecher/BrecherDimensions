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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Stores a snapshot of a player's position and dimension for emergency recovery
 */
public class PlayerSnapshot {
    private final ResourceKey<Level> dimension;
    private final Vec3 position;
    private final float yaw;
    private final float pitch;
    private final long timestamp;
    
    public PlayerSnapshot(ServerPlayer player) {
        this.dimension = player.level().dimension();
        this.position = player.position();
        this.yaw = player.getYRot();
        this.pitch = player.getXRot();
        this.timestamp = System.currentTimeMillis();
    }
    
    public static PlayerSnapshot create(ServerPlayer player) {
        return new PlayerSnapshot(player);
    }
    
    public PlayerSnapshot(CompoundTag tag) {
        this.dimension = ResourceKey.create(
            ResourceKey.createRegistryKey(ResourceLocation.parse(tag.getString("registry"))),
            ResourceLocation.parse(tag.getString("dimension"))
        );
        this.position = new Vec3(
            tag.getDouble("x"),
            tag.getDouble("y"),
            tag.getDouble("z")
        );
        this.yaw = tag.getFloat("yaw");
        this.pitch = tag.getFloat("pitch");
        this.timestamp = tag.getLong("timestamp");
    }
    
    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putString("registry", dimension.registry().toString());
        tag.putString("dimension", dimension.location().toString());
        tag.putDouble("x", position.x);
        tag.putDouble("y", position.y);
        tag.putDouble("z", position.z);
        tag.putFloat("yaw", yaw);
        tag.putFloat("pitch", pitch);
        tag.putLong("timestamp", timestamp);
        return tag;
    }
    
    public ResourceKey<Level> getDimension() {
        return dimension;
    }
    
    public Vec3 getPosition() {
        return position;
    }
    
    public BlockPos getBlockPos() {
        return BlockPos.containing(position);
    }
    
    public float getYaw() {
        return yaw;
    }
    
    public float getPitch() {
        return pitch;
    }

    public long timestamp() {
        return timestamp;
    }
    
    public boolean isExpired(long maxAgeMillis) {
        return System.currentTimeMillis() - timestamp > maxAgeMillis;
    }
}