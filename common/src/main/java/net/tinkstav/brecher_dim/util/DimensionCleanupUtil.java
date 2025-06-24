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
     * Clean up old exploration dimension folders, keeping only the most recent ones
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
            LOGGER.info("Cleaning up old exploration dimensions, keeping the {} most recent", retentionCount);
            
            // Find all exploration dimension folders
            List<DimensionFolder> explorationFolders = findExplorationDimensions(dimensionsPath);
            
            if (explorationFolders.size() <= retentionCount) {
                LOGGER.info("Found {} exploration dimension folders, no cleanup needed", explorationFolders.size());
                return;
            }
            
            // Sort by modification time (newest first)
            explorationFolders.sort(Comparator.comparing(DimensionFolder::lastModified).reversed());
            
            // Keep the most recent ones
            List<DimensionFolder> foldersToDelete = explorationFolders.subList(retentionCount, explorationFolders.size());
            
            LOGGER.info("Deleting {} old exploration dimension folders", foldersToDelete.size());
            
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