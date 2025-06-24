package net.tinkstav.brecher_dim.fabric;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.tinkstav.brecher_dim.event.BrecherEventHandlers;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * Fabric-specific event handler implementation that bridges Fabric events
 * to the common event handling system.
 */
public class FabricEventHandlersImpl {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public static void handleServerTick(MinecraftServer server) {
        BrecherEventHandlers.onServerTick(server);
    }
    
    public static void handlePlayerJoin(ServerPlayer player) {
        BrecherEventHandlers.onPlayerJoin(player);
    }
    
    public static void handlePlayerLeave(ServerPlayer player) {
        BrecherEventHandlers.onPlayerLeave(player);
    }
    
    public static void handlePlayerChangeDimension(ServerPlayer player, Level from, Level to) {
        BrecherEventHandlers.onPlayerChangeDimension(player, from, to);
    }
    
    public static void handlePlayerRespawn(ServerPlayer player, boolean keepInventory) {
        BrecherEventHandlers.onPlayerRespawn(player, keepInventory);
    }
    
    public static void handleChunkLoad(Level level, LevelChunk chunk) {
        if (!level.isClientSide()) {
            BrecherEventHandlers.onChunkLoad(level, chunk.getPos().x, chunk.getPos().z);
        }
    }
    
    public static void handleChunkUnload(Level level, LevelChunk chunk) {
        if (!level.isClientSide()) {
            BrecherEventHandlers.onChunkUnload(level, chunk.getPos().x, chunk.getPos().z);
        }
    }
}