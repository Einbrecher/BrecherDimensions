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

package net.tinkstav.brecher_dim.dimension;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.tinkstav.brecher_dim.BrecherDimensions;
import net.tinkstav.brecher_dim.config.BrecherConfig;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles creation of exploration dimensions at server startup
 * Creates all dimensions specified in enabledDimensions config using runtime registry manipulation
 * Dimensions persist for the entire server session and are recreated with new seeds on restart
 */
public class DimensionRegistrar {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // Static instance for global access
    private static DimensionRegistrar INSTANCE;
    
    // Runtime dimension tracking
    private final Map<ResourceKey<Level>, ServerLevel> runtimeDimensions = new ConcurrentHashMap<>(); // exploration key -> ServerLevel
    private final Map<ResourceLocation, ResourceKey<Level>> dimensionMappings = new ConcurrentHashMap<>(); // base location -> exploration key
    private final Map<ResourceKey<Level>, Long> dimensionSeeds = new ConcurrentHashMap<>(); // exploration key -> seed
    
    // Static initialization
    public static void initialize() {
        INSTANCE = new DimensionRegistrar();
    }
    
    // Static accessor
    public static DimensionRegistrar getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("DimensionRegistrar not initialized!");
        }
        return INSTANCE;
    }
    
    /**
     * Creates all configured exploration dimensions when the server starts
     * @param server The Minecraft server instance
     */
    public void createDimensionsOnServerStart(MinecraftServer server) {
        LOGGER.info("Creating exploration dimensions at runtime...");
        
        List<String> enabledDimensions = BrecherConfig.getEnabledDimensions();
        
        for (String baseDimStr : enabledDimensions) {
            try {
                ResourceLocation baseDim = ResourceLocation.parse(baseDimStr);
                if (baseDim == null) {
                    LOGGER.error("Invalid dimension name in config: {}", baseDimStr);
                    continue;
                }
                
                // Generate seed for this dimension
                long seed = SimpleSeedManager.generateDailySeed(baseDim);
                
                // Create the exploration dimension
                ServerLevel explorationLevel = DynamicDimensionFactory.createExplorationDimension(
                    server, baseDim, seed
                );
                
                if (explorationLevel != null) {
                    ResourceKey<Level> explorationKey = explorationLevel.dimension();
                    ResourceKey<Level> baseKey = ResourceKey.create(Registries.DIMENSION, baseDim);
                    
                    // Check if we actually got a different dimension
                    if (!explorationKey.equals(baseKey)) {
                        // Track the dimension
                        runtimeDimensions.put(explorationKey, explorationLevel);
                        dimensionMappings.put(baseDim, explorationKey);
                        dimensionSeeds.put(explorationKey, seed);
                        
                        LOGGER.info("Successfully created exploration dimension {} with seed {}", 
                            explorationKey.location(), seed);
                        
                    } else {
                        LOGGER.warn("Exploration dimension creation returned base dimension for {}. " +
                            "This may be due to platform limitations.", baseDim);
                    }
                } else {
                    LOGGER.error("Failed to create exploration dimension for {}", baseDim);
                }
                
            } catch (Exception e) {
                LOGGER.error("Error creating exploration dimension for: {}", baseDimStr, e);
            }
        }
        
        LOGGER.info("Finished creating {} exploration dimensions", runtimeDimensions.size());
    }
    
    /**
     * Gets all runtime dimensions created by this mod
     * @return Map of exploration dimension keys to their ServerLevels
     */
    public Map<ResourceKey<Level>, ServerLevel> getRuntimeDimensions() {
        return Collections.unmodifiableMap(runtimeDimensions);
    }
    
    /**
     * Gets the seed used for a specific exploration dimension
     * @param dimensionKey The dimension key
     * @return The seed, or empty if not found
     */
    public Optional<Long> getDimensionSeed(ResourceKey<Level> dimensionKey) {
        return Optional.ofNullable(dimensionSeeds.get(dimensionKey));
    }
    
    /**
     * Checks if a dimension is registered as an exploration dimension
     * @param baseDimension The base dimension location
     * @return true if registered
     */
    public boolean isRegistered(ResourceLocation baseDimension) {
        return dimensionMappings.containsKey(baseDimension);
    }
    
    /**
     * Gets the exploration dimension key for a base dimension
     * @param baseDimension The base dimension location
     * @return The exploration dimension key, or empty if not found
     */
    public Optional<ResourceKey<Level>> getExplorationDimension(ResourceLocation baseDimension) {
        return Optional.ofNullable(dimensionMappings.get(baseDimension));
    }
    
    /**
     * Gets the exploration dimension key for a base dimension key
     * @param baseDimension The base dimension key
     * @return The exploration dimension key, or empty if not found
     */
    public Optional<ResourceKey<Level>> getExplorationDimension(ResourceKey<Level> baseDimension) {
        return Optional.ofNullable(dimensionMappings.get(baseDimension.location()));
    }
    
    /**
     * Gets the exploration ServerLevel for a base dimension
     * @param baseDimension The base dimension key
     * @return The exploration ServerLevel, or empty if not found
     */
    public Optional<ServerLevel> getExplorationLevel(ResourceKey<Level> baseDimension) {
        // First get the exploration dimension key for this base dimension
        Optional<ResourceKey<Level>> explorationKey = getExplorationDimension(baseDimension);
        return explorationKey.flatMap(key -> Optional.ofNullable(runtimeDimensions.get(key)));
    }
    
    /**
     * Checks if a given dimension key is one of our exploration dimensions
     * @param dimensionKey The dimension key to check
     * @return true if this is an exploration dimension
     */
    public boolean isExplorationDimension(ResourceKey<Level> dimensionKey) {
        return dimensionMappings.containsValue(dimensionKey);
    }
    
    /**
     * Gets mapping of base dimensions to exploration dimensions
     * @return Unmodifiable map of the mappings
     */
    public Map<ResourceKey<Level>, ResourceKey<Level>> getRegisteredDimensions() {
        // Create a snapshot to ensure thread safety
        Map<ResourceLocation, ResourceKey<Level>> snapshot = new HashMap<>(dimensionMappings);
        Map<ResourceKey<Level>, ResourceKey<Level>> result = new HashMap<>();
        
        snapshot.forEach((baseLoc, exploreKey) -> {
            ResourceKey<Level> baseKey = ResourceKey.create(Registries.DIMENSION, baseLoc);
            result.put(baseKey, exploreKey);
        });
        return Collections.unmodifiableMap(result);
    }
    
    /**
     * Clears all runtime dimension tracking
     * Called when server is stopping
     */
    public void clearRuntimeDimensions() {
        runtimeDimensions.clear();
        dimensionMappings.clear();
        dimensionSeeds.clear();
        LOGGER.info("Cleared runtime dimension tracking");
    }
    
    /**
     * Gets information about all exploration dimensions for display
     * @return List of dimension info strings
     */
    public List<String> getDimensionInfo() {
        List<String> info = new ArrayList<>();
        dimensionMappings.forEach((baseLoc, exploreKey) -> {
            info.add(String.format("%s -> %s", 
                baseLoc, exploreKey.location()));
        });
        return info;
    }
    
    // --- Static convenience methods ---
    
    /**
     * Static method to check if a dimension is an exploration dimension
     * Used by mixins and other static contexts
     */
    public static boolean isExplorationDimensionStatic(ResourceKey<Level> dimensionKey) {
        return INSTANCE != null && INSTANCE.isExplorationDimension(dimensionKey);
    }
    
    /**
     * Static method to get all exploration dimension IDs
     * Used by networking code
     */
    public static Set<ResourceLocation> getExplorationDimensionIds() {
        if (INSTANCE == null) {
            return Collections.emptySet();
        }
        Set<ResourceLocation> ids = new HashSet<>();
        INSTANCE.dimensionMappings.values().forEach(key -> ids.add(key.location()));
        return ids;
    }
    
    /**
     * Static method to create exploration dimensions at startup
     */
    public static void createExplorationDimensionsAtStartup(MinecraftServer server) {
        if (INSTANCE == null) {
            initialize();
        }
        INSTANCE.createDimensionsOnServerStart(server);
    }
    
    /**
     * Static method to cleanup on shutdown
     */
    public static void cleanupOnShutdown() {
        if (INSTANCE != null) {
            INSTANCE.clearRuntimeDimensions();
            INSTANCE = null;
        }
    }
}