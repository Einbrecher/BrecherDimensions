package net.tinkstav.brecher_dim.teleport;

import net.minecraft.server.level.ServerPlayer;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * Handles teleportation to and from exploration dimensions
 * This is a stub that will be fully implemented later
 */
public class TeleportHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    /**
     * Return a player from an exploration dimension
     */
    public static void returnFromExploration(ServerPlayer player) {
        LOGGER.info("Returning player {} from exploration dimension", player.getName().getString());
        // Full implementation will be added when migrating teleport system
    }
}