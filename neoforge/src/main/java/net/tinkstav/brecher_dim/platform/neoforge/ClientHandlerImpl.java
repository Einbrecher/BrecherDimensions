/*
 * Brecher's Dimensions - Temporary resettable dimensions for exploration
 * Copyright (C) 2025 Einbrecher. All rights reserved.
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