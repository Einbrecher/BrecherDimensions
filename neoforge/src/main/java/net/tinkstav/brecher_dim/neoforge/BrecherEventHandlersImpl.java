package net.tinkstav.brecher_dim.neoforge;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.tinkstav.brecher_dim.event.BrecherEventHandlers;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * NeoForge-specific event handler implementation that bridges NeoForge events
 * to the common event handling system.
 */
public class BrecherEventHandlersImpl {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public static void handleServerTick(MinecraftServer server) {
        BrecherEventHandlers.onServerTick(server);
    }
    
    public static void handlePlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BrecherEventHandlers.onPlayerJoin(player);
        }
    }
    
    public static void handlePlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BrecherEventHandlers.onPlayerLeave(player);
        }
    }
    
    public static void handlePlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Level fromLevel = player.getServer().getLevel(event.getFrom());
            Level toLevel = player.getServer().getLevel(event.getTo());
            if (fromLevel != null && toLevel != null) {
                BrecherEventHandlers.onPlayerChangeDimension(player, fromLevel, toLevel);
            }
        }
    }
    
    public static void handlePlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // In NeoForge, we don't have direct access to keepInventory flag in this event
            // We'll assume false for now - this might need adjustment based on game rules
            boolean keepInventory = false;
            BrecherEventHandlers.onPlayerRespawn(player, keepInventory);
        }
    }
    
    public static void handleChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof Level level && !level.isClientSide()) {
            if (event.getChunk() instanceof LevelChunk chunk) {
                BrecherEventHandlers.onChunkLoad(level, chunk.getPos().x, chunk.getPos().z);
            }
        }
    }
    
    public static void handleChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof Level level && !level.isClientSide()) {
            if (event.getChunk() instanceof LevelChunk chunk) {
                BrecherEventHandlers.onChunkUnload(level, chunk.getPos().x, chunk.getPos().z);
            }
        }
    }
}