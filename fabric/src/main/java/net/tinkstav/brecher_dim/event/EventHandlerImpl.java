package net.tinkstav.brecher_dim.event;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.tinkstav.brecher_dim.fabric.FabricCommandPlatform;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * Fabric implementation of EventHandler
 * This class is discovered by Architectury's @ExpectPlatform system
 */
public class EventHandlerImpl {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    /**
     * Register all event handlers for Fabric
     * Note: In Fabric, event registration is done in the main mod class
     * during initialization, so this method is mostly a no-op
     */
    public static void registerEvents() {
        LOGGER.info("Fabric event registration handled in main mod class");
    }
    
    /**
     * Register commands with the platform's command system
     */
    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        FabricCommandPlatform.registerCommands(dispatcher);
    }
}