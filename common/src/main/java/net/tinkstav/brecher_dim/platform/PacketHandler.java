/*
 * Brecher's Dimensions - Temporary resettable dimensions for exploration
 * Copyright (C) 2025 Einbrecher. All rights reserved.
 */

package net.tinkstav.brecher_dim.platform;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Platform-specific packet handling.
 * Each platform implements this differently (Fabric uses ServerPlayNetworking, NeoForge uses SimpleChannel).
 */
public interface PacketHandler {
    /**
     * Registers all packets for the mod.
     * Must be called during mod initialization.
     */
    void registerPackets();
    
    /**
     * Sends a packet to a specific player.
     */
    void sendToPlayer(ServerPlayer player, CustomPacketPayload packet);
    
    /**
     * Sends a packet to all connected players.
     */
    void sendToAllPlayers(CustomPacketPayload packet);
    
    /**
     * Sends a packet to all players tracking an entity (including the entity if it's a player).
     */
    void sendToPlayersTrackingEntity(net.minecraft.world.entity.Entity entity, CustomPacketPayload packet);
    
    /**
     * Sends a packet to all players in a specific dimension.
     */
    void sendToDimension(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension, CustomPacketPayload packet);
}