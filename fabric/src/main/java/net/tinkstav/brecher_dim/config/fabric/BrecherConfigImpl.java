/*
 * Brecher's Dimensions - Temporary resettable dimensions for exploration
 * Copyright (C) 2025 Einbrecher. All rights reserved.
 */

package net.tinkstav.brecher_dim.config.fabric;

import com.mojang.logging.LogUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.tinkstav.brecher_dim.config.YamlConfigHandler;
import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * Fabric implementation using YAML configuration
 */
public class BrecherConfigImpl {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static YamlConfigHandler yamlHandler;
    
    /**
     * Initialize config system
     */
    public static void init() {
        LOGGER.info("Initializing Brecher's Exploration Dimensions YAML config for Fabric");
        
        // Get config directory and create brecher_exploration subdirectory
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve("brecher_exploration");
        
        // Initialize YAML handler
        yamlHandler = new YamlConfigHandler(configDir);
        yamlHandler.init();
    }
    
    /**
     * Reload config values
     */
    public static void reload() {
        LOGGER.info("Reloading Brecher's Exploration Dimensions YAML config for Fabric");
        if (yamlHandler != null) {
            yamlHandler.reload();
        } else {
            init();
        }
    }
}