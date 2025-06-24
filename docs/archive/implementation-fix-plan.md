# Brecher Dimensions Implementation Fix Plan

## Overview
This document outlines the step-by-step plan to fix compilation errors and complete the implementation of the Brecher Dimensions mod for Minecraft 1.20.1.

## Issues Identified

### 1. **Mixin Pattern Violations**
- Code attempts to cast `MinecraftServer` to `MixinMinecraftServer` directly
- Missing accessor interfaces for mixin methods
- Static method calls on instance-based classes

### 2. **API Mismatches**
- Using `FlatLevelGenerator` instead of `FlatLevelSource` (1.20.1 change)
- Incorrect entity counting methods
- Private field access instead of getter methods
- Wrong method names for MinecraftServer

### 3. **Missing Static Methods**
- `BrecherDimensionManager.isInExplorationDimension(ServerPlayer)`
- `BrecherDimensionManager.trackPlayerLeaving(ServerPlayer)`

### 4. **Type Safety Issues**
- No interfaces for mixin accessor methods
- Direct casting to mixin types

## Step-by-Step Implementation Plan

### Phase 1: Create Accessor Interfaces (Priority: HIGH)

#### Step 1.1: Create IServerDimensionAccessor interface
```java
package net.tinkstav.brecher_dim.mixin.accessor;

public interface IServerDimensionAccessor {
    void brecher_dim$createRuntimeDimension(ResourceKey<Level> dimensionKey, 
                                           DimensionType dimensionType, 
                                           ChunkGenerator chunkGenerator, 
                                           long seed);
    void brecher_dim$removeRuntimeDimension(ResourceKey<Level> dimensionKey);
    Map<ResourceKey<Level>, ServerLevel> brecher_dim$getRuntimeLevels();
}
```

#### Step 1.2: Create IRegistryAccessor interface
```java
package net.tinkstav.brecher_dim.mixin.accessor;

public interface IRegistryAccessor<T> {
    void brecher_dim$setFrozen(boolean frozen);
    void brecher_dim$addMapping(int id, ResourceKey<T> key, T value);
}
```

#### Step 1.3: Update MixinMinecraftServer to implement IServerDimensionAccessor
- Add `implements IServerDimensionAccessor` to the mixin class
- Ensure all interface methods are implemented

#### Step 1.4: Update MixinRegistry to implement IRegistryAccessor
- Add `implements IRegistryAccessor<T>` to the mixin class
- Ensure all interface methods are implemented

### Phase 2: Fix API Usage (Priority: HIGH)

#### Step 2.1: Update DynamicDimensionFactory.java
- Replace `MinecraftServer instanceof MixinMinecraftServer` with:
  ```java
  if (server instanceof IServerDimensionAccessor accessor) {
      accessor.brecher_dim$createRuntimeDimension(...);
  }
  ```

#### Step 2.2: Fix NoiseBasedChunkGenerator field access
- Change `noiseGen.settings` to `noiseGen.getSettings()`

#### Step 2.3: Update FlatLevelGenerator references
- Import `net.minecraft.world.level.levelgen.flat.FlatLevelSource`
- Replace all `FlatLevelGenerator` with `FlatLevelSource`

#### Step 2.4: Fix entity counting in BrecherEventHandlers
- Replace `level.getEntities().getAll().size()` with:
  ```java
  int entities = level.getAllEntities().iterator().hasNext() ? 
                 StreamSupport.stream(level.getAllEntities().spliterator(), false).count() : 0;
  ```

### Phase 3: Add Missing Static Methods (Priority: HIGH)

#### Step 3.1: Add static helper methods to BrecherDimensionManager
```java
public static boolean isInExplorationDimension(ServerPlayer player) {
    BrecherDimensionManager manager = Brecher_Dim.getDimensionManager();
    return manager != null && manager.isExplorationDimension(player.level().dimension().location());
}

public static void trackPlayerLeaving(ServerPlayer player) {
    BrecherDimensionManager manager = Brecher_Dim.getDimensionManager();
    if (manager != null) {
        manager.onPlayerLeaveExploration(player);
    }
}
```

### Phase 4: Fix Minecraft API Method Names (Priority: MEDIUM)

#### Step 4.1: Verify MinecraftServer method names
- Check if `getProgressListenerFactory()` exists or if it should be:
  - `getProgressListener()`
  - `progressListener()`
  - Or another method name

#### Step 4.2: Update @Shadow declarations
- Fix all @Shadow method declarations to match actual Minecraft 1.20.1 methods

### Phase 5: Fix Resource Location Deprecation (Priority: LOW)

#### Step 5.1: Update all ResourceLocation constructors
- Replace `new ResourceLocation(string)` with `ResourceLocation.parse(string)`
- Add try-catch blocks for parsing errors

### Phase 6: Fix Mixin Registry Casting (Priority: HIGH)

#### Step 6.1: Update client packet listener
- Replace direct casting to `MixinRegistry` with interface casting:
  ```java
  if (registry instanceof IRegistryAccessor<?> accessor) {
      // Use accessor methods
  }
  ```

### Phase 7: Update Build Configuration (Priority: LOW)

#### Step 7.1: Verify mixin configuration
- Ensure `brecher_dim.mixins.json` is correctly configured
- Add refmap configuration if missing

#### Step 7.2: Update access transformer
- Verify all field names match 1.20.1 mappings
- Remove references to non-existent classes

## Implementation Order

1. ✅ **Completed**: Create accessor interfaces (Phase 1)
2. ✅ **Completed**: Fix API usage and static methods (Phases 2-3)
3. ✅ **Completed**: Fix Minecraft API methods and mixin casting (Phases 4, 6)
4. ✅ **Completed**: Fix deprecation warnings and build config (Phases 5, 7)
5. **Next**: Testing and debugging (Ready for Phase 8)

## Implementation Status: ✅ COMPILATION SUCCESSFUL

All phases have been completed and the mod now compiles successfully. The build passes with only deprecation warnings (expected for Forge 1.20.1).

## Testing Plan

### Unit Tests
- Test dimension creation/removal
- Test player tracking
- Test registry manipulation

### Integration Tests
- Create test world with multiple players
- Test dimension creation under load
- Test cleanup on server shutdown

### Manual Testing
- Verify commands work correctly
- Test teleportation between dimensions
- Verify client synchronization

## Success Criteria

1. Code compiles without errors
2. All mixins load correctly at runtime
3. Dimensions can be created/removed dynamically
4. Players can teleport between dimensions
5. Client-server synchronization works
6. No memory leaks or crashes

## Risk Mitigation

### Risk: Mixin conflicts with other mods
**Mitigation**: Use specific injection points and avoid broad modifications

### Risk: Performance impact
**Mitigation**: Profile code and optimize hot paths

### Risk: Client desync
**Mitigation**: Comprehensive packet validation and error handling

## Estimated Time: 5-7 days of development