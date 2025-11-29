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

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.tinkstav.brecher_dim.BrecherDimensions;

/**
 * Centralized utility methods for dimension operations.
 * Use these methods for simple namespace+path checks that don't require the manager.
 * For checks against registered dimensions, use BrecherDimensionManager instance methods.
 */
public final class DimensionUtils {
    private DimensionUtils() {} // Prevent instantiation

    /**
     * Check if a ResourceKey represents an exploration dimension based on naming convention.
     * This is a simple check that doesn't require the dimension manager to be initialized.
     *
     * @param key The resource key to check (can be for Level, DimensionType, or any registry)
     * @return true if the key follows the exploration dimension naming convention
     */
    public static boolean isExplorationDimension(ResourceKey<?> key) {
        if (key == null) return false;
        return isExplorationDimension(key.location());
    }

    /**
     * Check if a ResourceLocation represents an exploration dimension based on naming convention.
     * This is a simple check that doesn't require the dimension manager to be initialized.
     *
     * @param location The resource location to check
     * @return true if the location follows the exploration dimension naming convention
     */
    public static boolean isExplorationDimension(ResourceLocation location) {
        if (location == null) return false;
        return location.getNamespace().equals(BrecherDimensions.MOD_ID)
            && location.getPath().startsWith("exploration_");
    }

    /**
     * Check if a folder name represents an exploration dimension.
     * Used for file system cleanup operations.
     *
     * @param folderName The folder name to check (e.g., "brecher_dim_exploration_overworld_0")
     * @return true if the folder name matches exploration dimension pattern
     */
    public static boolean isExplorationDimensionFolder(String folderName) {
        if (folderName == null || folderName.isEmpty()) return false;
        // Folder names are formatted as: modid_path (with : replaced by _)
        // e.g., "brecher_dim_exploration_overworld_0"
        return folderName.startsWith(BrecherDimensions.MOD_ID + "_exploration_");
    }
}
