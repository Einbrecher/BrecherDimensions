package net.tinkstav.brecher_dim.network;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.tinkstav.brecher_dim.mixin.MixinRegistryFixed;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.tinkstav.brecher_dim.Brecher_Dim;
import net.tinkstav.brecher_dim.config.BrecherConfig;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import java.util.*;
import java.util.function.Supplier;

public class BrecherNetworking {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String PROTOCOL_VERSION = "1";
    private static final int MAX_DIMENSIONS_PER_PACKET = 10; // Limit to prevent packet size issues
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        ResourceLocation.tryBuild(Brecher_Dim.MODID, "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );
    
    // Resource locations for custom payload packets
    public static final ResourceLocation REGISTRY_SYNC = ResourceLocation.tryBuild(Brecher_Dim.MODID, "registry_sync");
    public static final ResourceLocation DIMENSION_SYNC = ResourceLocation.tryBuild(Brecher_Dim.MODID, "dimension_sync");
    
    // Enhanced registry sync packet for comprehensive dimension data
    public static final ResourceLocation ENHANCED_REGISTRY_SYNC = ResourceLocation.tryBuild(Brecher_Dim.MODID, "enhanced_registry_sync");
    // Chunked registry sync packet for large dimension lists
    public static final ResourceLocation CHUNKED_REGISTRY_SYNC = ResourceLocation.tryBuild(Brecher_Dim.MODID, "chunked_registry_sync");
    
    public static void register() {
        int id = 0;
        
        // Register packets
        CHANNEL.registerMessage(id++, DimensionSyncPacket.class, 
            DimensionSyncPacket::encode, 
            DimensionSyncPacket::decode,
            DimensionSyncPacket::handle);
            
        CHANNEL.registerMessage(id++, DimensionResetPacket.class,
            DimensionResetPacket::encode,
            DimensionResetPacket::decode,
            DimensionResetPacket::handle);
            
        CHANNEL.registerMessage(id++, ResetWarningPacket.class,
            ResetWarningPacket::encode,
            ResetWarningPacket::decode,
            ResetWarningPacket::handle);
            
        LOGGER.info("Registered {} network packets", id);
    }
    
    // --- Packet Classes ---
    
    /**
     * Packet for syncing dimension existence to clients
     */
    public static class DimensionSyncPacket {
        private final ResourceLocation dimensionId;
        private final boolean exists;
        
        public DimensionSyncPacket(ResourceLocation dimensionId, boolean exists) {
            this.dimensionId = dimensionId;
            this.exists = exists;
        }
        
        public static void encode(DimensionSyncPacket packet, FriendlyByteBuf buf) {
            buf.writeResourceLocation(packet.dimensionId);
            buf.writeBoolean(packet.exists);
        }
        
        public static DimensionSyncPacket decode(FriendlyByteBuf buf) {
            return new DimensionSyncPacket(buf.readResourceLocation(), buf.readBoolean());
        }
        
        public static void handle(DimensionSyncPacket packet, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                // Client-side handling
                if (packet.exists) {
                    BrecherClientHandler.onDimensionAdded(packet.dimensionId);
                } else {
                    BrecherClientHandler.onDimensionRemoved(packet.dimensionId);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Packet for notifying about dimension resets
     */
    public static class DimensionResetPacket {
        private final ResourceLocation dimensionId;
        private final long resetTime;
        
        public DimensionResetPacket(ResourceLocation dimensionId, long resetTime) {
            this.dimensionId = dimensionId;
            this.resetTime = resetTime;
        }
        
        public static void encode(DimensionResetPacket packet, FriendlyByteBuf buf) {
            buf.writeResourceLocation(packet.dimensionId);
            buf.writeLong(packet.resetTime);
        }
        
        public static DimensionResetPacket decode(FriendlyByteBuf buf) {
            return new DimensionResetPacket(buf.readResourceLocation(), buf.readLong());
        }
        
        public static void handle(DimensionResetPacket packet, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                // Client-side handling
                BrecherClientHandler.onDimensionResetScheduled(packet.dimensionId, packet.resetTime);
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Packet for reset warnings
     */
    public static class ResetWarningPacket {
        private final int minutesRemaining;
        private final String message;
        
        public ResetWarningPacket(int minutesRemaining, String message) {
            this.minutesRemaining = minutesRemaining;
            this.message = message;
        }
        
        public static void encode(ResetWarningPacket packet, FriendlyByteBuf buf) {
            buf.writeInt(packet.minutesRemaining);
            buf.writeUtf(packet.message);
        }
        
        public static ResetWarningPacket decode(FriendlyByteBuf buf) {
            return new ResetWarningPacket(buf.readInt(), buf.readUtf());
        }
        
        public static void handle(ResetWarningPacket packet, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                // Client-side handling
                BrecherClientHandler.onResetWarning(packet.minutesRemaining, packet.message);
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    // --- Helper Methods ---
    
    /**
     * Sync dimension state to a specific player
     */
    public static void syncDimensionToPlayer(ServerPlayer player, ResourceLocation dimensionId, boolean exists) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), 
            new DimensionSyncPacket(dimensionId, exists));
    }
    
    /**
     * Sync dimension state to all players
     */
    public static void syncDimensionToAll(ResourceLocation dimensionId, boolean exists) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), 
            new DimensionSyncPacket(dimensionId, exists));
    }
    
    /**
     * Notify all players about a scheduled reset
     */
    public static void notifyResetToAll(ResourceLocation dimensionId, long resetTime) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), 
            new DimensionResetPacket(dimensionId, resetTime));
    }
    
    /**
     * Send reset warning to players in a dimension
     */
    public static void sendResetWarning(ServerLevel level, int minutesRemaining) {
        String message = String.format("Exploration dimension reset in %d minutes!", minutesRemaining);
        ResetWarningPacket packet = new ResetWarningPacket(minutesRemaining, message);
        
        for (ServerPlayer player : level.players()) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
        }
    }
    
    /**
     * Send a message to all players in a dimension
     */
    public static void sendMessageToDimension(ServerLevel level, String message) {
        // Use the reset warning packet to send a generic message (with 0 minutes)
        ResetWarningPacket packet = new ResetWarningPacket(0, message);
        
        for (ServerPlayer player : level.players()) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
        }
    }
    
    /**
     * Sync all exploration dimensions to a player (on join)
     */
    public static void syncAllDimensionsToPlayer(ServerPlayer player, Set<ResourceLocation> explorationDimensions) {
        for (ResourceLocation dimId : explorationDimensions) {
            syncDimensionToPlayer(player, dimId, true);
        }
    }
    
    /**
     * Sync dimension state to a specific player (overload for ServerLevel)
     */
    public static void syncDimensionToPlayer(ServerPlayer player, ServerLevel level) {
        ResourceLocation dimensionId = level.dimension().location();
        
        // Send basic dimension existence sync
        syncDimensionToPlayer(player, dimensionId, true);
        
        // Send enhanced registry sync for exploration dimensions
        if (dimensionId.getNamespace().equals(Brecher_Dim.MODID) && 
            dimensionId.getPath().startsWith("exploration_")) {
            sendRegistrySyncToPlayer(player, level);
        }
        
        // Send additional dimension info - Use retain() to prevent premature release
        FriendlyByteBuf buffer = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try {
            buffer.writeResourceLocation(dimensionId);
            buffer.writeLong(level.getSeed());
            buffer.writeBoolean(true); // isExploration flag
            
            // Retain buffer before sending to prevent premature release by networking layer
            buffer.retain();
            
            // Send as custom payload packet
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket(
                DIMENSION_SYNC, buffer
            ));
        } catch (Exception e) {
            LOGGER.error("Failed to send dimension sync packet to player {}", player.getName().getString(), e);
        } finally {
            // Release both references (original + retained)
            if (buffer.refCnt() > 0) {
                buffer.release();
            }
        }
    }
    
    /**
     * Send comprehensive registry sync to player for a specific dimension
     */
    public static void sendRegistrySyncToPlayer(ServerPlayer player, ServerLevel level) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try {
            ResourceLocation dimensionId = level.dimension().location();
            
            // Get registries from server
            Registry<DimensionType> dimTypeRegistry = player.server.registryAccess()
                .registryOrThrow(Registries.DIMENSION_TYPE);
            Registry<LevelStem> stemRegistry = player.server.registryAccess()
                .registryOrThrow(Registries.LEVEL_STEM);
            
            // Create keys for this dimension
            ResourceKey<DimensionType> dimTypeKey = ResourceKey.create(Registries.DIMENSION_TYPE, dimensionId);
            ResourceKey<LevelStem> stemKey = ResourceKey.create(Registries.LEVEL_STEM, dimensionId);
            
            // Check if dimension is registered
            if (!dimTypeRegistry.containsKey(dimTypeKey)) {
                LOGGER.warn("Dimension type {} not found in registry for sync", dimensionId);
                return;
            }
            
            // Write count (1 dimension to sync)
            buffer.writeVarInt(1);
            
            // Write dimension info
            buffer.writeResourceLocation(dimensionId);
            
            // Get registry IDs
            Integer dimTypeId = getRegistryId(dimTypeRegistry, dimTypeKey);
            buffer.writeVarInt(dimTypeId != null ? dimTypeId : -1);
            
            // Serialize dimension type
            DimensionType dimType = dimTypeRegistry.get(dimTypeKey);
            if (dimType != null) {
                CompoundTag dimTypeTag = (CompoundTag) DimensionType.DIRECT_CODEC
                    .encodeStart(NbtOps.INSTANCE, dimType)
                    .getOrThrow(false, error -> LOGGER.error("Failed to encode dimension type: {}", error));
                buffer.writeNbt(dimTypeTag);
            } else {
                buffer.writeNbt(null);
            }
            
            // Serialize level stem if available
            if (stemRegistry.containsKey(stemKey)) {
                LevelStem levelStem = stemRegistry.get(stemKey);
                CompoundTag stemTag = (CompoundTag) LevelStem.CODEC
                    .encodeStart(NbtOps.INSTANCE, levelStem)
                    .getOrThrow(false, error -> LOGGER.error("Failed to encode level stem: {}", error));
                buffer.writeNbt(stemTag);
            } else {
                buffer.writeNbt(null);
            }
            
            // Retain buffer before sending
            buffer.retain();
            
            // Send registry sync packet
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket(
                REGISTRY_SYNC, buffer
            ));
            
            LOGGER.debug("Sent registry sync for dimension {} to player {}", 
                dimensionId, player.getName().getString());
            
        } catch (Exception e) {
            LOGGER.error("Failed to send registry sync to player {}", player.getName().getString(), e);
        } finally {
            // Proper reference counting - only release if buffer still has references
            if (buffer.refCnt() > 0) {
                buffer.release();
            }
        }
    }
    
    /**
     * Send registry sync for all exploration dimensions to a player
     */
    public static void sendAllRegistrySyncToPlayer(ServerPlayer player) {
        try {
            Registry<DimensionType> dimTypeRegistry = player.server.registryAccess()
                .registryOrThrow(Registries.DIMENSION_TYPE);
            Registry<LevelStem> stemRegistry = player.server.registryAccess()
                .registryOrThrow(Registries.LEVEL_STEM);
            
            // Find all exploration dimensions
            List<ResourceKey<DimensionType>> explorationDimensions = new ArrayList<>();
            for (ResourceKey<DimensionType> key : dimTypeRegistry.registryKeySet()) {
                if (key.location().getNamespace().equals(Brecher_Dim.MODID) && 
                    key.location().getPath().startsWith("exploration_")) {
                    explorationDimensions.add(key);
                }
            }
            
            if (explorationDimensions.isEmpty()) {
                LOGGER.debug("No exploration dimensions to sync to player {}", player.getName().getString());
                return;
            }
            
            // Send dimensions in chunks to avoid packet size limits
            List<List<ResourceKey<DimensionType>>> chunks = new ArrayList<>();
            for (int i = 0; i < explorationDimensions.size(); i += MAX_DIMENSIONS_PER_PACKET) {
                chunks.add(explorationDimensions.subList(i, 
                    Math.min(i + MAX_DIMENSIONS_PER_PACKET, explorationDimensions.size())));
            }
            
            // Send each chunk as a separate packet
            for (int chunkIndex = 0; chunkIndex < chunks.size(); chunkIndex++) {
                List<ResourceKey<DimensionType>> chunk = chunks.get(chunkIndex);
                FriendlyByteBuf buffer = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
                try {
                    // Write chunk metadata
                    buffer.writeVarInt(chunkIndex); // Current chunk index
                    buffer.writeVarInt(chunks.size()); // Total chunks
                    buffer.writeVarInt(chunk.size()); // Dimensions in this chunk
                    
                    for (ResourceKey<DimensionType> dimTypeKey : chunk) {
                    ResourceLocation dimensionId = dimTypeKey.location();
                    ResourceKey<LevelStem> stemKey = ResourceKey.create(Registries.LEVEL_STEM, dimensionId);
                    
                    // Write dimension info
                    buffer.writeResourceLocation(dimensionId);
                    
                    // Get and write registry IDs
                    Integer dimTypeId = getRegistryId(dimTypeRegistry, dimTypeKey);
                    buffer.writeVarInt(dimTypeId != null ? dimTypeId : -1);
                    
                    // Serialize and write dimension type
                    DimensionType dimType = dimTypeRegistry.get(dimTypeKey);
                    if (dimType != null) {
                        CompoundTag dimTypeTag = (CompoundTag) DimensionType.DIRECT_CODEC
                            .encodeStart(NbtOps.INSTANCE, dimType)
                            .getOrThrow(false, error -> LOGGER.error("Failed to encode dimension type: {}", error));
                        buffer.writeNbt(dimTypeTag);
                    } else {
                        buffer.writeNbt(null);
                    }
                    
                    // Serialize and write level stem if available
                    if (stemRegistry.containsKey(stemKey)) {
                        LevelStem levelStem = stemRegistry.get(stemKey);
                        CompoundTag stemTag = (CompoundTag) LevelStem.CODEC
                            .encodeStart(NbtOps.INSTANCE, levelStem)
                            .getOrThrow(false, error -> LOGGER.error("Failed to encode level stem: {}", error));
                        buffer.writeNbt(stemTag);
                    } else {
                        buffer.writeNbt(null);
                    }
                }
                
                    // Retain buffer before sending
                    buffer.retain();
                    
                    // Send registry sync chunk
                    player.connection.send(new net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket(
                        CHUNKED_REGISTRY_SYNC, buffer
                    ));
                    
                    LOGGER.debug("Sent registry sync chunk {}/{} with {} dimensions to player {}", 
                        chunkIndex + 1, chunks.size(), chunk.size(), player.getName().getString());
                    
                } finally {
                    // Proper reference counting
                    if (buffer.refCnt() > 0) {
                        buffer.release();
                    }
                }
            }
            
            LOGGER.info("Sent registry sync for {} exploration dimensions to player {} in {} chunks", 
                explorationDimensions.size(), player.getName().getString(), chunks.size());
            
        } catch (Exception e) {
            LOGGER.error("Failed to send all registry sync to player {}", player.getName().getString(), e);
        }
    }
    
    /**
     * Get registry ID for a key, handling the case where registry might not have ID mappings
     */
    private static <T> Integer getRegistryId(Registry<T> registry, ResourceKey<T> key) {
        try {
            T value = registry.get(key);
            if (value != null) {
                // Try to get the ID - this might not work for all registry types
                return registry.getId(value);
            }
        } catch (Exception e) {
            LOGGER.debug("Could not get registry ID for {}: {}", key.location(), e.getMessage());
        }
        return null;
    }
    
    /**
     * Send dimension list to player with full registry sync
     */
    public static void sendDimensionListToPlayer(ServerPlayer player) {
        // Send basic dimension list
        Set<ResourceLocation> explorationDimensions = net.tinkstav.brecher_dim.dimension.DimensionRegistrar.getExplorationDimensionIds();
        syncAllDimensionsToPlayer(player, explorationDimensions);
        
        // Send comprehensive registry sync for all exploration dimensions
        sendAllRegistrySyncToPlayer(player);
    }
    
    /**
     * Broadcast registry sync to all players when a new dimension is created
     */
    public static void broadcastRegistrySync(ServerLevel level) {
        try {
            // Send to all connected players
            for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
                sendRegistrySyncToPlayer(player, level);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to broadcast registry sync for dimension {}", 
                level.dimension().location(), e);
        }
    }
}