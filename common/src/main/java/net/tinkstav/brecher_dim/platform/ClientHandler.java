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

package net.tinkstav.brecher_dim.platform;

import net.minecraft.resources.ResourceLocation;

/**
 * Client-side handler for processing packets.
 * Implementations handle platform-specific client logic.
 */
public interface ClientHandler {
    /**
     * Handle dimension sync packet on client
     */
    void handleDimensionSync(ResourceLocation dimensionId, boolean exists);
    
    /**
     * Handle dimension reset notification on client
     */
    void handleDimensionReset(ResourceLocation dimensionId, long resetTime);
    
    /**
     * Handle reset warning on client
     */
    void handleResetWarning(int minutesRemaining, String message);
    
    /**
     * Handle registry sync data on client
     */
    void handleRegistrySync(ResourceLocation dimensionId, byte[] nbtData);
    
    /**
     * Handle enhanced registry sync data on client
     */
    void handleEnhancedRegistrySync(byte[] nbtData);
    
    /**
     * Handle chunked registry sync data on client
     */
    void handleChunkedRegistrySync(int chunkIndex, int totalChunks, byte[] nbtData);
}