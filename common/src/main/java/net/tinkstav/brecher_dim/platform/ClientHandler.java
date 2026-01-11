/*
 * Brecher's Dimensions - Temporary resettable dimensions for exploration
 * Copyright (C) 2025 Einbrecher. All rights reserved.
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