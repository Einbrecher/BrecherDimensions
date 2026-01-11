/*
 * Brecher's Dimensions - Temporary resettable dimensions for exploration
 * Copyright (C) 2025 Einbrecher. All rights reserved.
 */

package net.tinkstav.brecher_dim.event;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.tinkstav.brecher_dim.BrecherDimensions;
import net.tinkstav.brecher_dim.config.BrecherConfig;
import net.tinkstav.brecher_dim.data.BrecherSavedData;
import net.tinkstav.brecher_dim.dimension.BrecherDimensionManager;
import net.tinkstav.brecher_dim.performance.ChunkManager;
import net.tinkstav.brecher_dim.teleport.TeleportHandler;
import net.tinkstav.brecher_dim.util.InventoryKeeper;
import net.tinkstav.brecher_dim.compat.CorpseModCompat;
import net.tinkstav.brecher_dim.dimension.SimpleSeedManager;
import net.tinkstav.brecher_dim.generation.ChunkPreGenerator;
import net.tinkstav.brecher_dim.util.DimensionEnvironment;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import java.time.Duration;

/**
 * Common event handlers for Brecher's Dimensions.
 * Called by platform-specific event systems.
 */
public class BrecherEventHandlers {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    private static int tickCounter = 0;
    
    /**
     * Handle player join event
     */
    public static void onPlayerJoin(ServerPlayer player) {
        LOGGER.debug("Player {} joined the server", player.getName().getString());
        
        // Check if player is in exploration dimension
        BrecherDimensionManager manager = BrecherDimensions.getDimensionManager();
        if (manager != null && manager.isExplorationDimension(player.level().dimension().location())) {
            // Sync dimension info to the player
            // Platform-specific networking will be handled elsewhere
            
            // Welcome message is sent by TeleportHandler when entering exploration dimensions
        }
    }
    
    /**
     * Handle player leave event
     */
    public static void onPlayerLeave(ServerPlayer player) {
        LOGGER.debug("Player {} left the server", player.getName().getString());

        // Clear any temporary invulnerability to prevent "god mode" persistence bug.
        // If player disconnects during the 5-second post-teleport invulnerability window,
        // their invulnerable state would be saved to disk without this cleanup, causing
        // them to be permanently invincible on reconnect.
        if (player.isInvulnerable()) {
            player.setInvulnerable(false);
            LOGGER.debug("Cleared invulnerability for disconnecting player {}",
                player.getName().getString());
        }

        BrecherDimensionManager manager = BrecherDimensions.getDimensionManager();
        if (manager != null) {
            // Record player location before they leave
            manager.recordPlayerLocation(player);
            
            // Update exploration stats
            BrecherSavedData savedData = BrecherSavedData.get(player.getServer());
            if (savedData != null) {
                var stats = savedData.getPlayerStats(player.getUUID());
                if (stats.isPresent()) {
                    stats.get().endVisit();
                }
            }
        }
    }
    
    /**
     * Handle player dimension change
     */
    public static void onPlayerChangeDimension(ServerPlayer player, Level from, Level to) {
        LOGGER.debug("Player {} changed dimension from {} to {}", 
            player.getName().getString(), 
            from.dimension().location(), 
            to.dimension().location());
        
        BrecherDimensionManager manager = BrecherDimensions.getDimensionManager();
        if (manager != null) {
            boolean fromExploration = manager.isExplorationDimension(from.dimension().location());
            boolean toExploration = manager.isExplorationDimension(to.dimension().location());
            
            if (fromExploration && !toExploration) {
                // Returning from exploration
                player.sendSystemMessage(Component.literal("[Brecher] " + BrecherConfig.getReturnMessage()));
                
                // Clear inventory if configured
                if (BrecherConfig.isClearInventoryOnReturn()) {
                    player.getInventory().clearContent();
                    player.sendSystemMessage(Component.literal("[Brecher] Your inventory has been cleared."));
                }
            } else if (!fromExploration && toExploration) {
                // Entering exploration dimension (via portal or other means)
                // Show seed lock information for players entering via portals  
                // (TeleportHandler shows this for command-based teleports)
                Duration timeUntilReset = SimpleSeedManager.getTimeUntilSeedReset();
                if (timeUntilReset != null) {
                    String timeRemaining = SimpleSeedManager.formatDuration(timeUntilReset);
                    player.sendSystemMessage(Component.literal("⏱ Seed lock expires in: " + timeRemaining));
                    player.sendSystemMessage(Component.literal("This dimension will reset with a new seed after the next server restart once the lock expires."));
                } else {
                    // Random seed strategy - dimension resets on every restart
                    player.sendSystemMessage(Component.literal("⚠ This dimension will reset with a new seed after the next server restart."));
                }
            }
        }
    }
    
    /**
     * Handle player death
     */
    public static void onPlayerDeath(ServerPlayer player) {
        BrecherDimensionManager manager = BrecherDimensions.getDimensionManager();
        if (manager != null && manager.isExplorationDimension(player.level().dimension().location())) {
            // Log death in exploration dimension for debugging
            BrecherDimensions.LOGGER.debug("Player {} died in exploration dimension {}", 
                player.getName().getString(), player.level().dimension().location());
            
            // Save inventory if keepInventoryInExploration is enabled
            if (BrecherConfig.isKeepInventoryInExploration() && 
                !player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY) &&
                CorpseModCompat.shouldHandleInventory(player)) {
                
                InventoryKeeper.saveInventory(player);
                
                // Clear inventory immediately to prevent corpse mods from capturing items
                // when deferToCorpseMods is false
                player.getInventory().clearContent();
                player.setExperienceLevels(0);
                player.experienceProgress = 0;
                
                BrecherDimensions.LOGGER.debug("Saved and cleared inventory for player {} dying in exploration dimension", 
                    player.getName().getString());
            }
        }
    }
    
    /**
     * Handle player respawn
     */
    public static void onPlayerRespawn(ServerPlayer player, boolean keepInventory) {
        // Check if we should restore inventory from our saved data
        if (BrecherConfig.isKeepInventoryInExploration() && !keepInventory) {
            // Try to restore inventory if player died in exploration dimension
            if (InventoryKeeper.restoreInventory(player)) {
                BrecherDimensions.LOGGER.debug("Restored inventory for player {} after respawn", 
                    player.getName().getString());
                player.displayClientMessage(
                    Component.literal("Your inventory has been restored from the exploration dimension.").withStyle(style -> style.withColor(0x55FF55)),
                    true
                );
            }
        }
        
        BrecherDimensionManager manager = BrecherDimensions.getDimensionManager();
        if (manager != null && manager.isExplorationDimension(player.level().dimension().location())) {
            // Force respawn in normal world if configured
            if (BrecherConfig.isPreventExplorationSpawnSetting()) {
                // This will be handled by the teleport system
                TeleportHandler.returnFromExploration(player);
            }
        }
    }
    
    /**
     * Handle player sleep attempt
     */
    public static InteractionResult onPlayerSleep(Player player, BlockPos pos) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        
        BrecherDimensionManager manager = BrecherDimensions.getDimensionManager();
        if (manager != null && manager.isExplorationDimension(player.level().dimension().location())) {
            if (BrecherConfig.isPreventExplorationSpawnSetting()) {
                // Allow beds to explode in Nether-like dimensions (for mining)
                // Uses property-based detection for modded dimension compatibility
                if (player.level() instanceof ServerLevel serverLevel &&
                    DimensionEnvironment.getDimensionEnvironment(serverLevel) == DimensionEnvironment.NETHER_LIKE) {
                    // Allow the interaction - bed will explode naturally
                    return InteractionResult.PASS;
                }

                // Block spawn setting in other dimensions
                player.displayClientMessage(
                    Component.literal("You cannot set your spawn in exploration dimensions!"),
                    true
                );
                return InteractionResult.FAIL;
            }
        }
        
        return InteractionResult.PASS;
    }
    
    /**
     * Handle respawn anchor interaction
     */
    public static InteractionResult onRespawnAnchorUse(Player player, BlockPos pos) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        
        BrecherDimensionManager manager = BrecherDimensions.getDimensionManager();
        if (manager != null && manager.isExplorationDimension(player.level().dimension().location())) {
            if (BrecherConfig.isPreventExplorationSpawnSetting()) {
                // Allow respawn anchors to explode in non-Nether dimensions (Overworld/End-like)
                // Uses property-based detection for modded dimension compatibility
                if (player.level() instanceof ServerLevel serverLevel) {
                    DimensionEnvironment dimEnv = DimensionEnvironment.getDimensionEnvironment(serverLevel);
                    if (dimEnv != DimensionEnvironment.NETHER_LIKE) {
                        // Allow the interaction - respawn anchor will explode naturally
                        return InteractionResult.PASS;
                    }
                }

                // Block respawn anchor in Nether-like dimensions (where it would actually set spawn)
                player.displayClientMessage(
                    Component.literal("You cannot set your spawn in exploration dimensions!"),
                    true
                );
                return InteractionResult.FAIL;
            }
        }

        return InteractionResult.PASS;
    }
    
    /**
     * Handle ender chest interaction
     */
    public static InteractionResult onEnderChestUse(Player player, Level level, BlockPos pos, InteractionHand hand) {
        if (level.isClientSide) {
            return InteractionResult.PASS;
        }
        
        BlockState state = level.getBlockState(pos);
        if (state.is(Blocks.ENDER_CHEST)) {
            BrecherDimensionManager manager = BrecherDimensions.getDimensionManager();
            if (manager != null && manager.isExplorationDimension(level.dimension().location())) {
                if (BrecherConfig.isDisableEnderChests()) {
                    player.displayClientMessage(
                        Component.literal("Ender chests are disabled in exploration dimensions!"), 
                        true
                    );
                    return InteractionResult.FAIL;
                }
            }
        }
        
        return InteractionResult.PASS;
    }
    
    /**
     * Handle entity portal use
     */
    public static boolean onEntityPortalUse(Entity entity, Level level) {
        BrecherDimensionManager manager = BrecherDimensions.getDimensionManager();
        if (manager != null && manager.isExplorationDimension(level.dimension().location())) {
            if (BrecherConfig.isDisableModdedPortals()) {
                // Check if this is a modded portal (not vanilla nether/end)
                // This is a simplified check - actual implementation would be more comprehensive
                if (entity instanceof Player player) {
                    player.displayClientMessage(
                        Component.literal("Portal travel is restricted in exploration dimensions!"), 
                        true
                    );
                }
                return false; // Cancel the teleport
            }
        }
        
        return true; // Allow the teleport
    }
    
    /**
     * Handle server tick
     */
    public static void onServerTick(MinecraftServer server) {
        tickCounter++;
        
        // Chunk cleanup based on configured interval to avoid performance issues
        // More frequent cleanup can cause server hangs with many loaded chunks
        if (tickCounter % BrecherConfig.getChunkCleanupInterval() == 0) {
            ChunkManager.performCleanup(server);
        }
        
        // Process chunk pre-generation tasks
        ChunkPreGenerator.tick(server);
        
        // Entity cleanup based on config interval
        if (tickCounter % BrecherConfig.getEntityCleanupInterval() == 0) {
            BrecherDimensionManager manager = BrecherDimensions.getDimensionManager();
            if (manager != null) {
                manager.performEntityCleanup();
            }
        }
        
        // Cleanup old saved inventories every hour (72000 ticks)
        if (tickCounter % 72000 == 0) {
            InventoryKeeper.cleanupOldInventories();
            int remaining = InventoryKeeper.getSavedInventoryCount();
            if (remaining > 0) {
                BrecherDimensions.LOGGER.debug("Cleaned up old saved inventories, {} remaining", remaining);
            }
        }
        
        // Reset tick counter to prevent overflow
        if (tickCounter > 1000000) {
            tickCounter = 0;
        }
    }
    
    /**
     * Handle chunk load
     */
    public static void onChunkLoad(Level level, int chunkX, int chunkZ) {
        BrecherDimensionManager manager = BrecherDimensions.getDimensionManager();
        if (manager != null && manager.isExplorationDimension(level.dimension().location())) {
            ChunkManager.onChunkLoad(level, chunkX, chunkZ);
        }
    }
    
    /**
     * Handle chunk unload
     */
    public static void onChunkUnload(Level level, int chunkX, int chunkZ) {
        BrecherDimensionManager manager = BrecherDimensions.getDimensionManager();
        if (manager != null && manager.isExplorationDimension(level.dimension().location())) {
            ChunkManager.onChunkUnload(level, chunkX, chunkZ);
        }
    }
}