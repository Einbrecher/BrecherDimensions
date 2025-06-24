package net.tinkstav.brecher_dim.platform;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.tinkstav.brecher_dim.neoforge.NeoForgeNetworkingPlatform;

/**
 * NeoForge implementation of NetworkingPlatform
 * This class is discovered by Architectury's @ExpectPlatform system
 */
public class NetworkingPlatformImpl {
    
    public static void syncDimensionToPlayer(ServerPlayer player, ResourceLocation dimension, boolean added) {
        NeoForgeNetworkingPlatform.syncDimensionToPlayer(player, dimension, added);
    }
    
    public static void syncDimensionToAll(ResourceLocation dimension, boolean added) {
        NeoForgeNetworkingPlatform.syncDimensionToAll(dimension, added);
    }
    
    public static void sendMessageToDimension(ServerLevel level, String message) {
        NeoForgeNetworkingPlatform.sendMessageToDimension(level, message);
    }
    
    public static void init() {
        NeoForgeNetworkingPlatform.init();
    }
}