package net.tinkstav.brecher_dim.event;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.tinkstav.brecher_dim.Brecher_Dim;
import net.tinkstav.brecher_dim.network.BrecherClientHandler;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import java.util.concurrent.TimeUnit;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = Brecher_Dim.MODID, value = Dist.CLIENT)
public class BrecherClientEvents {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    @SubscribeEvent
    public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        LOGGER.debug("Client disconnecting, clearing exploration dimension data");
        BrecherClientHandler.clearAll();
    }
    
    @SubscribeEvent
    public static void onClientLoggedIn(ClientPlayerNetworkEvent.LoggingIn event) {
        LOGGER.debug("Client logged in to server");
        // Client data will be populated by server sync packets
    }
    
    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        
        // Check if we're in an exploration dimension
        if (BrecherClientHandler.isExplorationDimension(mc.level.dimension().location())) {
            // Get time until reset if available
            BrecherClientHandler.getTimeUntilReset(mc.level.dimension().location()).ifPresent(timeRemaining -> {
                // Only show countdown when less than 30 minutes remain
                if (timeRemaining < 1800000) { // 30 minutes in milliseconds
                    GuiGraphics graphics = event.getGuiGraphics();
                    
                    long minutes = TimeUnit.MILLISECONDS.toMinutes(timeRemaining);
                    long seconds = TimeUnit.MILLISECONDS.toSeconds(timeRemaining) % 60;
                    
                    Component countdown = Component.literal(String.format("Reset in: %02d:%02d", minutes, seconds))
                        .withStyle(minutes <= 5 ? ChatFormatting.RED : ChatFormatting.YELLOW);
                    
                    // Position in top-right corner
                    int x = graphics.guiWidth() - mc.font.width(countdown) - 5;
                    int y = 5;
                    
                    // Draw semi-transparent background
                    graphics.fill(x - 2, y - 2, x + mc.font.width(countdown) + 2, y + mc.font.lineHeight + 2, 0x80000000);
                    
                    // Draw countdown text
                    graphics.drawString(mc.font, countdown, x, y, 0xFFFFFF);
                }
            });
        }
    }
}