package net.tinkstav.brecher_dim.mixin.client;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.tinkstav.brecher_dim.accessor.IRegistryAccessor;
import net.tinkstav.brecher_dim.platform.NetworkingPlatform;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;

@OnlyIn(Dist.CLIENT)
@Mixin(ClientPacketListener.class)
public abstract class MixinClientPacketListener {
    @Unique
    private static final Logger LOGGER = LogUtils.getLogger();
    
    @Shadow @Final private Minecraft minecraft;
    // Note: registryAccess field name may vary in 1.20.1, access through minecraft client instead
    
    /**
     * Handle runtime dimension sync from server
     */
    @Inject(method = "handleCustomPayload", at = @At("HEAD"), cancellable = true)
    private void brecher_dim$handleRuntimeDimensionSync(ClientboundCustomPayloadPacket packet, CallbackInfo ci) {
        if (packet.getIdentifier().equals(NetworkingPlatform.REGISTRY_SYNC)) {
            FriendlyByteBuf buffer = null;
            try {
                buffer = packet.getData();
                
                // Validate buffer has enough data for at least count + one entry
                if (buffer.readableBytes() < 8) {
                    LOGGER.error("Registry sync packet too small: {} bytes", buffer.readableBytes());
                    ci.cancel();
                    return;
                }
                
                // For now, just handle the dimension sync directly
                // Update client's dimension type registry
                brecher_dim$updateClientDimensionRegistry(buffer);
                ci.cancel();
                LOGGER.debug("Handled dimension registry sync packet");
            } catch (Exception e) {
                LOGGER.error("Error handling dimension sync packet", e);
            } finally {
                if (buffer != null) {
                    buffer.release();
                }
            }
        } else if (packet.getIdentifier().equals(NetworkingPlatform.DIMENSION_SYNC)) {
            FriendlyByteBuf buffer = null;
            try {
                buffer = packet.getData();
                
                // Validate buffer has enough data
                if (buffer.readableBytes() < 8) {
                    LOGGER.error("Dimension sync packet too small");
                    ci.cancel();
                    return;
                }
                
                // Handle individual dimension sync
                brecher_dim$handleDimensionSync(buffer);
                ci.cancel();
            } catch (Exception e) {
                LOGGER.error("Error handling dimension sync", e);
            } finally {
                if (buffer != null) {
                    buffer.release();
                }
            }
        }
    }
    
    @Unique
    private void brecher_dim$updateClientDimensionRegistry(FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        LOGGER.debug("Updating client dimension registry with {} entries", count);
        
        // Get the registries we'll be updating through the minecraft client
        if (minecraft.level == null) {
            LOGGER.warn("Cannot update client registries - no level loaded");
            return;
        }
        
        RegistryAccess registryAccess = minecraft.level.registryAccess();
        Registry<DimensionType> dimTypeRegistry = registryAccess.registryOrThrow(Registries.DIMENSION_TYPE);
        Registry<LevelStem> stemRegistry = registryAccess.registryOrThrow(Registries.LEVEL_STEM);
        
        for (int i = 0; i < count; i++) {
            try {
                ResourceLocation id = buffer.readResourceLocation();
                int registryId = buffer.readVarInt();
                CompoundTag dimTypeTag = buffer.readNbt();
                CompoundTag stemTag = buffer.readNbt();
                
                // Process dimension type
                if (dimTypeTag != null) {
                    DimensionType.DIRECT_CODEC.parse(NbtOps.INSTANCE, dimTypeTag)
                        .resultOrPartial(error -> LOGGER.error("Failed to parse dimension type: {}", error))
                        .ifPresent(dimType -> {
                            brecher_dim$registerClientDimensionType(dimTypeRegistry, id, registryId, dimType);
                        });
                }
                
                // Process level stem
                if (stemTag != null) {
                    LevelStem.CODEC.parse(NbtOps.INSTANCE, stemTag)
                        .resultOrPartial(error -> LOGGER.error("Failed to parse level stem: {}", error))
                        .ifPresent(levelStem -> {
                            brecher_dim$registerClientLevelStem(stemRegistry, id, registryId, levelStem);
                        });
                }
                
            } catch (Exception e) {
                LOGGER.error("Error processing registry sync entry {}: {}", i, e.getMessage());
                // Continue processing other entries even if one fails
            }
        }
        
        // Validate registry state after updates
        brecher_dim$validateClientRegistries();
    }
    
    @Unique
    @SuppressWarnings("unchecked")
    private void brecher_dim$registerClientDimensionType(Registry<DimensionType> registry, 
                                                         ResourceLocation id, 
                                                         int registryId, 
                                                         DimensionType dimType) {
        ResourceKey<DimensionType> key = ResourceKey.create(Registries.DIMENSION_TYPE, id);
        
        // Check if already registered
        if (registry.containsKey(key)) {
            LOGGER.debug("Dimension type {} already registered on client", id);
            return;
        }
        
        // CLIENT-REGISTRY-SYNC: Attempt to register dimension type on client
        try {
            // First, check if registry supports our accessor interface
            if (registry instanceof IRegistryAccessor) {
                IRegistryAccessor<DimensionType> accessor = (IRegistryAccessor<DimensionType>) registry;
                LOGGER.debug("Using IRegistryAccessor to register client dimension type: {}", id);
                
                // Register the runtime dimension type
                accessor.brecher_dim$registerRuntime(key, dimType);
                
                LOGGER.info("Successfully registered dimension type {} on client via mixin", id);
            } else {
                // Fallback: Try to use reflection if mixin is not available
                LOGGER.warn("Registry does not implement IRegistryAccessor, attempting reflection for: {}", id);
                
                // Attempt reflection-based registration as a last resort
                if (brecher_dim$attemptReflectiveRegistration(registry, key, dimType, registryId)) {
                    LOGGER.info("Successfully registered dimension type {} on client via reflection", id);
                } else {
                    LOGGER.error("Failed to register dimension type {} on client - no available method", id);
                }
            }
            
            // Update client world list regardless of registration method
            brecher_dim$updateClientWorldList(key);
            
        } catch (Exception e) {
            LOGGER.error("Failed to register dimension type {} on client", id, e);
        }
    }
    
    @Unique
    @SuppressWarnings("unchecked")
    private void brecher_dim$registerClientLevelStem(Registry<LevelStem> registry,
                                                     ResourceLocation id,
                                                     int registryId,
                                                     LevelStem levelStem) {
        ResourceKey<LevelStem> key = ResourceKey.create(Registries.LEVEL_STEM, id);
        
        // Check if already registered
        if (registry.containsKey(key)) {
            LOGGER.debug("Level stem {} already registered on client", id);
            return;
        }
        
        // CLIENT-REGISTRY-SYNC: Attempt to register level stem on client
        try {
            // First, check if registry supports our accessor interface
            if (registry instanceof IRegistryAccessor) {
                IRegistryAccessor<LevelStem> accessor = (IRegistryAccessor<LevelStem>) registry;
                LOGGER.debug("Using IRegistryAccessor to register client level stem: {}", id);
                
                // Register the runtime level stem
                accessor.brecher_dim$registerRuntime(key, levelStem);
                
                LOGGER.info("Successfully registered level stem {} on client via mixin", id);
            } else {
                // Fallback: Try to use reflection if mixin is not available
                LOGGER.warn("Registry does not implement IRegistryAccessor, attempting reflection for: {}", id);
                
                // Attempt reflection-based registration as a last resort
                if (brecher_dim$attemptReflectiveRegistration(registry, key, levelStem, registryId)) {
                    LOGGER.info("Successfully registered level stem {} on client via reflection", id);
                } else {
                    LOGGER.error("Failed to register level stem {} on client - no available method", id);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to register level stem {} on client", id, e);
        }
    }
    
    @Unique
    private void brecher_dim$updateClientWorldList(ResourceKey<DimensionType> dimTypeKey) {
        // Notify client that new dimension is available
        if (minecraft.level != null) {
            minecraft.execute(() -> {
                try {
                    // Update any client-side caches or UI elements
                    LOGGER.debug("Client world list updated with dimension: {}", dimTypeKey.location());
                    
                    // Here we could trigger UI updates, world list refreshes, etc.
                    // For now, just log the update
                    
                } catch (Exception e) {
                    LOGGER.error("Failed to update client world list for {}", dimTypeKey.location(), e);
                }
            });
        }
    }
    
    @Unique
    @SuppressWarnings("unchecked")
    private void brecher_dim$validateClientRegistries() {
        try {
            RegistryAccess registryAccess = minecraft.getConnection().registryAccess();
            Registry<DimensionType> dimTypeRegistry = registryAccess.registryOrThrow(Registries.DIMENSION_TYPE);
            Registry<LevelStem> stemRegistry = registryAccess.registryOrThrow(Registries.LEVEL_STEM);
            
            // Validate registries if they support our accessor interface
            if (dimTypeRegistry instanceof IRegistryAccessor) {
                IRegistryAccessor<DimensionType> dimAccessor = (IRegistryAccessor<DimensionType>) dimTypeRegistry;
                boolean dimValid = dimAccessor.brecher_dim$validateRegistryState();
                int runtimeCount = dimAccessor.brecher_dim$getRuntimeEntries().size();
                LOGGER.info("Client dimension type registry validation: {} (runtime entries: {})", 
                    dimValid ? "PASSED" : "FAILED", runtimeCount);
            }
            
            if (stemRegistry instanceof IRegistryAccessor) {
                IRegistryAccessor<LevelStem> stemAccessor = (IRegistryAccessor<LevelStem>) stemRegistry;
                boolean stemValid = stemAccessor.brecher_dim$validateRegistryState();
                int runtimeCount = stemAccessor.brecher_dim$getRuntimeEntries().size();
                LOGGER.info("Client level stem registry validation: {} (runtime entries: {})", 
                    stemValid ? "PASSED" : "FAILED", runtimeCount);
            }
            
            LOGGER.debug("Client registry validation complete");
            
        } catch (Exception e) {
            LOGGER.error("Failed to validate client registries", e);
        }
    }
    
    /**
     * Attempts to register an entry in a registry using reflection
     * This is a fallback method when the registry doesn't implement IRegistryAccessor
     * 
     * @param registry The registry to modify
     * @param key The resource key for the entry
     * @param value The value to register
     * @param id The numeric ID for the entry
     * @return true if successful, false otherwise
     */
    @Unique
    @SuppressWarnings("unchecked")
    private <T> boolean brecher_dim$attemptReflectiveRegistration(Registry<T> registry, 
                                                                  ResourceKey<T> key, 
                                                                  T value, 
                                                                  int id) {
        // REFLECTION: Client-side registry registration fallback
        try {
            // First, try to find the registry's internal map field
            Class<?> registryClass = registry.getClass();
            java.lang.reflect.Field[] fields = registryClass.getDeclaredFields();
            
            for (java.lang.reflect.Field field : fields) {
                if (java.util.Map.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    Object mapObj = field.get(registry);
                    
                    if (mapObj instanceof java.util.Map<?, ?> map) {
                        // Attempt to add to the map
                        Method putMethod = map.getClass().getMethod("put", Object.class, Object.class);
                        putMethod.invoke(map, key, value);
                        
                        LOGGER.debug("Successfully added {} to registry map via reflection", key.location());
                        return true;
                    }
                }
            }
            
            LOGGER.warn("Could not find suitable map field in registry class: {}", registryClass.getName());
            return false;
            
        } catch (Exception e) {
            LOGGER.error("Reflection-based registration failed for key: {}", key.location(), e);
            return false;
        }
    }
    
    @Unique
    private void brecher_dim$handleDimensionSync(FriendlyByteBuf buffer) {
        try {
            // Read dimension info
            ResourceLocation dimensionId = buffer.readResourceLocation();
            long seed = buffer.readLong();
            boolean isExploration = buffer.readBoolean();
            
            if (isExploration) {
                LOGGER.info("Received exploration dimension sync: {} with seed {}", dimensionId, seed);
                
                // Additional client-side handling
                brecher_dim$handleExplorationDimensionUpdate(dimensionId, seed);
            } else {
                LOGGER.debug("Received non-exploration dimension sync: {}", dimensionId);
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to handle dimension sync packet", e);
        }
    }
    
    @Unique
    private void brecher_dim$handleExplorationDimensionUpdate(ResourceLocation dimensionId, long seed) {
        if (minecraft.level != null) {
            minecraft.execute(() -> {
                try {
                    // Store exploration dimension info for client use
                    LOGGER.debug("Updated client exploration dimension info: {} (seed: {})", dimensionId, seed);
                    
                    // Here we could:
                    // - Update client-side dimension cache
                    // - Refresh world selection UI
                    // - Prepare client for potential dimension travel
                    // - Update exploration tracking
                    
                } catch (Exception e) {
                    LOGGER.error("Failed to update exploration dimension info for {}", dimensionId, e);
                }
            });
        }
    }
}