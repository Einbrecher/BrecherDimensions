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