package net.tinkstav.brecher_dim.network.packets;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.tinkstav.brecher_dim.BrecherDimensions;

/**
 * Packet to sync single dimension registry data
 */
public class RegistrySyncPacket implements NetworkManager.NetworkReceiver {
    private final ResourceLocation dimensionId;
    private final byte[] nbtData;
    
    public RegistrySyncPacket(ResourceLocation dimensionId, byte[] nbtData) {
        this.dimensionId = dimensionId;
        this.nbtData = nbtData;
    }
    
    public RegistrySyncPacket(FriendlyByteBuf buf) {
        this.dimensionId = buf.readResourceLocation();
        int length = buf.readVarInt();
        this.nbtData = new byte[length];
        buf.readBytes(this.nbtData);
    }
    
    public void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(dimensionId);
        buf.writeVarInt(nbtData.length);
        buf.writeBytes(nbtData);
    }
    
    public static void handle(FriendlyByteBuf buf, NetworkManager.PacketContext context) {
        RegistrySyncPacket packet = new RegistrySyncPacket(buf);
        
        context.queue(() -> {
            // Handle on client side
            if (context.getEnvironment().isClient()) {
                BrecherClientHandler.handleRegistrySync(packet.dimensionId, packet.nbtData);
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