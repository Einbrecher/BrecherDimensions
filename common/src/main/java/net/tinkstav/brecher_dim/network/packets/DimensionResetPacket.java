package net.tinkstav.brecher_dim.network.packets;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.tinkstav.brecher_dim.BrecherDimensions;

/**
 * Packet to notify clients about scheduled dimension resets
 */
public class DimensionResetPacket implements NetworkManager.NetworkReceiver {
    private final ResourceLocation dimensionId;
    private final long resetTime;
    
    public DimensionResetPacket(ResourceLocation dimensionId, long resetTime) {
        this.dimensionId = dimensionId;
        this.resetTime = resetTime;
    }
    
    public DimensionResetPacket(FriendlyByteBuf buf) {
        this.dimensionId = buf.readResourceLocation();
        this.resetTime = buf.readLong();
    }
    
    public void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(dimensionId);
        buf.writeLong(resetTime);
    }
    
    public static void handle(FriendlyByteBuf buf, NetworkManager.PacketContext context) {
        DimensionResetPacket packet = new DimensionResetPacket(buf);
        
        context.queue(() -> {
            // Handle on client side
            if (context.getEnvironment().isClient()) {
                BrecherClientHandler.handleDimensionReset(packet.dimensionId, packet.resetTime);
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