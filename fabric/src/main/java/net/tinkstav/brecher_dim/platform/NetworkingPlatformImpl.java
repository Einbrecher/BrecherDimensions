package net.tinkstav.brecher_dim.platform;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.tinkstav.brecher_dim.fabric.FabricNetworkingPlatform;

/**
 * Fabric implementation of NetworkingPlatform
 * This class is discovered by Architectury's @ExpectPlatform system
 */
public class NetworkingPlatformImpl {
    
    public static void syncDimensionToPlayer(ServerPlayer player, ResourceLocation dimension, boolean added) {
        FabricNetworkingPlatform.syncDimensionToPlayer(player, dimension, added);
    }
    
    public static void syncDimensionToAll(ResourceLocation dimension, boolean added) {
        FabricNetworkingPlatform.syncDimensionToAll(dimension, added);
    }
    
    public static void sendMessageToDimension(ServerLevel level, String message) {
        FabricNetworkingPlatform.sendMessageToDimension(level, message);
    }
    
    public static void init() {
        FabricNetworkingPlatform.init();
    }
}