package net.tinkstav.brecher_dim.network.packets;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.tinkstav.brecher_dim.BrecherDimensions;

/**
 * Packet to sync dimension existence state to clients
 */
public class DimensionSyncPacket implements NetworkManager.NetworkReceiver {
    private final ResourceLocation dimensionId;
    private final boolean exists;
    
    public DimensionSyncPacket(ResourceLocation dimensionId, boolean exists) {
        this.dimensionId = dimensionId;
        this.exists = exists;
    }
    
    public DimensionSyncPacket(FriendlyByteBuf buf) {
        this.dimensionId = buf.readResourceLocation();
        this.exists = buf.readBoolean();
    }
    
    public void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(dimensionId);
        buf.writeBoolean(exists);
    }
    
    public static void handle(FriendlyByteBuf buf, NetworkManager.PacketContext context) {
        DimensionSyncPacket packet = new DimensionSyncPacket(buf);
        
        context.queue(() -> {
            // Handle on client side
            if (context.getEnvironment().isClient()) {
                BrecherClientHandler.handleDimensionSync(packet.dimensionId, packet.exists);
            }
        });
    }
    
    // Convert to FriendlyByteBuf for sending
    public FriendlyByteBuf toByteBuf() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        write(buf);
        return buf;
    }
}