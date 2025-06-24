package net.tinkstav.brecher_dim.accessor;

import net.minecraft.resources.ResourceKey;
import java.util.Set;

/**
 * Accessor interface for Registry mixin methods
 * Provides safe access to registry manipulation functionality
 */
public interface IRegistryAccessor<T> {
    
    /**
     * Set the frozen state of the registry
     */
    void brecher_dim$setFrozen(boolean frozen);
    
    /**
     * Add a mapping to the registry with automatic ID allocation
     */
    void brecher_dim$addMapping(int id, ResourceKey<T> key, T value);
    
    /**
     * Check if the registry is frozen
     */
    boolean brecher_dim$isFrozen();
    
    /**
     * Register a runtime entry (exploration dimensions only)
     */
    void brecher_dim$registerRuntime(ResourceKey<T> key, T value);
    
    /**
     * Remove a runtime registry entry
     */
    void brecher_dim$removeRuntimeEntry(ResourceKey<T> key);
    
    /**
     * Clean up all runtime entries
     */
    void brecher_dim$cleanupAllRuntimeEntries();
    
    /**
     * Get all runtime entries
     */
    Set<ResourceKey<T>> brecher_dim$getRuntimeEntries();
    
    /**
     * Validate the current registry state
     */
    boolean brecher_dim$validateRegistryState();
    
    /**
     * Dump diagnostic information about the registry
     */
    void brecher_dim$dumpRegistryDiagnostics();
}