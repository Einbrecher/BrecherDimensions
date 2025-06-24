package net.tinkstav.brecher_dim.platform;

import com.mojang.brigadier.CommandDispatcher;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.commands.CommandSourceStack;

/**
 * Platform abstraction for command registration
 */
public class CommandPlatform {
    
    /**
     * Register commands with the platform's command system
     * @param dispatcher The command dispatcher
     */
    @ExpectPlatform
    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        throw new AssertionError("Platform implementation missing");
    }
}