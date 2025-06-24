package net.tinkstav.brecher_dim.platform;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.tinkstav.brecher_dim.network.BrecherNetworking;

/**
 * NeoForge implementation of NetworkingPlatform
 * This class is discovered by Architectury's @ExpectPlatform system
 * Delegates to the cross-platform BrecherNetworking implementation
 */
public class NetworkingPlatformImpl {
    
    public static void init() {
        BrecherNetworking.init();
    }
    
    public static void syncDimensionToPlayer(ServerPlayer player, ServerLevel dimension) {
        // Complex dimension sync - implement based on original logic
        ResourceLocation dimensionId = dimension.dimension().location();
        BrecherNetworking.sendDimensionSync(player, dimensionId, true);
        
        // TODO: Also send registry sync data for the dimension
        // This would require serializing the dimension type and level stem
    }
    
    public static void syncDimensionToPlayer(ServerPlayer player, ResourceLocation dimension, boolean exists) {
        BrecherNetworking.sendDimensionSync(player, dimension, exists);
    }
    
    public static void syncDimensionToAll(ResourceLocation dimension, boolean exists) {
        BrecherNetworking.sendDimensionSyncToAll(dimension, exists);
    }
    
    public static void sendResetWarning(ServerPlayer player, int minutesRemaining, String message) {
        BrecherNetworking.sendResetWarning(player, minutesRemaining, message);
    }
    
    public static void sendDimensionReset(ServerPlayer player, ResourceLocation dimension, long resetTime) {
        BrecherNetworking.sendDimensionReset(player, dimension, resetTime);
    }
    
    public static void sendRegistrySync(ServerPlayer player, ResourceLocation dimension, byte[] nbtData) {
        BrecherNetworking.sendRegistrySync(player, dimension, nbtData);
    }
    
    public static void sendEnhancedRegistrySync(ServerPlayer player, byte[] nbtData) {
        BrecherNetworking.sendEnhancedRegistrySync(player, nbtData);
    }
    
    public static void sendChunkedRegistrySync(ServerPlayer player, int chunkIndex, int totalChunks, byte[] nbtData) {
        BrecherNetworking.sendChunkedRegistrySync(player, chunkIndex, totalChunks, nbtData);
    }
    
    public static void sendMessageToDimension(ServerLevel level, String message) {
        Component component = Component.literal(message);
        level.players().forEach(player -> player.sendSystemMessage(component));
    }
}