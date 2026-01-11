/*
 * Brecher's Dimensions - Temporary resettable dimensions for exploration
 * Copyright (C) 2025 Einbrecher. All rights reserved.
 */

package net.tinkstav.brecher_dim.platform;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;

/**
 * Platform-specific command registration.
 */
public interface CommandHelper {
    /**
     * Registers all commands for the mod.
     * Called during server startup.
     */
    void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher);
    
    /**
     * Gets the required permission level for admin commands.
     * Default is 2 (operator).
     */
    default int getAdminPermissionLevel() {
        return 2;
    }
    
    /**
     * Gets the required permission level for player commands.
     * Default is 0 (all players).
     */
    default int getPlayerPermissionLevel() {
        return 0;
    }
}