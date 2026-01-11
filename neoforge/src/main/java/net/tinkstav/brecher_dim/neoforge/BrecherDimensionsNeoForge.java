/*
 * Brecher's Dimensions - Temporary resettable dimensions for exploration
 * Copyright (C) 2025 Einbrecher. All rights reserved.
 */

package net.tinkstav.brecher_dim.neoforge;

import net.tinkstav.brecher_dim.BrecherDimensions;
import net.tinkstav.brecher_dim.platform.Services;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.Commands;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.tinkstav.brecher_dim.client.BrecherClientHandlerNeoForge;
import org.slf4j.Logger;

/**
 * NeoForge entry point for Brecher's Dimensions.
 */
@Mod(BrecherDimensions.MOD_ID)
public class BrecherDimensionsNeoForge {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public BrecherDimensionsNeoForge(IEventBus modEventBus, net.neoforged.fml.ModContainer modContainer) {
        LOGGER.info("Initializing Brecher's Dimensions for NeoForge");
        
        // Register config first (YAML config doesn't need event bus registration)
        net.tinkstav.brecher_dim.config.neoforge.BrecherConfigImpl.register(modContainer);
        
        // Initialize common mod
        BrecherDimensions.init();
        
        // Register mod event bus listeners
        modEventBus.addListener(this::registerPayloadHandlers);
        
        // Register forge event bus listeners
        NeoForge.EVENT_BUS.addListener(this::registerCommands);
        
        // Register events through the service system
        Services.EVENTS.registerEvents();
        
        // Register client events if on client
        if (FMLEnvironment.dist.isClient()) {
            NeoForge.EVENT_BUS.addListener(this::onClientConnect);
            NeoForge.EVENT_BUS.addListener(this::onClientDisconnect);
        }
        
        LOGGER.info("Brecher's Dimensions for NeoForge initialized");
    }
    
    private void registerPayloadHandlers(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(BrecherDimensions.MOD_ID)
            .versioned("1.0");
        
        // Register packets through the platform implementation
        net.tinkstav.brecher_dim.platform.neoforge.PacketHandlerImpl.registerPayloads(registrar);
    }
    
    private void registerCommands(final RegisterCommandsEvent event) {
        Services.COMMANDS.registerCommands(event.getDispatcher());
    }
    
    private void onClientConnect(final ClientPlayerNetworkEvent.LoggingIn event) {
        LOGGER.debug("Client connected to server, cleaning up old map data");
        // Clean up old Xaero map data from previous sessions
        net.tinkstav.brecher_dim.client.XaeroMapCleanup.cleanupOldMapDataOnConnect();
    }
    
    private void onClientDisconnect(final ClientPlayerNetworkEvent.LoggingOut event) {
        LOGGER.debug("Client disconnected from server, clearing data");
        BrecherClientHandlerNeoForge.clearClientData();
    }
}