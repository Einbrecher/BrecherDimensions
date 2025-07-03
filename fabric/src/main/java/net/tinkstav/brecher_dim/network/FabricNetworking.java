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

package net.tinkstav.brecher_dim.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.tinkstav.brecher_dim.client.ClientPacketHandler;
import net.tinkstav.brecher_dim.network.payload.*;

/**
 * Fabric-specific networking implementation
 */
public class FabricNetworking {
    
    /**
     * Initialize and register all packet types
     */
    public static void init() {
        // Register S2C packet types
        PayloadTypeRegistry.playS2C().register(DimensionSyncPayload.TYPE, DimensionSyncPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(DimensionResetPayload.TYPE, DimensionResetPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(ResetWarningPayload.TYPE, ResetWarningPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(RegistrySyncPayload.TYPE, RegistrySyncPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(EnhancedRegistrySyncPayload.TYPE, EnhancedRegistrySyncPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(ChunkedRegistrySyncPayload.TYPE, ChunkedRegistrySyncPayload.STREAM_CODEC);
    }
    
    /**
     * Initialize client-side packet handlers
     * This should be called from the client initializer
     */
    public static void initClient() {
        // Register client handlers
        ClientPlayNetworking.registerGlobalReceiver(
            DimensionSyncPayload.TYPE,
            (payload, context) -> {
                context.client().execute(() -> {
                    ClientPacketHandler.handleDimensionSync(payload.dimensionId(), payload.exists());
                });
            }
        );
        
        ClientPlayNetworking.registerGlobalReceiver(
            DimensionResetPayload.TYPE,
            (payload, context) -> {
                context.client().execute(() -> {
                    ClientPacketHandler.handleDimensionReset(payload.dimensionId(), payload.resetTime());
                });
            }
        );
        
        ClientPlayNetworking.registerGlobalReceiver(
            ResetWarningPayload.TYPE,
            (payload, context) -> {
                context.client().execute(() -> {
                    ClientPacketHandler.handleResetWarning(payload.minutesRemaining(), payload.message());
                });
            }
        );
        
        ClientPlayNetworking.registerGlobalReceiver(
            RegistrySyncPayload.TYPE,
            (payload, context) -> {
                context.client().execute(() -> {
                    ClientPacketHandler.handleRegistrySync(payload.dimensionId(), payload.nbtData());
                });
            }
        );
        
        ClientPlayNetworking.registerGlobalReceiver(
            EnhancedRegistrySyncPayload.TYPE,
            (payload, context) -> {
                context.client().execute(() -> {
                    ClientPacketHandler.handleEnhancedRegistrySync(payload.nbtData());
                });
            }
        );
        
        ClientPlayNetworking.registerGlobalReceiver(
            ChunkedRegistrySyncPayload.TYPE,
            (payload, context) -> {
                context.client().execute(() -> {
                    ClientPacketHandler.handleChunkedRegistrySync(payload.chunkIndex(), payload.totalChunks(), payload.nbtData());
                });
            }
        );
    }
    
    /**
     * Send a packet to a specific player
     */
    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        ServerPlayNetworking.send(player, payload);
    }
    
}