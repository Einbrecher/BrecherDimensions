package net.tinkstav.brecher_dim.platform;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.tinkstav.brecher_dim.neoforge.NeoForgeCommandPlatform;

/**
 * NeoForge implementation of CommandPlatform
 * This class is discovered by Architectury's @ExpectPlatform system
 */
public class CommandPlatformImpl {
    
    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        NeoForgeCommandPlatform.registerCommands(dispatcher);
    }
}