package net.tinkstav.brecher_dim.teleport;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.TickTask;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraftforge.common.util.ITeleporter;
import net.tinkstav.brecher_dim.Brecher_Dim;
import net.tinkstav.brecher_dim.config.BrecherConfig;
import net.tinkstav.brecher_dim.data.BrecherSavedData;
import net.tinkstav.brecher_dim.data.ReturnPosition;
import net.tinkstav.brecher_dim.util.PlayerSnapshot;
import net.tinkstav.brecher_dim.dimension.BrecherDimensionManager;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Unique;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;

public class TeleportHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_ATTEMPTS = 100;
    private static final Map<UUID, PlayerSnapshot> emergencySnapshots = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastTeleportTime = new ConcurrentHashMap<>();
    
    // Flag to mark authorized teleportations
    public static final Set<UUID> AUTHORIZED_TELEPORTS = ConcurrentHashMap.newKeySet();
    private static final ScheduledExecutorService CLEANUP_EXECUTOR = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "BrecherDim-TeleportCleanup");
        t.setDaemon(true);
        return t;
    });
    private static final long CLEANUP_INTERVAL_MINUTES = 5;
    private static final long DATA_RETENTION_MINUTES = 30;
    private static final int MAX_SNAPSHOTS = 1000; // Prevent unbounded growth
    private static final int MAX_TELEPORT_RECORDS = 1000; // Prevent unbounded growth
    
    static {
        // Schedule periodic cleanup of old data
        CLEANUP_EXECUTOR.scheduleAtFixedRate(() -> {
            try {
                long cutoffTime = System.currentTimeMillis() - (DATA_RETENTION_MINUTES * 60 * 1000);
                
                // Clean up old emergency snapshots
                emergencySnapshots.entrySet().removeIf(entry -> {
                    PlayerSnapshot snapshot = entry.getValue();
                    return snapshot != null && snapshot.timestamp() < cutoffTime;
                });
                
                // Clean up old teleport times
                lastTeleportTime.entrySet().removeIf(entry -> {
                    Long time = entry.getValue();
                    return time != null && time < cutoffTime;
                });
                
                LOGGER.debug("Cleaned up teleport data. Snapshots: {}, Times: {}", 
                    emergencySnapshots.size(), lastTeleportTime.size());
                    
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
        
        // Create emergency snapshot in case something goes wrong
        // Enforce size limit to prevent unbounded growth
        if (emergencySnapshots.size() >= MAX_SNAPSHOTS) {
            // Remove oldest entry
            emergencySnapshots.entrySet().stream()
                .min(Map.Entry.comparingByValue((a, b) -> Long.compare(a.timestamp(), b.timestamp())))
                .ifPresent(entry -> emergencySnapshots.remove(entry.getKey()));
        }
        emergencySnapshots.put(player.getUUID(), PlayerSnapshot.create(player));
        
        // Save return position with rotation
        BrecherSavedData data = BrecherSavedData.get(player.server);
        ResourceLocation currentDim = player.level().dimension().location();
        BlockPos currentPos = player.blockPosition();
        
        BrecherDimensionManager manager = Brecher_Dim.getDimensionManager();
        boolean isFromExploration = manager != null && manager.isExplorationDimension(currentDim);
        
        // Only save return position when teleporting FROM a normal dimension
        // This preserves the original return point when hopping between exploration dimensions
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
            
            // Check if player has a return position saved
            if (!data.getReturnPosition(player.getUUID()).isPresent()) {
                LOGGER.warn("Player {} in exploration dimension without return position - saving emergency position", 
                    player.getName().getString());
                // This shouldn't happen, but save overworld spawn as emergency fallback
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
        
        // Execute teleportation
        try {
            // Use the world spawn as a starting point, but find a surface position
            BlockPos spawnPoint = destination.getSharedSpawnPos();
            BlockPos safePos;
            
            // Special handling for different dimension types
            String dimPath = destination.dimension().location().getPath();
            if (dimPath.contains("the_nether")) {
                // For Nether, don't look for surface - find safe position within dimension bounds
                safePos = findNetherSafePosition(destination, spawnPoint);
            } else if (dimPath.contains("the_end")) {
                // For End, use the obsidian platform area
                safePos = findEndSafePosition(destination);
            } else {
                // For overworld-like dimensions, use surface finding
                safePos = findSafePosition(destination, spawnPoint);
            }
            
            // Ensure we have a safe position
            if (safePos == null) {
                LOGGER.warn("Could not find safe position in {} - creating emergency platform", 
                    destination.dimension().location());
                safePos = createEmergencyPlatform(destination, spawnPoint);
            }
            
            LOGGER.debug("Teleporting player {} to exploration dimension {} at position {}", 
                player.getName().getString(), destination.dimension().location(), safePos);
            
            // Clear certain potion effects that could cause issues
            player.removeEffect(MobEffects.LEVITATION);
            player.removeEffect(MobEffects.BLINDNESS);
            
            // Mark this as an authorized teleport to bypass portal blocking
            AUTHORIZED_TELEPORTS.add(player.getUUID());
            
            // Use changeDimension for cross-dimensional teleportation
            if (!player.level().dimension().equals(destination.dimension())) {
                // Cross-dimensional teleport using ITeleporter with PortalInfo
                Vec3 targetPos = new Vec3(safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5);
                player.changeDimension(destination, new ITeleporter() {
                    @Override
                    public PortalInfo getPortalInfo(Entity entity, ServerLevel destWorld, Function<ServerLevel, PortalInfo> defaultPortalInfo) {
                        return new PortalInfo(targetPos, Vec3.ZERO, player.getYRot(), player.getXRot());
                    }
                });
            } else {
                // Same dimension teleport
                player.teleportTo(
                    destination,
                    safePos.getX() + 0.5,
                    safePos.getY(),
                    safePos.getZ() + 0.5,
                    player.getYRot(),
                    player.getXRot()
                );
            }
            
            // Grant brief invulnerability and effects
            player.setInvulnerable(true);
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 4));
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0));
            
            player.server.tell(new TickTask(player.server.getTickCount() + 100, () -> {
                player.setInvulnerable(false);
                emergencySnapshots.remove(player.getUUID());
            }));
            
            // Send appropriate message based on where we came from
            if (isFromExploration) {
                player.displayClientMessage(
                    Component.literal("Teleported between exploration dimensions!")
                        .withStyle(ChatFormatting.GREEN),
                    false
                );
                
                // Show return position info
                data.getReturnPosition(player.getUUID()).ifPresent(returnPos -> {
                    String dimName = returnPos.dimension().getPath();
                    player.displayClientMessage(
                        Component.literal("Your return point remains in: " + dimName)
                            .withStyle(ChatFormatting.GRAY),
                        false
                    );
                });
            } else {
                // Send normal welcome message
                String welcomeMsg = BrecherConfig.welcomeMessage.get();
                player.displayClientMessage(
                    Component.literal(welcomeMsg).withStyle(ChatFormatting.GREEN),
                    false
                );
            }
            
            // Track in dimension manager
            if (manager != null) {
                manager.onPlayerEnterExploration(player, destination.dimension().location());
            }
            
            // Update last teleport time
            // Enforce size limit to prevent unbounded growth
            if (lastTeleportTime.size() >= MAX_TELEPORT_RECORDS) {
                // Remove oldest entry
                lastTeleportTime.entrySet().stream()
                    .min(Map.Entry.comparingByValue())
                    .ifPresent(entry -> lastTeleportTime.remove(entry.getKey()));
            }
            lastTeleportTime.put(player.getUUID(), System.currentTimeMillis());
        } catch (Exception e) {
            LOGGER.error("Failed to teleport player", e);
            player.displayClientMessage(
                Component.literal("Teleportation failed! Please try again.")
                    .withStyle(ChatFormatting.RED),
                false
            );
            
            // Clean up on failure
            emergencySnapshots.remove(player.getUUID());
            data.clearReturnPosition(player.getUUID());
        } finally {
            // Always remove the authorization flag
            AUTHORIZED_TELEPORTS.remove(player.getUUID());
        }
    }
    
    /**
     * Return a player from exploration dimension
     */
    public static void returnFromExploration(ServerPlayer player) {
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
                if (BrecherConfig.clearInventoryOnReturn.get()) {
                    player.getInventory().clearContent();
                }
                
                // Check if return position is safe
                BlockPos safePos = returnPos.pos();
                if (!isSafePosition(returnLevel, safePos)) {
                    safePos = findSafePosition(returnLevel, safePos);
                    if (safePos == null) {
                        LOGGER.warn("Return position at {} is not safe for player {} - creating emergency platform", 
                            returnPos.pos(), player.getName().getString());
                        safePos = createEmergencyPlatform(returnLevel, returnPos.pos());
                    }
                }
                
                LOGGER.debug("Teleporting player {} to {} at position {}", 
                    player.getName().getString(), returnLevel.dimension().location(), safePos);
                
                // Mark this as an authorized teleport to bypass portal blocking
                AUTHORIZED_TELEPORTS.add(player.getUUID());
                
                try {
                    // Use changeDimension for cross-dimensional teleportation
                    if (!player.level().dimension().equals(returnLevel.dimension())) {
                        // Cross-dimensional teleport using ITeleporter with PortalInfo
                        Vec3 targetPos = new Vec3(safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5);
                        player.changeDimension(returnLevel, new ITeleporter() {
                            @Override
                            public PortalInfo getPortalInfo(Entity entity, ServerLevel destWorld, Function<ServerLevel, PortalInfo> defaultPortalInfo) {
                                return new PortalInfo(targetPos, Vec3.ZERO, returnPos.yRot(), returnPos.xRot());
                            }
                        });
                    } else {
                        // Same dimension teleport
                        player.teleportTo(
                            returnLevel,
                            safePos.getX() + 0.5,
                            safePos.getY(),
                            safePos.getZ() + 0.5,
                            returnPos.yRot(),
                            returnPos.xRot()
                        );
                    }
                    
                    // Clear return position
                    data.clearReturnPosition(player.getUUID());
                    
                    // Clear dimension tracking after successful evacuation
                    data.clearPlayerDimensionTracking(player.getUUID());
                
                // Track leaving exploration
                BrecherDimensionManager manager = Brecher_Dim.getDimensionManager();
                if (manager != null) {
                    manager.onPlayerLeaveExploration(player);
                }
                
                // Send return message with context
                String returnMsg = BrecherConfig.returnMessage.get();
                player.displayClientMessage(
                    Component.literal(returnMsg).withStyle(ChatFormatting.GREEN),
                    false
                );
                
                // Add dimension info
                player.displayClientMessage(
                    Component.literal("Returned to " + returnLevel.dimension().location().getPath())
                        .withStyle(ChatFormatting.GRAY),
                    false
                );
                
                    // Update last teleport time
                    // Enforce size limit to prevent unbounded growth
                    if (lastTeleportTime.size() >= MAX_TELEPORT_RECORDS) {
                        // Remove oldest entry
                        lastTeleportTime.entrySet().stream()
                            .min(Map.Entry.comparingByValue())
                            .ifPresent(entry -> lastTeleportTime.remove(entry.getKey()));
                    }
                    lastTeleportTime.put(player.getUUID(), System.currentTimeMillis());
                } finally {
                    // Always remove the authorization flag
                    AUTHORIZED_TELEPORTS.remove(player.getUUID());
                }
            } else {
                LOGGER.error("Return level {} not found for player {}", 
                    returnPos.dimension(), player.getName().getString());
                teleportToWorldSpawn(player);
            }
        }, () -> {
            LOGGER.debug("No return position found for player {}, falling back to world spawn", 
                player.getName().getString());
            // Fallback to world spawn
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
        
        long cooldownTicks = BrecherConfig.teleportCooldown.get();
        long cooldownMs = cooldownTicks * 50; // Convert ticks to milliseconds
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
        
        // Try player's respawn position first, then world spawn
        if (spawnPos == null) {
            spawnPos = overworld.getSharedSpawnPos();
        }
        
        // Ensure spawn is safe
        BlockPos safeSpawn = findSafePosition(overworld, spawnPos);
        
        // If no safe position found, create emergency platform
        if (safeSpawn == null) {
            LOGGER.warn("World spawn at {} is not safe for player {} - creating emergency platform", 
                spawnPos, player.getName().getString());
            safeSpawn = createEmergencyPlatform(overworld, spawnPos);
        }
        
        // Make it final for lambda usage
        final BlockPos finalSafeSpawn = safeSpawn;
        
        // Execute teleport immediately if already on server thread
        if (player.server.isSameThread()) {
            // Mark as authorized
            AUTHORIZED_TELEPORTS.add(player.getUUID());
            try {
                // Use changeDimension for cross-dimensional teleportation
                if (!player.level().dimension().equals(overworld.dimension())) {
                    Vec3 targetPos = new Vec3(finalSafeSpawn.getX() + 0.5, finalSafeSpawn.getY(), finalSafeSpawn.getZ() + 0.5);
                    player.changeDimension(overworld, new ITeleporter() {
                        @Override
                        public PortalInfo getPortalInfo(Entity entity, ServerLevel destWorld, Function<ServerLevel, PortalInfo> defaultPortalInfo) {
                            return new PortalInfo(targetPos, Vec3.ZERO, player.getYRot(), player.getXRot());
                        }
                    });
                } else {
                player.teleportTo(
                    overworld,
                    finalSafeSpawn.getX() + 0.5,
                    finalSafeSpawn.getY(),
                    finalSafeSpawn.getZ() + 0.5,
                    player.getYRot(),
                    player.getXRot()
                );
            }
            
            player.displayClientMessage(
                Component.literal("No return position saved - returned to world spawn")
                    .withStyle(ChatFormatting.YELLOW),
                false
            );
            
            player.displayClientMessage(
                Component.literal("Tip: Use /brecher tp to properly save your return position")
                    .withStyle(ChatFormatting.GRAY),
                false
            );
            } finally {
                AUTHORIZED_TELEPORTS.remove(player.getUUID());
            }
        } else {
            player.server.execute(() -> {
                // Mark as authorized
                AUTHORIZED_TELEPORTS.add(player.getUUID());
                try {
                    // Use changeDimension for cross-dimensional teleportation
                    if (!player.level().dimension().equals(overworld.dimension())) {
                        Vec3 targetPos = new Vec3(finalSafeSpawn.getX() + 0.5, finalSafeSpawn.getY(), finalSafeSpawn.getZ() + 0.5);
                        player.changeDimension(overworld, new ITeleporter() {
                            @Override
                            public PortalInfo getPortalInfo(Entity entity, ServerLevel destWorld, Function<ServerLevel, PortalInfo> defaultPortalInfo) {
                                return new PortalInfo(targetPos, Vec3.ZERO, player.getYRot(), player.getXRot());
                            }
                        });
                    } else {
                    player.teleportTo(
                        overworld,
                        finalSafeSpawn.getX() + 0.5,
                        finalSafeSpawn.getY(),
                        finalSafeSpawn.getZ() + 0.5,
                        player.getYRot(),
                        player.getXRot()
                    );
                }
                
                player.displayClientMessage(
                    Component.literal("No return position saved - returned to world spawn")
                        .withStyle(ChatFormatting.YELLOW),
                    false
                );
                
                player.displayClientMessage(
                    Component.literal("Tip: Use /brecher tp to properly save your return position")
                        .withStyle(ChatFormatting.GRAY),
                    false
                );
                } finally {
                    AUTHORIZED_TELEPORTS.remove(player.getUUID());
                }
            });
        }
    }
    
    /**
     * Find a safe position near the target, preferring surface locations
     */
    private static BlockPos findSafePosition(ServerLevel level, BlockPos center) {
        // First try to find a surface position
        BlockPos surfacePos = findSurfacePosition(level, center);
        if (surfacePos != null) {
            return surfacePos;
        }
        
        // If no surface position found, fall back to the original logic
        // First check if center is safe
        if (isSafePosition(level, center)) {
            return center;
        }
        
        // Search in expanding circles
        final int searchRadius = BrecherConfig.teleportSafetyRadius.get();
        for (int radius = 1; radius <= searchRadius; radius += 2) {
            for (int angle = 0; angle < 360; angle += 45) {
                double radians = Math.toRadians(angle);
                int x = center.getX() + (int)(radius * Math.cos(radians));
                int z = center.getZ() + (int)(radius * Math.sin(radians));
                
                // Check vertical column
                BlockPos safePos = findSafeInColumn(level, x, center.getY(), z);
                if (safePos != null) {
                    return safePos;
                }
            }
        }
        
        // Return null if no safe position found
        return null;
    }
    
    /**
     * Find safe position in a vertical column
     */
    private static BlockPos findSafeInColumn(ServerLevel level, int x, int centerY, int z) {
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
    @Unique
    private static BlockPos findSurfacePosition(ServerLevel level, BlockPos center) {
        // Search in expanding circles for surface positions
        final int searchRadius = BrecherConfig.teleportSafetyRadius.get();
        for (int radius = 0; radius <= searchRadius; radius += 2) {
            for (int angle = 0; angle < 360; angle += 30) {
                double radians = Math.toRadians(angle);
                int x = center.getX() + (int)(radius * Math.cos(radians));
                int z = center.getZ() + (int)(radius * Math.sin(radians));
                
                // Find the highest solid block (surface)
                BlockPos surfacePos = findHighestSolidBlock(level, x, z);
                if (surfacePos != null) {
                    // Check if the position above is safe for spawning
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
    @Unique
    private static BlockPos findHighestSolidBlock(ServerLevel level, int x, int z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, level.getMaxBuildHeight() - 1, z);
        
        // Start from the top and work down
        for (int y = level.getMaxBuildHeight() - 1; y >= level.getMinBuildHeight(); y--) {
            pos.setY(y);
            BlockState state = level.getBlockState(pos);
            
            // Skip non-solid blocks
            if (!state.isSolid() || state.isAir()) {
                continue;
            }
            
            // Check if this could be a surface block
            BlockPos above = pos.above();
            if (above.getY() < level.getMaxBuildHeight()) {
                BlockState aboveState = level.getBlockState(above);
                if (aboveState.isAir() || !aboveState.isSolid()) {
                    // Found a solid block with air above - this is likely the surface
                    return pos.immutable();
                }
            }
        }
        
        return null;
    }
    
    /**
     * Find a safe position in the Nether dimension
     * Searches within the dimension bounds (Y 0-127) instead of looking for surface
     */
    @Unique
    private static BlockPos findNetherSafePosition(ServerLevel level, BlockPos center) {
        // Nether has bedrock floor at Y=0-4 and ceiling at Y=123-127
        // Safe zone is roughly Y=5-122
        final int minY = 5;
        final int maxY = 122;
        final int searchRadius = BrecherConfig.teleportSafetyRadius.get();
        
        // Start searching from a reasonable height in the nether
        int startY = Math.min(Math.max(center.getY(), minY + 10), maxY - 10);
        
        // Search in expanding circles
        for (int radius = 0; radius <= searchRadius; radius += 2) {
            for (int angle = 0; angle < 360; angle += 30) {
                double radians = Math.toRadians(angle);
                int x = center.getX() + (int)(radius * Math.cos(radians));
                int z = center.getZ() + (int)(radius * Math.sin(radians));
                
                // Search vertically within nether bounds
                BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, startY, z);
                
                // Search around the start Y first
                for (int yOffset = 0; yOffset <= 20; yOffset++) {
                    // Check above
                    if (startY + yOffset <= maxY) {
                        pos.setY(startY + yOffset);
                        if (isSafePosition(level, pos)) {
                            return pos.immutable();
                        }
                    }
                    
                    // Check below
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
     * Uses the obsidian platform spawn area
     */
    @Unique
    private static BlockPos findEndSafePosition(ServerLevel level) {
        // The End spawn platform is at approximately 100, 50, 0
        BlockPos platformCenter = new BlockPos(100, 50, 0);
        
        // First check if the platform exists and is safe
        if (isSafePosition(level, platformCenter.above())) {
            return platformCenter.above();
        }
        
        // Search around the platform area
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                BlockPos checkPos = platformCenter.offset(x, 1, z);
                if (isSafePosition(level, checkPos)) {
                    return checkPos;
                }
            }
        }
        
        // If no safe position on platform, create the platform
        createEndPlatform(level, platformCenter);
        return platformCenter.above();
    }
    
    /**
     * Creates the End spawn platform
     */
    @Unique
    private static void createEndPlatform(ServerLevel level, BlockPos center) {
        // Create a 5x5 obsidian platform
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                BlockPos platformPos = center.offset(x, 0, z);
                level.setBlockAndUpdate(platformPos, Blocks.OBSIDIAN.defaultBlockState());
                
                // Clear space above
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
    @Unique
    private static boolean canSeeSky(ServerLevel level, BlockPos pos) {
        // Check if there are no solid blocks above this position
        BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos(pos.getX(), pos.getY() + 1, pos.getZ());
        
        for (int y = pos.getY() + 1; y < level.getMaxBuildHeight(); y++) {
            checkPos.setY(y);
            BlockState state = level.getBlockState(checkPos);
            
            // If we hit a solid block, we can't see the sky
            if (state.isSolid() && !state.isAir()) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Check if a position is safe for teleportation with enhanced safety checks
     */
    private static boolean isSafePosition(ServerLevel level, BlockPos pos) {
        // Check if position is within world bounds
        if (pos.getY() < level.getMinBuildHeight() + 2 || pos.getY() > level.getMaxBuildHeight() - 2) {
            return false;
        }
        
        // Check if chunk is loaded
        if (!level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
            return false;
        }
        
        BlockState ground = level.getBlockState(pos.below());
        BlockState feet = level.getBlockState(pos);
        BlockState head = level.getBlockState(pos.above());
        
        // Ground must be solid and safe
        if (!ground.isSolid() || ground.isAir()) {
            return false;
        }
        
        // Check for void spawn (bedrock at Y=0 in overworld-like dimensions)
        if (pos.getY() <= level.getMinBuildHeight() + 5 && ground.is(Blocks.BEDROCK)) {
            return false;
        }
        
        // Feet and head must be passable
        if (!feet.isAir() || !head.isAir()) {
            return false;
        }
        
        // Extended dangerous blocks check
        if (ground.is(Blocks.LAVA) || ground.is(Blocks.FIRE) || ground.is(Blocks.MAGMA_BLOCK) ||
            ground.is(Blocks.SOUL_SAND) || ground.is(Blocks.SOUL_FIRE) || 
            ground.is(Blocks.CAMPFIRE) || ground.is(Blocks.SOUL_CAMPFIRE) ||
            ground.is(Blocks.CACTUS) || ground.is(Blocks.SWEET_BERRY_BUSH) ||
            ground.is(Blocks.WITHER_ROSE) || ground.is(Blocks.POINTED_DRIPSTONE)) {
            return false;
        }
        
        // Check surrounding blocks for liquids (don't spawn player in water/lava)
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos checkPos = pos.offset(x, 0, z);
                BlockState state = level.getBlockState(checkPos);
                if (state.is(Blocks.LAVA) || state.is(Blocks.WATER)) {
                    return false;
                }
            }
        }
        
        // Additional safety: check for falling blocks above
        for (int y = 1; y <= 3; y++) {
            BlockState above = level.getBlockState(pos.above(y));
            if (above.is(Blocks.SAND) || above.is(Blocks.GRAVEL) || 
                above.is(Blocks.ANVIL)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Create emergency platform for teleportation
     */
    private static BlockPos createEmergencyPlatform(ServerLevel level, BlockPos center) {
        // Check if emergency platforms are enabled
        if (!BrecherConfig.createEmergencyPlatforms.get()) {
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
     * Shutdown cleanup - call when server is stopping
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
        
        // Clear all data
        emergencySnapshots.clear();
        lastTeleportTime.clear();
        
        LOGGER.info("TeleportHandler cleanup complete");
    }
}