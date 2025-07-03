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

package net.tinkstav.brecher_dim.compat;

import net.tinkstav.brecher_dim.config.BrecherConfig;

/**
 * Helper for managing event priorities across platforms.
 * This allows us to ensure our death handler runs at the right time
 * relative to corpse mods.
 */
public class EventPriorityHelper {
    
    /**
     * Event priority levels that map to platform-specific values
     */
    public enum Priority {
        /**
         * Run before most other mods (high priority)
         * Use this to save inventory before corpse mods
         */
        HIGH,
        
        /**
         * Normal priority (default)
         */
        NORMAL,
        
        /**
         * Run after most other mods (low priority)
         * Use this to let corpse mods handle death first
         */
        LOW
    }
    
    /**
     * Get the appropriate priority for death event handling
     * based on current configuration and detected mods
     */
    public static Priority getDeathEventPriority() {
        // If we're deferring to corpse mods, use LOW priority so they run first
        if (BrecherConfig.isDeferToCorpseMods() && CorpseModCompat.isCorpseModPresent()) {
            return Priority.LOW;
        }
        
        // Otherwise use HIGH priority to ensure we save inventory before item drops
        return Priority.HIGH;
    }
}