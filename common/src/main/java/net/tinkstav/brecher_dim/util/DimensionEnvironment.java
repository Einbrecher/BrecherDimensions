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

package net.tinkstav.brecher_dim.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Enum representing the general environment type of a dimension.
 *
 * <p>This classification is based on {@link net.minecraft.world.level.dimension.DimensionType}
 * properties rather than dimension names, making it compatible with modded dimensions.
 * Any dimension with similar properties to vanilla dimensions will be classified correctly.</p>
 *
 * <h3>Classification Logic:</h3>
 * <ul>
 *   <li><b>NETHER_LIKE:</b> Dimensions where {@code ultraWarm} is true (no water, lava flows faster)</li>
 *   <li><b>END_LIKE:</b> Dimensions with fixed time (always midnight) and no ceiling</li>
 *   <li><b>OVERWORLD_LIKE:</b> All other dimensions (day/night cycle, normal behavior)</li>
 * </ul>
 *
 * <p>This approach ensures that modded dimensions with Nether-like or End-like properties
 * are handled correctly without needing to hard-code dimension IDs.</p>
 */
public enum DimensionEnvironment {
    /** Normal overworld-like dimension with day/night cycle */
    OVERWORLD_LIKE,

    /** Hot dimension like the Nether (ultraWarm, no water, lava flows faster) */
    NETHER_LIKE,

    /** Void dimension like the End (fixed time, no ceiling) */
    END_LIKE;

    /**
     * Determines the environment type of a dimension based on its properties.
     *
     * <p>Uses dimension type properties ({@code ultraWarm}, {@code hasFixedTime}, {@code hasCeiling})
     * rather than string matching on dimension names for better modded dimension compatibility.</p>
     *
     * @param level The server level to check
     * @return The dimension environment type
     */
    public static DimensionEnvironment getDimensionEnvironment(ServerLevel level) {
        var dimType = level.dimensionType();

        // Nether-like: ultraWarm means no water, lava flows faster, compasses spin
        if (dimType.ultraWarm()) {
            return NETHER_LIKE;
        }

        // End-like: has fixed time (always midnight) and no ceiling
        // Note: Nether also has fixed time but HAS a ceiling
        if (dimType.hasFixedTime() && !dimType.hasCeiling()) {
            return END_LIKE;
        }

        // Default to overworld-like for everything else
        return OVERWORLD_LIKE;
    }

    /**
     * Convenience method that accepts a Level and casts to ServerLevel.
     *
     * @param level The level to check (must be a ServerLevel)
     * @return The dimension environment type
     * @throws IllegalArgumentException if level is not a ServerLevel
     */
    public static DimensionEnvironment getDimensionEnvironment(Level level) {
        if (level instanceof ServerLevel serverLevel) {
            return getDimensionEnvironment(serverLevel);
        }
        throw new IllegalArgumentException("Level must be a ServerLevel to determine dimension environment");
    }
}
