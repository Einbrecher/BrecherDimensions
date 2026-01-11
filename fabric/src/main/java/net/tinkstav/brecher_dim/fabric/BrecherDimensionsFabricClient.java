/*
 * Brecher's Dimensions - Temporary resettable dimensions for exploration
 * Copyright (C) 2025 Einbrecher. All rights reserved.
 */

package net.tinkstav.brecher_dim.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.tinkstav.brecher_dim.network.FabricNetworking;
import net.tinkstav.brecher_dim.client.BrecherClientHandlerFabric;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * Fabric client-side mod initializer
 * Handles client-specific initialization
 */
public class BrecherDimensionsFabricClient implements ClientModInitializer {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing Brecher's Dimensions client for Fabric");
        
        // Register client packet handlers
        FabricNetworking.initClient();
        
        // Register client connection events
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            LOGGER.debug("Client connected to server, cleaning up old map data");
            // Clean up old Xaero map data from previous sessions
            net.tinkstav.brecher_dim.client.XaeroMapCleanup.cleanupOldMapDataOnConnect();
        });
        
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            LOGGER.debug("Client disconnected from server, clearing data");
            BrecherClientHandlerFabric.clearClientData();
        });
        
        LOGGER.info("Brecher's Dimensions client packet handlers registered");
    }
}