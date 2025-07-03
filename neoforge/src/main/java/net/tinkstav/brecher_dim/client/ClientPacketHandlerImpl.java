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

/**
 * NeoForge implementation of ClientPacketHandler
 * Delegates to the actual client handler which contains the client-only code
 */
public class ClientPacketHandlerImpl {
    
    public static void handleDimensionSync(ResourceLocation dimensionId, boolean exists) {
        BrecherClientHandlerNeoForge.handleDimensionSync(dimensionId, exists);
    }
    
    public static void handleDimensionReset(ResourceLocation dimensionId, long resetTime) {
        BrecherClientHandlerNeoForge.handleDimensionReset(dimensionId, resetTime);
    }
    
    public static void handleResetWarning(int minutesRemaining, String message) {
        BrecherClientHandlerNeoForge.handleResetWarning(minutesRemaining, message);
    }
    
    public static void handleRegistrySync(ResourceLocation dimensionId, byte[] nbtData) {
        BrecherClientHandlerNeoForge.handleRegistrySync(dimensionId, nbtData);
    }
    
    public static void handleEnhancedRegistrySync(byte[] nbtData) {
        BrecherClientHandlerNeoForge.handleEnhancedRegistrySync(nbtData);
    }
    
    public static void handleChunkedRegistrySync(int chunkIndex, int totalChunks, byte[] nbtData) {
        BrecherClientHandlerNeoForge.handleChunkedRegistrySync(chunkIndex, totalChunks, nbtData);
    }
}