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