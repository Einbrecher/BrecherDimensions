/*
 * Brecher's Dimensions - Temporary resettable dimensions for exploration
 * Copyright (C) 2025 Einbrecher. All rights reserved.
 */

package net.tinkstav.brecher_dim.platform.fabric;

import net.tinkstav.brecher_dim.platform.PacketHandler;
import net.tinkstav.brecher_dim.network.FabricNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class PacketHandlerImpl implements PacketHandler {
    @Override
    public void registerPackets() {
        // Register packets through Fabric's networking system
        FabricNetworking.init();
    }
    
    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload packet) {
        ServerPlayNetworking.send(player, packet);
    }
    
    @Override
    public void sendToAllPlayers(CustomPacketPayload packet) {
        // Get server instance
        MinecraftServer server = null;
        
        // Try to get server from Fabric loader
        var loader = net.fabricmc.loader.api.FabricLoader.getInstance();
        Object gameInstance = loader.getGameInstance();
        if (gameInstance instanceof MinecraftServer minecraftServer) {
            server = minecraftServer;
        } else if (gameInstance instanceof net.minecraft.client.Minecraft) {
            // We're on the client side, can't send to all players
            return;
        }
        
        if (server != null) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                sendToPlayer(player, packet);
            }
        }
    }
    
    
    @Override
    public void sendToPlayersTrackingEntity(Entity entity, CustomPacketPayload packet) {
        if (entity.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.getChunkSource().broadcast(entity, 
                ServerPlayNetworking.createS2CPacket(packet));
        }
    }
    
    @Override
    public void sendToDimension(ResourceKey<Level> dimension, CustomPacketPayload packet) {
        // Get server instance
        MinecraftServer server = null;
        
        // Try to get server from Fabric loader
        var loader = net.fabricmc.loader.api.FabricLoader.getInstance();
        Object gameInstance = loader.getGameInstance();
        if (gameInstance instanceof MinecraftServer minecraftServer) {
            server = minecraftServer;
        }
        
        if (server != null) {
            var level = server.getLevel(dimension);
            if (level != null) {
                for (ServerPlayer player : level.players()) {
                    if (player instanceof ServerPlayer serverPlayer) {
                        sendToPlayer(serverPlayer, packet);
                    }
                }
            }
        }
    }
}