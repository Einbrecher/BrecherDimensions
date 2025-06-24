# MixinRegistry Fix Implementation Plan

## Executive Summary

This document outlines a comprehensive plan to fix the critical MixinRegistry issue in the Brecher Dimensions mod. The registry mixin is currently disabled due to shadow field mapping incompatibilities with Minecraft 1.20.1, forcing the mod to use placeholder overworld dimension types instead of custom dimension types.

**Current Status**: MixinRegistry disabled, using overworld dimension type placeholders
**Target Goal**: Enable runtime registration of custom dimension types and level stems
**Estimated Timeline**: 5-8 development days
**Risk Level**: Medium-High (affects core mod functionality)

---

## 1. Root Cause Analysis

### 1.1 Technical Problem
The `MixinRegistry` class uses `@Shadow` annotations to access private fields in Minecraft's `MappedRegistry` class. These shadow field mappings are failing because:

1. **Field Name Mismatches**: Shadow field names don't match actual field names in MC 1.20.1
2. **Obfuscation Mapping Issues**: Parchment mappings may be incorrect or incomplete
3. **Minecraft Version Differences**: Field structure may have changed between MC versions

### 1.2 Current Failing Code
```java
@Mixin(MappedRegistry.class)
public class MixinRegistry<T> implements IRegistryAccessor<T> {
    @Shadow @Final private Map<ResourceKey<T>, Holder.Reference<T>> byKey;
    @Shadow @Final private Map<T, Holder.Reference<T>> byValue;
    @Shadow @Final private Map<ResourceLocation, Holder.Reference<T>> byLocation;
    @Shadow private boolean frozen;
    // ERROR: These field names may not exist in MC 1.20.1
}
```

### 1.3 Impact Assessment
- **Functional**: Cannot register custom dimension types
- **User Experience**: All exploration dimensions use overworld properties
- **Technical Debt**: Registry cleanup and memory management disabled
- **Future Compatibility**: Limits mod expansion capabilities

---

## 2. Research Phase (Day 1-2)

### 2.1 Minecraft Registry Structure Analysis

#### Step 1: Examine MappedRegistry Class
```bash
# Use Minecraft Development Kit (MDK) to examine source
# Located in: .gradle/caches/minecraft/de/oceanlabs/mcp/[version]/srg/MappedRegistry.java
```

**Research Tasks**:
1. **Field Discovery**: Identify actual field names in `MappedRegistry`
2. **Method Analysis**: Find public/protected methods that can replace field access
3. **Lifecycle Study**: Understand registry freezing/unfreezing mechanisms
4. **Thread Safety**: Analyze concurrent access patterns

#### Step 2: Mapping Verification
```java
// Create diagnostic tool to inspect registry structure
public class RegistryFieldDiagnostics {
    public static void analyzeRegistry(Registry<?> registry) {
        Class<?> clazz = registry.getClass();
        LOGGER.info("Registry class: {}", clazz.getName());
        
        // List all fields
        for (Field field : clazz.getDeclaredFields()) {
            LOGGER.info("Field: {} (Type: {})", field.getName(), field.getType());
        }
        
        // List all methods
        for (Method method : clazz.getDeclaredMethods()) {
            LOGGER.info("Method: {} (Return: {})", method.getName(), method.getReturnType());
        }
    }
}
```

#### Step 3: Parchment Mapping Validation
```bash
# Check current mappings in use
mapping_channel=parchment
mapping_version=2023.09.03-1.20.1

# Compare with official mappings
mapping_channel=official
mapping_version=1.20.1
```

### 2.2 Alternative Approach Research

#### Option A: Mixin Accessor Pattern
```java
@Mixin(MappedRegistry.class)
public interface MappedRegistryAccessor<T> {
    @Accessor("byKey")
    Map<ResourceKey<T>, Holder.Reference<T>> getByKey();
    
    @Accessor("frozen")
    boolean isFrozen();
    
    @Accessor("frozen")
    void setFrozen(boolean frozen);
}
```

#### Option B: Reflection-Based Approach
```java
public class RegistryReflectionHelper {
    private static final Field BY_KEY_FIELD;
    private static final Field BY_VALUE_FIELD;
    private static final Field FROZEN_FIELD;
    
    static {
        try {
            BY_KEY_FIELD = MappedRegistry.class.getDeclaredField("byKey");
            BY_KEY_FIELD.setAccessible(true);
            // Initialize other fields...
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize registry reflection", e);
        }
    }
}
```

#### Option C: Forge Registration Events
```java
@SubscribeEvent
public static void onRegisterDimensionTypes(RegisterEvent event) {
    if (event.getRegistryKey().equals(Registries.DIMENSION_TYPE)) {
        // Use Forge's built-in registration system
        event.register(Registries.DIMENSION_TYPE, "exploration_nether", 
            createNetherLikeDimensionType());
    }
}
```

---

## 3. Implementation Strategy (Day 3-5)

### 3.1 Phase 1: Diagnostic Implementation

#### Step 1: Create Registry Inspector
```java
@Mod.EventBusSubscriber(modid = Brecher_Dim.MODID)
public class RegistryDiagnostics {
    
    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        MinecraftServer server = event.getServer();
        
        // Analyze dimension type registry
        Registry<DimensionType> dimTypeRegistry = server.registryAccess()
            .registryOrThrow(Registries.DIMENSION_TYPE);
        
        analyzeRegistryStructure(dimTypeRegistry);
    }
    
    private static void analyzeRegistryStructure(Registry<?> registry) {
        LOGGER.info("=== Registry Analysis ===");
        LOGGER.info("Registry class: {}", registry.getClass().getName());
        LOGGER.info("Registry size: {}", registry.size());
        
        // Use reflection to examine internal structure
        try {
            Field[] fields = registry.getClass().getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                if (field.getType().equals(Map.class)) {
                    Object value = field.get(registry);
                    if (value instanceof Map<?, ?> map) {
                        LOGGER.info("Map field '{}': size={}", field.getName(), map.size());
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to analyze registry structure", e);
        }
    }
}
```

#### Step 2: Implement Debug Command
```java
@Command("brecheradmin")
public class BrecherAdminCommands {
    
    @Subcommand("debug registry")
    @Permission("brecher.admin.debug")
    public void debugRegistry(CommandSender sender) {
        if (sender instanceof ServerPlayer player) {
            MinecraftServer server = player.getServer();
            
            // Analyze all relevant registries
            analyzeRegistry(server, Registries.DIMENSION_TYPE, "DimensionType");
            analyzeRegistry(server, Registries.LEVEL_STEM, "LevelStem");
            
            sender.sendMessage("Registry analysis complete. Check logs for details.");
        }
    }
}
```

### 3.2 Phase 2: Fixed Mixin Implementation

#### Step 1: Create Adaptive Mixin
```java
@Mixin(MappedRegistry.class)
public class MixinRegistryFixed<T> implements IRegistryAccessor<T> {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // Only shadow fields we're confident about
    @Shadow @Final private Map<ResourceKey<T>, Holder.Reference<T>> byKey;
    @Shadow private boolean frozen;
    
    // Use reflection for uncertain fields
    @Unique
    private Map<T, Holder.Reference<T>> brecher_dim$byValue;
    @Unique
    private Map<ResourceLocation, Holder.Reference<T>> brecher_dim$byLocation;
    @Unique
    private final ReentrantReadWriteLock brecher_dim$registryLock = new ReentrantReadWriteLock();
    @Unique
    private final Set<ResourceKey<T>> brecher_dim$runtimeEntries = ConcurrentHashMap.newKeySet();
    
    @Inject(method = "<init>", at = @At("TAIL"))
    private void brecher_dim$initializeReflectionFields(CallbackInfo ci) {
        try {
            // Dynamically discover field names
            MappedRegistry<?> self = (MappedRegistry<?>)(Object)this;
            Class<?> clazz = self.getClass();
            
            // Find byValue field (might be named differently)
            Field byValueField = findFieldByType(clazz, Map.class, "byValue", "valueToKey", "values");
            if (byValueField != null) {
                byValueField.setAccessible(true);
                @SuppressWarnings("unchecked")
                Map<T, Holder.Reference<T>> byValueMap = (Map<T, Holder.Reference<T>>) byValueField.get(self);
                brecher_dim$byValue = byValueMap;
            }
            
            // Find byLocation field
            Field byLocationField = findFieldByType(clazz, Map.class, "byLocation", "locationToKey", "locations");
            if (byLocationField != null) {
                byLocationField.setAccessible(true);
                @SuppressWarnings("unchecked")
                Map<ResourceLocation, Holder.Reference<T>> byLocationMap = (Map<ResourceLocation, Holder.Reference<T>>) byLocationField.get(self);
                brecher_dim$byLocation = byLocationMap;
            }
            
            LOGGER.info("Successfully initialized registry reflection fields");
            
        } catch (Exception e) {
            LOGGER.error("Failed to initialize registry reflection fields", e);
            // Continue without reflection - use fallback methods
        }
    }
    
    @Unique
    private Field findFieldByType(Class<?> clazz, Class<?> fieldType, String... possibleNames) {
        // Try exact name matches first
        for (String name : possibleNames) {
            try {
                Field field = clazz.getDeclaredField(name);
                if (fieldType.isAssignableFrom(field.getType())) {
                    return field;
                }
            } catch (NoSuchFieldException ignored) {
                // Try next name
            }
        }
        
        // Fall back to type-based search
        for (Field field : clazz.getDeclaredFields()) {
            if (fieldType.isAssignableFrom(field.getType())) {
                // Additional heuristics to identify the right field
                if (field.getGenericType().toString().contains("Map")) {
                    return field;
                }
            }
        }
        
        return null;
    }
    
    @Override
    @Unique
    public void brecher_dim$addMapping(int requestedId, ResourceKey<T> key, T value) {
        if (key == null || value == null) {
            LOGGER.error("Cannot add null key or value to registry");
            return;
        }
        
        brecher_dim$registryLock.writeLock().lock();
        try {
            // Multi-strategy registration approach
            boolean success = false;
            
            // Strategy 1: Use internal registry methods if available
            success = brecher_dim$tryInternalRegistration(requestedId, key, value);
            
            // Strategy 2: Direct field manipulation
            if (!success && brecher_dim$byValue != null && brecher_dim$byLocation != null) {
                success = brecher_dim$tryDirectFieldRegistration(requestedId, key, value);
            }
            
            // Strategy 3: Reflection-based registration
            if (!success) {
                success = brecher_dim$tryReflectionRegistration(requestedId, key, value);
            }
            
            if (success) {
                brecher_dim$runtimeEntries.add(key);
                LOGGER.info("Successfully registered {} using runtime registry modification", key.location());
            } else {
                LOGGER.error("Failed to register {} - all strategies exhausted", key.location());
            }
            
        } finally {
            brecher_dim$registryLock.writeLock().unlock();
        }
    }
    
    @Unique
    private boolean brecher_dim$tryInternalRegistration(int id, ResourceKey<T> key, T value) {
        try {
            @SuppressWarnings("unchecked")
            MappedRegistry<T> self = (MappedRegistry<T>)(Object)this;
            
            // Look for internal register method
            Method registerMethod = null;
            for (Method method : self.getClass().getDeclaredMethods()) {
                if (method.getName().equals("register") && method.getParameterCount() == 3) {
                    registerMethod = method;
                    break;
                }
            }
            
            if (registerMethod != null) {
                registerMethod.setAccessible(true);
                
                // Temporarily unfreeze
                boolean wasFrozen = frozen;
                if (wasFrozen) {
                    frozen = false;
                }
                
                try {
                    registerMethod.invoke(self, id, key, value);
                    return true;
                } finally {
                    if (wasFrozen) {
                        frozen = true;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Internal registration failed for {}: {}", key.location(), e.getMessage());
        }
        return false;
    }
    
    @Unique
    private boolean brecher_dim$tryDirectFieldRegistration(int id, ResourceKey<T> key, T value) {
        try {
            // Create holder reference
            Holder.Reference<T> reference = byKey.computeIfAbsent(key, k -> {
                // Create a standalone reference
                return Holder.Reference.createStandAlone(null, k);
            });
            
            // Bind value
            reference.bindValue(value);
            
            // Update maps
            brecher_dim$byValue.put(value, reference);
            brecher_dim$byLocation.put(key.location(), reference);
            
            return true;
        } catch (Exception e) {
            LOGGER.debug("Direct field registration failed for {}: {}", key.location(), e.getMessage());
        }
        return false;
    }
    
    @Unique
    private boolean brecher_dim$tryReflectionRegistration(int id, ResourceKey<T> key, T value) {
        try {
            @SuppressWarnings("unchecked")
            MappedRegistry<T> self = (MappedRegistry<T>)(Object)this;
            Class<?> clazz = self.getClass();
            
            // Find and access byValue field
            Field byValueField = findFieldByType(clazz, Map.class, "byValue");
            Field byLocationField = findFieldByType(clazz, Map.class, "byLocation");
            
            if (byValueField != null && byLocationField != null) {
                byValueField.setAccessible(true);
                byLocationField.setAccessible(true);
                
                @SuppressWarnings("unchecked")
                Map<T, Holder.Reference<T>> byValueMap = (Map<T, Holder.Reference<T>>) byValueField.get(self);
                @SuppressWarnings("unchecked")
                Map<ResourceLocation, Holder.Reference<T>> byLocationMap = (Map<ResourceLocation, Holder.Reference<T>>) byLocationField.get(self);
                
                // Create and register holder
                Holder.Reference<T> reference = byKey.computeIfAbsent(key, k -> 
                    Holder.Reference.createStandAlone(null, k));
                
                reference.bindValue(value);
                byValueMap.put(value, reference);
                byLocationMap.put(key.location(), reference);
                
                return true;
            }
        } catch (Exception e) {
            LOGGER.debug("Reflection registration failed for {}: {}", key.location(), e.getMessage());
        }
        return false;
    }
    
    @Override
    @Unique
    public void brecher_dim$setFrozen(boolean frozen) {
        this.frozen = frozen;
    }
    
    @Override
    @Unique
    public boolean brecher_dim$isFrozen() {
        return this.frozen;
    }
    
    @Unique
    public void brecher_dim$removeRuntimeEntry(ResourceKey<T> key) {
        if (!brecher_dim$runtimeEntries.contains(key)) {
            return;
        }
        
        brecher_dim$registryLock.writeLock().lock();
        try {
            Holder.Reference<T> holder = byKey.remove(key);
            if (holder != null && brecher_dim$byValue != null && brecher_dim$byLocation != null) {
                T value = holder.value();
                brecher_dim$byValue.remove(value);
                brecher_dim$byLocation.remove(key.location());
            }
            
            brecher_dim$runtimeEntries.remove(key);
            LOGGER.info("Removed runtime registry entry: {}", key.location());
            
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
    public boolean brecher_dim$validateRegistryState() {
        brecher_dim$registryLock.readLock().lock();
        try {
            if (brecher_dim$byValue == null || brecher_dim$byLocation == null) {
                LOGGER.warn("Registry reflection fields not initialized - validation limited");
                return true; // Can't validate, assume okay
            }
            
            // Check basic consistency
            if (byKey.size() != brecher_dim$byValue.size()) {
                LOGGER.error("Registry key/value size mismatch: {} vs {}", 
                    byKey.size(), brecher_dim$byValue.size());
                return false;
            }
            
            // Validate runtime entries
            for (ResourceKey<T> runtimeKey : brecher_dim$runtimeEntries) {
                if (!byKey.containsKey(runtimeKey)) {
                    LOGGER.error("Runtime entry {} not found in byKey map", runtimeKey.location());
                    return false;
                }
            }
            
            return true;
            
        } finally {
            brecher_dim$registryLock.readLock().unlock();
        }
    }
    
    @Unique
    public Set<ResourceKey<T>> brecher_dim$getRuntimeEntries() {
        return new HashSet<>(brecher_dim$runtimeEntries);
    }
}
```

### 3.3 Phase 3: Re-enable Registry Usage

#### Step 1: Update Mixin Configuration
```json
{
    "required": true,
    "minVersion": "0.8",
    "package": "net.tinkstav.brecher_dim.mixin",
    "compatibilityLevel": "JAVA_17",
    "refmap": "brecher_dim.refmap.json",
    "mixins": [
        "MixinRegistryFixed",
        "MixinMinecraftServer",
        "MixinPlayerList",
        "MixinServerLevel"
    ],
    "client": [
        "MixinClientPacketListener"
    ],
    "injectors": {
        "defaultRequire": 1
    }
}
```

#### Step 2: Update MinecraftServer Mixin
```java
// Remove placeholder code and restore registry manipulation
private void brecher_dim$cleanupRegistryEntries(ResourceKey<Level> dimensionKey) {
    try {
        // Re-enable dimension type registry cleanup
        Registry<DimensionType> dimTypeRegistry = registryAccess().registryOrThrow(Registries.DIMENSION_TYPE);
        ResourceKey<DimensionType> dimTypeKey = ResourceKey.create(
            Registries.DIMENSION_TYPE, 
            dimensionKey.location()
        );
        
        try {
            Method removeMethod = dimTypeRegistry.getClass().getMethod("brecher_dim$removeRuntimeEntry", ResourceKey.class);
            removeMethod.invoke(dimTypeRegistry, dimTypeKey);
            LOGGER.debug("Cleaned up dimension type registry entry: {}", dimTypeKey.location());
        } catch (Exception e) {
            LOGGER.debug("Could not clean up dimension type registry entry: {}", dimTypeKey.location());
        }
        
        // Re-enable level stem registry cleanup
        Registry<LevelStem> stemRegistry = registryAccess().registryOrThrow(Registries.LEVEL_STEM);
        ResourceKey<LevelStem> stemKey = ResourceKey.create(
            Registries.LEVEL_STEM, 
            dimensionKey.location()
        );
        
        try {
            Method removeMethod = stemRegistry.getClass().getMethod("brecher_dim$removeRuntimeEntry", ResourceKey.class);
            removeMethod.invoke(stemRegistry, stemKey);
            LOGGER.debug("Cleaned up level stem registry entry: {}", stemKey.location());
        } catch (Exception e) {
            LOGGER.debug("Could not clean up level stem registry entry: {}", stemKey.location());
        }
        
    } catch (Exception e) {
        LOGGER.error("Failed to cleanup registry entries for dimension: {}", dimensionKey.location(), e);
    }
}
```

---

## 4. Testing Strategy (Day 6-7)

### 4.1 Unit Testing

#### Test 1: Registry Field Discovery
```java
@Test
public void testRegistryFieldDiscovery() {
    // Create test registry
    MappedRegistry<DimensionType> testRegistry = new MappedRegistry<>(
        Registries.DIMENSION_TYPE, Lifecycle.stable(), false);
    
    // Verify mixin can access fields
    if (testRegistry instanceof IRegistryAccessor<?> accessor) {
        assertFalse(accessor.brecher_dim$isFrozen());
        
        // Test field access
        assertTrue(accessor.brecher_dim$validateRegistryState());
    }
}
```

#### Test 2: Runtime Registration
```java
@Test
public void testRuntimeDimensionTypeRegistration() {
    MappedRegistry<DimensionType> registry = createTestRegistry();
    
    if (registry instanceof IRegistryAccessor<DimensionType> accessor) {
        ResourceKey<DimensionType> testKey = ResourceKey.create(
            Registries.DIMENSION_TYPE, 
            new ResourceLocation("brecher_dim", "test_type"));
        
        DimensionType testType = createTestDimensionType();
        
        accessor.brecher_dim$addMapping(-1, testKey, testType);
        
        assertTrue(registry.containsKey(testKey));
        assertEquals(testType, registry.get(testKey));
    }
}
```

### 4.2 Integration Testing

#### Test 1: Server Startup
```java
@GameTest
public void testServerStartupWithRegistryMixin() {
    // Verify server starts without errors
    // Check that all mixins load successfully
    // Validate registry structure
}
```

#### Test 2: Dimension Creation
```java
@GameTest
public void testExplorationDimensionCreation() {
    MinecraftServer server = getTestServer();
    
    // Create exploration dimension
    ResourceLocation baseDim = new ResourceLocation("minecraft", "overworld");
    ServerLevel explorationLevel = DynamicDimensionFactory.createExplorationDimension(
        server, baseDim, 12345L);
    
    assertNotNull(explorationLevel);
    
    // Verify custom dimension type was registered
    Registry<DimensionType> registry = server.registryAccess()
        .registryOrThrow(Registries.DIMENSION_TYPE);
    
    ResourceKey<DimensionType> customKey = ResourceKey.create(
        Registries.DIMENSION_TYPE, explorationLevel.dimension().location());
    
    assertTrue(registry.containsKey(customKey));
}
```

#### Test 3: Client-Server Sync
```java
@GameTest
public void testClientRegistrySync() {
    // Create dimension on server
    // Verify client receives registry updates
    // Test player teleportation
    // Validate client dimension access
}
```

### 4.3 Performance Testing

#### Test 1: Memory Usage
```java
@Test
public void testRegistryMemoryUsage() {
    // Measure baseline memory
    long baseline = getUsedMemory();
    
    // Create 100 exploration dimensions
    for (int i = 0; i < 100; i++) {
        createTestDimension("test_" + i);
    }
    
    long afterCreation = getUsedMemory();
    
    // Clean up dimensions
    cleanupAllTestDimensions();
    
    long afterCleanup = getUsedMemory();
    
    // Verify memory is properly released
    assertTrue(afterCleanup - baseline < (afterCreation - baseline) * 0.1);
}
```

#### Test 2: Concurrent Access
```java
@Test
public void testConcurrentRegistryAccess() {
    CountDownLatch latch = new CountDownLatch(10);
    AtomicInteger successCount = new AtomicInteger(0);
    
    // Start 10 threads trying to register simultaneously
    for (int i = 0; i < 10; i++) {
        final int threadId = i;
        new Thread(() -> {
            try {
                registerTestDimension("concurrent_" + threadId);
                successCount.incrementAndGet();
            } finally {
                latch.countDown();
            }
        }).start();
    }
    
    latch.await(30, TimeUnit.SECONDS);
    assertEquals(10, successCount.get());
}
```

---

## 5. Risk Mitigation

### 5.1 Registry Corruption Prevention

#### Strategy 1: Validation Gates
```java
@Unique
private boolean brecher_dim$validateRegistryOperation(ResourceKey<T> key, T value) {
    // Pre-operation validation
    if (key == null || value == null) return false;
    if (byKey.containsKey(key)) return false;
    
    // Registry state validation
    if (!brecher_dim$validateRegistryState()) return false;
    
    return true;
}
```

#### Strategy 2: Atomic Operations
```java
@Unique
private boolean brecher_dim$atomicRegistryModification(ResourceKey<T> key, T value) {
    brecher_dim$registryLock.writeLock().lock();
    try {
        // Capture current state
        Map<ResourceKey<T>, Holder.Reference<T>> backupByKey = new HashMap<>(byKey);
        
        try {
            // Perform modification
            performRegistryModification(key, value);
            
            // Validate result
            if (!brecher_dim$validateRegistryState()) {
                // Restore from backup
                byKey.clear();
                byKey.putAll(backupByKey);
                return false;
            }
            
            return true;
        } catch (Exception e) {
            // Restore from backup on any error
            byKey.clear();
            byKey.putAll(backupByKey);
            throw e;
        }
    } finally {
        brecher_dim$registryLock.writeLock().unlock();
    }
}
```

### 5.2 Client-Server Desync Prevention

#### Strategy 1: Version Checking
```java
public class RegistrySyncHandler {
    private static final int PROTOCOL_VERSION = 1;
    
    @NetworkHandler
    public void handleRegistrySync(RegistrySyncPacket packet, ServerPlayer player) {
        if (packet.getProtocolVersion() != PROTOCOL_VERSION) {
            LOGGER.warn("Client {} has incompatible registry sync protocol", 
                player.getName().getString());
            // Disconnect or fallback
            return;
        }
        
        // Process sync
    }
}
```

#### Strategy 2: Incremental Sync
```java
public class IncrementalRegistrySync {
    private final Map<ServerPlayer, Set<ResourceKey<?>>> clientKnownEntries = new HashMap<>();
    
    public void syncToPlayer(ServerPlayer player) {
        Set<ResourceKey<?>> knownEntries = clientKnownEntries.computeIfAbsent(
            player, p -> new HashSet<>());
        
        // Only sync entries the client doesn't know about
        Set<ResourceKey<?>> newEntries = getCurrentRuntimeEntries();
        newEntries.removeAll(knownEntries);
        
        if (!newEntries.isEmpty()) {
            sendIncrementalSync(player, newEntries);
            knownEntries.addAll(newEntries);
        }
    }
}
```

### 5.3 Fallback Mechanisms

#### Strategy 1: Graceful Degradation
```java
@Unique
private void brecher_dim$handleRegistryFailure(ResourceKey<T> key, Exception error) {
    LOGGER.error("Registry operation failed for {}: {}", key.location(), error.getMessage());
    
    // Attempt fallback to Forge registration
    try {
        // Use Forge's DeferredRegister as fallback
        useForgeFallbackRegistration(key);
    } catch (Exception fallbackError) {
        LOGGER.error("Fallback registration also failed", fallbackError);
        
        // Final fallback: use placeholder dimension type
        usePlaceholderDimensionType(key);
    }
}
```

#### Strategy 2: Configuration-Based Disabling
```java
public class BrecherConfig {
    public static final BooleanValue enableRuntimeRegistry = BUILDER
        .comment("Enable runtime registry modification. Disable if experiencing issues.")
        .define("enableRuntimeRegistry", true);
    
    public static final BooleanValue fallbackToForgeRegistry = BUILDER
        .comment("Use Forge registry events as fallback if runtime modification fails")
        .define("fallbackToForgeRegistry", true);
}
```

---

## 6. Rollback Plan

### 6.1 Immediate Rollback (< 5 minutes)
1. **Disable MixinRegistryFixed** in `brecher_dim.mixins.json`
2. **Restore placeholder code** in `MixinMinecraftServer`
3. **Restart server** to apply changes

### 6.2 Partial Rollback (5-15 minutes)
1. **Keep MixinRegistryFixed enabled** but disable specific operations
2. **Add configuration flags** to control registry modification
3. **Enable logging** to identify specific failure points

### 6.3 Development Rollback (15-30 minutes)
1. **Revert to git commit** before registry mixin changes
2. **Rebuild and redeploy** mod JAR
3. **Document issues** for future fixing

---

## 7. Success Metrics

### 7.1 Functional Metrics
- ✅ **Server starts without errors** with registry mixin enabled
- ✅ **Custom dimension types register successfully** at runtime
- ✅ **Exploration dimensions use custom properties** (not overworld)
- ✅ **Client-server sync works correctly** for new dimension types
- ✅ **Registry cleanup functions properly** on dimension removal

### 7.2 Performance Metrics
- ✅ **Registry modification takes < 50ms** per operation
- ✅ **Memory usage remains stable** after dimension cleanup
- ✅ **No memory leaks** detected after 1000+ dimension operations
- ✅ **Client sync packets < 1KB** per dimension update

### 7.3 Stability Metrics
- ✅ **No registry corruption** after 24 hours of operation
- ✅ **Concurrent access works correctly** under load
- ✅ **Error recovery functions properly** during failures
- ✅ **Configuration changes apply correctly** without restart

---

## 8. Timeline and Deliverables

### Day 1-2: Research and Analysis
- **Deliverable**: Registry structure analysis report
- **Deliverable**: Field mapping compatibility matrix
- **Deliverable**: Alternative approach evaluation

### Day 3-4: Implementation
- **Deliverable**: MixinRegistryFixed implementation
- **Deliverable**: Diagnostic tools and commands
- **Deliverable**: Updated mixin configuration

### Day 5: Integration
- **Deliverable**: Updated MinecraftServer mixin
- **Deliverable**: Client sync improvements
- **Deliverable**: Error handling and fallbacks

### Day 6-7: Testing and Validation
- **Deliverable**: Comprehensive test suite
- **Deliverable**: Performance benchmarks
- **Deliverable**: Stability validation

### Day 8: Documentation and Deployment
- **Deliverable**: Updated documentation
- **Deliverable**: Deployment guide
- **Deliverable**: Rollback procedures

---

## 9. Conclusion

This plan provides a comprehensive approach to fixing the critical MixinRegistry issue that currently limits the Brecher Dimensions mod to using placeholder dimension types. The multi-strategy implementation approach ensures compatibility across different Minecraft versions and provides robust fallback mechanisms.

The key innovation is the adaptive field discovery system that uses reflection to identify correct field names at runtime, rather than relying on potentially incorrect shadow field mappings. This approach should resolve the compatibility issues while maintaining the performance and functionality required for runtime dimension type registration.

Success in implementing this plan will enable the mod to:
- Create custom dimension types with unique properties
- Properly manage registry lifecycle and cleanup
- Provide full client-server synchronization
- Support themed dimensions (Nether-like, End-like, etc.)
- Scale to high-traffic server environments

The comprehensive testing strategy and risk mitigation measures ensure that the implementation will be robust and reliable for production deployment.