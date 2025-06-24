package net.tinkstav.brecher_dim.neoforge;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.tinkstav.brecher_dim.commands.BrecherCommands;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * NeoForge implementation of CommandPlatform
 * Handles command registration for NeoForge
 */
public class NeoForgeCommandPlatform {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    /**
     * Handle the RegisterCommandsEvent from NeoForge
     */
    public static void handleRegisterCommands(RegisterCommandsEvent event) {
        LOGGER.info("Registering Brecher's Dimensions commands for NeoForge");
        registerCommands(event.getDispatcher());
    }
    
    /**
     * Register commands with the given dispatcher
     */
    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        BrecherCommands.register(dispatcher);
    }
}