package net.tinkstav.brecher_dim.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.core.registries.Registries;
import net.tinkstav.brecher_dim.Brecher_Dim;
import net.tinkstav.brecher_dim.network.BrecherNetworking;
import net.tinkstav.brecher_dim.teleport.TeleportHandler;
import net.tinkstav.brecher_dim.performance.ChunkManager;
import net.tinkstav.brecher_dim.performance.MemoryMonitor;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages exploration dimensions during server runtime
 * Tracks dimension mappings and player locations within exploration dimensions
 * Note: All dimensions are created at server startup and persist until shutdown
 */
public class BrecherDimensionManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    private final MinecraftServer server;
    private final Map<ResourceKey<Level>, ResourceKey<Level>> dimensionMappings;
    private final Set<ResourceLocation> activeDimensions;
    private final Map<UUID, ResourceLocation> playerLastDimension;
    
    public BrecherDimensionManager(MinecraftServer server, Map<ResourceKey<Level>, ResourceKey<Level>> registeredDimensions) {
        this.server = server;
        this.dimensionMappings = new ConcurrentHashMap<>(registeredDimensions);
        this.activeDimensions = ConcurrentHashMap.newKeySet();
        this.playerLastDimension = new ConcurrentHashMap<>();
        
        LOGGER.info("Initialized BrecherDimensionManager with {} dimension mappings", dimensionMappings.size());
    }
    
    /**
     * Get the exploration dimension for a base dimension
     */
    public synchronized Optional<ServerLevel> getExplorationDimension(ResourceLocation baseDimension) {
        ResourceKey<Level> baseKey = ResourceKey.create(Registries.DIMENSION, baseDimension);
        ResourceKey<Level> explorationKey = dimensionMappings.get(baseKey);
        
        if (explorationKey == null) {
            return Optional.empty();
        }
        
        ServerLevel level = server.getLevel(explorationKey);
        
        if (level != null) {
            // Only perform initialization once per dimension
            if (activeDimensions.add(explorationKey.location())) {
                // Configure chunk management for exploration
                ChunkManager.configureForExploration(level);
                
                // Notify clients
                BrecherNetworking.syncDimensionToAll(explorationKey.location(), true);
            }
        }
        
        return Optional.ofNullable(level);
    }
    
    /**
     * Check if a dimension is an exploration dimension
     */
    public boolean isExplorationDimension(ResourceLocation dimensionId) {
        return dimensionMappings.values().stream()
            .anyMatch(key -> key.location().equals(dimensionId));
    }
    
    /**
     * Get all currently active exploration dimensions
     */
    public Set<ResourceLocation> getExplorationDimensions() {
        return new HashSet<>(activeDimensions);
    }
    
    /**
     * Track player entering exploration dimension
     */
    public void onPlayerEnterExploration(ServerPlayer player, ResourceLocation dimension) {
        playerLastDimension.put(player.getUUID(), dimension);
        LOGGER.debug("Player {} entered exploration dimension {}", player.getName().getString(), dimension);
    }
    
    /**
     * Track player leaving exploration dimension
     */
    public void onPlayerLeaveExploration(ServerPlayer player) {
        playerLastDimension.remove(player.getUUID());
        LOGGER.debug("Player {} left exploration dimension", player.getName().getString());
    }
    
    /**
     * Get the last exploration dimension a player was in
     */
    public Optional<ResourceLocation> getPlayerLastDimension(UUID playerId) {
        return Optional.ofNullable(playerLastDimension.get(playerId));
    }
    
    /**
     * Get all players currently in exploration dimensions
     */
    public List<ServerPlayer> getPlayersInExplorationDimensions() {
        List<ServerPlayer> players = new ArrayList<>();
        
        for (ResourceKey<Level> explorationKey : dimensionMappings.values()) {
            ServerLevel level = server.getLevel(explorationKey);
            if (level != null) {
                players.addAll(level.players());
            }
        }
        
        return players;
    }
    
    /**
     * Evacuate all players from exploration dimensions
     * Called when server is stopping
     */
    public void evacuateAllPlayers() {
        LOGGER.info("Evacuating all players from exploration dimensions");
        
        List<ServerPlayer> playersToEvacuate = getPlayersInExplorationDimensions();
        
        for (ServerPlayer player : playersToEvacuate) {
            try {
                TeleportHandler.returnFromExploration(player);
                LOGGER.info("Evacuated player {} from exploration dimension", player.getName().getString());
            } catch (Exception e) {
                LOGGER.error("Failed to evacuate player {}", player.getName().getString(), e);
                // Force teleport to spawn as fallback
                BlockPos spawnPos = player.getRespawnPosition();
                if (spawnPos == null) {
                    spawnPos = server.overworld().getSharedSpawnPos();
                }
                player.teleportTo(server.overworld(), 
                    spawnPos.getX() + 0.5, 
                    spawnPos.getY(), 
                    spawnPos.getZ() + 0.5, 
                    0, 0);
            }
        }
        
        LOGGER.info("Evacuated {} players from exploration dimensions", playersToEvacuate.size());
    }
    
    /**
     * Send a message to all players in exploration dimensions
     */
    public void sendMessageToExplorationPlayers(String message) {
        for (ResourceKey<Level> explorationKey : dimensionMappings.values()) {
            ServerLevel level = server.getLevel(explorationKey);
            if (level != null && !level.players().isEmpty()) {
                BrecherNetworking.sendMessageToDimension(level, message);
            }
        }
    }
    
    /**
     * Get statistics about exploration dimensions
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalMappings", dimensionMappings.size());
        stats.put("activeDimensions", activeDimensions.size());
        stats.put("playersInExploration", getPlayersInExplorationDimensions().size());
        
        // Per-dimension stats
        Map<String, Integer> perDimStats = new HashMap<>();
        for (ResourceKey<Level> explorationKey : dimensionMappings.values()) {
            ServerLevel level = server.getLevel(explorationKey);
            if (level != null) {
                perDimStats.put(explorationKey.location().toString(), level.players().size());
            }
        }
        stats.put("perDimensionPlayers", perDimStats);
        
        return stats;
    }
    
    /**
     * Clear all caches and tracking
     */
    public void clearCaches() {
        playerLastDimension.clear();
        activeDimensions.clear();
        LOGGER.info("Cleared dimension manager caches");
    }
    
    /**
     * Get the MinecraftServer instance
     */
    public MinecraftServer getServer() {
        return server;
    }
    
    /**
     * Shutdown the dimension manager
     */
    public void shutdown() {
        // No complex cleanup needed for runtime dimensions
        // They will be regenerated on next server start
        
        // Log memory usage
        MemoryMonitor.logMemoryUsage("Dimension Manager Shutdown");
        
        // Clear all caches
        clearCaches();
        
        LOGGER.info("BrecherDimensionManager shutdown complete");
    }
    
    // --- Static helper methods for mixin usage ---
    
    /**
     * Static helper to check if a player is in an exploration dimension
     * Used by mixins that need static access
     */
    public static boolean isInExplorationDimension(ServerPlayer player) {
        BrecherDimensionManager manager = Brecher_Dim.getDimensionManager();
        return manager != null && manager.isExplorationDimension(player.level().dimension().location());
    }
    
    /**
     * Static helper to track when a player leaves
     * Used by mixins that need static access
     */
    public static void trackPlayerLeaving(ServerPlayer player) {
        BrecherDimensionManager manager = Brecher_Dim.getDimensionManager();
        if (manager != null) {
            manager.onPlayerLeaveExploration(player);
        }
    }
}