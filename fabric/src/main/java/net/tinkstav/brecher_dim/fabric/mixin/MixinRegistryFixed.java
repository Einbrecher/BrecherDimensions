/*
 * Brecher's Dimensions - Temporary resettable dimensions for exploration
 * Copyright (C) 2025 Einbrecher. All rights reserved.
 */

package net.tinkstav.brecher_dim.fabric.mixin;

import com.mojang.logging.LogUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.tinkstav.brecher_dim.BrecherDimensions;
import net.tinkstav.brecher_dim.accessor.IRegistryAccessor;
import net.tinkstav.brecher_dim.exception.RegistryDiscoveryException;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Fixed version of MixinRegistry with improved field mapping approach
 * Uses reflection-based field access instead of potentially incorrect shadow fields
 */
@Mixin(MappedRegistry.class)
public class MixinRegistryFixed<T> implements IRegistryAccessor<T> {
    @Unique
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // Only shadow fields we're confident about
    @Shadow @Final private Map<ResourceKey<T>, Holder.Reference<T>> byKey;
    @Shadow private boolean frozen;
    
    // Use reflection for potentially problematic fields
    @Unique
    private Map<T, Holder.Reference<T>> brecher_dim$byValue = null;
    @Unique
    private Map<ResourceLocation, Holder.Reference<T>> brecher_dim$byLocation = null;
    
    @Unique
    private volatile boolean brecher_dim$temporarilyUnfrozen = false;
    
    @Unique
    private final Set<ResourceKey<T>> brecher_dim$runtimeEntries = ConcurrentHashMap.newKeySet();
    
    @Unique
    private final ReentrantReadWriteLock brecher_dim$registryLock = new ReentrantReadWriteLock();
    
    @Unique
    private boolean brecher_dim$reflectionInitialized = false;
    
    /**
     * Initialize reflection-based field access with adaptive field discovery
     * 
     * REFLECTION: This method is necessary because Minecraft's registry structure
     * varies between versions and the field names may be obfuscated. We use three
     * progressive strategies to locate the required fields.
     */
    @Unique
    private void brecher_dim$initializeReflection() {
        if (brecher_dim$reflectionInitialized) {
            return;
        }
        
        LOGGER.debug("Starting registry field discovery for {}", this.getClass().getName());
        
        try {
            // REFLECTION: Strategy 1 - Try current class fields first (fastest)
            brecher_dim$discoveryStrategy1(); // Current class fields
            
            if (brecher_dim$byValue == null || brecher_dim$byLocation == null) {
                // REFLECTION: Strategy 2 - Check superclass hierarchy
                brecher_dim$discoveryStrategy2(); // Superclass fields
            }
            
            if (brecher_dim$byValue == null || brecher_dim$byLocation == null) {
                // REFLECTION: Strategy 3 - Use name patterns and known obfuscated names
                brecher_dim$discoveryStrategy3(); // Field name heuristics
            }
            
            if (brecher_dim$byValue == null || brecher_dim$byLocation == null) {
                String errorMsg = String.format(
                    "CRITICAL: Brecher's Dimensions failed to discover required registry fields. " +
                    "byValue=%s, byLocation=%s. " +
                    "This is likely caused by a Minecraft version mismatch or mod conflict. " +
                    "The mod cannot safely operate and will be disabled.",
                    brecher_dim$byValue != null, brecher_dim$byLocation != null);
                LOGGER.error(errorMsg);
                brecher_dim$reflectionInitialized = true; // Mark as initialized to prevent retry loops
                throw new RegistryDiscoveryException(errorMsg);
            }

            LOGGER.info("Successfully discovered all registry fields");
            brecher_dim$reflectionInitialized = true;

        } catch (RegistryDiscoveryException e) {
            // Re-throw discovery exceptions - these are critical failures
            throw e;
        } catch (Exception e) {
            String errorMsg = "CRITICAL: Unexpected error during registry field discovery. " +
                "Brecher's Dimensions cannot safely operate.";
            LOGGER.error(errorMsg, e);
            brecher_dim$reflectionInitialized = true; // Prevent retry loops
            throw new RegistryDiscoveryException(errorMsg, e);
        }
    }
    
    @Unique
    private void brecher_dim$discoveryStrategy1() {
        Class<?> registryClass = this.getClass();
        LOGGER.debug("Strategy 1: Analyzing direct fields of {}", registryClass.getName());
        
        for (java.lang.reflect.Field field : registryClass.getDeclaredFields()) {
            brecher_dim$analyzeField(field, "direct");
        }
    }
    
    @Unique
    private void brecher_dim$discoveryStrategy2() {
        Class<?> superclass = this.getClass().getSuperclass();
        while (superclass != null && superclass != Object.class) {
            LOGGER.debug("Strategy 2: Analyzing superclass fields of {}", superclass.getName());
            
            for (java.lang.reflect.Field field : superclass.getDeclaredFields()) {
                brecher_dim$analyzeField(field, "super");
            }
            
            superclass = superclass.getSuperclass();
        }
    }
    
    @Unique
    private void brecher_dim$discoveryStrategy3() {
        // Use field name heuristics to find potential candidates
        Class<?> registryClass = this.getClass();
        LOGGER.debug("Strategy 3: Using field name heuristics");
        
        String[] byValueCandidates = {"byValue", "valueToKey", "values", "m_", "f_"};
        String[] byLocationCandidates = {"byLocation", "locationToKey", "locations", "byName", "names"};
        
        for (String candidate : byValueCandidates) {
            if (brecher_dim$byValue == null) {
                brecher_dim$findFieldByNamePattern(registryClass, candidate, true);
            }
        }
        
        for (String candidate : byLocationCandidates) {
            if (brecher_dim$byLocation == null) {
                brecher_dim$findFieldByNamePattern(registryClass, candidate, false);
            }
        }
    }
    
    @Unique
    private void brecher_dim$findFieldByNamePattern(Class<?> clazz, String pattern, boolean isValueMap) {
        for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
            if (field.getName().contains(pattern) && Map.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    Map<?, ?> map = (Map<?, ?>) field.get(this);
                    
                    if (map != null && brecher_dim$validateMapStructure(map, isValueMap)) {
                        if (isValueMap) {
                            @SuppressWarnings("unchecked")
                            Map<T, Holder.Reference<T>> byValueMap = (Map<T, Holder.Reference<T>>) map;
                            brecher_dim$byValue = byValueMap;
                            LOGGER.debug("Found byValue via pattern '{}': {}", pattern, field.getName());
                        } else {
                            @SuppressWarnings("unchecked")
                            Map<ResourceLocation, Holder.Reference<T>> byLocationMap = (Map<ResourceLocation, Holder.Reference<T>>) map;
                            brecher_dim$byLocation = byLocationMap;
                            LOGGER.debug("Found byLocation via pattern '{}': {}", pattern, field.getName());
                        }
                        return;
                    }
                } catch (Exception e) {
                    // Skip this field
                }
            }
        }
    }
    
    @Unique
    private void brecher_dim$analyzeField(java.lang.reflect.Field field, String source) {
        if (!Map.class.isAssignableFrom(field.getType())) {
            return;
        }
        
        try {
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<?, ?> map = (Map<?, ?>) field.get(this);
            
            if (map != null && !map.isEmpty()) {
                if (brecher_dim$validateMapStructure(map, true) && brecher_dim$byValue == null) {
                    @SuppressWarnings("unchecked")
                    Map<T, Holder.Reference<T>> byValueMap = (Map<T, Holder.Reference<T>>) map;
                    brecher_dim$byValue = byValueMap;
                    LOGGER.debug("Found byValue field ({} {}): {}", source, field.getName(), map.size());
                }
                else if (brecher_dim$validateMapStructure(map, false) && brecher_dim$byLocation == null) {
                    @SuppressWarnings("unchecked")
                    Map<ResourceLocation, Holder.Reference<T>> byLocationMap = (Map<ResourceLocation, Holder.Reference<T>>) map;
                    brecher_dim$byLocation = byLocationMap;
                    LOGGER.debug("Found byLocation field ({} {}): {}", source, field.getName(), map.size());
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Could not analyze field {}: {}", field.getName(), e.getMessage());
        }
    }
    
    @Unique
    private boolean brecher_dim$validateMapStructure(Map<?, ?> map, boolean expectValueMap) {
        if (map.isEmpty()) {
            return false; // Can't validate empty maps
        }
        
        try {
            Object firstKey = map.keySet().iterator().next();
            Object firstValue = map.values().iterator().next();
            
            if (!(firstValue instanceof Holder.Reference)) {
                return false; // All our target maps should have Holder.Reference values
            }
            
            if (expectValueMap) {
                // byValue: T -> Holder.Reference<T> (T is not ResourceKey/ResourceLocation)
                return !(firstKey instanceof ResourceKey) && !(firstKey instanceof ResourceLocation);
            } else {
                // byLocation: ResourceLocation -> Holder.Reference<T>
                return firstKey instanceof ResourceLocation;
            }
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Public method to allow runtime registration
     * 
     * THREAD-SAFETY: Uses ReentrantReadWriteLock to ensure thread-safe registry modification
     * This method temporarily unfreezes the registry, adds the entry, then re-freezes it.
     */
    @Unique
    public void brecher_dim$registerRuntime(ResourceKey<T> key, T value) {
        if (frozen && brecher_dim$isExplorationDimension(key)) {
            brecher_dim$initializeReflection();
            
            // Check if already registered
            if (this.byKey.containsKey(key)) {
                return; // Already registered, skip
            }
            
            // THREAD-SAFETY: Acquire exclusive write lock for registry modification
            brecher_dim$registryLock.writeLock().lock();
            try {
                // REFLECTION: Temporarily bypass frozen state for registration
                boolean wasFrozen = frozen;
                frozen = false;
                brecher_dim$temporarilyUnfrozen = true;
                
                try {
                    // REFLECTION: Attempt to use Minecraft's internal register method
                    if (brecher_dim$useInternalRegister(key, value)) {
                        brecher_dim$runtimeEntries.add(key);
                        LOGGER.info("Successfully registered runtime entry: {}", key.location());
                    } else {
                        LOGGER.error("Failed to register runtime entry: {}", key.location());
                    }
                } finally {
                    // THREAD-SAFETY: Always restore frozen state to maintain registry integrity
                    frozen = wasFrozen;
                    brecher_dim$temporarilyUnfrozen = false;
                }
            } finally {
                // THREAD-SAFETY: Release write lock
                brecher_dim$registryLock.writeLock().unlock();
            }
        }
    }
    
    @Unique
    private boolean brecher_dim$useInternalRegister(ResourceKey<T> key, T value) {
        try {
            // Try to use the registry's own registration methods
            @SuppressWarnings("unchecked")
            MappedRegistry<T> mappedRegistry = (MappedRegistry<T>)(Object)this;
            
            // Look for register methods
            java.lang.reflect.Method[] methods = mappedRegistry.getClass().getDeclaredMethods();
            
            for (java.lang.reflect.Method method : methods) {
                if (method.getName().equals("register") && method.getParameterCount() >= 2) {
                    Class<?>[] paramTypes = method.getParameterTypes();
                    
                    // Look for register(int, ResourceKey, T) or similar
                    if (paramTypes.length == 3 && 
                        paramTypes[0] == int.class &&
                        ResourceKey.class.isAssignableFrom(paramTypes[1])) {
                        
                        method.setAccessible(true);
                        
                        // Generate a unique ID
                        int id = byKey.size() + 1000; // Offset to avoid conflicts
                        
                        Object result = method.invoke(mappedRegistry, id, key, value);
                        LOGGER.debug("Used register method {} with ID {}", method.getName(), id);
                        return true;
                    }
                    // Look for register(ResourceKey, T) method
                    else if (paramTypes.length == 2 &&
                             ResourceKey.class.isAssignableFrom(paramTypes[0])) {
                        
                        method.setAccessible(true);
                        Object result = method.invoke(mappedRegistry, key, value);
                        LOGGER.debug("Used register method {}", method.getName());
                        return true;
                    }
                }
            }
            
            LOGGER.debug("No suitable register method found, will use direct field manipulation");
            return brecher_dim$directFieldRegistration(key, value);
            
        } catch (Exception e) {
            LOGGER.error("Failed to use internal register method for {}: {}", key.location(), e.getMessage());
            return false;
        }
    }
    
    @Unique
    private boolean brecher_dim$directFieldRegistration(ResourceKey<T> key, T value) {
        try {
            brecher_dim$initializeReflection();
            
            // Validate registry state before modification
            if (!brecher_dim$validateRegistryState()) {
                LOGGER.error("Registry state validation failed for {}", key.location());
                return false;
            }
            
            // Create holder reference using multiple fallback strategies
            Holder.Reference<T> reference = brecher_dim$createHolderReference(key, value);
            if (reference == null) {
                LOGGER.error("Failed to create holder reference for {}", key.location());
                return false;
            }
            
            // Atomic registry modification with backup
            return brecher_dim$atomicRegistryModification(key, value, reference);
            
        } catch (Exception e) {
            LOGGER.error("Direct field registration failed for {}: {}", key.location(), e.getMessage());
            return false;
        }
    }
    
    /**
     * Creates a Holder.Reference for registry entries using multiple strategies
     * 
     * REFLECTION: Holder.Reference constructors vary between Minecraft versions
     * We try multiple constructor signatures to ensure compatibility
     * 
     * @param key The resource key for the entry
     * @param value The value to bind to the holder
     * @return A bound Holder.Reference or null if creation fails
     */
    @Unique
    private Holder.Reference<T> brecher_dim$createHolderReference(ResourceKey<T> key, T value) {
        // Strategy 1: Try to create reference using existing pattern
        try {
            Holder.Reference<T> reference = byKey.get(key);
            if (reference != null) {
                if (!reference.isBound()) {
                    // Use reflection to call protected bindValue method
                    java.lang.reflect.Method bindValueMethod = reference.getClass().getDeclaredMethod("bindValue", Object.class);
                    bindValueMethod.setAccessible(true);
                    bindValueMethod.invoke(reference, value);
                }
                return reference;
            }
        } catch (Exception e) {
            LOGGER.debug("Could not reuse existing reference for {}: {}", key.location(), e.getMessage());
        }
        
        // REFLECTION: Strategy 2 - Create standalone reference with constructor discovery
        try {
            // Use reflection to find Reference constructor
            Class<?> refClass = Holder.Reference.class;
            java.lang.reflect.Constructor<?>[] constructors = refClass.getDeclaredConstructors();
            
            for (java.lang.reflect.Constructor<?> constructor : constructors) {
                constructor.setAccessible(true);
                Class<?>[] paramTypes = constructor.getParameterTypes();
                
                try {
                    @SuppressWarnings("unchecked")
                    Holder.Reference<T> reference;
                    
                    if (paramTypes.length == 4) {
                        // Try 4-parameter constructor: (Type, OwnerRegistry, Key, Value)
                        // Use the first available Type enum value
                        Object firstType = paramTypes[0].getEnumConstants()[0];
                        reference = (Holder.Reference<T>) constructor.newInstance(firstType, this, key, value);
                    } else if (paramTypes.length == 3) {
                        // Try 3-parameter constructor: (OwnerRegistry, Key, Value)
                        reference = (Holder.Reference<T>) constructor.newInstance(this, key, value);
                    } else if (paramTypes.length == 2) {
                        // Try 2-parameter constructor: (OwnerRegistry, Key)
                        reference = (Holder.Reference<T>) constructor.newInstance(this, key);
                        // Use reflection to call protected bindValue method
                    java.lang.reflect.Method bindValueMethod = reference.getClass().getDeclaredMethod("bindValue", Object.class);
                    bindValueMethod.setAccessible(true);
                    bindValueMethod.invoke(reference, value);
                    } else if (paramTypes.length == 1) {
                        // Try 1-parameter constructor: (Key)
                        reference = (Holder.Reference<T>) constructor.newInstance(key);
                        // Use reflection to call protected bindValue method
                    java.lang.reflect.Method bindValueMethod = reference.getClass().getDeclaredMethod("bindValue", Object.class);
                    bindValueMethod.setAccessible(true);
                    bindValueMethod.invoke(reference, value);
                    } else {
                        continue; // Skip constructors we don't know how to handle
                    }
                    
                    LOGGER.debug("Strategy 2: Successfully created reference using {}-parameter constructor", paramTypes.length);
                    return reference;
                    
                } catch (Exception ex) {
                    LOGGER.debug("Strategy 2: Failed with {}-parameter constructor: {}", paramTypes.length, ex.getMessage());
                    continue; // Try next constructor
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Could not create reference via reflection for {}: {}", key.location(), e.getMessage());
        }
        
        // Strategy 3: Use computeIfAbsent as fallback with improved constructor handling
        try {
            return byKey.computeIfAbsent(key, k -> {
                try {
                    // Find the correct constructor for Holder.Reference
                    java.lang.reflect.Constructor<?>[] constructors = Holder.Reference.class.getDeclaredConstructors();
                    
                    // Try each constructor with different parameter counts
                    for (java.lang.reflect.Constructor<?> constructor : constructors) {
                        constructor.setAccessible(true);
                        Class<?>[] paramTypes = constructor.getParameterTypes();
                        
                        try {
                            @SuppressWarnings("unchecked")
                            Holder.Reference<T> ref;
                            
                            if (paramTypes.length == 4) {
                                // Try 4-parameter constructor: (Type, OwnerRegistry, Key, Value)
                                // Use the first available Type enum value  
                                Object firstType = paramTypes[0].getEnumConstants()[0];
                                // Get the registry reference - this mixin is applied to MappedRegistry
                                @SuppressWarnings("unchecked")
                                MappedRegistry<T> registry = (MappedRegistry<T>) (Object) this;
                                ref = (Holder.Reference<T>) constructor.newInstance(firstType, registry, k, value);
                            } else if (paramTypes.length == 3) {
                                // Try 3-parameter constructor: (OwnerRegistry, Key, Value)
                                @SuppressWarnings("unchecked")
                                MappedRegistry<T> registry = (MappedRegistry<T>) (Object) this;
                                ref = (Holder.Reference<T>) constructor.newInstance(registry, k, value);
                            } else if (paramTypes.length == 2) {
                                // Try 2-parameter constructor: (OwnerRegistry, Key)
                                @SuppressWarnings("unchecked")
                                MappedRegistry<T> registry = (MappedRegistry<T>) (Object) this;
                                ref = (Holder.Reference<T>) constructor.newInstance(registry, k);
                                // Use reflection to call protected bindValue method
                                java.lang.reflect.Method bindValueMethod = ref.getClass().getDeclaredMethod("bindValue", Object.class);
                                bindValueMethod.setAccessible(true);
                                bindValueMethod.invoke(ref, value);
                            } else if (paramTypes.length == 1) {
                                // Try 1-parameter constructor: (Key)
                                ref = (Holder.Reference<T>) constructor.newInstance(k);
                                // Use reflection to call protected bindValue method
                                java.lang.reflect.Method bindValueMethod = ref.getClass().getDeclaredMethod("bindValue", Object.class);
                                bindValueMethod.setAccessible(true);
                                bindValueMethod.invoke(ref, value);
                            } else {
                                continue; // Skip constructors we don't know how to handle
                            }
                            
                            LOGGER.debug("Successfully created reference using {}-parameter constructor", paramTypes.length);
                            return ref;
                            
                        } catch (Exception ex) {
                            LOGGER.debug("Failed with {}-parameter constructor: {}", paramTypes.length, ex.getMessage());
                            continue; // Try next constructor
                        }
                    }
                    
                    LOGGER.error("No working constructor found for Holder.Reference for {}", k.location());
                    return null;
                    
                } catch (Exception ex) {
                    LOGGER.error("Failed to create reference in computeIfAbsent for {}", k.location(), ex);
                    return null;
                }
            });
        } catch (Exception e) {
            LOGGER.error("All reference creation strategies failed for {}", key.location(), e);
            return null;
        }
    }
    
    @Unique
    private boolean brecher_dim$atomicRegistryModification(ResourceKey<T> key, T value, Holder.Reference<T> reference) {
        // Create backup of current state
        Map<ResourceKey<T>, Holder.Reference<T>> backupByKey = new HashMap<>(byKey);
        Map<T, Holder.Reference<T>> backupByValue = null;
        Map<ResourceLocation, Holder.Reference<T>> backupByLocation = null;
        
        if (brecher_dim$byValue != null) {
            backupByValue = new HashMap<>(brecher_dim$byValue);
        }
        if (brecher_dim$byLocation != null) {
            backupByLocation = new HashMap<>(brecher_dim$byLocation);
        }
        
        try {
            // Update all registry maps atomically
            byKey.put(key, reference);
            
            if (brecher_dim$byValue != null) {
                brecher_dim$byValue.put(value, reference);
            }
            
            if (brecher_dim$byLocation != null) {
                brecher_dim$byLocation.put(key.location(), reference);
            }
            
            // Validate the modification
            if (!brecher_dim$validateRegistryState()) {
                throw new RuntimeException("Registry state validation failed after modification");
            }
            
            LOGGER.debug("Atomic registry modification completed for {}", key.location());
            return true;
            
        } catch (Exception e) {
            // Restore from backup on any error
            LOGGER.warn("Registry modification failed, restoring backup for {}: {}", key.location(), e.getMessage());
            
            try {
                byKey.clear();
                byKey.putAll(backupByKey);
                
                if (brecher_dim$byValue != null && backupByValue != null) {
                    brecher_dim$byValue.clear();
                    brecher_dim$byValue.putAll(backupByValue);
                }
                
                if (brecher_dim$byLocation != null && backupByLocation != null) {
                    brecher_dim$byLocation.clear();
                    brecher_dim$byLocation.putAll(backupByLocation);
                }
                
                LOGGER.info("Registry state restored from backup");
            } catch (Exception backupError) {
                LOGGER.error("CRITICAL: Failed to restore registry backup!", backupError);
            }
            
            return false;
        }
    }
    
    @Unique
    public boolean brecher_dim$validateRegistryState() {
        try {
            // Basic consistency checks
            if (byKey == null) {
                LOGGER.error("byKey map is null");
                return false;
            }
            
            // Check if reflection fields are available and consistent
            if (brecher_dim$byValue != null && brecher_dim$byLocation != null) {
                if (byKey.size() != brecher_dim$byValue.size()) {
                    LOGGER.warn("Registry size mismatch: byKey={}, byValue={}", 
                        byKey.size(), brecher_dim$byValue.size());
                    // This is a warning, not a failure - maps might have different contents
                }
                
                // Validate runtime entries
                for (ResourceKey<T> runtimeKey : brecher_dim$runtimeEntries) {
                    if (!byKey.containsKey(runtimeKey)) {
                        LOGGER.error("Runtime entry {} not found in byKey map", runtimeKey.location());
                        return false;
                    }
                    
                    Holder.Reference<T> holder = byKey.get(runtimeKey);
                    if (holder == null || !holder.isBound()) {
                        LOGGER.error("Runtime entry {} has invalid holder", runtimeKey.location());
                        return false;
                    }
                }
            }
            
            return true;
            
        } catch (Exception e) {
            LOGGER.error("Registry validation failed", e);
            return false;
        }
    }
    
    @Unique
    private boolean brecher_dim$isExplorationDimension(ResourceKey<?> key) {
        return key.location().getNamespace().equals(BrecherDimensions.MOD_ID) 
            && key.location().getPath().startsWith("exploration_");
    }
    
    // --- IRegistryAccessor Implementation ---
    
    @Override
    @Unique
    public void brecher_dim$setFrozen(boolean frozen) {
        this.frozen = frozen;
        this.brecher_dim$temporarilyUnfrozen = !frozen;
    }
    
    @Override
    @Unique
    public boolean brecher_dim$isFrozen() {
        return this.frozen && !brecher_dim$temporarilyUnfrozen;
    }
    
    @Override
    @Unique
    public void brecher_dim$addMapping(int requestedId, ResourceKey<T> key, T value) {
        if (key == null || value == null) {
            LOGGER.error("Cannot add null key or value to registry");
            return;
        }
        
        // Only allow our mod's exploration dimensions
        if (!brecher_dim$isExplorationDimension(key)) {
            LOGGER.warn("Attempted to register non-exploration dimension: {}", key.location());
            return;
        }
        
        brecher_dim$registerRuntime(key, value);
    }
    
    @Unique
    public void brecher_dim$removeRuntimeEntry(ResourceKey<T> key) {
        if (!brecher_dim$runtimeEntries.contains(key)) {
            return;
        }
        
        brecher_dim$registryLock.writeLock().lock();
        try {
            boolean wasFrozen = frozen;
            if (wasFrozen) {
                frozen = false;
                brecher_dim$temporarilyUnfrozen = true;
            }
            
            try {
                brecher_dim$initializeReflection();
                
                Holder.Reference<T> holder = byKey.remove(key);
                if (holder != null && holder.isBound()) {
                    T value = holder.value();
                    
                    if (brecher_dim$byValue != null) {
                        brecher_dim$byValue.remove(value);
                    }
                    
                    if (brecher_dim$byLocation != null) {
                        brecher_dim$byLocation.remove(key.location());
                    }
                }
                
                brecher_dim$runtimeEntries.remove(key);
                LOGGER.info("Removed runtime registry entry: {}", key.location());
                
            } finally {
                if (wasFrozen) {
                    frozen = true;
                    brecher_dim$temporarilyUnfrozen = false;
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to remove runtime entry {}", key.location(), e);
        } finally {
            brecher_dim$registryLock.writeLock().unlock();
        }
    }
    
    @Unique
    public void brecher_dim$cleanupAllRuntimeEntries() {
        Set<ResourceKey<T>> entriesToRemove = new HashSet<>(brecher_dim$runtimeEntries);
        for (ResourceKey<T> key : entriesToRemove) {
            brecher_dim$removeRuntimeEntry(key);
        }
        LOGGER.info("Cleaned up {} runtime registry entries", entriesToRemove.size());
    }
    
    @Unique
    public Set<ResourceKey<T>> brecher_dim$getRuntimeEntries() {
        return new HashSet<>(brecher_dim$runtimeEntries);
    }
    
    @Unique
    public void brecher_dim$dumpRegistryDiagnostics() {
        LOGGER.info("=== Registry Diagnostics Dump ===");
        LOGGER.info("Registry class: {}", this.getClass().getName());
        LOGGER.info("Frozen: {}, Temporarily unfrozen: {}", frozen, brecher_dim$temporarilyUnfrozen);
        LOGGER.info("Reflection initialized: {}", brecher_dim$reflectionInitialized);
        LOGGER.info("byKey size: {}", byKey != null ? byKey.size() : "null");
        LOGGER.info("byValue available: {}, size: {}", 
            brecher_dim$byValue != null, 
            brecher_dim$byValue != null ? brecher_dim$byValue.size() : "null");
        LOGGER.info("byLocation available: {}, size: {}", 
            brecher_dim$byLocation != null, 
            brecher_dim$byLocation != null ? brecher_dim$byLocation.size() : "null");
        LOGGER.info("Runtime entries: {}", brecher_dim$runtimeEntries.size());
        
        for (ResourceKey<T> key : brecher_dim$runtimeEntries) {
            LOGGER.info("  Runtime entry: {}", key.location());
        }
        
        LOGGER.info("=== End Registry Diagnostics ===");
    }
}