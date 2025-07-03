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

package net.tinkstav.brecher_dim.util;

import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.tinkstav.brecher_dim.BrecherDimensions;
import net.tinkstav.brecher_dim.config.BrecherConfig;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility class for cleaning up old exploration dimension folders
 */
public class DimensionCleanupUtil {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    /**
     * Clean up old exploration dimension folders, keeping the most recent N folders for each dimension type
     * @param server The Minecraft server instance
     */
    public static void cleanupOldDimensions(MinecraftServer server) {
        try {
            Path dimensionsPath = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                .resolve("dimensions")
                .resolve(BrecherDimensions.MOD_ID);
            
            if (!Files.exists(dimensionsPath)) {
                LOGGER.debug("No brecher_dim dimensions folder found, skipping cleanup");
                return;
            }
            
            int retentionCount = BrecherConfig.getOldDimensionRetentionCount();
            LOGGER.info("Cleaning up old exploration dimensions, keeping the {} most recent per dimension type", retentionCount);
            
            // Find all exploration dimension folders
            List<DimensionFolder> explorationFolders = findExplorationDimensions(dimensionsPath);
            
            if (explorationFolders.isEmpty()) {
                LOGGER.info("No exploration dimension folders found, no cleanup needed");
                return;
            }
            
            // Group folders by dimension type
            Map<String, List<DimensionFolder>> foldersByType = new HashMap<>();
            for (DimensionFolder folder : explorationFolders) {
                String dimensionType = extractDimensionType(folder.path.getFileName().toString());
                if (dimensionType != null) {
                    foldersByType.computeIfAbsent(dimensionType, k -> new ArrayList<>()).add(folder);
                }
            }
            
            List<DimensionFolder> foldersToDelete = new ArrayList<>();
            
            // Process each dimension type separately
            for (Map.Entry<String, List<DimensionFolder>> entry : foldersByType.entrySet()) {
                String dimensionType = entry.getKey();
                List<DimensionFolder> typeFolders = entry.getValue();
                
                if (typeFolders.size() <= retentionCount) {
                    LOGGER.info("Found {} {} dimension folders, no cleanup needed for this type", 
                        typeFolders.size(), dimensionType);
                    continue;
                }
                
                // Sort by modification time (newest first)
                typeFolders.sort(Comparator.comparing(DimensionFolder::lastModified).reversed());
                
                // Keep the most recent ones for this dimension type
                List<DimensionFolder> typeDeleteList = typeFolders.subList(retentionCount, typeFolders.size());
                foldersToDelete.addAll(typeDeleteList);
                
                LOGGER.info("Will delete {} old {} dimension folders", typeDeleteList.size(), dimensionType);
            }
            
            if (foldersToDelete.isEmpty()) {
                LOGGER.info("No dimension folders need cleanup");
                return;
            }
            
            LOGGER.info("Deleting {} old exploration dimension folders total", foldersToDelete.size());
            
            for (DimensionFolder folder : foldersToDelete) {
                try {
                    deleteDirectoryRecursively(folder.path);
                    LOGGER.info("Deleted old exploration dimension: {}", folder.path.getFileName());
                } catch (IOException e) {
                    LOGGER.error("Failed to delete dimension folder: {}", folder.path, e);
                }
            }
            
            // Also clean up any empty brecher_dim folder if no dimensions remain
            cleanupEmptyModFolder(dimensionsPath);
            
            LOGGER.info("Dimension cleanup complete");
            
        } catch (Exception e) {
            LOGGER.error("Error during dimension cleanup", e);
        }
    }
    
    /**
     * Find all exploration dimension folders
     */
    private static List<DimensionFolder> findExplorationDimensions(Path dimensionsPath) throws IOException {
        List<DimensionFolder> folders = new ArrayList<>();
        
        try (Stream<Path> stream = Files.list(dimensionsPath)) {
            stream.filter(Files::isDirectory)
                  .filter(path -> isExplorationDimension(path.getFileName().toString()))
                  .forEach(path -> {
                      try {
                          FileTime lastModified = Files.getLastModifiedTime(path);
                          folders.add(new DimensionFolder(path, lastModified));
                      } catch (IOException e) {
                          LOGGER.warn("Failed to get modification time for: {}", path, e);
                      }
                  });
        }
        
        return folders;
    }
    
    /**
     * Check if a folder name matches the exploration dimension pattern
     */
    private static boolean isExplorationDimension(String folderName) {
        // Exploration dimensions have names like "exploration_overworld_1", "exploration_the_nether_2", etc.
        return folderName.startsWith("exploration_");
    }
    
    /**
     * Extract the dimension type from a folder name
     * e.g., "exploration_overworld_1" -> "overworld"
     * e.g., "exploration_the_nether_2" -> "the_nether"
     */
    private static String extractDimensionType(String folderName) {
        if (!folderName.startsWith("exploration_")) {
            return null;
        }
        
        // Remove "exploration_" prefix
        String remainder = folderName.substring("exploration_".length());
        
        // Find the last underscore (before the counter)
        int lastUnderscore = remainder.lastIndexOf('_');
        if (lastUnderscore == -1) {
            return null;
        }
        
        // Extract dimension type (everything before the last underscore)
        return remainder.substring(0, lastUnderscore);
    }
    
    /**
     * Delete a directory and all its contents recursively
     */
    private static void deleteDirectoryRecursively(Path path) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
    
    /**
     * Clean up empty mod folder if no dimensions remain
     */
    private static void cleanupEmptyModFolder(Path modDimensionsPath) {
        try {
            // Check if the brecher_dim folder is empty
            try (Stream<Path> stream = Files.list(modDimensionsPath)) {
                if (stream.findAny().isEmpty()) {
                    // Folder is empty, delete it
                    Files.delete(modDimensionsPath);
                    LOGGER.debug("Deleted empty brecher_dim folder");
                    
                    // Also check if the dimensions folder is empty and delete it
                    Path dimensionsPath = modDimensionsPath.getParent();
                    if (dimensionsPath != null && dimensionsPath.getFileName().toString().equals("dimensions")) {
                        try (Stream<Path> parentStream = Files.list(dimensionsPath)) {
                            if (parentStream.findAny().isEmpty()) {
                                Files.delete(dimensionsPath);
                                LOGGER.debug("Deleted empty dimensions folder");
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            // Not critical if we can't delete empty folders
            LOGGER.debug("Could not clean up empty folders: {}", e.getMessage());
        }
    }
    
    /**
     * Helper class to store dimension folder information
     */
    private static class DimensionFolder {
        final Path path;
        final FileTime lastModified;
        
        DimensionFolder(Path path, FileTime lastModified) {
            this.path = path;
            this.lastModified = lastModified;
        }
        
        FileTime lastModified() {
            return lastModified;
        }
    }
}