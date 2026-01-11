/*
 * Brecher's Dimensions - Temporary resettable dimensions for exploration
 * Copyright (C) 2025 Einbrecher. All rights reserved.
 */

package net.tinkstav.brecher_dim.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.tinkstav.brecher_dim.BrecherDimensions;
import net.tinkstav.brecher_dim.network.payload.*;
import net.tinkstav.brecher_dim.platform.Services;

/**
 * Cross-platform networking handler using platform-specific implementations
 * This delegates to Fabric or NeoForge networking APIs via service loader pattern
 */
public class BrecherNetworking {
    // Packet channel identifiers (kept for backward compatibility)
    public static final ResourceLocation DIMENSION_SYNC = 
        ResourceLocation.fromNamespaceAndPath(BrecherDimensions.MOD_ID, "dimension_sync");
    public static final ResourceLocation DIMENSION_RESET = 
        ResourceLocation.fromNamespaceAndPath(BrecherDimensions.MOD_ID, "dimension_reset");
    public static final ResourceLocation RESET_WARNING = 
        ResourceLocation.fromNamespaceAndPath(BrecherDimensions.MOD_ID, "reset_warning");
    public static final ResourceLocation REGISTRY_SYNC = 
        ResourceLocation.fromNamespaceAndPath(BrecherDimensions.MOD_ID, "registry_sync");
    public static final ResourceLocation ENHANCED_REGISTRY_SYNC = 
        ResourceLocation.fromNamespaceAndPath(BrecherDimensions.MOD_ID, "enhanced_registry_sync");
    public static final ResourceLocation CHUNKED_REGISTRY_SYNC = 
        ResourceLocation.fromNamespaceAndPath(BrecherDimensions.MOD_ID, "chunked_registry_sync");
    
    /**
     * Initialize networking channels and register packet handlers
     * Platform-specific registration happens in each platform module
     */
    public static void init() {
        BrecherDimensions.LOGGER.info("Initializing Brecher Dimensions networking");
        // Platform-specific initialization is handled by Services.PACKETS.registerPackets()
        Services.PACKETS.registerPackets();
    }
    
    /**
     * Send a dimension sync packet to a specific player
     */
    public static void sendDimensionSync(ServerPlayer player, ResourceLocation dimensionId, boolean exists) {
        Services.PACKETS.sendToPlayer(player, new DimensionSyncPayload(dimensionId, exists));
    }
    
    /**
     * Send a dimension sync packet to all players
     */
    public static void sendDimensionSyncToAll(ResourceLocation dimensionId, boolean exists) {
        Services.PACKETS.sendToAllPlayers(new DimensionSyncPayload(dimensionId, exists));
    }
    
    /**
     * Send a dimension reset notification to a player
     */
    public static void sendDimensionReset(ServerPlayer player, ResourceLocation dimensionId, long resetTime) {
        Services.PACKETS.sendToPlayer(player, new DimensionResetPayload(dimensionId, resetTime));
    }
    
    /**
     * Send a reset warning to a player
     */
    public static void sendResetWarning(ServerPlayer player, int minutesRemaining, String message) {
        Services.PACKETS.sendToPlayer(player, new ResetWarningPayload(minutesRemaining, message));
    }
    
    /**
     * Send registry sync data to a player
     */
    public static void sendRegistrySync(ServerPlayer player, ResourceLocation dimensionId, byte[] nbtData) {
        Services.PACKETS.sendToPlayer(player, new RegistrySyncPayload(dimensionId, nbtData));
    }
    
    /**
     * Send enhanced registry sync data to a player
     */
    public static void sendEnhancedRegistrySync(ServerPlayer player, byte[] nbtData) {
        Services.PACKETS.sendToPlayer(player, new EnhancedRegistrySyncPayload(nbtData));
    }
    
    /**
     * Send chunked registry sync data to a player
     */
    public static void sendChunkedRegistrySync(ServerPlayer player, int chunkIndex, int totalChunks, byte[] nbtData) {
        Services.PACKETS.sendToPlayer(player, new ChunkedRegistrySyncPayload(chunkIndex, totalChunks, nbtData));
    }
}