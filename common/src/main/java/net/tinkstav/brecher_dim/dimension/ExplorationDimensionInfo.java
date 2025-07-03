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

package net.tinkstav.brecher_dim.dimension;

import net.minecraft.nbt.CompoundTag;
import java.util.UUID;

public record ExplorationDimensionInfo(
    String baseDimension,
    long createdTime,
    long lastResetTime,
    int playerCount
) {
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("baseDimension", baseDimension);
        tag.putLong("createdTime", createdTime);
        tag.putLong("lastResetTime", lastResetTime);
        tag.putInt("playerCount", playerCount);
        return tag;
    }
    
    public static ExplorationDimensionInfo fromNBT(CompoundTag tag) {
        return new ExplorationDimensionInfo(
            tag.getString("baseDimension"),
            tag.getLong("createdTime"),
            tag.getLong("lastResetTime"),
            tag.getInt("playerCount")
        );
    }
}