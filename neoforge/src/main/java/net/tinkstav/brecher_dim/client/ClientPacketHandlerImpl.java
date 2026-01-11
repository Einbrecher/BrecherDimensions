/*
 * Brecher's Dimensions - Temporary resettable dimensions for exploration
 * Copyright (C) 2025 Einbrecher. All rights reserved.
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