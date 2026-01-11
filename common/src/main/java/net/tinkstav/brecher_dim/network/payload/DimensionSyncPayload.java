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
 * Packet payload to sync dimension existence state to clients
 */
public record DimensionSyncPayload(ResourceLocation dimensionId, boolean exists) implements CustomPacketPayload {
    // Type wrapper for the packet
    public static final CustomPacketPayload.Type<DimensionSyncPayload> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BrecherDimensions.MOD_ID, "dimension_sync"));
    
    // StreamCodec for serialization
    public static final StreamCodec<RegistryFriendlyByteBuf, DimensionSyncPayload> STREAM_CODEC = 
        StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, DimensionSyncPayload::dimensionId,
            ByteBufCodecs.BOOL, DimensionSyncPayload::exists,
            DimensionSyncPayload::new
        );
    
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}