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

package net.tinkstav.brecher_dim.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.tinkstav.brecher_dim.BrecherDimensions;

/**
 * Packet payload to sync single dimension registry data
 */
public record RegistrySyncPayload(ResourceLocation dimensionId, byte[] nbtData) implements CustomPacketPayload {
    // Type wrapper for the packet
    public static final CustomPacketPayload.Type<RegistrySyncPayload> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BrecherDimensions.MOD_ID, "registry_sync"));
    
    // StreamCodec for serialization
    public static final StreamCodec<RegistryFriendlyByteBuf, RegistrySyncPayload> STREAM_CODEC = 
        StreamCodec.of(
            (buf, payload) -> {
                ResourceLocation.STREAM_CODEC.encode(buf, payload.dimensionId);
                ByteBufCodecs.BYTE_ARRAY.encode(buf, payload.nbtData);
            },
            (buf) -> {
                ResourceLocation dimensionId = ResourceLocation.STREAM_CODEC.decode(buf);
                byte[] nbtData = ByteBufCodecs.BYTE_ARRAY.decode(buf);
                return new RegistrySyncPayload(dimensionId, nbtData);
            }
        );
    
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}