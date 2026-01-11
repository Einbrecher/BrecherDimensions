/*
 * Brecher's Dimensions - Temporary resettable dimensions for exploration
 * Copyright (C) 2025 Einbrecher. All rights reserved.
 */

package net.tinkstav.brecher_dim.platform;

import net.minecraft.server.MinecraftServer;
import java.nio.file.Path;

/**
 * Platform-specific helper methods.
 */
public interface PlatformHelper {
    /**
     * Gets the name of the current platform (e.g., "fabric", "neoforge").
     */
    String getPlatformName();
    
    /**
     * Checks if the mod is running on a specific platform.
     */
    boolean isModLoaded(String modId);
    
    /**
     * Gets the config directory for the current platform.
     */
    Path getConfigDirectory();
    
    /**
     * Gets the game directory.
     */
    Path getGameDirectory();
    
    /**
     * Checks if we're running on the physical client.
     */
    boolean isPhysicalClient();
    
    /**
     * Checks if we're in a development environment.
     */
    boolean isDevelopmentEnvironment();
}