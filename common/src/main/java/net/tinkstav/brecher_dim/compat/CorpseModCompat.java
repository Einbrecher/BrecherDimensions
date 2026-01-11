/*
 * Brecher's Dimensions - Temporary resettable dimensions for exploration
 * Copyright (C) 2025 Einbrecher. All rights reserved.
 */

package net.tinkstav.brecher_dim.compat;

import net.minecraft.server.level.ServerPlayer;
import net.tinkstav.brecher_dim.BrecherDimensions;
import net.tinkstav.brecher_dim.config.BrecherConfig;
import net.tinkstav.brecher_dim.platform.Services;

import java.util.HashSet;
import java.util.Set;

/**
 * Compatibility handler for corpse and gravestone mods.
 * Detects if common corpse mods are present and adjusts behavior accordingly.
 */
public class CorpseModCompat {
    private static final Set<String> KNOWN_CORPSE_MODS = new HashSet<>();
    private static Boolean corpseModDetected = null;
    
    static {
        // Add known corpse/gravestone mod IDs
        KNOWN_CORPSE_MODS.add("corpse");           // henkelmax's Corpse mod
        KNOWN_CORPSE_MODS.add("gravestones");      // Gravestones mod
        KNOWN_CORPSE_MODS.add("gravestone");       // GraveStone Mod
        KNOWN_CORPSE_MODS.add("tombstone");        // Corail Tombstone
        KNOWN_CORPSE_MODS.add("yigd");             // You're in Grave Danger
        KNOWN_CORPSE_MODS.add("bodycorpse");       // Body Corpse
        KNOWN_CORPSE_MODS.add("vanilladeathchest"); // VanillaDeathChest
    }
    
    /**
     * Check if any known corpse mod is loaded
     */
    public static boolean isCorpseModPresent() {
        if (corpseModDetected == null) {
            corpseModDetected = KNOWN_CORPSE_MODS.stream()
                .anyMatch(Services.PLATFORM::isModLoaded);
            
            if (corpseModDetected) {
                BrecherDimensions.LOGGER.info("Detected corpse/gravestone mod - adjusting keepInventory behavior");
            }
        }
        return corpseModDetected;
    }
    
    /**
     * Check if we should handle inventory keeping for this death
     */
    public static boolean shouldHandleInventory(ServerPlayer player) {
        // If configured to defer to corpse mods and one is present, let it handle the inventory
        if (BrecherConfig.isDeferToCorpseMods() && isCorpseModPresent()) {
            BrecherDimensions.LOGGER.debug("Corpse mod detected - deferring inventory handling");
            return false;
        }
        
        // Check if the player still has items (another mod might have already handled them)
        boolean hasItems = false;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (!player.getInventory().getItem(i).isEmpty()) {
                hasItems = true;
                break;
            }
        }
        
        if (!hasItems) {
            BrecherDimensions.LOGGER.debug("Player inventory already empty - another mod likely handled death");
            return false;
        }
        
        return true;
    }
    
    /**
     * Add a mod ID to the corpse mod detection list
     */
    public static void addCorpseModId(String modId) {
        KNOWN_CORPSE_MODS.add(modId);
        corpseModDetected = null; // Reset detection cache
    }
}