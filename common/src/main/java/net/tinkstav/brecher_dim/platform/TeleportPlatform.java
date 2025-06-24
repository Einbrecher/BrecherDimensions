package net.tinkstav.brecher_dim.platform;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Platform abstraction for teleportation operations
 */
public class TeleportPlatform {
    
    /**
     * Teleport a player to another dimension
     * @param player The player to teleport
     * @param destination The destination level
     * @param position The target position
     * @param yRot The y rotation (yaw)
     * @param xRot The x rotation (pitch)
     * @return true if successful
     */
    @ExpectPlatform
    public static boolean teleportToDimension(ServerPlayer player, ServerLevel destination, 
                                            Vec3 position, float yRot, float xRot) {
        throw new AssertionError("Platform implementation missing");
    }
    
    /**
     * Check if the platform supports authorized teleports (bypassing portal restrictions)
     */
    @ExpectPlatform
    public static boolean supportsAuthorizedTeleports() {
        throw new AssertionError("Platform implementation missing");
    }
    
    /**
     * Mark a teleport as authorized (to bypass portal restrictions)
     */
    @ExpectPlatform
    public static void markTeleportAuthorized(ServerPlayer player) {
        throw new AssertionError("Platform implementation missing");
    }
    
    /**
     * Clear authorized teleport status
     */
    @ExpectPlatform
    public static void clearTeleportAuthorization(ServerPlayer player) {
        throw new AssertionError("Platform implementation missing");
    }
}