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
 * Packet payload for comprehensive dimension data sync
 */
public record EnhancedRegistrySyncPayload(byte[] nbtData) implements CustomPacketPayload {
    // Type wrapper for the packet
    public static final CustomPacketPayload.Type<EnhancedRegistrySyncPayload> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BrecherDimensions.MOD_ID, "enhanced_registry_sync"));
    
    // StreamCodec for serialization
    public static final StreamCodec<RegistryFriendlyByteBuf, EnhancedRegistrySyncPayload> STREAM_CODEC = 
        StreamCodec.of(
            (buf, payload) -> buf.writeByteArray(payload.nbtData()),
            buf -> new EnhancedRegistrySyncPayload(buf.readByteArray())
        );
    
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}