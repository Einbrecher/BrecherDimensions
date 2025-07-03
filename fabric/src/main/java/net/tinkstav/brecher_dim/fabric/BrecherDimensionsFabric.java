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

package net.tinkstav.brecher_dim.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.tinkstav.brecher_dim.BrecherDimensions;
import net.tinkstav.brecher_dim.platform.Services;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class BrecherDimensionsFabric implements ModInitializer {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Brecher's Dimensions for Fabric");
        
        // Initialize common mod (this also initializes networking)
        BrecherDimensions.init();
        
        // Register events
        Services.EVENTS.registerEvents();
        
        // Register commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            Services.COMMANDS.registerCommands(dispatcher);
        });
        
        LOGGER.info("Brecher's Dimensions for Fabric initialized");
    }
}