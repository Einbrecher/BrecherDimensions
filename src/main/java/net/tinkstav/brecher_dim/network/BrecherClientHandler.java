package net.tinkstav.brecher_dim.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@OnlyIn(Dist.CLIENT)
public class BrecherClientHandler {
    private static final Set<ResourceLocation> explorationDimensions = ConcurrentHashMap.newKeySet();
    private static final Map<ResourceLocation, Long> scheduledResets = new ConcurrentHashMap<>();
    
    /**
     * Called when an exploration dimension is added
     */
    public static void onDimensionAdded(ResourceLocation dimensionId) {
        explorationDimensions.add(dimensionId);
        
        // Optional: Update any client-side UI elements
        if (Minecraft.getInstance().player != null) {
            // Could trigger UI updates here
        }
    }
    
    /**
     * Called when an exploration dimension is removed
     */
    public static void onDimensionRemoved(ResourceLocation dimensionId) {
        explorationDimensions.remove(dimensionId);
        scheduledResets.remove(dimensionId);
        
        // Check if player is in the removed dimension
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.level != null) {
            if (mc.level.dimension().location().equals(dimensionId)) {
                // Show urgent warning
                mc.gui.setOverlayMessage(
                    Component.literal("This dimension is being reset!")
                        .withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                    false
                );
            }
        }
    }
    
    /**
     * Called when a dimension reset is scheduled
     */
    public static void onDimensionResetScheduled(ResourceLocation dimensionId, long resetTime) {
        scheduledResets.put(dimensionId, resetTime);
        
        // Calculate time remaining
        long timeRemaining = resetTime - System.currentTimeMillis();
        if (timeRemaining > 0) {
            long minutes = timeRemaining / 60000;
            
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.gui.setOverlayMessage(
                    Component.literal("Dimension reset scheduled in " + minutes + " minutes")
                        .withStyle(ChatFormatting.YELLOW),
                    false
                );
            }
        }
    }
    
    /**
     * Called when a reset warning is received
     */
    public static void onResetWarning(int minutesRemaining, String message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        
        // Show warning message
        Component warningComponent = Component.literal(message)
            .withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
        
        // Display in chat
        mc.player.displayClientMessage(warningComponent, false);
        
        // Display as overlay for urgent warnings
        if (minutesRemaining <= 5) {
            mc.gui.setOverlayMessage(warningComponent, false);
            
            // Play warning sound
            mc.player.playSound(SoundEvents.NOTE_BLOCK_BELL.value(), 1.0F, 1.0F);
        }
        
        // Extra urgent for final countdown
        if (minutesRemaining <= 1) {
            // Could add screen effects or more prominent warnings
            mc.player.playSound(SoundEvents.BELL_BLOCK, 2.0F, 0.5F);
        }
    }
    
    /**
     * Check if a dimension is an exploration dimension (client-side)
     */
    public static boolean isExplorationDimension(ResourceLocation dimensionId) {
        return explorationDimensions.contains(dimensionId);
    }
    
    /**
     * Get time until reset for a dimension
     */
    public static Optional<Long> getTimeUntilReset(ResourceLocation dimensionId) {
        Long resetTime = scheduledResets.get(dimensionId);
        if (resetTime != null) {
            long remaining = resetTime - System.currentTimeMillis();
            if (remaining > 0) {
                return Optional.of(remaining);
            }
        }
        return Optional.empty();
    }
    
    /**
     * Clear all client-side data (on disconnect)
     */
    public static void clearAll() {
        explorationDimensions.clear();
        scheduledResets.clear();
    }
}