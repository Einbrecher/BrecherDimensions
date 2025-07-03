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

package net.tinkstav.brecher_dim.client.fabric;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.tinkstav.brecher_dim.BrecherDimensions;
import net.tinkstav.brecher_dim.platform.ClientHandler;
import net.tinkstav.brecher_dim.client.BrecherClientHandlerFabric;

public class ClientHandlerImpl implements ClientHandler {
    @Override
    public void handleDimensionSync(ResourceLocation dimensionId, boolean exists) {
        BrecherClientHandlerFabric.handleDimensionSync(dimensionId, exists);
    }
    
    @Override
    public void handleDimensionReset(ResourceLocation dimensionId, long resetTime) {
        BrecherClientHandlerFabric.handleDimensionReset(dimensionId, resetTime);
    }
    
    @Override
    public void handleResetWarning(int minutesRemaining, String message) {
        BrecherClientHandlerFabric.handleResetWarning(minutesRemaining, message);
    }
    
    @Override
    public void handleRegistrySync(ResourceLocation dimensionId, byte[] nbtData) {
        BrecherClientHandlerFabric.handleRegistrySync(dimensionId, nbtData);
    }
    
    @Override
    public void handleEnhancedRegistrySync(byte[] nbtData) {
        BrecherClientHandlerFabric.handleEnhancedRegistrySync(nbtData);
    }
    
    @Override
    public void handleChunkedRegistrySync(int chunkIndex, int totalChunks, byte[] nbtData) {
        BrecherClientHandlerFabric.handleChunkedRegistrySync(chunkIndex, totalChunks, nbtData);
    }
}