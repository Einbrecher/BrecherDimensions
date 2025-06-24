package net.tinkstav.brecher_dim.neoforge;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.ITeleporter;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * NeoForge implementation of TeleportPlatform
 * Handles cross-dimensional teleportation for NeoForge
 */
public class NeoForgeTeleportPlatform {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<UUID> AUTHORIZED_TELEPORTS = new HashSet<>();
    
    /**
     * Custom teleporter that bypasses portal creation
     */
    private static class BrecherTeleporter implements ITeleporter {
        private final Vec3 targetPos;
        private final float yRot;
        private final float xRot;
        
        public BrecherTeleporter(Vec3 targetPos, float yRot, float xRot) {
            this.targetPos = targetPos;
            this.yRot = yRot;
            this.xRot = xRot;
        }
        
        @Override
        public Entity placeEntity(Entity entity, ServerLevel currentWorld, ServerLevel destWorld, float yaw, Function<Boolean, Entity> repositionEntity) {
            // Get the repositioned entity
            Entity repositioned = repositionEntity.apply(false);
            
            if (repositioned instanceof ServerPlayer player) {
                // Set position and rotation
                player.moveTo(targetPos.x, targetPos.y, targetPos.z, this.yRot, this.xRot);
                player.setYHeadRot(this.yRot);
                
                // Reset fall distance and motion
                player.fallDistance = 0.0F;
                player.setDeltaMovement(Vec3.ZERO);
                
                // Force position sync
                player.hasImpulse = true;
                player.connection.teleport(targetPos.x, targetPos.y, targetPos.z, this.yRot, this.xRot);
            }
            
            return repositioned;
        }
        
        @Override
        public boolean playTeleportSound(ServerPlayer player, ServerLevel sourceWorld, ServerLevel destWorld) {
            return false; // We'll handle sounds in our teleport handler
        }
        
        @Override
        public PortalInfo getPortalInfo(Entity entity, ServerLevel destWorld, Function<ServerLevel, PortalInfo> defaultPortalInfo) {
            // Return custom portal info with our target position
            return new PortalInfo(targetPos, Vec3.ZERO, this.yRot, this.xRot);
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
                return true;
            }
            
            // Cross-dimensional teleport
            BrecherTeleporter teleporter = new BrecherTeleporter(position, yRot, xRot);
            
            // Store current motion state
            Vec3 motion = player.getDeltaMovement();
            boolean noGravity = player.isNoGravity();
            
            // Perform the teleport
            Entity result = player.changeDimension(destination, teleporter);
            
            if (result instanceof ServerPlayer teleportedPlayer) {
                // Restore motion state
                teleportedPlayer.setDeltaMovement(Vec3.ZERO);
                teleportedPlayer.fallDistance = 0.0F;
                
                // Ensure position is correct
                teleportedPlayer.moveTo(position.x, position.y, position.z, yRot, xRot);
                teleportedPlayer.setYHeadRot(yRot);
                
                // Force client sync
                teleportedPlayer.connection.teleport(position.x, position.y, position.z, yRot, xRot);
                
                LOGGER.debug("Successfully teleported player {} to {}", 
                    player.getName().getString(), destination.dimension().location());
                return true;
            }
            
            LOGGER.error("Failed to teleport player {} - changeDimension returned null", 
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