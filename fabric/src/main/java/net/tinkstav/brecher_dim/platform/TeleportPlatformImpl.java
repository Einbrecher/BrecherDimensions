package net.tinkstav.brecher_dim.platform;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.tinkstav.brecher_dim.fabric.FabricTeleportPlatform;

/**
 * Fabric implementation of TeleportPlatform
 * This class is discovered by Architectury's @ExpectPlatform system
 */
public class TeleportPlatformImpl {
    
    public static boolean teleportToDimension(ServerPlayer player, ServerLevel destination, 
                                            Vec3 position, float yRot, float xRot) {
        return FabricTeleportPlatform.teleportToDimension(player, destination, position, yRot, xRot);
    }
    
    public static boolean supportsAuthorizedTeleports() {
        return FabricTeleportPlatform.supportsAuthorizedTeleports();
    }
    
    public static void markTeleportAuthorized(ServerPlayer player) {
        FabricTeleportPlatform.markTeleportAuthorized(player);
    }
    
    public static void clearTeleportAuthorization(ServerPlayer player) {
        FabricTeleportPlatform.clearTeleportAuthorization(player);
    }
}