/*
 * Brecher's Dimensions - Temporary resettable dimensions for exploration
 * Copyright (C) 2025 Einbrecher. All rights reserved.
 */

package net.tinkstav.brecher_dim.client.neoforge;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.tinkstav.brecher_dim.BrecherDimensions;
import net.tinkstav.brecher_dim.platform.ClientHandler;
import net.tinkstav.brecher_dim.client.BrecherClientHandlerNeoForge;

public class ClientHandlerImpl implements ClientHandler {
    @Override
    public void handleDimensionSync(ResourceLocation dimensionId, boolean exists) {
        BrecherClientHandlerNeoForge.handleDimensionSync(dimensionId, exists);
    }
    
    @Override
    public void handleDimensionReset(ResourceLocation dimensionId, long resetTime) {
        BrecherClientHandlerNeoForge.handleDimensionReset(dimensionId, resetTime);
    }
    
    @Override
    public void handleResetWarning(int minutesRemaining, String message) {
        BrecherClientHandlerNeoForge.handleResetWarning(minutesRemaining, message);
    }
    
    @Override
    public void handleRegistrySync(ResourceLocation dimensionId, byte[] nbtData) {
        BrecherClientHandlerNeoForge.handleRegistrySync(dimensionId, nbtData);
    }
    
    @Override
    public void handleEnhancedRegistrySync(byte[] nbtData) {
        BrecherClientHandlerNeoForge.handleEnhancedRegistrySync(nbtData);
    }
    
    @Override
    public void handleChunkedRegistrySync(int chunkIndex, int totalChunks, byte[] nbtData) {
        BrecherClientHandlerNeoForge.handleChunkedRegistrySync(chunkIndex, totalChunks, nbtData);
    }
}