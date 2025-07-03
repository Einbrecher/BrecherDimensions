/*
 * Brecher's Dimensions - Temporary resettable dimensions for exploration
 * Copyright (C) 2025 Einbrecher
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.tinkstav.brecher_dim.client;

import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fabric client-side handler for Brecher Dimensions packets
 * Handles dimension sync, reset notifications, and warnings
 */
public class BrecherClientHandlerFabric {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // Client-side tracking of exploration dimensions
    private static final Set<ResourceLocation> explorationDimensions = new HashSet<>();
    private static final Map<ResourceLocation, Long> scheduledResets = new ConcurrentHashMap<>();
    
    // Chunked registry sync tracking
    private static CompoundTag chunkAccumulator = null;
    private static int expectedChunks = 0;
    private static int receivedChunks = 0;
    
    /**
     * Handle dimension sync packet
     */
    public static void handleDimensionSync(ResourceLocation dimensionId, boolean exists) {
        if (exists) {
            explorationDimensions.add(dimensionId);
            LOGGER.debug("Added exploration dimension: {}", dimensionId);
        } else {
            explorationDimensions.remove(dimensionId);
            LOGGER.debug("Removed exploration dimension: {}", dimensionId);
        }
    }
    
    /**
     * Handle dimension reset notification
     */
    public static void handleDimensionReset(ResourceLocation dimensionId, long resetTime) {
        scheduledResets.put(dimensionId, resetTime);
        LOGGER.info("Scheduled reset for dimension {} at {}", dimensionId, resetTime);
        
        // Show toast notification
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            SystemToast.add(
                minecraft.getToasts(),
                SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                Component.translatable("brecher_dim.reset.scheduled", dimensionId.toString()),
                Component.translatable("brecher_dim.reset.time", new java.util.Date(resetTime))
            );
        }
    }
    
    /**
     * Handle reset warning packet
     */
    public static void handleResetWarning(int minutesRemaining, String message) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        
        if (player == null) return;
        
        // Display chat message
        Component warningMessage = Component.literal(message)
            .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD);
        player.displayClientMessage(warningMessage, false);
        
        // Play warning sound
        float pitch;
        if (minutesRemaining <= 1) {
            pitch = 0.5f; // Deep bell for final minute
        } else if (minutesRemaining <= 5) {
            pitch = 0.8f; // Lower pitch for urgent warnings
        } else {
            pitch = 1.0f; // Normal pitch
        }
        
        player.playNotifySound(SoundEvents.BELL_BLOCK, SoundSource.MASTER, 1.0f, pitch);
        
        // Show overlay for urgent warnings (5 minutes or less)
        if (minutesRemaining <= 5) {
            minecraft.gui.setOverlayMessage(
                Component.literal(message).withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                false
            );
        }
    }
    
    /**
     * Handle registry sync packet
     */
    public static void handleRegistrySync(ResourceLocation dimensionId, byte[] nbtData) {
        try {
            CompoundTag tag = NbtIo.readCompressed(new ByteArrayInputStream(nbtData), NbtAccounter.unlimitedHeap());
            LOGGER.debug("Received registry sync for dimension: {}", dimensionId);
            
            // Process registry data
            processRegistryData(dimensionId, tag);
            
        } catch (Exception e) {
            LOGGER.error("Failed to process registry sync for dimension: {}", dimensionId, e);
        }
    }
    
    /**
     * Handle enhanced registry sync packet
     */
    public static void handleEnhancedRegistrySync(byte[] nbtData) {
        try {
            CompoundTag tag = NbtIo.readCompressed(new ByteArrayInputStream(nbtData), NbtAccounter.unlimitedHeap());
            LOGGER.debug("Received enhanced registry sync");
            
            // Process all dimensions in the tag
            for (String key : tag.getAllKeys()) {
                ResourceLocation dimensionId = ResourceLocation.tryParse(key);
                if (dimensionId != null && tag.contains(key, 10)) { // 10 = Compound tag
                    processRegistryData(dimensionId, tag.getCompound(key));
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to process enhanced registry sync", e);
        }
    }
    
    /**
     * Handle chunked registry sync packet
     */
    public static void handleChunkedRegistrySync(int chunkIndex, int totalChunks, byte[] nbtData) {
        try {
            // Initialize accumulator on first chunk
            if (chunkIndex == 0) {
                chunkAccumulator = new CompoundTag();
                expectedChunks = totalChunks;
                receivedChunks = 0;
            }
            
            // Verify chunk ordering
            if (chunkIndex != receivedChunks) {
                LOGGER.error("Received chunk {} out of order, expected {}", chunkIndex, receivedChunks);
                resetChunkAccumulator();
                return;
            }
            
            // Parse and accumulate chunk data
            CompoundTag chunkTag = NbtIo.readCompressed(new ByteArrayInputStream(nbtData), NbtAccounter.unlimitedHeap());
            for (String key : chunkTag.getAllKeys()) {
                chunkAccumulator.put(key, chunkTag.get(key));
            }
            
            receivedChunks++;
            LOGGER.debug("Received chunk {}/{} of registry sync", receivedChunks, expectedChunks);
            
            // Process complete data when all chunks received
            if (receivedChunks == expectedChunks) {
                LOGGER.info("Received all {} chunks, processing complete registry sync", expectedChunks);
                
                // Process all accumulated dimensions
                for (String key : chunkAccumulator.getAllKeys()) {
                    ResourceLocation dimensionId = ResourceLocation.tryParse(key);
                    if (dimensionId != null && chunkAccumulator.contains(key, 10)) { // 10 = Compound tag
                        processRegistryData(dimensionId, chunkAccumulator.getCompound(key));
                    }
                }
                
                resetChunkAccumulator();
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to process chunked registry sync", e);
            resetChunkAccumulator();
        }
    }
    
    /**
     * Process registry data for a dimension
     */
    private static void processRegistryData(ResourceLocation dimensionId, CompoundTag data) {
        // Mark as exploration dimension
        explorationDimensions.add(dimensionId);
        
        // Extract and store any client-relevant data
        if (data.contains("seed", 4)) { // 4 = Long tag
            long seed = data.getLong("seed");
            LOGGER.debug("Dimension {} has seed: {}", dimensionId, seed);
        }
        
        // Additional processing can be added here as needed
    }
    
    /**
     * Reset chunk accumulator state
     */
    private static void resetChunkAccumulator() {
        chunkAccumulator = null;
        expectedChunks = 0;
        receivedChunks = 0;
    }
    
    /**
     * Clear all client-side data (called on disconnect)
     */
    public static void clearClientData() {
        explorationDimensions.clear();
        scheduledResets.clear();
        resetChunkAccumulator();
        LOGGER.debug("Cleared client-side dimension data");
    }
}