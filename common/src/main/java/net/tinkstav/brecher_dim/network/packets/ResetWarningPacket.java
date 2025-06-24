package net.tinkstav.brecher_dim.network.packets;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.tinkstav.brecher_dim.BrecherDimensions;

/**
 * Packet to send reset warnings to players in dimensions
 */
public class ResetWarningPacket implements NetworkManager.NetworkReceiver {
    private final int minutesRemaining;
    private final String message;
    
    public ResetWarningPacket(int minutesRemaining, String message) {
        this.minutesRemaining = minutesRemaining;
        this.message = message;
    }
    
    public ResetWarningPacket(FriendlyByteBuf buf) {
        this.minutesRemaining = buf.readInt();
        this.message = buf.readUtf();
    }
    
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(minutesRemaining);
        buf.writeUtf(message);
    }
    
    public static void handle(FriendlyByteBuf buf, NetworkManager.PacketContext context) {
        ResetWarningPacket packet = new ResetWarningPacket(buf);
        
        context.queue(() -> {
            // Handle on client side
            if (context.getEnvironment().isClient()) {
                BrecherClientHandler.handleResetWarning(packet.minutesRemaining, packet.message);
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