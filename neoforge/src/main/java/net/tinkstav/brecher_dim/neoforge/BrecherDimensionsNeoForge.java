package net.tinkstav.brecher_dim.neoforge;

import net.tinkstav.brecher_dim.BrecherDimensions;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

@Mod(BrecherDimensions.MOD_ID)
public class BrecherDimensionsNeoForge {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public BrecherDimensionsNeoForge(IEventBus modEventBus) {
        LOGGER.info("Initializing Brecher's Dimensions for NeoForge");
        
        // Initialize common mod
        BrecherDimensions.init();
        
        // Register mod event handlers
        modEventBus.addListener(this::onCommonSetup);
        
        // Register game event handlers
        NeoForge.EVENT_BUS.addListener(this::onServerStarting);
        NeoForge.EVENT_BUS.addListener(this::onServerStopping);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(this::onServerTick);
        NeoForge.EVENT_BUS.addListener(this::onPlayerJoin);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLeave);
        NeoForge.EVENT_BUS.addListener(this::onPlayerChangeDimension);
        NeoForge.EVENT_BUS.addListener(this::onPlayerRespawn);
        NeoForge.EVENT_BUS.addListener(this::onChunkLoad);
        NeoForge.EVENT_BUS.addListener(this::onChunkUnload);
    }
    
    private void onCommonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Common setup for Brecher's Dimensions");
    }
    
    private void onServerStarting(ServerStartingEvent event) {
        BrecherDimensions.onServerStarting(event.getServer());
    }
    
    private void onServerStopping(ServerStoppingEvent event) {
        BrecherDimensions.onServerStopping(event.getServer());
    }
    
    private void onRegisterCommands(RegisterCommandsEvent event) {
        NeoForgeCommandPlatform.handleRegisterCommands(event);
    }
    
    private void onServerTick(ServerTickEvent.Post event) {
        BrecherEventHandlersImpl.handleServerTick(event.getServer());
    }
    
    private void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        BrecherEventHandlersImpl.handlePlayerJoin(event);
    }
    
    private void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        BrecherEventHandlersImpl.handlePlayerLeave(event);
    }
    
    private void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        BrecherEventHandlersImpl.handlePlayerChangeDimension(event);
    }
    
    private void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        BrecherEventHandlersImpl.handlePlayerRespawn(event);
    }
    
    private void onChunkLoad(ChunkEvent.Load event) {
        BrecherEventHandlersImpl.handleChunkLoad(event);
    }
    
    private void onChunkUnload(ChunkEvent.Unload event) {
        BrecherEventHandlersImpl.handleChunkUnload(event);
    }
}