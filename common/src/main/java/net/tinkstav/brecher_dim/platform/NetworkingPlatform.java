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
     * Initialize the networking system
     */
    @ExpectPlatform
    public static void init() {
        throw new AssertionError("Platform implementation missing");
    }
    
    /**
     * Sync dimension information to a specific player
     * This is the main method used by the Mixins
     */
    @ExpectPlatform
    public static void syncDimensionToPlayer(ServerPlayer player, ServerLevel dimension) {
        throw new AssertionError("Platform implementation missing");
    }
    
    /**
     * Sync dimension existence to a specific player
     */
    @ExpectPlatform
    public static void syncDimensionToPlayer(ServerPlayer player, ResourceLocation dimension, boolean exists) {
        throw new AssertionError("Platform implementation missing");
    }
    
    /**
     * Sync dimension information to all players
     */
    @ExpectPlatform
    public static void syncDimensionToAll(ResourceLocation dimension, boolean exists) {
        throw new AssertionError("Platform implementation missing");
    }
    
    /**
     * Send a reset warning to a player
     */
    @ExpectPlatform
    public static void sendResetWarning(ServerPlayer player, int minutesRemaining, String message) {
        throw new AssertionError("Platform implementation missing");
    }
    
    /**
     * Send a dimension reset notification to a player
     */
    @ExpectPlatform
    public static void sendDimensionReset(ServerPlayer player, ResourceLocation dimension, long resetTime) {
        throw new AssertionError("Platform implementation missing");
    }
    
    /**
     * Send registry sync data to a player
     */
    @ExpectPlatform
    public static void sendRegistrySync(ServerPlayer player, ResourceLocation dimension, byte[] nbtData) {
        throw new AssertionError("Platform implementation missing");
    }
    
    /**
     * Send enhanced registry sync data to a player
     */
    @ExpectPlatform
    public static void sendEnhancedRegistrySync(ServerPlayer player, byte[] nbtData) {
        throw new AssertionError("Platform implementation missing");
    }
    
    /**
     * Send chunked registry sync data to a player
     */
    @ExpectPlatform
    public static void sendChunkedRegistrySync(ServerPlayer player, int chunkIndex, int totalChunks, byte[] nbtData) {
        throw new AssertionError("Platform implementation missing");
    }
    
    /**
     * Send a message to all players in a dimension
     */
    @ExpectPlatform
    public static void sendMessageToDimension(ServerLevel level, String message) {
        throw new AssertionError("Platform implementation missing");
    }
}