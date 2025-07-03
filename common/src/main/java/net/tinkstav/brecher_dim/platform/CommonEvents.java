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

package net.tinkstav.brecher_dim.platform;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Common event abstraction for both platforms.
 * Fabric uses callbacks, NeoForge uses @SubscribeEvent.
 */
public interface CommonEvents {
    /**
     * Called when the server is starting up.
     */
    void onServerStarting(MinecraftServer server);
    
    /**
     * Called when the server has started.
     */
    void onServerStarted(MinecraftServer server);
    
    /**
     * Called when the server is stopping.
     */
    void onServerStopping(MinecraftServer server);
    
    /**
     * Called when a player joins the server.
     */
    void onPlayerJoin(ServerPlayer player);
    
    /**
     * Called when a player leaves the server.
     */
    void onPlayerLeave(ServerPlayer player);
    
    /**
     * Called when a player changes dimensions.
     */
    void onPlayerChangeDimension(ServerPlayer player, ResourceKey<Level> from, ResourceKey<Level> to);
    
    /**
     * Called when a player dies.
     */
    void onPlayerDeath(ServerPlayer player);
    
    /**
     * Called when a player respawns after death.
     */
    void onPlayerRespawn(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean alive);
    
    /**
     * Called every server tick.
     */
    void onServerTick(MinecraftServer server);
    
    /**
     * Registers platform-specific event handlers.
     * Must be called during mod initialization.
     */
    void registerEvents();
}