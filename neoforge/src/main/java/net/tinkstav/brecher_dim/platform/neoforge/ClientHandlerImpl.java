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

package net.tinkstav.brecher_dim.platform.neoforge;

import net.minecraft.resources.ResourceLocation;
import net.tinkstav.brecher_dim.platform.ClientHandler;

public class ClientHandlerImpl implements ClientHandler {
    @Override
    public void handleDimensionSync(ResourceLocation dimensionId, boolean exists) {
        // Server-side implementation - no-op
        // Client handling is done in separate client module
    }
    
    @Override
    public void handleDimensionReset(ResourceLocation dimensionId, long resetTime) {
        // Server-side implementation - no-op
    }
    
    @Override
    public void handleResetWarning(int minutesRemaining, String message) {
        // Server-side implementation - no-op
    }
    
    @Override
    public void handleRegistrySync(ResourceLocation dimensionId, byte[] nbtData) {
        // Server-side implementation - no-op
    }
    
    @Override
    public void handleEnhancedRegistrySync(byte[] nbtData) {
        // Server-side implementation - no-op
    }
    
    @Override
    public void handleChunkedRegistrySync(int chunkIndex, int totalChunks, byte[] nbtData) {
        // Server-side implementation - no-op
    }
}