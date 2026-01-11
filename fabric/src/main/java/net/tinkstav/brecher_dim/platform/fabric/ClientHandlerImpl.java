/*
 * Brecher's Dimensions - Temporary resettable dimensions for exploration
 * Copyright (C) 2025 Einbrecher. All rights reserved.
 */

package net.tinkstav.brecher_dim.platform.fabric;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.tinkstav.brecher_dim.BrecherDimensions;
import net.tinkstav.brecher_dim.client.XaeroMapCleanup;
import net.tinkstav.brecher_dim.platform.ClientHandler;

public class ClientHandlerImpl implements ClientHandler {
    @Override
    public void handleDimensionSync(ResourceLocation dimensionId, boolean exists) {
        BrecherDimensions.LOGGER.debug("Client received dimension sync: {} exists={}", dimensionId, exists);
        
        // Update dimension tracking and cleanup if needed
        if (exists) {
            // Track exploration dimension
            XaeroMapCleanup.trackExplorationDimension(dimensionId);
        } else {
            // Dimension removed - cleanup Xaero's map data
            XaeroMapCleanup.cleanupDimensionData(dimensionId);
        }
    }
    
    @Override
    public void handleDimensionReset(ResourceLocation dimensionId, long resetTime) {
        BrecherDimensions.LOGGER.info("Dimension {} will reset at {}", dimensionId, resetTime);
        
        // Display message to player
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.displayClientMessage(
                Component.translatable("brecher_dim.dimension_reset", dimensionId.toString()),
                false
            );
        }
    }
    
    @Override
    public void handleResetWarning(int minutesRemaining, String message) {
        // Display warning message
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.displayClientMessage(
                Component.literal(message),
                true // Show in action bar
            );
        }
    }
    
    @Override
    public void handleRegistrySync(ResourceLocation dimensionId, byte[] nbtData) {
        BrecherDimensions.LOGGER.debug("Client received registry sync for dimension: {}", dimensionId);
        // TODO: Process registry sync data
    }
    
    @Override
    public void handleEnhancedRegistrySync(byte[] nbtData) {
        BrecherDimensions.LOGGER.debug("Client received enhanced registry sync");
        // TODO: Process enhanced registry sync data
    }
    
    @Override
    public void handleChunkedRegistrySync(int chunkIndex, int totalChunks, byte[] nbtData) {
        BrecherDimensions.LOGGER.debug("Client received chunked registry sync: {}/{}", chunkIndex + 1, totalChunks);
        // TODO: Process chunked registry sync data
    }
}