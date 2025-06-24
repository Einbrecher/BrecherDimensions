# Immediate Compilation Fixes

## Quick fixes to get the mod compiling

### 1. Create Accessor Interfaces

#### Create IServerDimensionAccessor.java
```java
package net.tinkstav.brecher_dim.mixin.accessor;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import java.util.Map;

public interface IServerDimensionAccessor {
    void brecher_dim$createRuntimeDimension(ResourceKey<Level> dimensionKey, 
                                           DimensionType dimensionType, 
                                           ChunkGenerator chunkGenerator, 
                                           long seed);
    void brecher_dim$removeRuntimeDimension(ResourceKey<Level> dimensionKey);
    Map<ResourceKey<Level>, ServerLevel> brecher_dim$getRuntimeLevels();
}
```

#### Create IRegistryAccessor.java
```java
package net.tinkstav.brecher_dim.mixin.accessor;

import net.minecraft.resources.ResourceKey;

public interface IRegistryAccessor<T> {
    void brecher_dim$setFrozen(boolean frozen);
    void brecher_dim$addMapping(int id, ResourceKey<T> key, T value);
}
```

### 2. Fix DynamicDimensionFactory.java

Replace lines 65-66:
```java
// OLD:
if (server instanceof MixinMinecraftServer mixinServer) {
    return mixinServer.brecher_dim$createRuntimeDimension(

// NEW:
if (server instanceof IServerDimensionAccessor accessor) {
    accessor.brecher_dim$createRuntimeDimension(
```

Replace line 99:
```java
// OLD:
noiseGen.settings

// NEW:
noiseGen.getSettings()
```

Replace lines 101-103:
```java
// OLD:
} else if (original instanceof FlatLevelGenerator flatGen) {
    // Clone with same settings but new seed
    return new FlatLevelGenerator(flatGen.settings());

// NEW:
} else if (original instanceof FlatLevelSource flatGen) {
    // Clone with same settings but new seed
    return new FlatLevelSource(flatGen.settings());
```

Replace line 115:
```java
// OLD:
if (server instanceof MixinMinecraftServer mixinServer) {

// NEW:
if (server instanceof IServerDimensionAccessor accessor) {
```

Add import:
```java
import net.minecraft.world.level.levelgen.flat.FlatLevelSource;
import net.tinkstav.brecher_dim.mixin.accessor.IServerDimensionAccessor;
```

### 3. Fix BrecherDimensionManager.java

Add these static methods:
```java
/**
 * Static helper to check if a player is in an exploration dimension
 */
public static boolean isInExplorationDimension(ServerPlayer player) {
    BrecherDimensionManager manager = Brecher_Dim.getDimensionManager();
    return manager != null && manager.isExplorationDimension(player.level().dimension().location());
}

/**
 * Static helper to track when a player leaves
 */
public static void trackPlayerLeaving(ServerPlayer player) {
    BrecherDimensionManager manager = Brecher_Dim.getDimensionManager();
    if (manager != null) {
        manager.onPlayerLeaveExploration(player);
    }
}
```

### 4. Fix BrecherEventHandlers.java

Replace line 341:
```java
// OLD:
int entities = level.getEntities().getAll().size();

// NEW:
int entities = 0;
for (Entity entity : level.getAllEntities()) {
    entities++;
}
```

### 5. Update MixinMinecraftServer.java

Add interface implementation:
```java
@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer implements IServerDimensionAccessor {
```

Comment out or fix line 122:
```java
// OLD:
server.getProgressListenerFactory(),

// NEW (comment out for now):
// server.getProgressListenerFactory(),
null, // Progress listener - TODO: find correct method
```

### 6. Update MixinRegistry.java

Add interface implementation:
```java
@Mixin(MappedRegistry.class)
public abstract class MixinRegistry<T> implements IRegistryAccessor<T> {
```

### 7. Fix MixinClientPacketListener.java

Replace line 56:
```java
// OLD:
ResourceKey<? extends Registry<?>> registryKey = buffer.readResourceKey();

// NEW:
ResourceKey<? extends Registry<?>> registryKey = buffer.readResourceKey(Registries.ROOT);
```

Replace line 111:
```java
// OLD:
if (registry instanceof MixinRegistry<DimensionType> mixinRegistry) {

// NEW:
if (registry instanceof IRegistryAccessor<DimensionType> accessor) {
```

### 8. Fix remaining ResourceLocation deprecations

In all data classes, replace:
```java
// OLD:
new ResourceLocation(string)

// NEW:
ResourceLocation.parse(string)
```

### 9. Fix MinecraftServer casts

Replace all instances of:
```java
// OLD:
if (server instanceof MixinMinecraftServer mixinServer) {

// NEW:
if (server instanceof IServerDimensionAccessor accessor) {
```

## After these fixes:

1. Clean the project: `gradlew clean`
2. Compile: `gradlew compileJava`
3. If successful, build: `gradlew build`

## ✅ STATUS: COMPLETED SUCCESSFULLY

All fixes have been applied and the mod now compiles and builds successfully!

### What was accomplished:
1. ✅ Created accessor interfaces for safe mixin method access
2. ✅ Fixed all casting issues from MinecraftServer to mixin types  
3. ✅ Added missing static methods to BrecherDimensionManager
4. ✅ Fixed entity counting to avoid concurrent modification
5. ✅ Updated all mixins to implement accessor interfaces
6. ✅ Fixed all ResourceLocation deprecation warnings
7. ✅ Resolved API compatibility issues with Minecraft 1.20.1
8. ✅ Build passes with only expected deprecation warnings

### Next steps:
- Runtime testing with actual Minecraft server
- Implement full registry manipulation (currently placeholders)
- Add comprehensive error handling
- Performance testing with multiple players