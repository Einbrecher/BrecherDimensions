package net.tinkstav.brecher_dim.fabric;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.tinkstav.brecher_dim.BrecherDimensions;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * Fabric implementation of NetworkingPlatform
 */
public class FabricNetworkingPlatform {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // Packet identifiers
    public static final ResourceLocation DIMENSION_SYNC_PACKET = 
        ResourceLocation.fromNamespaceAndPath(BrecherDimensions.MOD_ID, "dimension_sync");
    
    public static void init() {
        LOGGER.info("Initializing Fabric networking for Brecher's Dimensions");
        
        // Register any client-bound packet handlers here
        // For now, we're using vanilla packets for dimension sync
    }
    
    public static void syncDimensionToPlayer(ServerPlayer player, ResourceLocation dimension, boolean added) {
        // In 1.21.1, dimension sync is handled automatically by the game
        // We may need to implement custom packets later for additional sync
        LOGGER.debug("Syncing dimension {} to player {} (added: {})", dimension, player.getName().getString(), added);
        
        // If we need custom sync in the future:
        /*
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeResourceLocation(dimension);
        buf.writeBoolean(added);
        ServerPlayNetworking.send(player, DIMENSION_SYNC_PACKET, buf);
        */
    }
    
    public static void syncDimensionToAll(ResourceLocation dimension, boolean added) {
        // Get all players on the server
        for (ServerPlayer player : PlayerLookup.all(player.getServer())) {
            syncDimensionToPlayer(player, dimension, added);
        }
    }
    
    public static void sendMessageToDimension(ServerLevel level, String message) {
        Component component = Component.literal(message);
        for (ServerPlayer player : PlayerLookup.dimension(level)) {
            player.displayClientMessage(component, false);
        }
    }
}