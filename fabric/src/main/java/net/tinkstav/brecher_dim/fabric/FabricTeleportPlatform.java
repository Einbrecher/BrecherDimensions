package net.tinkstav.brecher_dim.fabric;

import net.fabricmc.fabric.api.dimension.v1.FabricDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Fabric implementation of TeleportPlatform
 * Handles cross-dimensional teleportation for Fabric
 */
public class FabricTeleportPlatform {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<UUID> AUTHORIZED_TELEPORTS = new HashSet<>();
    
    /**
     * Custom teleport target that positions the player at the exact location
     */
    private static class BrecherTeleportTarget implements FabricDimensions.TeleportTarget {
        private final Vec3 position;
        private final float yRot;
        private final float xRot;
        
        public BrecherTeleportTarget(Vec3 position, float yRot, float xRot) {
            this.position = position;
            this.yRot = yRot;
            this.xRot = xRot;
        }
        
        @Override
        public Entity placeEntity(Entity entity, ServerLevel currentWorld, ServerLevel destWorld, Entity passenger) {
            if (entity instanceof ServerPlayer player) {
                // Set position and rotation
                player.moveTo(position.x, position.y, position.z, this.yRot, this.xRot);
                player.setYHeadRot(this.yRot);
                
                // Reset fall distance and motion
                player.fallDistance = 0.0F;
                player.setDeltaMovement(Vec3.ZERO);
                
                // Force position sync
                player.hasImpulse = true;
                player.connection.teleport(position.x, position.y, position.z, this.yRot, this.xRot);
            }
            
            return entity;
        }
    }
    
    public static boolean teleportToDimension(ServerPlayer player, ServerLevel destination, 
                                            Vec3 position, float yRot, float xRot) {
        try {
            LOGGER.debug("Teleporting player {} to dimension {} at position {}", 
                player.getName().getString(), destination.dimension().location(), position);
            
            if (player.level() == destination) {
                // Same dimension teleport
                player.teleportTo(position.x, position.y, position.z);
                player.setYRot(yRot);
                player.setXRot(xRot);
                player.setYHeadRot(yRot);
                player.connection.teleport(position.x, position.y, position.z, yRot, xRot);
                return true;
            }
            
            // Cross-dimensional teleport using Fabric API
            BrecherTeleportTarget target = new BrecherTeleportTarget(position, yRot, xRot);
            
            // Store current state
            Vec3 oldMotion = player.getDeltaMovement();
            float oldFallDistance = player.fallDistance;
            
            // Perform the teleport
            Entity result = FabricDimensions.teleport(player, destination, target);
            
            if (result instanceof ServerPlayer teleportedPlayer) {
                // Ensure clean state after teleport
                teleportedPlayer.setDeltaMovement(Vec3.ZERO);
                teleportedPlayer.fallDistance = 0.0F;
                
                // Double-check position
                if (!teleportedPlayer.position().equals(position)) {
                    teleportedPlayer.moveTo(position.x, position.y, position.z, yRot, xRot);
                    teleportedPlayer.connection.teleport(position.x, position.y, position.z, yRot, xRot);
                }
                
                LOGGER.debug("Successfully teleported player {} to {}", 
                    player.getName().getString(), destination.dimension().location());
                return true;
            }
            
            LOGGER.error("Failed to teleport player {} - teleport returned unexpected result", 
                player.getName().getString());
            return false;
            
        } catch (Exception e) {
            LOGGER.error("Error teleporting player {} to dimension {}", 
                player.getName().getString(), destination.dimension().location(), e);
            return false;
        }
    }
    
    public static boolean supportsAuthorizedTeleports() {
        return true;
    }
    
    public static void markTeleportAuthorized(ServerPlayer player) {
        AUTHORIZED_TELEPORTS.add(player.getUUID());
    }
    
    public static void clearTeleportAuthorization(ServerPlayer player) {
        AUTHORIZED_TELEPORTS.remove(player.getUUID());
    }
    
    public static boolean isAuthorized(ServerPlayer player) {
        return AUTHORIZED_TELEPORTS.contains(player.getUUID());
    }
}