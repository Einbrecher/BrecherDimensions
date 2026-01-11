/*
 * Brecher's Dimensions - Temporary resettable dimensions for exploration
 * Copyright (C) 2025 Einbrecher. All rights reserved.
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