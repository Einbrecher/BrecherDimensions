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

package net.tinkstav.brecher_dim.platform.neoforge;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.tinkstav.brecher_dim.commands.BrecherCommands;
import net.tinkstav.brecher_dim.platform.CommandHelper;

public class CommandHelperImpl implements CommandHelper {
    @Override
    public void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        BrecherCommands.register(dispatcher);
    }
    
    @Override
    public int getAdminPermissionLevel() {
        return 2; // Operator level
    }
    
    @Override
    public int getPlayerPermissionLevel() {
        return 0; // All players
    }
}