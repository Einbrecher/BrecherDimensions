/*
 * Brecher's Dimensions - Temporary resettable dimensions for exploration
 * Copyright (C) 2025 Einbrecher. All rights reserved.
 */

package net.tinkstav.brecher_dim.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.Container;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player inventory preservation when dying in exploration dimensions.
 * Stores inventories temporarily and restores them on respawn.
 */
public class InventoryKeeper {
    private static final Map<UUID, PlayerInventoryData> savedInventories = new ConcurrentHashMap<>();
    
    /**
     * Data class to store player inventory state
     */
    private static class PlayerInventoryData {
        final List<ItemStack> items;
        final int experienceLevel;
        final float experienceProgress;
        final long timestamp;
        
        PlayerInventoryData(ServerPlayer player) {
            this.items = new ArrayList<>();
            this.experienceLevel = player.experienceLevel;
            this.experienceProgress = player.experienceProgress;
            this.timestamp = System.currentTimeMillis();
            
            // Save all inventory items
            Container inventory = player.getInventory();
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                if (!stack.isEmpty()) {
                    items.add(stack.copy());
                }
            }
        }
        
        void restoreTo(ServerPlayer player) {
            // Clear existing inventory first
            player.getInventory().clearContent();
            
            // Restore items
            Container inventory = player.getInventory();
            for (ItemStack stack : items) {
                if (!player.getInventory().add(stack.copy())) {
                    // If inventory is full, drop the item
                    player.drop(stack.copy(), false);
                }
            }
            
            // Restore experience
            player.setExperienceLevels(experienceLevel);
            player.experienceProgress = experienceProgress;
        }
    }
    
    /**
     * Save a player's inventory when they die in an exploration dimension
     */
    public static void saveInventory(ServerPlayer player) {
        PlayerInventoryData data = new PlayerInventoryData(player);
        savedInventories.put(player.getUUID(), data);
    }
    
    /**
     * Restore a player's inventory when they respawn
     */
    public static boolean restoreInventory(ServerPlayer player) {
        PlayerInventoryData data = savedInventories.remove(player.getUUID());
        if (data != null) {
            data.restoreTo(player);
            return true;
        }
        return false;
    }
    
    /**
     * Check if a player has a saved inventory
     */
    public static boolean hasSavedInventory(UUID playerId) {
        return savedInventories.containsKey(playerId);
    }
    
    /**
     * Clear a player's saved inventory without restoring it
     */
    public static void clearSavedInventory(UUID playerId) {
        savedInventories.remove(playerId);
    }
    
    /**
     * Clear all saved inventories (used on server shutdown)
     */
    public static void clearAll() {
        savedInventories.clear();
    }
    
    /**
     * Clean up old saved inventories (older than 1 hour)
     */
    public static void cleanupOldInventories() {
        long cutoffTime = System.currentTimeMillis() - (60 * 60 * 1000); // 1 hour
        savedInventories.entrySet().removeIf(entry -> entry.getValue().timestamp < cutoffTime);
    }
    
    /**
     * Get the count of saved inventories (for debugging/stats)
     */
    public static int getSavedInventoryCount() {
        return savedInventories.size();
    }
}