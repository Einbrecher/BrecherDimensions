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

package net.tinkstav.brecher_dim.platform.neoforge;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.tinkstav.brecher_dim.BrecherDimensions;
import net.tinkstav.brecher_dim.platform.PacketHandler;
import net.tinkstav.brecher_dim.network.payload.*;

public class PacketHandlerImpl implements PacketHandler {
    private static PayloadRegistrar registrar;
    
    @Override
    public void registerPackets() {
        // This will be called from the mod event bus during registration
        // The actual registration happens in registerPayloads method
    }
    
    public static void registerPayloads(final PayloadRegistrar registrar) {
        PacketHandlerImpl.registrar = registrar;
        
        // Register S2C packets
        registrar.playToClient(
            DimensionSyncPayload.TYPE,
            DimensionSyncPayload.STREAM_CODEC,
            (payload, context) -> handleClientPayload(payload, context)
        );
        
        registrar.playToClient(
            DimensionResetPayload.TYPE,
            DimensionResetPayload.STREAM_CODEC,
            (payload, context) -> handleClientPayload(payload, context)
        );
        
        registrar.playToClient(
            ResetWarningPayload.TYPE,
            ResetWarningPayload.STREAM_CODEC,
            (payload, context) -> handleClientPayload(payload, context)
        );
        
        registrar.playToClient(
            RegistrySyncPayload.TYPE,
            RegistrySyncPayload.STREAM_CODEC,
            (payload, context) -> handleClientPayload(payload, context)
        );
        
        registrar.playToClient(
            ChunkedRegistrySyncPayload.TYPE,
            ChunkedRegistrySyncPayload.STREAM_CODEC,
            (payload, context) -> handleClientPayload(payload, context)
        );
        
        registrar.playToClient(
            EnhancedRegistrySyncPayload.TYPE,
            EnhancedRegistrySyncPayload.STREAM_CODEC,
            (payload, context) -> handleClientPayload(payload, context)
        );
    }
    
    private static void handleClientPayload(CustomPacketPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (payload instanceof DimensionSyncPayload p) {
                net.tinkstav.brecher_dim.client.ClientPacketHandlerImpl.handleDimensionSync(p.dimensionId(), p.exists());
            } else if (payload instanceof DimensionResetPayload p) {
                net.tinkstav.brecher_dim.client.ClientPacketHandlerImpl.handleDimensionReset(p.dimensionId(), p.resetTime());
            } else if (payload instanceof ResetWarningPayload p) {
                net.tinkstav.brecher_dim.client.ClientPacketHandlerImpl.handleResetWarning(p.minutesRemaining(), p.message());
            } else if (payload instanceof RegistrySyncPayload p) {
                net.tinkstav.brecher_dim.client.ClientPacketHandlerImpl.handleRegistrySync(p.dimensionId(), p.nbtData());
            } else if (payload instanceof EnhancedRegistrySyncPayload p) {
                net.tinkstav.brecher_dim.client.ClientPacketHandlerImpl.handleEnhancedRegistrySync(p.nbtData());
            } else if (payload instanceof ChunkedRegistrySyncPayload p) {
                net.tinkstav.brecher_dim.client.ClientPacketHandlerImpl.handleChunkedRegistrySync(p.chunkIndex(), p.totalChunks(), p.nbtData());
            }
        });
    }
    
    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload packet) {
        PacketDistributor.sendToPlayer(player, packet);
    }
    
    @Override
    public void sendToAllPlayers(CustomPacketPayload packet) {
        PacketDistributor.sendToAllPlayers(packet);
    }
    
    @Override
    public void sendToPlayersTrackingEntity(Entity entity, CustomPacketPayload packet) {
        PacketDistributor.sendToPlayersTrackingEntity(entity, packet);
    }
    
    @Override
    public void sendToDimension(ResourceKey<Level> dimension, CustomPacketPayload packet) {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            var level = server.getLevel(dimension);
            if (level != null) {
                PacketDistributor.sendToPlayersInDimension(level, packet);
            }
        }
    }
}