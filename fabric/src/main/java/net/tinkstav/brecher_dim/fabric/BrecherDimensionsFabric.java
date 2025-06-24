package net.tinkstav.brecher_dim.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.tinkstav.brecher_dim.BrecherDimensions;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class BrecherDimensionsFabric implements ModInitializer {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Brecher's Dimensions for Fabric");
        
        // Initialize common mod
        BrecherDimensions.init();
        
        // Register lifecycle events
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            BrecherDimensions.onServerStarting(server);
        });
        
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            BrecherDimensions.onServerStopping(server);
        });
        
        // Register command events
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            FabricCommandPlatform.handleRegisterCommands(dispatcher);
        });
        
        // Register tick events
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            FabricEventHandlersImpl.handleServerTick(server);
        });
        
        // Register player events
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            FabricEventHandlersImpl.handlePlayerJoin(handler.player);
        });
        
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            FabricEventHandlersImpl.handlePlayerLeave(handler.player);
        });
        
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            FabricEventHandlersImpl.handlePlayerRespawn(newPlayer, !alive);
        });
        
        // Register chunk events
        ServerChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
            FabricEventHandlersImpl.handleChunkLoad(world, chunk);
        });
        
        ServerChunkEvents.CHUNK_UNLOAD.register((world, chunk) -> {
            FabricEventHandlersImpl.handleChunkUnload(world, chunk);
        });
        
        // Note: Fabric doesn't have a direct PlayerChangedDimensionEvent equivalent
        // We'll need to handle this through a mixin or different approach
        
        LOGGER.info("Brecher's Dimensions for Fabric initialized");
    }
}