/*
 * Brecher's Dimensions - Temporary resettable dimensions for exploration
 * Copyright (C) 2025 Einbrecher. All rights reserved.
 */

package net.tinkstav.brecher_dim.teleport;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.server.TickTask;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.Entity;
import net.tinkstav.brecher_dim.BrecherDimensions;
import net.tinkstav.brecher_dim.config.BrecherConfig;
import net.tinkstav.brecher_dim.data.BrecherSavedData;
import net.tinkstav.brecher_dim.data.ReturnPosition;
import net.tinkstav.brecher_dim.dimension.BrecherDimensionManager;
import net.tinkstav.brecher_dim.dimension.SimpleSeedManager;
import net.tinkstav.brecher_dim.platform.Services;
import net.tinkstav.brecher_dim.util.DimensionEnvironment;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

public class TeleportHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_ATTEMPTS = 100;
    private static final Map<UUID, Long> lastTeleportTime = new ConcurrentHashMap<>();
    
    private static final ScheduledExecutorService CLEANUP_EXECUTOR = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "BrecherDim-TeleportCleanup");
        t.setDaemon(true);
        return t;
    });
    private static final long CLEANUP_INTERVAL_MINUTES = 5;
    private static final long DATA_RETENTION_MINUTES = 30;
    private static final int MAX_TELEPORT_RECORDS = 1000;
    // 5ms = 10% of tick budget (50ms per tick at 20 TPS); prevents server lag during teleport.
    // If no safe position is found within budget, emergency platform is created as fallback.
    private static final long MAX_SEARCH_TIME_MS = 5;

    /**
     * Dismounts the player from any vehicle before teleportation.
     * This prevents glitchy behavior, ghost entities, or client desync when
     * teleporting while riding a horse, boat, minecart, or other vehicle.
     *
     * @param player The player to dismount
     */
    private static void dismountBeforeTeleport(ServerPlayer player) {
        if (player.getVehicle() != null) {
            Entity vehicle = player.getVehicle();
            LOGGER.debug("Dismounting player {} from {} before teleport",
                player.getName().getString(), vehicle.getType().getDescriptionId());
            player.stopRiding();
            player.displayClientMessage(
                Component.literal("Dismounted for dimensional travel.")
                    .withStyle(ChatFormatting.YELLOW),
                false
            );
        }
        // Also eject any passengers riding the player (rare but possible with mods)
        if (!player.getPassengers().isEmpty()) {
            LOGGER.debug("Ejecting {} passengers from player {} before teleport",
                player.getPassengers().size(), player.getName().getString());
            player.ejectPassengers();
        }
    }

    static {
        // Schedule periodic cleanup of old teleport time data
        CLEANUP_EXECUTOR.scheduleAtFixedRate(() -> {
            try {
                long cutoffTime = System.currentTimeMillis() - (DATA_RETENTION_MINUTES * 60 * 1000);

                // Clean up old teleport times
                lastTeleportTime.entrySet().removeIf(entry -> {
                    Long time = entry.getValue();
                    return time != null && time < cutoffTime;
                });

                LOGGER.debug("Cleaned up teleport data. Times: {}", lastTeleportTime.size());

            } catch (Exception e) {
                LOGGER.error("Error during teleport data cleanup", e);
            }
        }, CLEANUP_INTERVAL_MINUTES, CLEANUP_INTERVAL_MINUTES, TimeUnit.MINUTES);
    }
    
    /**
     * Teleport a player to an exploration dimension
     */
    public static void teleportToExploration(ServerPlayer player, ServerLevel destination) {
        // Check cooldown
        if (!checkTeleportCooldown(player)) {
            return;
        }

        // Dismount player from any vehicle to prevent glitches
        dismountBeforeTeleport(player);

        // Save return position
        BrecherSavedData data = BrecherSavedData.get(player.server);
        ResourceLocation currentDim = player.level().dimension().location();
        BlockPos currentPos = player.blockPosition();
        
        BrecherDimensionManager manager = BrecherDimensions.getDimensionManager();
        boolean isFromExploration = manager != null && manager.isExplorationDimension(currentDim);
        
        if (!isFromExploration) {
            LOGGER.debug("Saving return position for player {} - Current dimension: {}, position: {}", 
                player.getName().getString(), currentDim, currentPos);
                
            data.saveReturnPosition(
                player.getUUID(), 
                currentPos, 
                currentDim,
                player.getYRot(),
                player.getXRot()
            );
        } else {
            LOGGER.debug("Player {} teleporting from exploration dimension {} - preserving original return position", 
                player.getName().getString(), currentDim);
            
            if (!data.getReturnPosition(player.getUUID()).isPresent()) {
                LOGGER.warn("Player {} in exploration dimension without return position - saving emergency position", 
                    player.getName().getString());
                BlockPos overworldSpawn = player.server.overworld().getSharedSpawnPos();
                data.saveReturnPosition(
                    player.getUUID(),
                    overworldSpawn,
                    player.server.overworld().dimension().location(),
                    0.0F,
                    0.0F
                );
            }
        }
        
        // Record dimension access
        data.recordDimensionAccess(destination.dimension().location(), player.getUUID());

        // Track whether preTeleport was called to ensure postTeleport is only called if preTeleport succeeded.
        // This prevents mismatched event callbacks when teleport fails early.
        boolean preTeleportCalled = false;

        // Execute teleportation
        try {
            BlockPos spawnPoint = destination.getSharedSpawnPos();
            BlockPos safePos;

            // Force-load the center chunk before searching for safe position
            // This prevents the "emergency platform trap" where all distant teleports
            // would fail to find terrain because chunks aren't loaded
            ChunkPos centerChunk = new ChunkPos(spawnPoint);
            destination.getChunkSource().addRegionTicket(TicketType.PORTAL, centerChunk, 3, spawnPoint);

            // Special handling for different dimension types
            // Uses dimension properties (ultraWarm, fixedTime) for better modded dimension compatibility
            DimensionEnvironment dimEnv = DimensionEnvironment.getDimensionEnvironment(destination);
            if (dimEnv == DimensionEnvironment.NETHER_LIKE) {
                safePos = findNetherSafePosition(destination, spawnPoint);
            } else if (dimEnv == DimensionEnvironment.END_LIKE) {
                safePos = findEndSafePosition(destination);
            } else {
                safePos = findSafePosition(destination, spawnPoint);
            }
            
            if (safePos == null) {
                // Initial search failed - chunk ticket was async and chunk may not be loaded yet
                // Force synchronous chunk load and retry before falling back to emergency platform
                // WARN: This may cause a brief lag spike on first teleport to ungenerated chunks
                LOGGER.debug("Initial safe position search failed, forcing synchronous chunk load at {}", centerChunk);
                destination.getChunk(spawnPoint.getX() >> 4, spawnPoint.getZ() >> 4, ChunkStatus.FULL);

                // Retry the search now that chunk is loaded
                if (dimEnv == DimensionEnvironment.NETHER_LIKE) {
                    safePos = findNetherSafePosition(destination, spawnPoint);
                } else if (dimEnv == DimensionEnvironment.END_LIKE) {
                    safePos = findEndSafePosition(destination);
                } else {
                    safePos = findSafePosition(destination, spawnPoint);
                }
            }

            if (safePos == null) {
                LOGGER.warn("Could not find safe position in {} after chunk load - creating emergency platform",
                    destination.dimension().location());
                safePos = createEmergencyPlatform(destination, spawnPoint);
            }
            
            // Log detailed spawn information
            boolean isSurface = canSeeSky(destination, safePos);
            LOGGER.debug("Teleporting player {} to exploration dimension {} at position {} (surface: {}, spawn point was: {})", 
                player.getName().getString(), destination.dimension().location(), safePos, isSurface, spawnPoint);
            
            // Clear certain potion effects
            player.removeEffect(MobEffects.LEVITATION);
            player.removeEffect(MobEffects.BLINDNESS);
            
            // Perform teleportation
            Vec3 targetPos = new Vec3(safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5);
            Services.TELEPORT.preTeleport(player, destination);
            preTeleportCalled = true;
            boolean success = Services.TELEPORT.teleportPlayer(player, destination, targetPos, player.getYRot(), player.getXRot());
            
            if (success) {
                // Grant brief invulnerability and effects (5 seconds = 100 ticks)
                player.setInvulnerable(true);
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 4));
                player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0));

                // Schedule removal of invulnerability after 100 ticks
                // Use UUID lookup to handle player disconnect/reconnect during the 5 second window
                UUID playerUuid = player.getUUID();
                net.minecraft.server.MinecraftServer server = player.server;
                server.tell(new TickTask(server.getTickCount() + 100, () -> {
                    ServerPlayer currentPlayer = server.getPlayerList().getPlayer(playerUuid);
                    if (currentPlayer != null) {
                        currentPlayer.setInvulnerable(false);
                    }
                }));

                // Send appropriate message
                if (isFromExploration) {
                    player.displayClientMessage(
                        Component.literal("Teleported between exploration dimensions!")
                            .withStyle(ChatFormatting.GREEN),
                        false
                    );
                    
                    data.getReturnPosition(player.getUUID()).ifPresent(returnPos -> {
                        String dimName = returnPos.dimension().getPath();
                        player.displayClientMessage(
                            Component.literal("Your return point remains in: " + dimName)
                                .withStyle(ChatFormatting.GRAY),
                            false
                        );
                    });
                    
                    // Display seed lock information when teleporting between exploration dimensions
                    Duration timeUntilReset = SimpleSeedManager.getTimeUntilSeedReset();
                    if (timeUntilReset != null) {
                        String timeRemaining = SimpleSeedManager.formatDuration(timeUntilReset);
                        player.displayClientMessage(
                            Component.literal("⏱ Seed lock expires in: " + timeRemaining)
                                .withStyle(ChatFormatting.YELLOW),
                            false
                        );
                    }
                } else {
                    String welcomeMsg = BrecherConfig.getWelcomeMessage();
                    player.displayClientMessage(
                        Component.literal(welcomeMsg).withStyle(ChatFormatting.GREEN),
                        false
                    );
                    
                    // Display seed lock information
                    Duration timeUntilReset = SimpleSeedManager.getTimeUntilSeedReset();
                    if (timeUntilReset != null) {
                        String timeRemaining = SimpleSeedManager.formatDuration(timeUntilReset);
                        player.displayClientMessage(
                            Component.literal("⏱ Seed lock expires in: " + timeRemaining)
                                .withStyle(ChatFormatting.YELLOW),
                            false
                        );
                        player.displayClientMessage(
                            Component.literal("This dimension will reset with a new seed after the next server restart once the lock expires.")
                                .withStyle(ChatFormatting.GRAY),
                            false
                        );
                    } else {
                        // Random seed strategy - dimension resets on every restart
                        player.displayClientMessage(
                            Component.literal("⚠ This dimension will reset with a new seed after the next server restart.")
                                .withStyle(ChatFormatting.YELLOW),
                            false
                        );
                    }
                }
                
                // Track in dimension manager
                if (manager != null) {
                    manager.onPlayerEnterExploration(player, destination.dimension().location());
                }
                
                // Update last teleport time
                if (lastTeleportTime.size() >= MAX_TELEPORT_RECORDS) {
                    lastTeleportTime.entrySet().stream()
                        .min(Map.Entry.comparingByValue())
                        .ifPresent(entry -> lastTeleportTime.remove(entry.getKey()));
                }
                lastTeleportTime.put(player.getUUID(), System.currentTimeMillis());
            } else {
                throw new RuntimeException("Teleportation failed");
            }
        } catch (Exception e) {
            LOGGER.error("Failed to teleport player {} to {}: {}",
                player.getName().getString(),
                destination.dimension().location(),
                e.getMessage());
            player.displayClientMessage(
                Component.literal("Teleportation failed! Please try again.")
                    .withStyle(ChatFormatting.RED),
                false
            );

            // Only clear return position if we failed initial entry (not exploration-to-exploration)
            // This preserves the original return coordinates when inter-exploration teleport fails
            if (!isFromExploration) {
                data.clearReturnPosition(player.getUUID());
            }
        } finally {
            // Only call postTeleport if preTeleport was successfully called
            // This prevents mismatched event callbacks when teleport fails early
            if (preTeleportCalled) {
                Services.TELEPORT.postTeleport(player, player.level() instanceof ServerLevel ? (ServerLevel) player.level() : null);
            }
        }
    }

    /**
     * Return a player from an exploration dimension
     */
    public static void returnFromExploration(ServerPlayer player) {
        // Dismount player from any vehicle to prevent glitches
        dismountBeforeTeleport(player);

        BrecherSavedData data = BrecherSavedData.get(player.server);

        LOGGER.debug("Attempting to return player {} from exploration dimension", player.getName().getString());
        
        data.getReturnPosition(player.getUUID()).ifPresentOrElse(returnPos -> {
            LOGGER.debug("Found return position for player {}: {} in dimension {}", 
                player.getName().getString(), returnPos.pos(), returnPos.dimension());
            ServerLevel returnLevel = player.server.getLevel(
                ResourceKey.create(Registries.DIMENSION, returnPos.dimension())
            );
            
            if (returnLevel != null) {
                // Clear inventory if configured
                if (BrecherConfig.isClearInventoryOnReturn()) {
                    player.getInventory().clearContent();
                }
                
                // Check if return position is safe
                BlockPos safePos = returnPos.pos();
                if (!isSafePosition(returnLevel, safePos)) {
                    // If preferring surface spawns and original position is underground, try to find surface first
                    // Only for overworld-like dimensions where surface spawns make sense
                    if (BrecherConfig.isPreferSurfaceSpawns() &&
                        DimensionEnvironment.getDimensionEnvironment(returnLevel) == DimensionEnvironment.OVERWORLD_LIKE &&
                        !canSeeSky(returnLevel, safePos)) {
                        BlockPos surfacePos = findSurfacePosition(returnLevel, safePos);
                        if (surfacePos != null) {
                            LOGGER.debug("Found surface position {} for underground return position {} for player {}", 
                                surfacePos, returnPos.pos(), player.getName().getString());
                            safePos = surfacePos;
                        } else {
                            // Fallback to normal safe position search
                            safePos = findSafePosition(returnLevel, safePos);
                        }
                    } else {
                        safePos = findSafePosition(returnLevel, safePos);
                    }
                    
                    if (safePos == null) {
                        LOGGER.warn("Return position at {} is not safe for player {} - creating emergency platform", 
                            returnPos.pos(), player.getName().getString());
                        safePos = createEmergencyPlatform(returnLevel, returnPos.pos());
                    }
                }
                // For saved positions, we now try to find surface alternatives if configured and appropriate
                
                LOGGER.debug("Teleporting player {} to {} at position {}",
                    player.getName().getString(), returnLevel.dimension().location(), safePos);

                // Track whether preTeleport was called to ensure postTeleport is only called if preTeleport succeeded
                boolean preTeleportCalled = false;

                try {
                    Vec3 targetPos = new Vec3(safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5);
                    Services.TELEPORT.preTeleport(player, returnLevel);
                    preTeleportCalled = true;
                    boolean success = Services.TELEPORT.teleportPlayer(player, returnLevel, targetPos,
                                                                      returnPos.yRot(), returnPos.xRot());
                    
                    if (success) {
                        // Add temporary effects for safety
                        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 4));
                        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0));
                        
                        // Clear return position
                        data.clearReturnPosition(player.getUUID());
                        
                        // Clear dimension tracking
                        data.clearPlayerDimensionTracking(player.getUUID());
                        
                        // Track leaving exploration
                        BrecherDimensionManager manager = BrecherDimensions.getDimensionManager();
                        if (manager != null) {
                            manager.onPlayerLeaveExploration(player);
                        }
                        
                        // Send return message
                        String returnMsg = BrecherConfig.getReturnMessage();
                        player.displayClientMessage(
                            Component.literal(returnMsg).withStyle(ChatFormatting.GREEN),
                            false
                        );
                        
                        player.displayClientMessage(
                            Component.literal("Returned to " + returnLevel.dimension().location().getPath())
                                .withStyle(ChatFormatting.GRAY),
                            false
                        );
                        
                        // Update last teleport time
                        if (lastTeleportTime.size() >= MAX_TELEPORT_RECORDS) {
                            lastTeleportTime.entrySet().stream()
                                .min(Map.Entry.comparingByValue())
                                .ifPresent(entry -> lastTeleportTime.remove(entry.getKey()));
                        }
                        lastTeleportTime.put(player.getUUID(), System.currentTimeMillis());
                    } else {
                        throw new RuntimeException("Return teleportation failed");
                    }
                } finally {
                    // Only call postTeleport if preTeleport was successfully called
                    if (preTeleportCalled) {
                        Services.TELEPORT.postTeleport(player, returnLevel);
                    }
                }
            } else {
                LOGGER.error("Return level {} not found for player {}", 
                    returnPos.dimension(), player.getName().getString());
                teleportToWorldSpawn(player);
            }
        }, () -> {
            LOGGER.debug("No return position found for player {}, falling back to world spawn", 
                player.getName().getString());
            teleportToWorldSpawn(player);
        });
    }
    
    /**
     * Check if player can teleport (cooldown)
     */
    private static boolean checkTeleportCooldown(ServerPlayer player) {
        if (player.hasPermissions(2)) {
            return true; // Admins bypass cooldown
        }
        
        Long lastTeleport = lastTeleportTime.get(player.getUUID());
        if (lastTeleport == null) {
            return true;
        }
        
        long cooldownSeconds = BrecherConfig.getTeleportCooldown();
        long cooldownMs = cooldownSeconds * 1000; // Convert seconds to milliseconds
        long timeSinceLast = System.currentTimeMillis() - lastTeleport;
        
        if (timeSinceLast < cooldownMs) {
            long remainingSeconds = (cooldownMs - timeSinceLast) / 1000;
            player.displayClientMessage(
                Component.literal("Teleport cooldown: " + remainingSeconds + " seconds remaining")
                    .withStyle(ChatFormatting.RED),
                true
            );
            return false;
        }
        
        return true;
    }
    
    /**
     * Teleport player to world spawn
     */
    private static void teleportToWorldSpawn(ServerPlayer player) {
        ServerLevel overworld = player.server.overworld();
        BlockPos spawnPos = player.getRespawnPosition();
        
        if (spawnPos == null) {
            spawnPos = overworld.getSharedSpawnPos();
        }
        
        // For emergency returns without saved positions, prefer surface spawns
        BlockPos safeSpawn = null;
        if (BrecherConfig.isPreferSurfaceSpawns()) {
            // Try to find a surface position first
            safeSpawn = findSurfacePosition(overworld, spawnPos);
            LOGGER.debug("Emergency return - found surface spawn: {}", safeSpawn != null);
        }
        
        // If no surface position or surface spawns disabled, use normal safe position finding
        if (safeSpawn == null) {
            safeSpawn = findSafePosition(overworld, spawnPos);
        }
        
        if (safeSpawn == null) {
            LOGGER.warn("World spawn at {} is not safe for player {} - creating emergency platform", 
                spawnPos, player.getName().getString());
            safeSpawn = createEmergencyPlatform(overworld, spawnPos);
        }
        
        final BlockPos finalSafeSpawn = safeSpawn;

        // Track whether preTeleport was called to ensure postTeleport is only called if preTeleport succeeded
        boolean preTeleportCalled = false;

        // Execute teleport
        try {
            Vec3 targetPos = new Vec3(finalSafeSpawn.getX() + 0.5, finalSafeSpawn.getY(), finalSafeSpawn.getZ() + 0.5);
            Services.TELEPORT.preTeleport(player, overworld);
            preTeleportCalled = true;
            boolean success = Services.TELEPORT.teleportPlayer(player, overworld, targetPos,
                                                              player.getYRot(), player.getXRot());
            
            if (success) {
                // Add temporary effects for safety
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 4));
                player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0));
                
                player.displayClientMessage(
                    Component.literal("No return position saved - returned to world spawn")
                        .withStyle(ChatFormatting.YELLOW),
                    false
                );
                
                player.displayClientMessage(
                    Component.literal("Tip: Use /exploration tp to properly save your return position")
                        .withStyle(ChatFormatting.GRAY),
                    false
                );
            }
        } finally {
            // Only call postTeleport if preTeleport was successfully called
            if (preTeleportCalled) {
                Services.TELEPORT.postTeleport(player, overworld);
            }
        }
    }

    /**
     * Find a safe position near the target
     * Note: This method is used for:
     * 1. Finding spawn positions when teleporting TO exploration dimensions
     * 2. Finding safe alternatives when a saved return position is unsafe
     * 3. Emergency spawns when no saved position exists
     * 
     * Surface preference only applies to exploration dimension spawns and emergency returns,
     * NOT to saved return positions (which are handled separately)
     */
    private static BlockPos findSafePosition(ServerLevel level, BlockPos center) {
        // Determine dimension environment once for efficiency
        DimensionEnvironment dimEnv = DimensionEnvironment.getDimensionEnvironment(level);

        // Check if center is safe first
        if (isSafePosition(level, center)) {
            // For exploration dimensions, check if we should prefer surface
            // Only for overworld-like dimensions where surface spawns make sense
            if (BrecherConfig.isPreferSurfaceSpawns() &&
                dimEnv == DimensionEnvironment.OVERWORLD_LIKE &&
                BrecherDimensions.getDimensionManager() != null &&
                BrecherDimensions.getDimensionManager().isExplorationDimension(level.dimension().location())) {

                // If the center is in a cave, try to find a surface position first
                if (!canSeeSky(level, center)) {
                    BlockPos surfacePos = findSurfacePosition(level, center, 16);
                    if (surfacePos != null) {
                        LOGGER.debug("Preferring surface position {} over cave position {}", surfacePos, center);
                        return surfacePos;
                    }
                }
            }
            return center;
        }

        // Progressive search system - use dimension environment for behavior
        boolean isNether = (dimEnv == DimensionEnvironment.NETHER_LIKE);
        boolean preferSurface = BrecherConfig.isPreferSurfaceSpawns() &&
                               (dimEnv == DimensionEnvironment.OVERWORLD_LIKE);

        // Track time budget to prevent blocking main thread too long
        long startTime = System.currentTimeMillis();

        // Phase 1: Try small radius (8 blocks) for nearby positions
        BlockPos nearbyPos = searchInRadius(level, center, 8, preferSurface);
        if (nearbyPos != null) {
            LOGGER.debug("Found safe position within 8 blocks of center");
            return nearbyPos;
        }

        // Check time budget before continuing
        if (System.currentTimeMillis() - startTime > MAX_SEARCH_TIME_MS) {
            LOGGER.warn("Safe position search exceeded {}ms budget at radius 8 - aborting to emergency platform", MAX_SEARCH_TIME_MS);
            return null;
        }

        // Phase 2: Expand to medium radius (16 blocks)
        BlockPos mediumPos = searchInRadius(level, center, 16, preferSurface);
        if (mediumPos != null) {
            LOGGER.debug("Found safe position within 16 blocks of center");
            return mediumPos;
        }

        // Check time budget before continuing
        if (System.currentTimeMillis() - startTime > MAX_SEARCH_TIME_MS) {
            LOGGER.warn("Safe position search exceeded {}ms budget at radius 16 - aborting to emergency platform", MAX_SEARCH_TIME_MS);
            return null;
        }

        // Phase 3: For Nether or if configured, try larger radius (32 blocks)
        if (isNether || BrecherConfig.isExtendedSearchRadius()) {
            BlockPos farPos = searchInRadius(level, center, 32, preferSurface);
            if (farPos != null) {
                LOGGER.debug("Found safe position within 32 blocks of center");
                return farPos;
            }

            // Check time budget before continuing
            if (System.currentTimeMillis() - startTime > MAX_SEARCH_TIME_MS) {
                LOGGER.warn("Safe position search exceeded {}ms budget at radius 32 - aborting to emergency platform", MAX_SEARCH_TIME_MS);
                return null;
            }

            // Phase 4: For Nether only, last attempt with 48 blocks
            if (isNether) {
                BlockPos veryFarPos = searchInRadius(level, center, 48, preferSurface);
                if (veryFarPos != null) {
                    LOGGER.debug("Found safe position within 48 blocks of center");
                    return veryFarPos;
                }
            }
        }

        // Check time budget before final attempt
        if (System.currentTimeMillis() - startTime > MAX_SEARCH_TIME_MS) {
            LOGGER.warn("Safe position search exceeded {}ms budget - aborting to emergency platform", MAX_SEARCH_TIME_MS);
            return null;
        }

        // If surface preference was on, try one more time without it
        if (preferSurface) {
            LOGGER.debug("Retrying search without surface preference");
            BlockPos anyPos = searchInRadius(level, center, 16, false);
            if (anyPos != null) {
                return anyPos;
            }
        }

        return null;
    }
    
    /**
     * Search for a safe position within a specific radius
     */
    private static BlockPos searchInRadius(ServerLevel level, BlockPos center, int maxRadius, boolean preferSurface) {
        // First check if we should try surface positions
        if (preferSurface) {
            BlockPos surfacePos = findSurfacePosition(level, center, maxRadius);
            if (surfacePos != null) {
                return surfacePos;
            }
        }
        
        // Search in expanding circles
        for (int radius = 1; radius <= maxRadius; radius += 2) {
            // Use more angles for larger radii to ensure coverage
            int angleStep = radius <= 8 ? 45 : (radius <= 16 ? 30 : 20);
            
            for (int angle = 0; angle < 360; angle += angleStep) {
                double radians = Math.toRadians(angle);
                int x = center.getX() + (int)(radius * Math.cos(radians));
                int z = center.getZ() + (int)(radius * Math.sin(radians));
                
                // If preferring surface in early phases, try to find actual surface first
                if (preferSurface && radius <= 16) {
                    // Try to find the true surface position at this x,z
                    BlockPos surfaceCheck = findHighestSolidBlock(level, x, z);
                    if (surfaceCheck != null) {
                        BlockPos aboveSurface = surfaceCheck.above();
                        if (isSafePosition(level, aboveSurface) && canSeeSky(level, aboveSurface)) {
                            return aboveSurface;
                        }
                    }
                }
                
                // Fall back to column search (respects vertical limits)
                BlockPos safePos = findSafeInColumn(level, x, center.getY(), z);
                if (safePos != null) {
                    // If preferring surface and this is a cave position, skip it in early phases
                    if (preferSurface && !canSeeSky(level, safePos) && radius <= 16) {
                        continue;
                    }
                    return safePos;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Find safe position in a vertical column
     */
    private static BlockPos findSafeInColumn(ServerLevel level, int x, int centerY, int z) {
        // Skip unloaded chunks to prevent synchronous loading
        if (!level.hasChunk(x >> 4, z >> 4)) {
            return null;
        }

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, centerY, z);
        
        // Search upward first
        for (int y = centerY; y <= Math.min(centerY + 32, level.getMaxBuildHeight() - 2); y++) {
            pos.setY(y);
            if (isSafePosition(level, pos)) {
                return pos.immutable();
            }
        }
        
        // Then search downward
        for (int y = centerY - 1; y >= Math.max(centerY - 32, level.getMinBuildHeight() + 1); y--) {
            pos.setY(y);
            if (isSafePosition(level, pos)) {
                return pos.immutable();
            }
        }
        
        return null;
    }
    
    /**
     * Find a surface position near the target
     */
    private static BlockPos findSurfacePosition(ServerLevel level, BlockPos center) {
        return findSurfacePosition(level, center, BrecherConfig.getTeleportSafetyRadius());
    }
    
    /**
     * Find a surface position near the target with specific radius
     */
    private static BlockPos findSurfacePosition(ServerLevel level, BlockPos center, int searchRadius) {
        
        // First try to find a position directly above/below the center if it's underground/in the air
        BlockPos directSurface = findDirectSurfacePosition(level, center);
        if (directSurface != null && isSafePosition(level, directSurface) && canSeeSky(level, directSurface)) {
            return directSurface;
        }
        
        // Search in expanding circles
        for (int radius = 0; radius <= searchRadius; radius += 2) {
            for (int angle = 0; angle < 360; angle += 30) {
                double radians = Math.toRadians(angle);
                int x = center.getX() + (int)(radius * Math.cos(radians));
                int z = center.getZ() + (int)(radius * Math.sin(radians));
                
                BlockPos surfacePos = findHighestSolidBlock(level, x, z);
                if (surfacePos != null) {
                    BlockPos spawnPos = surfacePos.above();
                    if (isSafePosition(level, spawnPos) && canSeeSky(level, spawnPos)) {
                        return spawnPos;
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * Find the highest solid block at the given x,z coordinates
     */
    private static BlockPos findHighestSolidBlock(ServerLevel level, int x, int z) {
        // Prevent synchronous chunk loading for OUTER search radius
        // Center chunk is pre-loaded by teleport handler, but radius search should skip unloaded
        if (!level.hasChunk(x >> 4, z >> 4)) {
            return null;
        }

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, level.getMaxBuildHeight() - 1, z);
        
        // Start from actual max build height to find true surface (no artificial limit)
        int startY = level.getMaxBuildHeight() - 1;
        
        for (int y = startY; y >= level.getMinBuildHeight(); y--) {
            pos.setY(y);
            BlockState state = level.getBlockState(pos);
            
            if (!state.isSolid() || state.isAir() || state.getBlock() instanceof LeavesBlock) {
                continue;
            }
            
            // Skip dangerous blocks
            if (state.is(Blocks.LAVA) || state.is(Blocks.FIRE) || state.is(Blocks.MAGMA_BLOCK) ||
                state.is(Blocks.CACTUS) || state.is(Blocks.POINTED_DRIPSTONE) || state.is(Blocks.POWDER_SNOW)) {
                continue;
            }
            
            BlockPos above = pos.above();
            if (above.getY() < level.getMaxBuildHeight()) {
                BlockState aboveState = level.getBlockState(above);
                if (isPassableForSpawn(aboveState)) {
                    // Make sure there's enough headroom
                    BlockPos aboveAbove = above.above();
                    if (aboveAbove.getY() < level.getMaxBuildHeight()) {
                        BlockState headState = level.getBlockState(aboveAbove);
                        if (isPassableForSpawn(headState)) {
                            return pos.immutable();
                        }
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * Find a surface position directly above or below the given position
     */
    private static BlockPos findDirectSurfacePosition(ServerLevel level, BlockPos center) {
        // If we're underground, find the actual surface above us
        if (!canSeeSky(level, center)) {
            // Use findHighestSolidBlock to get the true surface from top-down
            BlockPos surface = findHighestSolidBlock(level, center.getX(), center.getZ());
            if (surface != null) {
                BlockPos aboveSurface = surface.above();
                if (isSafePosition(level, aboveSurface) && canSeeSky(level, aboveSurface)) {
                    return aboveSurface;
                }
            }
        }
        
        // If we're in the air, find ground below
        BlockState centerState = level.getBlockState(center.below());
        if (!centerState.isSolid() || centerState.isAir()) {
            BlockPos ground = findHighestSolidBlock(level, center.getX(), center.getZ());
            if (ground != null) {
                BlockPos above = ground.above();
                if (isSafePosition(level, above) && canSeeSky(level, above)) {
                    return above;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Find a safe position in the Nether dimension
     */
    private static BlockPos findNetherSafePosition(ServerLevel level, BlockPos center) {
        final int minY = 5;
        final int maxY = 122;
        final int searchRadius = BrecherConfig.getTeleportSafetyRadius();
        
        int startY = Math.min(Math.max(center.getY(), minY + 10), maxY - 10);
        
        for (int radius = 0; radius <= searchRadius; radius += 2) {
            for (int angle = 0; angle < 360; angle += 30) {
                double radians = Math.toRadians(angle);
                int x = center.getX() + (int)(radius * Math.cos(radians));
                int z = center.getZ() + (int)(radius * Math.sin(radians));
                
                BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, startY, z);
                
                for (int yOffset = 0; yOffset <= 20; yOffset++) {
                    if (startY + yOffset <= maxY) {
                        pos.setY(startY + yOffset);
                        if (isSafePosition(level, pos)) {
                            return pos.immutable();
                        }
                    }
                    
                    if (startY - yOffset >= minY && yOffset > 0) {
                        pos.setY(startY - yOffset);
                        if (isSafePosition(level, pos)) {
                            return pos.immutable();
                        }
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * Find a safe position in the End dimension
     */
    private static BlockPos findEndSafePosition(ServerLevel level) {
        // For exploration End dimensions with gateways, spawn on the main island
        // This is where players would normally spawn after defeating the dragon
        BlockPos mainIslandSpawn = new BlockPos(0, 65, 0);
        
        // First try to find a safe position on the main island
        BlockPos safePos = findSafePosition(level, mainIslandSpawn);
        if (safePos != null) {
            return safePos;
        }
        
        // If no safe position on main island, use the traditional side platform
        BlockPos platformCenter = new BlockPos(100, 50, 0);
        
        if (isSafePosition(level, platformCenter.above())) {
            return platformCenter.above();
        }
        
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                BlockPos checkPos = platformCenter.offset(x, 1, z);
                if (isSafePosition(level, checkPos)) {
                    return checkPos;
                }
            }
        }
        
        createEndPlatform(level, platformCenter);
        return platformCenter.above();
    }
    
    /**
     * Creates the End spawn platform
     */
    private static void createEndPlatform(ServerLevel level, BlockPos center) {
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                BlockPos platformPos = center.offset(x, 0, z);
                level.setBlockAndUpdate(platformPos, Blocks.OBSIDIAN.defaultBlockState());
                
                for (int y = 1; y <= 3; y++) {
                    BlockPos clearPos = platformPos.above(y);
                    if (!level.getBlockState(clearPos).isAir()) {
                        level.setBlockAndUpdate(clearPos, Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }
    }
    
    /**
     * Check if a position can see the sky
     */
    private static boolean canSeeSky(ServerLevel level, BlockPos pos) {
        BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos(pos.getX(), pos.getY() + 1, pos.getZ());
        
        for (int y = pos.getY() + 1; y < level.getMaxBuildHeight(); y++) {
            checkPos.setY(y);
            BlockState state = level.getBlockState(checkPos);
            
            if (state.isSolid() && !state.isAir()) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Check if a position is safe for teleportation
     */
    private static boolean isSafePosition(ServerLevel level, BlockPos pos) {
        if (pos.getY() < level.getMinBuildHeight() + 2 || pos.getY() > level.getMaxBuildHeight() - 2) {
            return false;
        }
        
        if (!level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
            return false;
        }
        
        BlockState ground = level.getBlockState(pos.below());
        BlockState feet = level.getBlockState(pos);
        BlockState head = level.getBlockState(pos.above());
        
        if (!ground.isSolid() || ground.isAir()) {
            return false;
        }
        
        if (pos.getY() <= level.getMinBuildHeight() + 5 && ground.is(Blocks.BEDROCK)) {
            return false;
        }
        
        // Check if feet and head positions are safe (air or passable snow layers)
        if (!isPassableForSpawn(feet) || !isPassableForSpawn(head)) {
            return false;
        }
        
        // Check for dangerous blocks
        if (ground.is(Blocks.LAVA) || ground.is(Blocks.FIRE) || ground.is(Blocks.SOUL_FIRE) ||
            ground.is(Blocks.MAGMA_BLOCK) || ground.is(Blocks.CAMPFIRE) || ground.is(Blocks.SOUL_CAMPFIRE) ||
            ground.is(Blocks.CACTUS) || ground.is(Blocks.SWEET_BERRY_BUSH) ||
            ground.is(Blocks.WITHER_ROSE) || ground.is(Blocks.POINTED_DRIPSTONE) || ground.is(Blocks.POWDER_SNOW)) {
            return false;
        }
        
        // Soul sand and soul soil are fine - they're just slower to walk on

        // Check surrounding blocks for liquids (expanded check for Nether-like dimensions)
        // Uses dimension properties for better modded dimension compatibility
        boolean isNetherLike = DimensionEnvironment.getDimensionEnvironment(level) == DimensionEnvironment.NETHER_LIKE;
        int checkRadius = isNetherLike ? 2 : 1; // Wider check in Nether-like dimensions
        int checkHeight = isNetherLike ? 3 : 1; // Check more vertical levels in Nether-like dimensions
        
        for (int y = -1; y <= checkHeight; y++) {
            for (int x = -checkRadius; x <= checkRadius; x++) {
                for (int z = -checkRadius; z <= checkRadius; z++) {
                    BlockPos checkPos = pos.offset(x, y, z);
                    BlockState state = level.getBlockState(checkPos);
                    if (state.is(Blocks.LAVA) || state.is(Blocks.WATER)) {
                        return false;
                    }
                    // Also check for flowing lava that might not be a source block
                    if (state.getFluidState().is(Fluids.LAVA) || state.getFluidState().is(Fluids.FLOWING_LAVA)) {
                        return false;
                    }
                }
            }
        }
        
        // Check for falling blocks above
        for (int y = 1; y <= 3; y++) {
            BlockState above = level.getBlockState(pos.above(y));
            if (above.is(Blocks.SAND) || above.is(Blocks.GRAVEL) || 
                above.is(Blocks.ANVIL)) {
                return false;
            }
        }
        
        // Additional Nether safety: Check for lava lakes below (reduced from 5 to 2 blocks)
        // Uses ultraWarm property for better modded dimension compatibility
        if (DimensionEnvironment.getDimensionEnvironment(level) == DimensionEnvironment.NETHER_LIKE) {
            // Only check up to 2 blocks below for immediate danger
            for (int y = 1; y <= 2; y++) {
                BlockPos belowPos = pos.below(y);
                if (belowPos.getY() >= level.getMinBuildHeight()) {
                    BlockState below = level.getBlockState(belowPos);
                    if (below.is(Blocks.LAVA) || below.getFluidState().is(Fluids.LAVA)) {
                        // Check if there's a solid platform between player and lava
                        BlockState intermediate = level.getBlockState(pos.below());
                        
                        // Trust player-built platforms (obsidian, stone, etc)
                        if (isPlayerBuiltPlatform(intermediate)) {
                            continue; // This is safe - player built protection
                        }
                        
                        // For natural blocks, ensure they're solid enough
                        if (!intermediate.isSolid() || intermediate.is(Blocks.GLASS) || 
                            intermediate.is(Blocks.ICE) || intermediate.getBlock() instanceof LeavesBlock) {
                            return false; // Not enough protection from lava
                        }
                    }
                }
            }
        }
        
        return true;
    }
    
    /**
     * Check if a block state is passable for spawn (air or thin snow layers)
     */
    private static boolean isPassableForSpawn(BlockState state) {
        if (state.isAir()) {
            return true;
        }
        
        // Snow layers with less than 5 layers are passable
        if (state.is(Blocks.SNOW)) {
            int layers = state.getValue(BlockStateProperties.LAYERS);
            return layers < 5; // Players can spawn in snow up to 4 layers thick
        }
        
        return false;
    }
    
    /**
     * Check if a block is likely player-built (common building materials)
     */
    private static boolean isPlayerBuiltPlatform(BlockState state) {
        return state.is(Blocks.OBSIDIAN) ||
               state.is(Blocks.COBBLESTONE) ||
               state.is(Blocks.STONE) ||
               state.is(Blocks.STONE_BRICKS) ||
               state.is(Blocks.DEEPSLATE) ||
               state.is(Blocks.DEEPSLATE_BRICKS) ||
               state.is(Blocks.BLACKSTONE) ||
               state.is(Blocks.POLISHED_BLACKSTONE) ||
               state.is(Blocks.POLISHED_BLACKSTONE_BRICKS) ||
               state.is(Blocks.NETHER_BRICKS) ||
               state.is(Blocks.RED_NETHER_BRICKS) ||
               state.is(Blocks.QUARTZ_BLOCK) ||
               state.is(Blocks.SMOOTH_QUARTZ) ||
               state.is(Blocks.IRON_BLOCK) ||
               state.is(Blocks.GOLD_BLOCK) ||
               state.is(Blocks.DIAMOND_BLOCK) ||
               state.is(Blocks.NETHERITE_BLOCK) ||
               state.is(Blocks.CRYING_OBSIDIAN) ||
               state.is(Blocks.RESPAWN_ANCHOR);
    }
    
    /**
     * Create emergency platform for teleportation
     */
    private static BlockPos createEmergencyPlatform(ServerLevel level, BlockPos center) {
        if (!BrecherConfig.isCreateEmergencyPlatforms()) {
            LOGGER.warn("Emergency platforms disabled - returning original position despite safety concerns");
            return center;
        }
        
        BlockPos platformPos = new BlockPos(center.getX(), 
            Math.max(level.getMinBuildHeight() + 10, 64), center.getZ());
        
        // Build 3x3 platform
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos pos = platformPos.offset(x, -1, z);
                level.setBlockAndUpdate(pos, Blocks.OBSIDIAN.defaultBlockState());
            }
        }
        
        // Clear space above
        for (int y = 0; y < 3; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos pos = platformPos.offset(x, y, z);
                    if (!level.getBlockState(pos).isAir()) {
                        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }
        
        LOGGER.warn("Created emergency platform at {}", platformPos);
        return platformPos;
    }
    
    /**
     * Force return a player to spawn with safety checks
     * Used as emergency fallback during evacuation
     */
    public static void forceReturnToSpawn(ServerPlayer player) {
        LOGGER.warn("Force returning player {} to spawn (emergency evacuation)", player.getName().getString());
        
        // Clear any exploration dimension tracking
        BrecherSavedData data = BrecherSavedData.get(player.server);
        data.clearReturnPosition(player.getUUID());
        data.clearPlayerDimensionTracking(player.getUUID());
        
        // Clear dimension tracking
        BrecherDimensionManager manager = BrecherDimensions.getDimensionManager();
        if (manager != null) {
            manager.onPlayerLeaveExploration(player);
        }
        
        // Use the existing safe teleport to world spawn method
        // This will prefer surface spawns for emergency returns
        teleportToWorldSpawn(player);
    }
    
    /**
     * Shutdown cleanup
     */
    public static void shutdown() {
        CLEANUP_EXECUTOR.shutdown();
        try {
            if (!CLEANUP_EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
                CLEANUP_EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            CLEANUP_EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }

        lastTeleportTime.clear();

        LOGGER.info("TeleportHandler cleanup complete");
    }
}