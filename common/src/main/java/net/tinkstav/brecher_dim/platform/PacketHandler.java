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