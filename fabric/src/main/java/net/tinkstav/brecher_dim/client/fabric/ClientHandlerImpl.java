/*
 * Brecher's Dimensions - Temporary resettable dimensions for exploration
 * Copyright (C) 2025 Einbrecher. All rights reserved.
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