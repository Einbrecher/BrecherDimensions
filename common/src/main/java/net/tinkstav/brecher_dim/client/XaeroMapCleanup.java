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

package net.tinkstav.brecher_dim.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.tinkstav.brecher_dim.BrecherDimensions;
import net.tinkstav.brecher_dim.config.BrecherConfig;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Utility for cleaning up Xaero's map data for removed dimensions.
 * Handles both Xaero's Minimap and World Map data.
 */
public class XaeroMapCleanup {
    private static final Set<ResourceLocation> EXPLORATION_DIMENSIONS = ConcurrentHashMap.newKeySet();
    private static final String BRECHER_DIM_PREFIX = "brecher_dim$exploration_";
    
    /**
     * Track an exploration dimension
     */
    public static void trackExplorationDimension(ResourceLocation dimensionId) {
        if (dimensionId.getNamespace().equals(BrecherDimensions.MOD_ID)) {
            EXPLORATION_DIMENSIONS.add(dimensionId);
            BrecherDimensions.LOGGER.debug("Tracking exploration dimension: {}", dimensionId);
        }
    }
    
    /**
     * Clean up map data for a removed dimension
     */
    public static void cleanupDimensionData(ResourceLocation dimensionId) {
        // Check if cleanup is enabled
        if (!BrecherConfig.isCleanupXaeroMapData()) {
            BrecherDimensions.LOGGER.debug("Xaero map cleanup is disabled in config");
            return;
        }
        
        if (!EXPLORATION_DIMENSIONS.contains(dimensionId)) {
            BrecherDimensions.LOGGER.debug("Dimension {} is not an exploration dimension, skipping cleanup", dimensionId);
            return;
        }
        
        // Remove from tracking
        EXPLORATION_DIMENSIONS.remove(dimensionId);
        
        // Perform cleanup asynchronously to avoid blocking
        CompletableFuture.runAsync(() -> {
            try {
                cleanupXaeroData(dimensionId);
            } catch (Exception e) {
                BrecherDimensions.LOGGER.error("Failed to cleanup Xaero data for dimension {}", dimensionId, e);
            }
        });
    }
    
    private static void cleanupXaeroData(ResourceLocation dimensionId) {
        Path gameDir = Minecraft.getInstance().gameDirectory.toPath();
        Path xaeroDir = gameDir.resolve("xaero");
        
        if (!Files.exists(xaeroDir)) {
            BrecherDimensions.LOGGER.debug("Xaero directory not found, skipping cleanup");
            return;
        }
        
        // Build the full dimension folder name as used by Xaero's
        // Format: brecher_dim$exploration_[type]_[id]
        String xaeroDimensionName = BrecherDimensions.MOD_ID + "$" + dimensionId.getPath();
        
        BrecherDimensions.LOGGER.debug("Cleaning up Xaero data for dimension: {}", xaeroDimensionName);
        
        // Clean up World Map data
        cleanupWorldMapData(xaeroDir, xaeroDimensionName);
        
        // Clean up Minimap data (waypoints)
        cleanupMinimapData(xaeroDir, xaeroDimensionName);
    }
    
    /**
     * Get the full dimension name as used by Xaero's mod
     * @param dimensionPath The dimension path from ResourceLocation (e.g., "exploration_overworld_0")
     * @return The full dimension name (e.g., "brecher_dim$exploration_overworld_0")
     */
    private static String getXaeroDimensionName(String dimensionPath) {
        return BrecherDimensions.MOD_ID + "$" + dimensionPath;
    }
    
    private static void cleanupWorldMapData(Path xaeroDir, String xaeroDimensionName) {
        Path worldMapDir = xaeroDir.resolve("world-map");
        if (!Files.exists(worldMapDir)) {
            return;
        }
        
        List<String> targetDirs = BrecherConfig.getXaeroCleanupTargets();
        
        try {
            // Find all server/world directories
            Files.walk(worldMapDir, 2)
                .filter(Files::isDirectory)
                .filter(path -> {
                    String dirName = path.getFileName().toString();
                    // If target directories specified, only clean those
                    if (!targetDirs.isEmpty()) {
                        return targetDirs.contains(dirName);
                    }
                    // Otherwise clean all Multiplayer/Singleplayer directories
                    return dirName.startsWith("Multiplayer_") || dirName.startsWith("Singleplayer_");
                })
                .forEach(serverDir -> {
                    // Look for the specific dimension folder by exact name
                    Path dimFolder = serverDir.resolve(xaeroDimensionName);
                    if (Files.exists(dimFolder)) {
                        try {
                            FileUtils.deleteDirectory(dimFolder.toFile());
                            BrecherDimensions.LOGGER.info("Deleted Xaero World Map data: {}", dimFolder);
                        } catch (IOException e) {
                            BrecherDimensions.LOGGER.error("Failed to delete World Map folder: {}", dimFolder, e);
                        }
                    }
                });
        } catch (IOException e) {
            BrecherDimensions.LOGGER.error("Error walking World Map directory", e);
        }
    }
    
    private static void cleanupMinimapData(Path xaeroDir, String xaeroDimensionName) {
        Path minimapDir = xaeroDir.resolve("minimap");
        if (!Files.exists(minimapDir)) {
            return;
        }
        
        try {
            // Clean up waypoint data that contains the dimension name
            // Xaero's waypoints may include the dimension name in the file
            Files.walk(minimapDir)
                .filter(Files::isRegularFile)
                .filter(path -> {
                    String fileName = path.getFileName().toString();
                    // Check if file contains the dimension name
                    return fileName.contains(xaeroDimensionName) || 
                           // Also check for URL-encoded version ($ becomes %24)
                           fileName.contains(xaeroDimensionName.replace("$", "%24"));
                })
                .forEach(file -> {
                    try {
                        Files.delete(file);
                        BrecherDimensions.LOGGER.info("Deleted Xaero Minimap data: {}", file);
                    } catch (IOException e) {
                        BrecherDimensions.LOGGER.error("Failed to delete Minimap file: {}", file, e);
                    }
                });
        } catch (IOException e) {
            BrecherDimensions.LOGGER.error("Error walking Minimap directory", e);
        }
    }
    
    /**
     * Clear all tracked dimensions (called on disconnect)
     */
    public static void clearTrackedDimensions() {
        EXPLORATION_DIMENSIONS.clear();
    }
    
    /**
     * Clean up old Xaero map data on server connection
     * This removes map data for any exploration dimensions from previous sessions
     */
    public static void cleanupOldMapDataOnConnect() {
        // Check if cleanup is enabled
        if (!BrecherConfig.isCleanupXaeroMapData()) {
            BrecherDimensions.LOGGER.debug("Xaero map cleanup is disabled in config");
            return;
        }
        
        // Perform cleanup asynchronously to avoid blocking connection
        CompletableFuture.runAsync(() -> {
            try {
                BrecherDimensions.LOGGER.info("Cleaning up old Xaero map data from previous sessions");
                cleanupAllExplorationDimensions();
            } catch (Exception e) {
                BrecherDimensions.LOGGER.error("Failed to cleanup old Xaero map data", e);
            }
        });
    }
    
    /**
     * Clean up all exploration dimension map data
     */
    private static void cleanupAllExplorationDimensions() {
        Path gameDir = Minecraft.getInstance().gameDirectory.toPath();
        Path xaeroDir = gameDir.resolve("xaero");
        
        if (!Files.exists(xaeroDir)) {
            BrecherDimensions.LOGGER.debug("Xaero directory not found, skipping cleanup");
            return;
        }
        
        // Clean up World Map data
        cleanupAllWorldMapData(xaeroDir);
        
        // Clean up Minimap data
        cleanupAllMinimapData(xaeroDir);
    }
    
    /**
     * Clean up all exploration dimension world map data
     */
    private static void cleanupAllWorldMapData(Path xaeroDir) {
        Path worldMapDir = xaeroDir.resolve("world-map");
        if (!Files.exists(worldMapDir)) {
            return;
        }
        
        List<String> targetDirs = BrecherConfig.getXaeroCleanupTargets();
        
        try {
            // Find all server/world directories
            Files.walk(worldMapDir, 2)
                .filter(Files::isDirectory)
                .filter(path -> {
                    String dirName = path.getFileName().toString();
                    // If target directories specified, only clean those
                    if (!targetDirs.isEmpty()) {
                        return targetDirs.contains(dirName);
                    }
                    // Otherwise clean all Multiplayer/Singleplayer directories
                    return dirName.startsWith("Multiplayer_") || dirName.startsWith("Singleplayer_");
                })
                .forEach(serverDir -> {
                    // Look for any folders that match exploration dimension pattern
                    try {
                        Files.list(serverDir)
                            .filter(Files::isDirectory)
                            .filter(path -> {
                                String folderName = path.getFileName().toString();
                                // Check for Brecher dimension folders
                                return folderName.startsWith(BRECHER_DIM_PREFIX);
                            })
                            .forEach(dimFolder -> {
                                try {
                                    FileUtils.deleteDirectory(dimFolder.toFile());
                                    BrecherDimensions.LOGGER.info("Deleted old Xaero World Map data: {}", dimFolder);
                                } catch (IOException e) {
                                    BrecherDimensions.LOGGER.error("Failed to delete World Map folder: {}", dimFolder, e);
                                }
                            });
                    } catch (IOException e) {
                        BrecherDimensions.LOGGER.error("Error listing server directory: {}", serverDir, e);
                    }
                });
        } catch (IOException e) {
            BrecherDimensions.LOGGER.error("Error walking World Map directory", e);
        }
    }
    
    /**
     * Clean up all exploration dimension minimap data
     */
    private static void cleanupAllMinimapData(Path xaeroDir) {
        Path minimapDir = xaeroDir.resolve("minimap");
        if (!Files.exists(minimapDir)) {
            return;
        }
        
        try {
            // Clean up any waypoint data for exploration dimensions
            Files.walk(minimapDir)
                .filter(Files::isRegularFile)
                .filter(path -> {
                    String fileName = path.getFileName().toString();
                    // Look for files that contain the Brecher dimension prefix
                    return fileName.contains(BRECHER_DIM_PREFIX) || 
                           // Also check for URL-encoded version
                           fileName.contains(BRECHER_DIM_PREFIX.replace("$", "%24"));
                })
                .forEach(file -> {
                    try {
                        Files.delete(file);
                        BrecherDimensions.LOGGER.info("Deleted old Xaero Minimap data: {}", file);
                    } catch (IOException e) {
                        BrecherDimensions.LOGGER.error("Failed to delete Minimap file: {}", file, e);
                    }
                });
        } catch (IOException e) {
            BrecherDimensions.LOGGER.error("Error walking Minimap directory", e);
        }
    }
    
    /**
     * Check if cleanup should log detailed information
     * @return true if debug logging is enabled
     */
    private static boolean isDebugEnabled() {
        return BrecherDimensions.LOGGER.isDebugEnabled();
    }
}