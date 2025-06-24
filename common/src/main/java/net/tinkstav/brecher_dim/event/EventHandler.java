package net.tinkstav.brecher_dim.event;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;

/**
 * Platform-agnostic event handler interface.
 * Platform-specific implementations will register these handlers.
 */
public interface EventHandler {
    
    /**
     * Register all event handlers for the current platform
     */
    @ExpectPlatform
    static void registerEvents() {
        throw new AssertionError("Platform implementation missing");
    }
    
    /**
     * Called when a player joins the server
     */
    static void onPlayerJoin(ServerPlayer player) {
        BrecherEventHandlers.onPlayerJoin(player);
    }
    
    /**
     * Called when a player leaves the server
     */
    static void onPlayerLeave(ServerPlayer player) {
        BrecherEventHandlers.onPlayerLeave(player);
    }
    
    /**
     * Called when a player changes dimension
     */
    static void onPlayerChangeDimension(ServerPlayer player, Level from, Level to) {
        BrecherEventHandlers.onPlayerChangeDimension(player, from, to);
    }
    
    /**
     * Called when a player respawns
     */
    static void onPlayerRespawn(ServerPlayer player, boolean keepInventory) {
        BrecherEventHandlers.onPlayerRespawn(player, keepInventory);
    }
    
    /**
     * Called when a player uses a bed
     */
    static InteractionResult onPlayerSleep(Player player, BlockPos pos) {
        return BrecherEventHandlers.onPlayerSleep(player, pos);
    }
    
    /**
     * Called when a player interacts with an ender chest
     */
    static InteractionResult onEnderChestUse(Player player, Level level, BlockPos pos, InteractionHand hand) {
        return BrecherEventHandlers.onEnderChestUse(player, level, pos, hand);
    }
    
    /**
     * Called when an entity tries to use a portal
     */
    static boolean onEntityPortalUse(Entity entity, Level level) {
        return BrecherEventHandlers.onEntityPortalUse(entity, level);
    }
    
    /**
     * Called every server tick
     */
    static void onServerTick(MinecraftServer server) {
        BrecherEventHandlers.onServerTick(server);
    }
    
    /**
     * Called when a chunk loads
     */
    static void onChunkLoad(Level level, int chunkX, int chunkZ) {
        BrecherEventHandlers.onChunkLoad(level, chunkX, chunkZ);
    }
    
    /**
     * Called when a chunk unloads
     */
    static void onChunkUnload(Level level, int chunkX, int chunkZ) {
        BrecherEventHandlers.onChunkUnload(level, chunkX, chunkZ);
    }
}