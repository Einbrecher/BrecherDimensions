package net.tinkstav.brecher_dim.fabric;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.tinkstav.brecher_dim.commands.BrecherCommands;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * Fabric implementation of CommandPlatform
 * Handles command registration for Fabric
 */
public class FabricCommandPlatform {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    /**
     * Handle command registration from Fabric's CommandRegistrationCallback
     */
    public static void handleRegisterCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        LOGGER.info("Registering Brecher's Dimensions commands for Fabric");
        registerCommands(dispatcher);
    }
    
    /**
     * Register commands with the given dispatcher
     */
    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        BrecherCommands.register(dispatcher);
    }
}