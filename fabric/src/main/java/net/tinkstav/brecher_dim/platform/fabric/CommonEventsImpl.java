/*
 * Brecher's Dimensions - Temporary resettable dimensions for exploration
 * Copyright (C) 2025 Einbrecher. All rights reserved.
 */

package net.tinkstav.brecher_dim.platform.fabric;

import net.tinkstav.brecher_dim.platform.CommonEvents;
import net.tinkstav.brecher_dim.event.BrecherEventHandlers;
import net.tinkstav.brecher_dim.BrecherDimensions;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public class CommonEventsImpl implements CommonEvents {
    @Override
    public void onServerStarting(MinecraftServer server) {
        // Server starting event - call the main initialization
        BrecherDimensions.onServerStarting(server);
    }
    
    @Override
    public void onServerStarted(MinecraftServer server) {
        // Server started event - additional initialization if needed
        BrecherDimensions.LOGGER.info("Server started - Brecher's Dimensions ready");
    }
    
    @Override
    public void onServerStopping(MinecraftServer server) {
        // Clean up when server stops
        BrecherDimensions.onServerStopping(server);
    }
    
    @Override
    public void onPlayerJoin(ServerPlayer player) {
        BrecherEventHandlers.onPlayerJoin(player);
    }
    
    @Override
    public void onPlayerLeave(ServerPlayer player) {
        BrecherEventHandlers.onPlayerLeave(player);
    }
    
    @Override
    public void onPlayerChangeDimension(ServerPlayer player, ResourceKey<Level> from, ResourceKey<Level> to) {
        // Convert ResourceKey to Level for the event handler
        if (player.getServer() != null) {
            var fromLevel = player.getServer().getLevel(from);
            var toLevel = player.getServer().getLevel(to);
            if (fromLevel != null && toLevel != null) {
                BrecherEventHandlers.onPlayerChangeDimension(player, fromLevel, toLevel);
            }
        }
    }
    
    @Override
    public void onPlayerDeath(ServerPlayer player) {
        BrecherEventHandlers.onPlayerDeath(player);
    }
    
    @Override
    public void onPlayerRespawn(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean alive) {
        BrecherEventHandlers.onPlayerRespawn(newPlayer, alive);
    }
    
    @Override
    public void onServerTick(MinecraftServer server) {
        BrecherEventHandlers.onServerTick(server);
    }
    
    @Override
    public void registerEvents() {
        
        // Server tick event
        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);
        
        // Block interaction events
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            var pos = hitResult.getBlockPos();
            var state = world.getBlockState(pos);
            
            // Handle bed interactions
            if (state.getBlock() instanceof net.minecraft.world.level.block.BedBlock) {
                var result = BrecherEventHandlers.onPlayerSleep(player, pos);
                if (result == InteractionResult.FAIL) {
                    return InteractionResult.FAIL;
                }
            }
            
            // Handle respawn anchor interactions
            if (state.is(Blocks.RESPAWN_ANCHOR)) {
                var result = BrecherEventHandlers.onRespawnAnchorUse(player, pos);
                if (result == InteractionResult.FAIL) {
                    return InteractionResult.FAIL;
                }
            }
            
            // Handle ender chest interactions
            if (state.is(Blocks.ENDER_CHEST)) {
                var result = BrecherEventHandlers.onEnderChestUse(player, world, pos, hand);
                if (result == InteractionResult.FAIL) {
                    return InteractionResult.FAIL;
                }
            }
            
            return InteractionResult.PASS;
        });
        
        // Server lifecycle events
        ServerLifecycleEvents.SERVER_STARTING.register(this::onServerStarting);
        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStopping);
        
        // Player events
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            this.onPlayerJoin(handler.getPlayer());
        });
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            this.onPlayerLeave(handler.getPlayer());
        });
        ServerPlayerEvents.AFTER_RESPAWN.register(this::onPlayerRespawn);
        
        // Death event
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, damageSource, damageAmount) -> {
            if (entity instanceof ServerPlayer player) {
                this.onPlayerDeath(player);
            }
            return true; // Allow the death to proceed
        });
        
        // Use entity API to track dimension changes
        net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
            this.onPlayerChangeDimension(player, origin.dimension(), destination.dimension());
        });
        
        // Chunk events
        ServerChunkEvents.CHUNK_LOAD.register((serverLevel, chunk) -> {
            BrecherEventHandlers.onChunkLoad(serverLevel, chunk.getPos().x, chunk.getPos().z);
        });
        
        ServerChunkEvents.CHUNK_UNLOAD.register((serverLevel, chunk) -> {
            BrecherEventHandlers.onChunkUnload(serverLevel, chunk.getPos().x, chunk.getPos().z);
        });
    }
}