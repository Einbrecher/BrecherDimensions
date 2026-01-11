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

package net.tinkstav.brecher_dim.exception;

/**
 * Exception thrown when registry field discovery via reflection fails.
 *
 * This indicates a critical failure that prevents the mod from safely operating.
 * Common causes include:
 * - Minecraft version mismatch (registry structure changed)
 * - Mod conflict (another mod modified registry internals)
 * - Obfuscation mapping changes
 *
 * When this exception is thrown, the mod should be disabled to prevent
 * undefined behavior or world corruption.
 */
public class RegistryDiscoveryException extends RuntimeException {

    public RegistryDiscoveryException(String message) {
        super(message);
    }

    public RegistryDiscoveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
