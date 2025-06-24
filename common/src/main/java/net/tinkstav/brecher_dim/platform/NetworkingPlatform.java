package net.tinkstav.brecher_dim.platform;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Platform abstraction for networking operations
 */
public class NetworkingPlatform {
    
    /**
     * Sync dimension information to a specific player
     */
    @ExpectPlatform
    public static void syncDimensionToPlayer(ServerPlayer player, ResourceLocation dimension, boolean added) {
        throw new AssertionError("Platform implementation missing");
    }
    
    /**
     * Sync dimension information to all players
     */
    @ExpectPlatform
    public static void syncDimensionToAll(ResourceLocation dimension, boolean added) {
        throw new AssertionError("Platform implementation missing");
    }
    
    /**
     * Send a message to all players in a dimension
     */
    @ExpectPlatform
    public static void sendMessageToDimension(ServerLevel level, String message) {
        throw new AssertionError("Platform implementation missing");
    }
    
    /**
     * Initialize the networking system
     */
    @ExpectPlatform
    public static void init() {
        throw new AssertionError("Platform implementation missing");
    }
}