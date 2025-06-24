package net.tinkstav.brecher_dim.neoforge;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.network.NetworkRegistry;
import net.neoforged.neoforge.network.simple.SimpleChannel;
import net.neoforged.neoforge.network.NetworkDirection;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.tinkstav.brecher_dim.BrecherDimensions;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * NeoForge implementation of NetworkingPlatform
 */
public class NeoForgeNetworkingPlatform {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String PROTOCOL_VERSION = "1";
    
    private static SimpleChannel CHANNEL;
    
    public static void init() {
        LOGGER.info("Initializing NeoForge networking for Brecher's Dimensions");
        
        CHANNEL = NetworkRegistry.ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath(BrecherDimensions.MOD_ID, "main"))
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .simpleChannel();
        
        // Register packets here when we implement them
        // For now, we'll use vanilla packets for dimension sync
    }
    
    public static void syncDimensionToPlayer(ServerPlayer player, ResourceLocation dimension, boolean added) {
        // In 1.21.1, dimension sync is handled automatically by the game
        // We may need to implement custom packets later for additional sync
        LOGGER.debug("Syncing dimension {} to player {} (added: {})", dimension, player.getName().getString(), added);
    }
    
    public static void syncDimensionToAll(ResourceLocation dimension, boolean added) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                syncDimensionToPlayer(player, dimension, added);
            }
        }
    }
    
    public static void sendMessageToDimension(ServerLevel level, String message) {
        Component component = Component.literal(message);
        for (ServerPlayer player : level.players()) {
            player.displayClientMessage(component, false);
        }
    }
}