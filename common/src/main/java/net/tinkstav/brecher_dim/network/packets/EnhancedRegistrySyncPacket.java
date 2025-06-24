package net.tinkstav.brecher_dim.network.packets;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.tinkstav.brecher_dim.BrecherDimensions;

/**
 * Packet for comprehensive dimension data sync
 */
public class EnhancedRegistrySyncPacket implements NetworkManager.NetworkReceiver {
    private final byte[] nbtData;
    
    public EnhancedRegistrySyncPacket(byte[] nbtData) {
        this.nbtData = nbtData;
    }
    
    public EnhancedRegistrySyncPacket(FriendlyByteBuf buf) {
        int length = buf.readVarInt();
        this.nbtData = new byte[length];
        buf.readBytes(this.nbtData);
    }
    
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(nbtData.length);
        buf.writeBytes(nbtData);
    }
    
    public static void handle(FriendlyByteBuf buf, NetworkManager.PacketContext context) {
        EnhancedRegistrySyncPacket packet = new EnhancedRegistrySyncPacket(buf);
        
        context.queue(() -> {
            // Handle on client side
            if (context.getEnvironment().isClient()) {
                BrecherClientHandler.handleEnhancedRegistrySync(packet.nbtData);
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