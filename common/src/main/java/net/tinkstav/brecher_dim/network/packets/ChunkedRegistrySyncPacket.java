package net.tinkstav.brecher_dim.network.packets;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.tinkstav.brecher_dim.BrecherDimensions;

/**
 * Packet for sending large dimension lists in chunks
 */
public class ChunkedRegistrySyncPacket implements NetworkManager.NetworkReceiver {
    private final int chunkIndex;
    private final int totalChunks;
    private final byte[] nbtData;
    
    public ChunkedRegistrySyncPacket(int chunkIndex, int totalChunks, byte[] nbtData) {
        this.chunkIndex = chunkIndex;
        this.totalChunks = totalChunks;
        this.nbtData = nbtData;
    }
    
    public ChunkedRegistrySyncPacket(FriendlyByteBuf buf) {
        this.chunkIndex = buf.readVarInt();
        this.totalChunks = buf.readVarInt();
        int length = buf.readVarInt();
        this.nbtData = new byte[length];
        buf.readBytes(this.nbtData);
    }
    
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(chunkIndex);
        buf.writeVarInt(totalChunks);
        buf.writeVarInt(nbtData.length);
        buf.writeBytes(nbtData);
    }
    
    public static void handle(FriendlyByteBuf buf, NetworkManager.PacketContext context) {
        ChunkedRegistrySyncPacket packet = new ChunkedRegistrySyncPacket(buf);
        
        context.queue(() -> {
            // Handle on client side
            if (context.getEnvironment().isClient()) {
                BrecherClientHandler.handleChunkedRegistrySync(
                    packet.chunkIndex, packet.totalChunks, packet.nbtData);
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