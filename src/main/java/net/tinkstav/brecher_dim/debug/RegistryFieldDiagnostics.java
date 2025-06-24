package net.tinkstav.brecher_dim.debug;

import com.mojang.logging.LogUtils;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

/**
 * Diagnostic utility to analyze MappedRegistry field structure
 * Helps identify correct field names for Mixin shadow fields
 */
public class RegistryFieldDiagnostics {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    /**
     * Analyzes the MappedRegistry class structure and logs field information
     * This helps identify the correct field names for mixin shadow fields
     */
    public static void analyzeMappedRegistryFields() {
        LOGGER.info("=== MappedRegistry Field Analysis ===");
        
        try {
            // Get the DimensionType registry as an example MappedRegistry
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) {
                LOGGER.error("Server not available for registry analysis");
                return;
            }
            Registry<DimensionType> registry = server.registryAccess().registryOrThrow(Registries.DIMENSION_TYPE);
            Class<?> registryClass = registry.getClass();
            
            LOGGER.info("Registry class: {}", registryClass.getName());
            LOGGER.info("Registry superclass: {}", registryClass.getSuperclass().getName());
            
            // Analyze all fields
            Field[] allFields = registryClass.getDeclaredFields();
            LOGGER.info("Total fields found: {}", allFields.length);
            
            for (Field field : allFields) {
                field.setAccessible(true);
                String modifiers = Modifier.toString(field.getModifiers());
                String type = field.getType().getName();
                String name = field.getName();
                
                LOGGER.info("Field: {} {} {} ({})", modifiers, type, name, field.getGenericType());
                
                // Try to identify Map fields that might be our target fields
                if (Map.class.isAssignableFrom(field.getType())) {
                    LOGGER.info("  -> Map field detected: {}", name);
                    try {
                        Object value = field.get(registry);
                        if (value instanceof Map<?, ?> map) {
                            LOGGER.info("  -> Map size: {}", map.size());
                            if (!map.isEmpty()) {
                                Object firstKey = map.keySet().iterator().next();
                                Object firstValue = map.values().iterator().next();
                                LOGGER.info("  -> Key type: {}, Value type: {}", 
                                    firstKey.getClass().getName(), 
                                    firstValue.getClass().getName());
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.info("  -> Could not access map contents: {}", e.getMessage());
                    }
                } else if (field.getType() == boolean.class || field.getType() == Boolean.class) {
                    LOGGER.info("  -> Boolean field detected: {}", name);
                    try {
                        Object value = field.get(registry);
                        LOGGER.info("  -> Value: {}", value);
                    } catch (Exception e) {
                        LOGGER.info("  -> Could not access boolean value: {}", e.getMessage());
                    }
                }
            }
            
            // Also check superclass fields
            Class<?> superclass = registryClass.getSuperclass();
            if (superclass != null && superclass != Object.class) {
                LOGGER.info("=== Superclass Fields: {} ===", superclass.getName());
                Field[] superFields = superclass.getDeclaredFields();
                for (Field field : superFields) {
                    field.setAccessible(true);
                    String modifiers = Modifier.toString(field.getModifiers());
                    String type = field.getType().getName();
                    String name = field.getName();
                    
                    LOGGER.info("Super Field: {} {} {} ({})", modifiers, type, name, field.getGenericType());
                    
                    if (Map.class.isAssignableFrom(field.getType())) {
                        LOGGER.info("  -> Super Map field detected: {}", name);
                    } else if (field.getType() == boolean.class || field.getType() == Boolean.class) {
                        LOGGER.info("  -> Super Boolean field detected: {}", name);
                    }
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to analyze MappedRegistry fields", e);
        }
        
        LOGGER.info("=== End Registry Analysis ===");
    }
    
    /**
     * Tests if our current mixin shadow fields work correctly
     */
    public static void testCurrentShadowFields() {
        LOGGER.info("=== Testing Current Shadow Field Access ===");
        
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) {
                LOGGER.error("Server not available for testing shadow fields");
                return;
            }
            Registry<DimensionType> registry = server.registryAccess().registryOrThrow(Registries.DIMENSION_TYPE);
            
            // Test if we can cast to our mixin interface
            if (registry instanceof net.tinkstav.brecher_dim.accessor.IRegistryAccessor) {
                @SuppressWarnings("unchecked")
                net.tinkstav.brecher_dim.accessor.IRegistryAccessor<DimensionType> accessor = 
                    (net.tinkstav.brecher_dim.accessor.IRegistryAccessor<DimensionType>) registry;
                
                LOGGER.info("Registry successfully cast to IRegistryAccessor");
                
                // Test frozen state access
                boolean frozen = accessor.brecher_dim$isFrozen();
                LOGGER.info("Frozen state accessible: {}", frozen);
                
            } else {
                LOGGER.error("Registry cannot be cast to IRegistryAccessor - mixin not applied");
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to test shadow field access", e);
        }
        
        LOGGER.info("=== End Shadow Field Test ===");
    }
    
    /**
     * Identifies potential field mapping issues
     */
    public static void identifyFieldMappingIssues() {
        LOGGER.info("=== Field Mapping Issue Detection ===");
        
        try {
            // Get actual registry instance
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) {
                LOGGER.error("Server not available for field mapping analysis");
                return;
            }
            Registry<DimensionType> registry = server.registryAccess().registryOrThrow(Registries.DIMENSION_TYPE);
            Class<?> actualClass = registry.getClass();
            
            // Expected shadow field names based on our mixin
            String[] expectedFields = {"byKey", "byValue", "byLocation", "frozen"};
            
            for (String expectedField : expectedFields) {
                try {
                    Field field = actualClass.getDeclaredField(expectedField);
                    LOGGER.info("✓ Field '{}' found: {} {}", expectedField, 
                        field.getType().getSimpleName(), field.getName());
                } catch (NoSuchFieldException e) {
                    LOGGER.error("✗ Field '{}' NOT FOUND", expectedField);
                    
                    // Try to find similar fields
                    Field[] allFields = actualClass.getDeclaredFields();
                    LOGGER.info("  Available fields that might match:");
                    for (Field field : allFields) {
                        String name = field.getName();
                        if (name.toLowerCase().contains(expectedField.toLowerCase()) ||
                            expectedField.toLowerCase().contains(name.toLowerCase()) ||
                            name.length() > 2 && expectedField.contains(name.substring(0, Math.min(3, name.length())))) {
                            LOGGER.info("    Candidate: {} {} {}", 
                                field.getType().getSimpleName(), field.getName(), field.getGenericType());
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to identify field mapping issues", e);
        }
        
        LOGGER.info("=== End Field Mapping Issue Detection ===");
    }
}