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

package net.tinkstav.brecher_dim;

import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.tinkstav.brecher_dim.config.BrecherConfig;
import net.tinkstav.brecher_dim.dimension.BrecherDimensionManager;
import net.tinkstav.brecher_dim.dimension.DimensionRegistrar;
import net.tinkstav.brecher_dim.dimension.ExplorationSeedManager;
import net.tinkstav.brecher_dim.network.BrecherNetworking;
import net.tinkstav.brecher_dim.generation.ChunkPreGenerator;
import net.tinkstav.brecher_dim.performance.ChunkManager;
import net.tinkstav.brecher_dim.teleport.TeleportHandler;
import net.tinkstav.brecher_dim.platform.Services;
import net.tinkstav.brecher_dim.util.DimensionCleanupUtil;
import net.tinkstav.brecher_dim.util.DimensionCounterUtil;
import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * Main class for Brecher's Dimensions mod.
 * Platform-specific implementations are in fabric/neoforge modules.
 */
public class BrecherDimensions {
    public static final String MOD_ID = "brecher_dim";
    public static final Logger LOGGER = LogUtils.getLogger();
    
    private static BrecherDimensionManager dimensionManager;
    
    public static void init() {
        LOGGER.info("Initializing Brecher's Dimensions on platform: {}", Services.PLATFORM.getPlatformName());
        
        // Initialize configuration
        BrecherConfig.init();
        
        // Initialize networking
        BrecherNetworking.init();
        
        LOGGER.info("Brecher's Dimensions core systems initialized");
    }
    
    /**
     * Get the platform name (Fabric/NeoForge)
     */
    public static String getPlatformName() {
        return Services.PLATFORM.getPlatformName();
    }
    
    /**
     * Get the config directory for the current platform
     */
    public static Path getConfigDirectory() {
        return Services.PLATFORM.getConfigDirectory();
    }
    
    /**
     * Check if we're on the physical client
     */
    public static boolean isPhysicalClient() {
        return Services.PLATFORM.isPhysicalClient();
    }
    
    /**
     * Called when the server is starting
     */
    public static void onServerStarting(MinecraftServer server) {
        LOGGER.info("Brecher's Dimensions server starting");
        
        try {
            // Initialize dimension counter from saved data
            DimensionCounterUtil.initialize(server);
            
            // Validate configuration settings
            // Note: Config system needs to be migrated
            // BrecherConfig.validateConfig();
            
            // Clean up old exploration dimension folders based on retention count
            DimensionCleanupUtil.cleanupOldDimensions(server);
            
            // Initialize DimensionRegistrar
            DimensionRegistrar.initialize();
            
            // Create exploration dimensions at runtime
            DimensionRegistrar.getInstance().createDimensionsOnServerStart(server);
            
            // Log created exploration dimensions
            LOGGER.info("Created exploration dimensions:");
            DimensionRegistrar.getInstance().getDimensionInfo().forEach(info -> LOGGER.info("  {}", info));
            
            // Initialize dimension manager with runtime dimensions
            dimensionManager = new BrecherDimensionManager(
                server, 
                DimensionRegistrar.getInstance().getRegisteredDimensions()
            );
            
            LOGGER.info("Brecher's Dimensions server setup complete with {} exploration dimensions", 
                DimensionRegistrar.getInstance().getRuntimeDimensions().size());
                
        } catch (Exception e) {
            LOGGER.error("Failed to start Brecher's Dimensions properly", e);
            throw new RuntimeException("Critical failure during Brecher's Dimensions startup", e);
        }
    }
    
    /**
     * Called when the server is stopping
     */
    public static void onServerStopping(MinecraftServer server) {
        LOGGER.info("Brecher's Dimensions server stopping - beginning cleanup sequence");
        
        try {
            if (dimensionManager != null) {
                // Evacuate all players from exploration dimensions
                dimensionManager.evacuateAllPlayers();
                
                // Shutdown dimension manager
                dimensionManager.shutdown();
                LOGGER.info("Dimension manager shutdown complete");
            }
            
            // Clear runtime dimension tracking
            DimensionRegistrar.cleanupOnShutdown();
            
            // Save dimension counters to disk
            DimensionCounterUtil.saveIfDirty();
            
            // Clear exploration seed manager for next dimension creation
            ExplorationSeedManager.clearAll();
            
            // Shutdown teleport handler executor
            TeleportHandler.shutdown();

            // Shutdown chunk manager static caches
            ChunkManager.shutdown();

            // Shutdown chunk pre-generator tasks (with server for progress saving)
            ChunkPreGenerator.shutdown(server);

            LOGGER.info("Brecher's Dimensions cleanup complete - new exploration dimensions will be created on next start");
            
        } catch (Exception e) {
            LOGGER.error("Error during Brecher's Dimensions shutdown cleanup", e);
        }
    }
    
    public static BrecherDimensionManager getDimensionManager() {
        return dimensionManager;
    }
    
    public static DimensionRegistrar getDimensionRegistrar() {
        return DimensionRegistrar.getInstance();
    }
}