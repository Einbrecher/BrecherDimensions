/*
 * Brecher's Dimensions - Temporary resettable dimensions for exploration
 * Copyright (C) 2025 Einbrecher. All rights reserved.
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