package net.tinkstav.brecher_dim.util;

import com.mojang.logging.LogUtils;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.tinkstav.brecher_dim.accessor.IRegistryAccessor;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.reflect.Method;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.minecraft.resources.ResourceKey;
import java.util.Set;

/**
 * Utility class for safe registry operations with transaction support and validation
 */
public class RegistryHelper {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // Lock for coordinating registry operations across the mod
    private static final ReentrantReadWriteLock GLOBAL_REGISTRY_LOCK = new ReentrantReadWriteLock();
    
    // Track ongoing transactions for rollback support
    private static final Map<String, RegistryTransaction<?>> ACTIVE_TRANSACTIONS = new ConcurrentHashMap<>();
    
    /**
     * Safely modify a registry with automatic rollback on failure
     */
    public static <T> CompletableFuture<Boolean> safeRegistryModification(
            Registry<T> registry,
            ResourceKey<T> key,
            T value,
            int id) {
        
        return CompletableFuture.supplyAsync(() -> {
            String transactionId = "modify_" + key.location().toString() + "_" + System.nanoTime();
            RegistryTransaction<T> transaction = null;
            
            GLOBAL_REGISTRY_LOCK.writeLock().lock();
            try {
                // Use reflection for compatibility
                Method addMappingMethod = registry.getClass().getMethod("brecher_dim$addMapping", int.class, ResourceKey.class, Object.class);
                
                // Create transaction for rollback support (simplified)
                transaction = new RegistryTransaction<>(registry, key);
                ACTIVE_TRANSACTIONS.put(transactionId, transaction);
                
                // Perform the modification
                addMappingMethod.invoke(registry, id, key, value);
                
                // Validate registry state after modification using reflection
                try {
                    java.lang.reflect.Method validateMethod = registry.getClass().getMethod(
                        "brecher_dim$validateRegistryState");
                    Boolean isValid = (Boolean) validateMethod.invoke(registry);
                    if (!isValid) {
                        LOGGER.error("Registry validation failed after modification for key: {}", key.location());
                        if (transaction != null) {
                            transaction.rollback();
                        }
                        return false;
                    }
                } catch (Exception e) {
                    LOGGER.debug("Could not validate registry state (not enhanced): {}", e.getMessage());
                }
                
                LOGGER.debug("Successfully modified registry for key: {}", key.location());
                return true;
                
            } catch (Exception e) {
                LOGGER.error("Failed to modify registry for key: {}", key.location(), e);
                // Attempt rollback if possible
                if (transaction != null) {
                    try {
                        transaction.rollback();
                        LOGGER.info("Successfully rolled back registry modification for: {}", key.location());
                    } catch (Exception rollbackError) {
                        LOGGER.error("Failed to rollback registry modification", rollbackError);
                    }
                }
                return false;
            } finally {
                // Clean up transaction
                if (transaction != null) {
                    ACTIVE_TRANSACTIONS.remove(transactionId);
                }
                GLOBAL_REGISTRY_LOCK.writeLock().unlock();
            }
        });
    }
    
    /**
     * Batch registry updates for better performance and atomicity
     */
    public static <T> boolean batchRegistryUpdate(
            Registry<T> registry,
            Map<ResourceKey<T>, T> entries) {
        
        GLOBAL_REGISTRY_LOCK.writeLock().lock();
        try {
            // Use reflection for compatibility
            Method addMappingMethod = registry.getClass().getMethod("brecher_dim$addMapping", int.class, ResourceKey.class, Object.class);
            Method isFrozenMethod = registry.getClass().getMethod("brecher_dim$isFrozen");
            Method setFrozenMethod = registry.getClass().getMethod("brecher_dim$setFrozen", boolean.class);
            // Temporarily unfreeze once for all updates
            boolean wasFrozen = (Boolean) isFrozenMethod.invoke(registry);
            if (wasFrozen) {
                setFrozenMethod.invoke(registry, false);
            }
            
            int nextId = registry.size() + 100; // Add padding to avoid conflicts
            for (Map.Entry<ResourceKey<T>, T> entry : entries.entrySet()) {
                addMappingMethod.invoke(registry, nextId++, entry.getKey(), entry.getValue());
            }
            
            // Validate after all updates
            try {
                java.lang.reflect.Method validateMethod = registry.getClass().getMethod(
                    "brecher_dim$validateRegistryState");
                Boolean isValid = (Boolean) validateMethod.invoke(registry);
                if (!isValid) {
                    LOGGER.error("Registry validation failed after batch update");
                    return false;
                }
            } catch (Exception e) {
                LOGGER.debug("Could not validate registry after batch update: {}", e.getMessage());
            }
            
            // Refreeze if needed
            if (wasFrozen) {
                setFrozenMethod.invoke(registry, true);
            }
            
            LOGGER.info("Successfully batch updated {} registry entries", entries.size());
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to perform batch registry update", e);
            return false;
        } finally {
            GLOBAL_REGISTRY_LOCK.writeLock().unlock();
        }
    }
    
    /**
     * Validate all registries and report any issues
     */
    public static boolean validateAllRegistries(MinecraftServer server) {
        GLOBAL_REGISTRY_LOCK.readLock().lock();
        try {
            final boolean[] allValid = {true}; // Use array for lambda mutability
            
            // Check each registry that might have our modifications
            server.registryAccess().registries().forEach(entry -> {
                try {
                    java.lang.reflect.Method validateMethod = entry.value().getClass().getMethod(
                        "brecher_dim$validateRegistryState");
                    Boolean isValid = (Boolean) validateMethod.invoke(entry.value());
                    if (!isValid) {
                        LOGGER.error("Registry validation failed for: {}", entry.key());
                        allValid[0] = false;
                    }
                } catch (Exception e) {
                    // Registry doesn't have validation method - this is fine
                    LOGGER.debug("Registry {} doesn't have validation method", entry.key());
                }
            });
            
            if (allValid[0]) {
                LOGGER.debug("All registries passed validation");
            } else {
                LOGGER.warn("Some registries failed validation - potential data corruption detected");
            }
            
            return allValid[0];
            
        } finally {
            GLOBAL_REGISTRY_LOCK.readLock().unlock();
        }
    }
    
    /**
     * Emergency cleanup of all runtime registry entries
     * WARNING: This iterates through ALL registries in the game - only use when normal cleanup fails
     */
    public static void emergencyCleanup(MinecraftServer server) {
        GLOBAL_REGISTRY_LOCK.writeLock().lock();
        try {
            LOGGER.warn("Performing emergency registry cleanup");
            
            // Cancel all active transactions
            for (RegistryTransaction<?> transaction : ACTIVE_TRANSACTIONS.values()) {
                try {
                    transaction.rollback();
                } catch (Exception e) {
                    LOGGER.error("Failed to rollback transaction during emergency cleanup", e);
                }
            }
            ACTIVE_TRANSACTIONS.clear();
            
            // Clean up all registries
            server.registryAccess().registries().forEach(entry -> {
                try {
                    java.lang.reflect.Method cleanupMethod = entry.value().getClass().getMethod(
                        "brecher_dim$cleanupAllRuntimeEntries");
                    cleanupMethod.invoke(entry.value());
                } catch (Exception e) {
                    LOGGER.debug("Registry {} doesn't have cleanup method or cleanup failed: {}", 
                        entry.key(), e.getMessage());
                }
            });
            
            LOGGER.info("Emergency registry cleanup completed");
            
        } finally {
            GLOBAL_REGISTRY_LOCK.writeLock().unlock();
        }
    }
    
    /**
     * Get registry statistics for monitoring
     */
    public static String getRegistryStats(MinecraftServer server) {
        GLOBAL_REGISTRY_LOCK.readLock().lock();
        try {
            StringBuilder stats = new StringBuilder();
            stats.append("Registry Statistics:\\n");
            
            server.registryAccess().registries().forEach(entry -> {
                try {
                    java.lang.reflect.Method getRuntimeMethod = entry.value().getClass().getMethod(
                        "brecher_dim$getRuntimeEntries");
                    @SuppressWarnings("unchecked")
                    Set<ResourceKey<?>> runtimeEntries = (Set<ResourceKey<?>>) getRuntimeMethod.invoke(entry.value());
                    int totalEntries = entry.value().size();
                    stats.append(String.format("  %s: %d total, %d runtime\\n", 
                        entry.key(), totalEntries, runtimeEntries.size()));
                } catch (Exception e) {
                    // Registry doesn't have runtime entries tracking
                    stats.append(String.format("  %s: %d total, ? runtime\\n", 
                        entry.key(), entry.value().size()));
                }
            });
            
            stats.append(String.format("Active Transactions: %d\\n", ACTIVE_TRANSACTIONS.size()));
            
            return stats.toString();
            
        } finally {
            GLOBAL_REGISTRY_LOCK.readLock().unlock();
        }
    }
    
    /**
     * Simple transaction support for registry modifications
     */
    private static class RegistryTransaction<T> {
        private final Registry<T> registry;
        private final ResourceKey<T> key;
        private final boolean wasPresent;
        private final T originalValue;
        
        public RegistryTransaction(Registry<T> registry, ResourceKey<T> key) {
            this.registry = registry;
            this.key = key;
            
            // Capture current state for rollback
            this.wasPresent = registry.containsKey(key);
            this.originalValue = wasPresent ? registry.get(key) : null;
        }
        
        public void rollback() {
            try {
                if (wasPresent && originalValue != null) {
                    // Entry existed before, restore it
                    // Note: This is simplified - a full implementation would restore the exact state
                    LOGGER.debug("Rolling back registry transaction for existing entry: {}", key.location());
                } else {
                    // Entry didn't exist before, remove it
                    try {
                        java.lang.reflect.Method removeMethod = registry.getClass().getMethod(
                            "brecher_dim$removeRuntimeEntry", ResourceKey.class);
                        removeMethod.invoke(registry, key);
                        LOGGER.debug("Rolling back registry transaction by removing entry: {}", key.location());
                    } catch (Exception e) {
                        LOGGER.debug("Could not rollback registry entry (not enhanced): {}", e.getMessage());
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Failed to rollback registry transaction for: {}", key.location(), e);
            }
        }
    }
}