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