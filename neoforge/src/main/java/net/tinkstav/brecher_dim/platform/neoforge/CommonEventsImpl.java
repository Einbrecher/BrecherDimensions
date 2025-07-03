/*
 * Brecher's Dimensions - Temporary resettable dimensions for exploration
 * Copyright (C) 2025 Einbrecher
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.tinkstav.brecher_dim.platform.neoforge;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.tinkstav.brecher_dim.BrecherDimensions;
import net.tinkstav.brecher_dim.dimension.BrecherDimensionManager;
import net.tinkstav.brecher_dim.event.BrecherEventHandlers;
import net.tinkstav.brecher_dim.platform.CommonEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import java.util.HashMap;

public class CommonEventsImpl implements CommonEvents {
    @Override
    public void onServerStarting(MinecraftServer server) {
        // Call the main initialization
        BrecherDimensions.onServerStarting(server);
    }
    
    @Override
    public void onServerStarted(MinecraftServer server) {
        BrecherDimensions.LOGGER.info("Server started - initializing Brecher's Dimensions");
        // Additional server started logic if needed
    }
    
    @Override
    public void onServerStopping(MinecraftServer server) {
        // Call the main cleanup which handles counter saving and dimension cleanup
        BrecherDimensions.onServerStopping(server);
    }
    
    @Override
    public void onPlayerJoin(ServerPlayer player) {
        BrecherEventHandlers.onPlayerJoin(player);
    }
    
    @Override
    public void onPlayerLeave(ServerPlayer player) {
        BrecherEventHandlers.onPlayerLeave(player);
    }
    
    @Override
    public void onPlayerChangeDimension(ServerPlayer player, ResourceKey<Level> from, ResourceKey<Level> to) {
        // Get the actual Level objects from the server
        MinecraftServer server = player.getServer();
        if (server != null) {
            Level fromLevel = server.getLevel(from);
            Level toLevel = server.getLevel(to);
            if (fromLevel != null && toLevel != null) {
                BrecherEventHandlers.onPlayerChangeDimension(player, fromLevel, toLevel);
            }
        }
    }
    
    @Override
    public void onPlayerDeath(ServerPlayer player) {
        BrecherEventHandlers.onPlayerDeath(player);
    }
    
    @Override
    public void onPlayerRespawn(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean alive) {
        // BrecherEventHandlers expects player and keepInventory boolean
        BrecherEventHandlers.onPlayerRespawn(newPlayer, alive);
    }
    
    @Override
    public void onServerTick(MinecraftServer server) {
        BrecherEventHandlers.onServerTick(server);
    }
    
    @Override
    public void registerEvents() {
        NeoForge.EVENT_BUS.register(new EventListener());
    }
    
    public static class EventListener {
        @SubscribeEvent
        public void onServerStarting(ServerStartingEvent event) {
            CommonEventsImpl instance = new CommonEventsImpl();
            instance.onServerStarting(event.getServer());
        }
        
        @SubscribeEvent
        public void onServerStarted(ServerStartedEvent event) {
            BrecherDimensions.LOGGER.info("NeoForge server started event");
            CommonEventsImpl instance = new CommonEventsImpl();
            instance.onServerStarted(event.getServer());
        }
        
        @SubscribeEvent
        public void onServerStopping(ServerStoppingEvent event) {
            CommonEventsImpl instance = new CommonEventsImpl();
            instance.onServerStopping(event.getServer());
        }
        
        @SubscribeEvent
        public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                BrecherEventHandlers.onPlayerJoin(serverPlayer);
            }
        }
        
        @SubscribeEvent
        public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
            if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                BrecherEventHandlers.onPlayerLeave(serverPlayer);
            }
        }
        
        @SubscribeEvent
        public void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
            if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                MinecraftServer server = serverPlayer.getServer();
                if (server != null) {
                    Level fromLevel = server.getLevel(event.getFrom());
                    Level toLevel = server.getLevel(event.getTo());
                    if (fromLevel != null && toLevel != null) {
                        BrecherEventHandlers.onPlayerChangeDimension(serverPlayer, fromLevel, toLevel);
                    }
                }
            }
        }
        
        @SubscribeEvent
        public void onLivingDeath(LivingDeathEvent event) {
            if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                CommonEventsImpl instance = new CommonEventsImpl();
                instance.onPlayerDeath(serverPlayer);
            }
        }
        
        @SubscribeEvent
        public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
            if (event.getEntity() instanceof ServerPlayer newPlayer) {
                // NeoForge doesn't provide keepInventory info, use isEndConquered as approximation
                BrecherEventHandlers.onPlayerRespawn(newPlayer, event.isEndConquered());
            }
        }
        
        @SubscribeEvent
        public void onServerTick(ServerTickEvent.Post event) {
            BrecherEventHandlers.onServerTick(event.getServer());
        }
        
        @SubscribeEvent
        public void onBlockRightClick(PlayerInteractEvent.RightClickBlock event) {
            var player = event.getEntity();
            var world = event.getLevel();
            var pos = event.getPos();
            var state = world.getBlockState(pos);
            
            // Handle bed interactions
            if (state.getBlock() instanceof net.minecraft.world.level.block.BedBlock) {
                var result = BrecherEventHandlers.onPlayerSleep(player, pos);
                if (result == InteractionResult.FAIL) {
                    event.setCanceled(true);
                    event.setCancellationResult(InteractionResult.FAIL);
                }
            }
            
            // Handle respawn anchor interactions
            if (state.is(Blocks.RESPAWN_ANCHOR)) {
                var result = BrecherEventHandlers.onRespawnAnchorUse(player, pos);
                if (result == InteractionResult.FAIL) {
                    event.setCanceled(true);
                    event.setCancellationResult(InteractionResult.FAIL);
                }
            }
            
            // Handle ender chest interactions
            if (state.is(Blocks.ENDER_CHEST)) {
                var result = BrecherEventHandlers.onEnderChestUse(player, world, pos, event.getHand());
                if (result == InteractionResult.FAIL) {
                    event.setCanceled(true);
                    event.setCancellationResult(InteractionResult.FAIL);
                }
            }
        }
        
        @SubscribeEvent
        public void onChunkLoad(ChunkEvent.Load event) {
            if (!event.getLevel().isClientSide() && event.getLevel() instanceof Level level) {
                var chunk = event.getChunk();
                BrecherEventHandlers.onChunkLoad(level, chunk.getPos().x, chunk.getPos().z);
            }
        }
        
        @SubscribeEvent
        public void onChunkUnload(ChunkEvent.Unload event) {
            if (!event.getLevel().isClientSide() && event.getLevel() instanceof Level level) {
                var chunk = event.getChunk();
                BrecherEventHandlers.onChunkUnload(level, chunk.getPos().x, chunk.getPos().z);
            }
        }
    }
}