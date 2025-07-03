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

package net.tinkstav.brecher_dim.client;

import net.minecraft.resources.ResourceLocation;
import net.tinkstav.brecher_dim.platform.Services;

/**
 * Client-side packet handler that delegates to platform implementations.
 */
public class ClientPacketHandler {
    
    public static void handleDimensionSync(ResourceLocation dimensionId, boolean exists) {
        if (Services.CLIENT != null) {
            Services.CLIENT.handleDimensionSync(dimensionId, exists);
        }
    }
    
    public static void handleDimensionReset(ResourceLocation dimensionId, long resetTime) {
        if (Services.CLIENT != null) {
            Services.CLIENT.handleDimensionReset(dimensionId, resetTime);
        }
    }
    
    public static void handleResetWarning(int minutesRemaining, String message) {
        if (Services.CLIENT != null) {
            Services.CLIENT.handleResetWarning(minutesRemaining, message);
        }
    }
    
    public static void handleRegistrySync(ResourceLocation dimensionId, byte[] nbtData) {
        if (Services.CLIENT != null) {
            Services.CLIENT.handleRegistrySync(dimensionId, nbtData);
        }
    }
    
    public static void handleEnhancedRegistrySync(byte[] nbtData) {
        if (Services.CLIENT != null) {
            Services.CLIENT.handleEnhancedRegistrySync(nbtData);
        }
    }
    
    public static void handleChunkedRegistrySync(int chunkIndex, int totalChunks, byte[] nbtData) {
        if (Services.CLIENT != null) {
            Services.CLIENT.handleChunkedRegistrySync(chunkIndex, totalChunks, nbtData);
        }
    }
}