package net.tinkstav.brecher_dim.platform;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.tinkstav.brecher_dim.fabric.FabricCommandPlatform;

/**
 * Fabric implementation of CommandPlatform
 * This class is discovered by Architectury's @ExpectPlatform system
 */
public class CommandPlatformImpl {
    
    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        FabricCommandPlatform.registerCommands(dispatcher);
    }
}