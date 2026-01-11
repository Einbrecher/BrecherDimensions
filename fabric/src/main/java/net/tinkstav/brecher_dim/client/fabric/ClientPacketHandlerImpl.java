/*
 * Brecher's Dimensions - Temporary resettable dimensions for exploration
 * Copyright (C) 2025 Einbrecher. All rights reserved.
 */

package net.tinkstav.brecher_dim.client.fabric;

import net.minecraft.resources.ResourceLocation;
import net.tinkstav.brecher_dim.client.BrecherClientHandlerFabric;

/**
 * Fabric implementation of ClientPacketHandler
 * Delegates to the actual client handler which contains the client-only code
 */
public class ClientPacketHandlerImpl {
    
    public static void handleDimensionSync(ResourceLocation dimensionId, boolean exists) {
        BrecherClientHandlerFabric.handleDimensionSync(dimensionId, exists);
    }
    
    public static void handleDimensionReset(ResourceLocation dimensionId, long resetTime) {
        BrecherClientHandlerFabric.handleDimensionReset(dimensionId, resetTime);
    }
    
    public static void handleResetWarning(int minutesRemaining, String message) {
        BrecherClientHandlerFabric.handleResetWarning(minutesRemaining, message);
    }
    
    public static void handleRegistrySync(ResourceLocation dimensionId, byte[] nbtData) {
        BrecherClientHandlerFabric.handleRegistrySync(dimensionId, nbtData);
    }
    
    public static void handleEnhancedRegistrySync(byte[] nbtData) {
        BrecherClientHandlerFabric.handleEnhancedRegistrySync(nbtData);
    }
    
    public static void handleChunkedRegistrySync(int chunkIndex, int totalChunks, byte[] nbtData) {
        BrecherClientHandlerFabric.handleChunkedRegistrySync(chunkIndex, totalChunks, nbtData);
    }
}