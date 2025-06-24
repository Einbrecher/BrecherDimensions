package net.tinkstav.brecher_dim.event;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.ChunkWatchEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.tinkstav.brecher_dim.Brecher_Dim;
import net.tinkstav.brecher_dim.config.BrecherConfig;
import net.tinkstav.brecher_dim.data.BrecherSavedData;
import net.tinkstav.brecher_dim.dimension.BrecherDimensionManager;
import net.tinkstav.brecher_dim.teleport.TeleportHandler;
import net.tinkstav.brecher_dim.network.BrecherNetworking;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import java.util.concurrent.TimeUnit;
import java.util.List;

@Mod.EventBusSubscriber(modid = Brecher_Dim.MODID)
public class BrecherEventHandlers {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    @SubscribeEvent
    public static void onPortalTravel(EntityTravelToDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        
        // Check if this is an authorized Brecher teleport - allow it
        if (TeleportHandler.AUTHORIZED_TELEPORTS.contains(player.getUUID())) {
            return;
        }
        
        ResourceLocation currentDim = player.level().dimension().location();
        ResourceLocation targetDim = event.getDimension().location();
        
        BrecherDimensionManager manager = Brecher_Dim.getDimensionManager();
        if (manager == null) return;
        
        // Cache dimension checks to avoid repeated lookups
        boolean isCurrentExploration = manager.isExplorationDimension(currentDim);
        boolean isTargetExploration = manager.isExplorationDimension(targetDim);
        
        // Check if we're in an exploration dimension
        if (isCurrentExploration) {
            // Determine if this is a vanilla portal
            PortalInfo portalInfo = detectPortalType(player);
            
            // Check if modded portals are disabled
            if (portalInfo.isModded() && BrecherConfig.disableModdedPortals.get()) {
                event.setCanceled(true);
                player.displayClientMessage(
                    Component.literal("Modded portals are disabled in exploration dimensions!")
                        .withStyle(ChatFormatting.RED),
                    true
                );
                return;
            }
            
            // Allow vanilla nether/end portals within exploration dimensions
            if (portalInfo.isVanilla() && isTargetExploration) {
                // Allow travel between exploration dimensions
                return;
            }
            
            // Prevent travel to non-exploration dimensions
            if (!isTargetExploration) {
                event.setCanceled(true);
                player.displayClientMessage(
                    Component.literal("Use /explore return to leave the exploration dimension!")
                        .withStyle(ChatFormatting.RED),
                    true
                );
            }
        }
    }
    
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        
        // Sync all exploration dimensions to the joining player
        BrecherDimensionManager manager = Brecher_Dim.getDimensionManager();
        if (manager != null) {
            BrecherNetworking.syncAllDimensionsToPlayer(
                player, 
                manager.getExplorationDimensions()
            );
        }
        
        // Get saved data for dimension tracking
        BrecherSavedData data = BrecherSavedData.get(player.server);
        
        // Check if player was in exploration dimension
        ResourceLocation currentDim = player.level().dimension().location();
        
        // Update player's current dimension
        data.updatePlayerDimension(player.getUUID(), currentDim);
        
        if (manager != null && manager.isExplorationDimension(currentDim)) {
            // IMMEDIATELY evacuate player who logged into exploration dimension after restart
            LOGGER.warn("Player {} logged into exploration dimension {} after restart - evacuating", 
                player.getName().getString(), currentDim);
            
            // Schedule evacuation after a brief delay to ensure client is ready
            player.server.tell(new TickTask(
                player.server.getTickCount() + 20, // 1 second delay for client sync
                () -> {
                    // Show immediate warning
                    player.displayClientMessage(
                        Component.literal("⚠ EXPLORATION DIMENSION DETECTED AFTER RESTART ⚠")
                            .withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                        false
                    );
                    
                    player.displayClientMessage(
                        Component.literal("Evacuating you to safety...")
                            .withStyle(ChatFormatting.YELLOW),
                        false
                    );
                    
                    // Perform evacuation
                    try {
                        TeleportHandler.returnFromExploration(player);
                        
                        player.displayClientMessage(
                            Component.literal("You were evacuated from the exploration dimension due to server restart.")
                                .withStyle(ChatFormatting.GREEN),
                            false
                        );
                        
                        player.displayClientMessage(
                            Component.literal("Your items and position have been preserved.")
                                .withStyle(ChatFormatting.GRAY),
                            false
                        );
                    } catch (Exception e) {
                        LOGGER.error("Failed to evacuate player {} on login", player.getName().getString(), e);
                        
                        // Emergency fallback - force teleport to overworld spawn
                        BlockPos spawnPos = player.server.overworld().getSharedSpawnPos();
                        player.teleportTo(player.server.overworld(), 
                            spawnPos.getX() + 0.5, 
                            spawnPos.getY(), 
                            spawnPos.getZ() + 0.5, 
                            0, 0);
                        
                        player.displayClientMessage(
                            Component.literal("Emergency evacuation to world spawn completed.")
                                .withStyle(ChatFormatting.YELLOW),
                            false
                        );
                    }
                }
            ));
        }
    }
    
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        
        BrecherDimensionManager manager = Brecher_Dim.getDimensionManager();
        if (manager != null && manager.isExplorationDimension(player.level().dimension().location())) {
            // Force respawn in overworld
            player.setRespawnPosition(
                Level.OVERWORLD,
                player.server.overworld().getSharedSpawnPos(),
                0.0F,
                true,
                false
            );
            
            // Send helpful message
            player.sendSystemMessage(
                Component.literal("You died in the exploration dimension! ")
                    .append(Component.literal("Respawning in the overworld.")
                        .withStyle(ChatFormatting.YELLOW))
            );
        }
    }
    
    @SubscribeEvent
    public static void onBedUse(PlayerSleepInBedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        
        if (BrecherConfig.preventBedSpawn.get()) {
            BrecherDimensionManager manager = Brecher_Dim.getDimensionManager();
            if (manager != null && manager.isExplorationDimension(player.level().dimension().location())) {
                event.setResult(Player.BedSleepingProblem.NOT_POSSIBLE_HERE);
                player.displayClientMessage(
                    Component.literal("You cannot set spawn in exploration dimensions!")
                        .withStyle(ChatFormatting.RED),
                    true
                );
            }
        }
    }
    
    @SubscribeEvent
    public static void onEnderChestOpen(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        
        if (BrecherConfig.disableEnderChests.get()) {
            BrecherDimensionManager manager = Brecher_Dim.getDimensionManager();
            if (manager != null && manager.isExplorationDimension(player.level().dimension().location())) {
                if (event.getLevel().getBlockState(event.getPos()).is(Blocks.ENDER_CHEST)) {
                    event.setCanceled(true);
                    player.displayClientMessage(
                        Component.literal("Ender chests are disabled in exploration dimensions!")
                            .withStyle(ChatFormatting.RED),
                        true
                    );
                }
            }
        }
    }
    
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        
        BrecherDimensionManager manager = Brecher_Dim.getDimensionManager();
        if (manager == null) return;
        
        // Aggressive unloading for exploration dimensions
        if (manager.isExplorationDimension(level.dimension().location())) {
            if (event.getChunk() instanceof LevelChunk levelChunk) {
                // Clear entities in chunk bounds
                var chunkPos = levelChunk.getPos();
                int minX = chunkPos.getMinBlockX();
                int minZ = chunkPos.getMinBlockZ();
                int maxX = chunkPos.getMaxBlockX();
                int maxZ = chunkPos.getMaxBlockZ();
                
                // Collect entities to remove to avoid concurrent modification
                List<Entity> entitiesToRemove = level.getEntities(null, new net.minecraft.world.phys.AABB(minX, level.getMinBuildHeight(), minZ, maxX + 1, level.getMaxBuildHeight(), maxZ + 1))
                    .stream()
                    .filter(BrecherEventHandlers::shouldRemoveEntity)
                    .toList();
                
                // Remove entities after collecting
                entitiesToRemove.forEach(Entity::discard);
            }
        }
    }
    
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info("Server stopping - cleaning up exploration dimensions");
        
        BrecherDimensionManager manager = Brecher_Dim.getDimensionManager();
        if (manager == null) return;
        
        // Return all players to their home dimensions
        MinecraftServer server = event.getServer();
        server.getPlayerList().getPlayers().forEach(player -> {
            if (manager.isExplorationDimension(player.level().dimension().location())) {
                TeleportHandler.returnFromExploration(player);
            }
        });
        
        // Save all data
        BrecherSavedData data = BrecherSavedData.get(server);
        if (data != null) {
            data.setDirty();
        }
        
        // Clean up dimension resources
        manager.getExplorationDimensions().forEach(dimLoc -> {
            ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, dimLoc));
            if (level != null) {
                // Force save and close
                level.save(null, true, false);
                try {
                    level.getChunkSource().close();
                } catch (java.io.IOException e) {
                    LOGGER.error("Failed to close chunk source for dimension {}", dimLoc, e);
                }
            }
        });
        
        // Clear caches
        manager.clearCaches();
        
        // Save dimension counters
        net.tinkstav.brecher_dim.util.DimensionCounterUtil.saveIfDirty();
        
        // Shutdown chunk pre-generator if needed
        net.tinkstav.brecher_dim.performance.SpawnChunkPreGenerator.shutdown();
    }
    
    @SubscribeEvent
    public static void onWorldUnload(LevelEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        
        BrecherDimensionManager manager = Brecher_Dim.getDimensionManager();
        if (manager != null && manager.isExplorationDimension(level.dimension().location())) {
            LOGGER.info("Unloading exploration dimension: {}", level.dimension().location());
            
            // Ensure all players are removed
            // Create a copy to avoid concurrent modification
            List<ServerPlayer> playersToReturn = level.players().stream()
                .filter(player -> player instanceof ServerPlayer)
                .map(player -> (ServerPlayer) player)
                .toList();
            
            playersToReturn.forEach(TeleportHandler::returnFromExploration);
            
            // Clean up resources
            try {
                level.getChunkSource().close();
            } catch (java.io.IOException e) {
                LOGGER.error("Failed to close chunk source during level unload", e);
            }
        }
    }
    
    @SubscribeEvent
    public static void onChunkWatch(ChunkWatchEvent.Watch event) {
        ServerPlayer player = event.getPlayer();
        BrecherDimensionManager manager = Brecher_Dim.getDimensionManager();
        if (manager != null && manager.isExplorationDimension(player.level().dimension().location())) {
            // Track chunk loading for performance monitoring
            BrecherSavedData.get(player.server).trackChunkLoad(
                player.getUUID(), 
                event.getPos()
            );
        }
    }
    
    @SubscribeEvent
    public static void onChunkUnwatch(ChunkWatchEvent.UnWatch event) {
        ServerPlayer player = event.getPlayer();
        BrecherDimensionManager manager = Brecher_Dim.getDimensionManager();
        if (manager != null && manager.isExplorationDimension(player.level().dimension().location())) {
            // Track chunk unloading
            BrecherSavedData.get(player.server).trackChunkUnload(
                player.getUUID(), 
                event.getPos()
            );
        }
    }
    
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || server.getTickCount() % 1200 != 0) return; // Every minute
        
        BrecherDimensionManager manager = Brecher_Dim.getDimensionManager();
        if (manager == null) return;
        
        // Periodic cleanup and monitoring
        manager.getExplorationDimensions().forEach(dimLoc -> {
            ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, dimLoc));
            if (level != null) {
                // Log dimension statistics
                int loadedChunks = level.getChunkSource().getLoadedChunksCount();
                // Count entities efficiently
                long entities = level.getAllEntities().spliterator().getExactSizeIfKnown();
                if (entities == -1) {
                    // If exact size not known, count manually
                    entities = 0;
                    for (Entity entity : level.getAllEntities()) {
                        entities++;
                    }
                }
                int players = level.players().size();
                
                if (players == 0 && loadedChunks > 0) {
                    LOGGER.debug("Exploration dimension {} has {} loaded chunks with no players", 
                        dimLoc, loadedChunks);
                }
            }
        });
    }
    
    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        
        BrecherSavedData data = BrecherSavedData.get(player.server);
        ResourceLocation newDim = player.level().dimension().location();
        
        // Update player's current dimension
        data.updatePlayerDimension(player.getUUID(), newDim);
        
        LOGGER.debug("Player {} changed dimension to {}", 
            player.getName().getString(), newDim);
    }
    
    // Helper method to detect portal type
    private static PortalInfo detectPortalType(ServerPlayer player) {
        // Check for nether portal (obsidian frame)
        if (player.level().getBlockState(player.blockPosition()).is(Blocks.NETHER_PORTAL)) {
            return new PortalInfo(PortalType.NETHER, false);
        }
        
        // Check for end portal
        if (player.level().getBlockState(player.blockPosition()).is(Blocks.END_PORTAL)) {
            return new PortalInfo(PortalType.END, false);
        }
        
        // Otherwise assume modded
        return new PortalInfo(PortalType.MODDED, true);
    }
    
    // Helper method to determine if entity should be removed
    private static boolean shouldRemoveEntity(Entity entity) {
        // Keep players
        if (entity instanceof Player) return false;
        
        // Keep persistent entities (mobs with custom persistence)
        if (entity instanceof net.minecraft.world.entity.Mob mob && mob.requiresCustomPersistence()) return false;
        
        // Keep items for a bit
        if (entity instanceof ItemEntity item) {
            return item.getAge() > 6000; // 5 minutes
        }
        
        // Remove most other entities
        return true;
    }
    
    private record PortalInfo(PortalType type, boolean isModded) {
        boolean isVanilla() {
            return !isModded && (type == PortalType.NETHER || type == PortalType.END);
        }
    }
    
    private enum PortalType {
        NETHER, END, MODDED, UNKNOWN
    }
}