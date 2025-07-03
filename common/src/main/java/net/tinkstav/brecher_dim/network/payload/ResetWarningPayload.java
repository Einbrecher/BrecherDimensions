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
 * Packet payload to send reset warnings to players in dimensions
 */
public record ResetWarningPayload(int minutesRemaining, String message) implements CustomPacketPayload {
    // Type wrapper for the packet
    public static final CustomPacketPayload.Type<ResetWarningPayload> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BrecherDimensions.MOD_ID, "reset_warning"));
    
    // StreamCodec for serialization
    public static final StreamCodec<RegistryFriendlyByteBuf, ResetWarningPayload> STREAM_CODEC = 
        StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ResetWarningPayload::minutesRemaining,
            ByteBufCodecs.STRING_UTF8, ResetWarningPayload::message,
            ResetWarningPayload::new
        );
    
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}