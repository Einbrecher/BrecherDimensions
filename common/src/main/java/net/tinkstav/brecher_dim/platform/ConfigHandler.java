/*
 * Brecher's Dimensions - Temporary resettable dimensions for exploration
 * Copyright (C) 2025 Einbrecher. All rights reserved.
 */

package net.tinkstav.brecher_dim.platform;

/**
 * Platform-specific configuration handler.
 * Handles loading and saving configuration values.
 */
public interface ConfigHandler {
    /**
     * Initialize the configuration system.
     * This typically loads config files and registers config specs.
     */
    void init();
    
    /**
     * Reload configuration values from disk.
     */
    void reload();
    
    /**
     * Get the path to the config file.
     */
    String getConfigPath();
}