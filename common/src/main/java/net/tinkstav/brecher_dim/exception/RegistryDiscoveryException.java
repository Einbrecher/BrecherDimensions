/*
 * Brecher's Dimensions - Temporary resettable dimensions for exploration
 * Copyright (C) 2025 Einbrecher. All rights reserved.
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
