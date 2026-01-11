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
 * Packet payload to notify clients about scheduled dimension resets
 */
public record DimensionResetPayload(ResourceLocation dimensionId, long resetTime) implements CustomPacketPayload {
    // Type wrapper for the packet
    public static final CustomPacketPayload.Type<DimensionResetPayload> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BrecherDimensions.MOD_ID, "dimension_reset"));
    
    // StreamCodec for serialization
    public static final StreamCodec<RegistryFriendlyByteBuf, DimensionResetPayload> STREAM_CODEC = 
        StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, DimensionResetPayload::dimensionId,
            ByteBufCodecs.VAR_LONG, DimensionResetPayload::resetTime,
            DimensionResetPayload::new
        );
    
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}