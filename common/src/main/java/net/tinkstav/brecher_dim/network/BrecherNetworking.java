package net.tinkstav.brecher_dim.network;

import dev.architectury.networking.NetworkManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.tinkstav.brecher_dim.BrecherDimensions;
import net.tinkstav.brecher_dim.network.packets.*;

/**
 * Cross-platform networking handler using Architectury's networking API
 */
public class BrecherNetworking {
    // Packet channel identifiers
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
     */
    public static void init() {
        // Register server-to-client packets
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, DIMENSION_SYNC, DimensionSyncPacket::handle);
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, DIMENSION_RESET, DimensionResetPacket::handle);
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, RESET_WARNING, ResetWarningPacket::handle);
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, REGISTRY_SYNC, RegistrySyncPacket::handle);
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, ENHANCED_REGISTRY_SYNC, EnhancedRegistrySyncPacket::handle);
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, CHUNKED_REGISTRY_SYNC, ChunkedRegistrySyncPacket::handle);
        
        BrecherDimensions.LOGGER.info("Initialized cross-platform networking channels");
    }
    
    /**
     * Send a dimension sync packet to a specific player
     */
    public static void sendDimensionSync(ServerPlayer player, ResourceLocation dimensionId, boolean exists) {
        DimensionSyncPacket packet = new DimensionSyncPacket(dimensionId, exists);
        NetworkManager.sendToPlayer(player, DIMENSION_SYNC, packet.toByteBuf());
    }
    
    /**
     * Send a dimension sync packet to all players
     */
    public static void sendDimensionSyncToAll(ResourceLocation dimensionId, boolean exists) {
        DimensionSyncPacket packet = new DimensionSyncPacket(dimensionId, exists);
        NetworkManager.sendToPlayers(DIMENSION_SYNC, packet.toByteBuf());
    }
    
    /**
     * Send a dimension reset notification to a player
     */
    public static void sendDimensionReset(ServerPlayer player, ResourceLocation dimensionId, long resetTime) {
        DimensionResetPacket packet = new DimensionResetPacket(dimensionId, resetTime);
        NetworkManager.sendToPlayer(player, DIMENSION_RESET, packet.toByteBuf());
    }
    
    /**
     * Send a reset warning to a player
     */
    public static void sendResetWarning(ServerPlayer player, int minutesRemaining, String message) {
        ResetWarningPacket packet = new ResetWarningPacket(minutesRemaining, message);
        NetworkManager.sendToPlayer(player, RESET_WARNING, packet.toByteBuf());
    }
    
    /**
     * Send registry sync data to a player
     */
    public static void sendRegistrySync(ServerPlayer player, ResourceLocation dimensionId, byte[] nbtData) {
        RegistrySyncPacket packet = new RegistrySyncPacket(dimensionId, nbtData);
        NetworkManager.sendToPlayer(player, REGISTRY_SYNC, packet.toByteBuf());
    }
    
    /**
     * Send enhanced registry sync data to a player
     */
    public static void sendEnhancedRegistrySync(ServerPlayer player, byte[] nbtData) {
        EnhancedRegistrySyncPacket packet = new EnhancedRegistrySyncPacket(nbtData);
        NetworkManager.sendToPlayer(player, ENHANCED_REGISTRY_SYNC, packet.toByteBuf());
    }
    
    /**
     * Send chunked registry sync data to a player
     */
    public static void sendChunkedRegistrySync(ServerPlayer player, int chunkIndex, int totalChunks, byte[] nbtData) {
        ChunkedRegistrySyncPacket packet = new ChunkedRegistrySyncPacket(chunkIndex, totalChunks, nbtData);
        NetworkManager.sendToPlayer(player, CHUNKED_REGISTRY_SYNC, packet.toByteBuf());
    }
}