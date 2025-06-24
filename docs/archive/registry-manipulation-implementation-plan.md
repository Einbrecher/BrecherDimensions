# Registry Manipulation Implementation Plan

## Overview
This document provides a step-by-step plan to implement proper registry manipulation for runtime dimension creation in the Brecher Dimensions mod for Minecraft 1.20.1.

## Current State
- Registry manipulation uses placeholder implementations that only log warnings
- Dimension creation works at a basic level but doesn't properly update registries
- Client synchronization is incomplete
- Missing proper ID allocation and Holder.Reference creation

## Implementation Plan

### Phase 1: Complete MixinRegistry Implementation

#### Step 1.1: Add Missing Shadow Fields
**File**: `src/main/java/net/tinkstav/brecher_dim/mixin/MixinRegistry.java`

Add these shadow fields:
```java
@Shadow private Map<T, Integer> toId;
@Shadow private List<Holder.Reference<T>> byId;
@Shadow private int nextId;
@Shadow @Final private Map<ResourceKey<T>, Holder.Reference<T>> byKey;
@Shadow @Final private Map<T, Holder.Reference<T>> byValue;
@Shadow @Final private Map<ResourceLocation, Holder.Reference<T>> byLocation;
@Shadow private Lifecycle elementsLifecycle;
```

#### Step 1.2: Implement Proper Registry ID Allocation
Create method to allocate IDs safely:
```java
@Unique
private int brecher_dim$allocateId() {
    // Find highest used ID
    int maxId = -1;
    if (toId != null && !toId.isEmpty()) {
        maxId = toId.values().stream().mapToInt(Integer::intValue).max().orElse(-1);
    }
    if (byId != null && !byId.isEmpty()) {
        maxId = Math.max(maxId, byId.size() - 1);
    }
    // Use next available ID
    return Math.max(maxId + 1, nextId);
}
```

#### Step 1.3: Implement Full brecher_dim$addMapping Method
Replace placeholder with full implementation:
```java
@Override
public void brecher_dim$addMapping(int requestedId, ResourceKey<T> key, T value) {
    // Validate inputs
    if (key == null || value == null) {
        LOGGER.error("Cannot add null key or value to registry");
        return;
    }
    
    // Check if already registered
    if (byKey != null && byKey.containsKey(key)) {
        LOGGER.warn("Key {} already registered, skipping", key.location());
        return;
    }
    
    boolean wasFrozen = frozen;
    if (wasFrozen) {
        frozen = false;
        brecher_dim$temporarilyUnfrozen = true;
    }
    
    try {
        // Allocate ID if not provided or if provided ID is already used
        int id = requestedId;
        if (id < 0 || (toId != null && toId.containsValue(id))) {
            id = brecher_dim$allocateId();
        }
        
        // Create holder reference
        Holder.Reference<T> reference = Holder.Reference.createStandAlone(
            this.asLookup(), key
        );
        reference.bindValue(value);
        
        // Update all registry maps atomically
        synchronized (this) {
            // Core registry maps
            if (byKey != null) byKey.put(key, reference);
            if (byValue != null) byValue.put(value, reference);
            if (byLocation != null) byLocation.put(key.location(), reference);
            
            // ID mappings
            if (toId != null) {
                toId.put(value, id);
            }
            
            // Ensure byId list is large enough
            if (byId != null) {
                while (byId.size() <= id) {
                    byId.add(null);
                }
                byId.set(id, reference);
            }
            
            // Update lifecycle
            if (elementsLifecycle != null) {
                elementsLifecycle = elementsLifecycle.add(Lifecycle.stable());
            }
            
            // Update next ID
            if (id >= nextId) {
                nextId = id + 1;
            }
        }
        
        LOGGER.info("Successfully registered {} with ID {} in registry", key.location(), id);
        
    } catch (Exception e) {
        LOGGER.error("Failed to add mapping for {}", key.location(), e);
    } finally {
        if (wasFrozen) {
            frozen = true;
            brecher_dim$temporarilyUnfrozen = false;
        }
    }
}
```

#### Step 1.4: Add Registry State Tracking
Add fields to track runtime additions:
```java
@Unique
private final Set<ResourceKey<T>> brecher_dim$runtimeEntries = new HashSet<>();

@Unique
public void brecher_dim$trackRuntimeEntry(ResourceKey<T> key) {
    brecher_dim$runtimeEntries.add(key);
}

@Unique
public void brecher_dim$cleanupRuntimeEntries() {
    // Remove all runtime entries
    for (ResourceKey<T> key : brecher_dim$runtimeEntries) {
        // Implementation for removal
    }
    brecher_dim$runtimeEntries.clear();
}
```

### Phase 2: Fix MinecraftServer Dimension Registration

#### Step 2.1: Implement Proper Registry Updates
**File**: `src/main/java/net/tinkstav/brecher_dim/mixin/MixinMinecraftServer.java`

Replace placeholder code (lines 91-110) with:
```java
// Register dimension type
Registry<DimensionType> dimTypeRegistry = registryAccess().registryOrThrow(Registries.DIMENSION_TYPE);
ResourceKey<DimensionType> dimTypeKey = ResourceKey.create(
    Registries.DIMENSION_TYPE, 
    dimensionKey.location()
);

// Check if already registered
if (!dimTypeRegistry.containsKey(dimTypeKey)) {
    if (dimTypeRegistry instanceof IRegistryAccessor<DimensionType> accessor) {
        int dimTypeId = dimTypeRegistry.size();
        accessor.brecher_dim$addMapping(dimTypeId, dimTypeKey, dimensionType);
        
        // Track for cleanup
        if (dimTypeRegistry instanceof MixinRegistry<DimensionType> mixinReg) {
            mixinReg.brecher_dim$trackRuntimeEntry(dimTypeKey);
        }
    } else {
        LOGGER.error("Cannot cast dimension type registry to accessor");
        return null;
    }
}

// Register level stem
Registry<LevelStem> stemRegistry = registryAccess().registryOrThrow(Registries.LEVEL_STEM);
ResourceKey<LevelStem> stemKey = ResourceKey.create(
    Registries.LEVEL_STEM, 
    dimensionKey.location()
);

if (!stemRegistry.containsKey(stemKey)) {
    if (stemRegistry instanceof IRegistryAccessor<LevelStem> accessor) {
        int stemId = stemRegistry.size();
        accessor.brecher_dim$addMapping(stemId, stemKey, levelStem);
        
        // Track for cleanup
        if (stemRegistry instanceof MixinRegistry<LevelStem> mixinReg) {
            mixinReg.brecher_dim$trackRuntimeEntry(stemKey);
        }
    } else {
        LOGGER.error("Cannot cast level stem registry to accessor");
        return null;
    }
}
```

#### Step 2.2: Add Registry Cleanup on Dimension Removal
In `brecher_dim$removeRuntimeDimension` method, add:
```java
// Clean up registry entries
Registry<DimensionType> dimTypeRegistry = registryAccess().registryOrThrow(Registries.DIMENSION_TYPE);
if (dimTypeRegistry instanceof MixinRegistry<DimensionType> mixinReg) {
    ResourceKey<DimensionType> dimTypeKey = ResourceKey.create(
        Registries.DIMENSION_TYPE, 
        dimensionKey.location()
    );
    // Implementation for removal would go here
}

Registry<LevelStem> stemRegistry = registryAccess().registryOrThrow(Registries.LEVEL_STEM);
if (stemRegistry instanceof MixinRegistry<LevelStem> mixinReg) {
    ResourceKey<LevelStem> stemKey = ResourceKey.create(
        Registries.LEVEL_STEM, 
        dimensionKey.location()
    );
    // Implementation for removal would go here
}
```

### Phase 3: Complete Client Synchronization

#### Step 3.1: Implement Client Registry Updates
**File**: `src/main/java/net/tinkstav/brecher_dim/mixin/client/MixinClientPacketListener.java`

Replace placeholder implementation (lines 107-110) with:
```java
@Unique
private void brecher_dim$updateClientDimensionRegistry(FriendlyByteBuf buffer) {
    int count = buffer.readVarInt();
    LOGGER.debug("Updating client dimension registry with {} entries", count);
    
    Registry<DimensionType> registry = registryAccess.registryOrThrow(Registries.DIMENSION_TYPE);
    
    for (int i = 0; i < count; i++) {
        ResourceLocation id = buffer.readResourceLocation();
        int registryId = buffer.readVarInt();
        CompoundTag tag = buffer.readNbt();
        
        if (tag != null) {
            DimensionType.DIRECT_CODEC.parse(NbtOps.INSTANCE, tag)
                .resultOrPartial(LOGGER::error)
                .ifPresent(dimType -> {
                    // Update client registry
                    ResourceKey<DimensionType> key = ResourceKey.create(Registries.DIMENSION_TYPE, id);
                    
                    if (registry instanceof IRegistryAccessor<DimensionType> accessor) {
                        accessor.brecher_dim$addMapping(registryId, key, dimType);
                        LOGGER.info("Registered dimension type {} with ID {} on client", id, registryId);
                    } else {
                        LOGGER.error("Cannot update client registry - not an accessor");
                    }
                });
        }
    }
}
```

#### Step 3.2: Enhance Server->Client Sync Packet
Update `BrecherNetworking.syncDimensionToPlayer` to include registry IDs:
```java
public static void syncDimensionToPlayer(ServerPlayer player, ServerLevel level) {
    // Include registry ID in sync packet
    Registry<DimensionType> registry = level.getServer().registryAccess()
        .registryOrThrow(Registries.DIMENSION_TYPE);
    
    Integer dimTypeId = null;
    if (registry instanceof MixinRegistry<?>) {
        // Get ID from registry's toId map
        // Implementation needed
    }
    
    // Build packet with ID information
    FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
    buffer.writeResourceLocation(level.dimension().location());
    buffer.writeVarInt(dimTypeId != null ? dimTypeId : -1);
    // ... rest of packet data
}
```

### Phase 4: Safety and Error Handling

#### Step 4.1: Add Registry Validation
Create validation methods:
```java
@Unique
private boolean brecher_dim$validateRegistryState() {
    // Check registry consistency
    if (byKey.size() != byValue.size()) {
        LOGGER.error("Registry inconsistency detected: key/value size mismatch");
        return false;
    }
    
    // Validate ID mappings
    if (toId != null && byId != null) {
        for (Map.Entry<T, Integer> entry : toId.entrySet()) {
            int id = entry.getValue();
            if (id >= byId.size() || byId.get(id) == null) {
                LOGGER.error("Invalid ID mapping: {} -> {}", entry.getKey(), id);
                return false;
            }
        }
    }
    
    return true;
}
```

#### Step 4.2: Add Transaction Support
Implement rollback capability:
```java
@Unique
private static class RegistryTransaction<T> {
    private final Map<ResourceKey<T>, Holder.Reference<T>> oldByKey;
    private final Map<T, Integer> oldToId;
    private final List<Holder.Reference<T>> oldById;
    private final int oldNextId;
    
    // Constructor saves current state
    // rollback() method restores saved state
}
```

### Phase 5: Testing Implementation

#### Step 5.1: Unit Tests
Create tests for:
- Registry ID allocation
- Concurrent registry modifications
- Client-server sync
- Registry state validation

#### Step 5.2: Integration Tests
Test scenarios:
- Create multiple dimensions rapidly
- Client join during dimension creation
- Server restart with active dimensions
- Memory usage under load

### Phase 6: Alternative Implementation (Fallback)

If runtime registry manipulation proves too unstable:

#### Option A: Pre-allocated Dimension Pool
- Register 10-20 dimensions at startup
- Use metadata to mark which are "active"
- Recycle dimensions instead of creating new ones

#### Option B: Custom Dimension Registry
- Implement separate registry for exploration dimensions
- Avoid modifying vanilla registries
- Use custom packets for all synchronization

## Risk Assessment

### High Risk Issues
1. **Registry Corruption**: Could break world saves
   - Mitigation: Transaction support, validation checks
   
2. **Client Desync**: Players unable to join
   - Mitigation: Comprehensive sync packets, fallback handling

3. **Memory Leaks**: Registry entries not cleaned up
   - Mitigation: Tracking system, cleanup on shutdown

### Medium Risk Issues
1. **Mod Compatibility**: Other mods expect static registries
   - Mitigation: Only modify our own entries, compatibility testing

2. **Performance Impact**: Registry operations are slow
   - Mitigation: Batch operations, async where possible

## Implementation Timeline

### Week 1: Core Registry Implementation
- Days 1-2: Complete MixinRegistry implementation
- Days 3-4: Fix MinecraftServer registration
- Day 5: Initial testing

### Week 2: Client Sync and Safety
- Days 1-2: Client packet handling
- Days 3-4: Safety mechanisms
- Day 5: Integration testing

### Week 3: Polish and Alternatives
- Days 1-2: Performance optimization
- Days 3-4: Implement fallback approach
- Day 5: Final testing and documentation

## Success Criteria

1. ✅ Dimensions can be created at runtime without errors
2. ✅ Clients can join and see new dimensions
3. ✅ Registry state remains consistent
4. ✅ No memory leaks or performance degradation
5. ✅ Compatible with common Forge mods
6. ✅ Graceful fallback if issues occur

## Conclusion

This implementation plan provides a comprehensive approach to completing the registry manipulation system. The phased approach allows for incremental testing and validation, with fallback options if the primary approach proves problematic.