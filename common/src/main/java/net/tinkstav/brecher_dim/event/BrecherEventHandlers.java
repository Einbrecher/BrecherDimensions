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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.tinkstav.brecher_dim.BrecherDimensions;
import net.tinkstav.brecher_dim.config.BrecherConfig;
import net.tinkstav.brecher_dim.data.BrecherSavedData;
import net.tinkstav.brecher_dim.dimension.BrecherDimensionManager;
import net.tinkstav.brecher_dim.performance.ChunkManager;
import net.tinkstav.brecher_dim.teleport.TeleportHandler;
import org.slf4j.Logger;

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
            
            // Send welcome message
            player.sendSystemMessage(Component.literal("[Brecher] " + BrecherConfig.getWelcomeMessage()));
        }
    }
    
    /**
     * Handle player leave event
     */
    public static void onPlayerLeave(ServerPlayer player) {
        LOGGER.debug("Player {} left the server", player.getName().getString());
        
        BrecherDimensionManager manager = BrecherDimensions.getDimensionManager();
        if (manager != null) {
            // Record player location before they leave
            manager.recordPlayerLocation(player);
            
            // Update exploration stats
            BrecherSavedData savedData = BrecherSavedData.get(player.getServer());
            if (savedData != null) {
                var stats = savedData.getPlayerStats(player.getUUID());
                if (stats != null) {
                    stats.endVisit();
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
                // Entering exploration
                player.sendSystemMessage(Component.literal("[Brecher] " + BrecherConfig.getWelcomeMessage()));
            }
        }
    }
    
    /**
     * Handle player respawn
     */
    public static void onPlayerRespawn(ServerPlayer player, boolean keepInventory) {
        BrecherDimensionManager manager = BrecherDimensions.getDimensionManager();
        if (manager != null && manager.isExplorationDimension(player.level().dimension().location())) {
            // Force respawn in normal world if configured
            if (BrecherConfig.isPreventBedSpawn()) {
                // This will be handled by the teleport system
                TeleportHandler.returnToNormalDimension(player);
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
            if (BrecherConfig.isPreventBedSpawn()) {
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
        
        // Chunk cleanup every second (20 ticks)
        if (tickCounter % 20 == 0) {
            ChunkManager.performCleanup(server);
        }
        
        // Entity cleanup based on config interval
        if (tickCounter % BrecherConfig.getEntityCleanupInterval() == 0) {
            BrecherDimensionManager manager = BrecherDimensions.getDimensionManager();
            if (manager != null) {
                manager.performEntityCleanup();
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