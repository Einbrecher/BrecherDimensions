/*
 * Brecher's Dimensions - Temporary resettable dimensions for exploration
 * Copyright (C) 2025 Einbrecher. All rights reserved.
 */

package net.tinkstav.brecher_dim.platform.fabric;

import net.tinkstav.brecher_dim.platform.CommandHelper;
import net.tinkstav.brecher_dim.commands.BrecherCommands;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class CommandHelperImpl implements CommandHelper {
    @Override
    public void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Register Brecher commands
        BrecherCommands.register(dispatcher);
    }
    
}