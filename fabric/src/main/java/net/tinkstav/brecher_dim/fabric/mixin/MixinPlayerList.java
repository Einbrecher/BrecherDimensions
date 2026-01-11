/*
 * Brecher's Dimensions - Temporary resettable dimensions for exploration
 * Copyright (C) 2025 Einbrecher. All rights reserved.
 */

package net.tinkstav.brecher_dim.fabric.mixin;

import com.mojang.logging.LogUtils;
import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import net.tinkstav.brecher_dim.dimension.BrecherDimensionManager;
import net.tinkstav.brecher_dim.network.BrecherNetworking;
import net.tinkstav.brecher_dim.accessor.IServerDimensionAccessor;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public abstract class MixinPlayerList {
    @Unique
    private static final Logger LOGGER = LogUtils.getLogger();
    
    @Shadow public abstract void sendLevelInfo(ServerPlayer player, ServerLevel level);
    @Shadow @Final private MinecraftServer server;
    
    /**
     * Sync new dimension to specific player when they join
     */
    @Inject(method = "placeNewPlayer", at = @At("TAIL"))
    private void brecher_dim$syncRuntimeDimensions(Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo ci) {
        // Check if player needs emergency evacuation from exploration dimension
        if (BrecherDimensionManager.isInExplorationDimension(player)) {
            LOGGER.warn("Player {} logged in within exploration dimension {} - performing emergency evacuation", 
                player.getName().getString(), player.level().dimension().location());
            
            // Schedule evacuation for next tick to ensure player is fully initialized
            server.tell(new net.minecraft.server.TickTask(server.getTickCount() + 1, () -> {
                try {
                    net.tinkstav.brecher_dim.teleport.TeleportHandler.returnFromExploration(player);
                    LOGGER.info("Successfully evacuated player {} from exploration dimension", 
                        player.getName().getString());
                } catch (Exception e) {
                    LOGGER.error("Failed to evacuate player {} from exploration dimension", 
                        player.getName().getString(), e);
                    // Force teleport to spawn as ultimate fallback
                    try {
                        net.tinkstav.brecher_dim.teleport.TeleportHandler.forceReturnToSpawn(player);
                    } catch (Exception fallbackE) {
                        LOGGER.error("Failed to force return player {} to spawn", 
                            player.getName().getString(), fallbackE);
                    }
                }
            }));
        }
        
        // Send info about all runtime dimensions to new players
        if (server instanceof IServerDimensionAccessor accessor) {
            accessor.brecher_dim$getRuntimeLevels().forEach((key, level) -> {
                try {
                    // Send level info
                    sendLevelInfo(player, level);
                    
                    // Send custom packet with dimension metadata
                    BrecherNetworking.sendDimensionSync(player, level.dimension().location(), true);
                    
                    LOGGER.debug("Synced runtime dimension {} to player {}", 
                        key.location(), player.getName().getString());
                } catch (Exception e) {
                    LOGGER.error("Failed to sync dimension {} to player {}", 
                        key.location(), player.getName().getString(), e);
                }
            });
        }
    }
    
    /**
     * Handle dimension transition for runtime dimensions
     */
    @Inject(method = "sendPlayerPermissionLevel", at = @At("HEAD"))
    private void brecher_dim$handleRuntimeDimensionChange(ServerPlayer player, CallbackInfo ci) {
        // Ensure player has latest dimension registry when changing dimensions
        if (BrecherDimensionManager.isInExplorationDimension(player)) {
            // Additional sync if needed - send all runtime dimensions
            if (server instanceof IServerDimensionAccessor accessor) {
                accessor.brecher_dim$getRuntimeLevels().forEach((key, level) -> {
                    BrecherNetworking.sendDimensionSync(player, key.location(), true);
                });
            }
        }
    }
    
    /**
     * Clean up when player disconnects
     */
    @Inject(method = "remove", at = @At("HEAD"))
    private void brecher_dim$onPlayerDisconnect(ServerPlayer player, CallbackInfo ci) {
        // Track player leaving exploration dimension
        if (BrecherDimensionManager.isInExplorationDimension(player)) {
            BrecherDimensionManager.trackPlayerLeaving(player);
        }
    }
}