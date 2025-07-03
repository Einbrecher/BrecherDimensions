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