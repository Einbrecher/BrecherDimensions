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

package net.tinkstav.brecher_dim.platform;

import net.tinkstav.brecher_dim.BrecherDimensions;
import java.util.ServiceLoader;

/**
 * Service loader for platform-specific implementations.
 * This allows the common module to access platform-specific functionality
 * without directly depending on platform code.
 */
public class Services {
    public static final PlatformHelper PLATFORM = load(PlatformHelper.class);
    public static final PacketHandler PACKETS = load(PacketHandler.class);
    public static final CommonEvents EVENTS = load(CommonEvents.class);
    public static final DimensionHelper DIMENSIONS = load(DimensionHelper.class);
    public static final TeleportHelper TELEPORT = load(TeleportHelper.class);
    public static final CommandHelper COMMANDS = load(CommandHelper.class);
    public static final ConfigHandler CONFIG = load(ConfigHandler.class);
    public static final ClientHandler CLIENT = loadOptional(ClientHandler.class);
    
    private static <T> T load(Class<T> clazz) {
        T service = ServiceLoader.load(clazz)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Failed to load service: " + clazz.getName()));
        BrecherDimensions.LOGGER.info("Loaded service: {} -> {}", clazz.getSimpleName(), service.getClass().getName());
        return service;
    }
    
    private static <T> T loadOptional(Class<T> clazz) {
        return ServiceLoader.load(clazz)
            .findFirst()
            .orElse(null);
    }
    
    private Services() {
        // Prevent instantiation
    }
}