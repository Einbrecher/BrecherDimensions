package net.tinkstav.brecher_dim.test;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.tinkstav.brecher_dim.Brecher_Dim;
import org.junit.jupiter.api.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Basic tests for registry manipulation functionality
 * Tests core logic without requiring full Minecraft server setup
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RegistryBasicTest {
    
    @BeforeAll
    static void setup() {
        System.out.println("=== Starting Registry Basic Tests ===");
        System.out.println("Testing core registry manipulation logic");
    }
    
    @AfterAll
    static void cleanup() {
        System.out.println("=== Registry Basic Tests Complete ===");
    }
    
    @Test
    @Order(1)
    @DisplayName("Test Resource Key Creation")
    void testResourceKeyCreation() {
        // Test that we can create resource keys for our dimensions
        ResourceKey<Level> dimensionKey = ResourceKey.create(
            net.minecraft.core.registries.Registries.DIMENSION,
            new ResourceLocation(Brecher_Dim.MODID, "exploration_test")
        );
        
        Assertions.assertNotNull(dimensionKey, "Dimension key should not be null");
        Assertions.assertEquals(Brecher_Dim.MODID, dimensionKey.location().getNamespace());
        Assertions.assertTrue(dimensionKey.location().getPath().startsWith("exploration_"));
        
        System.out.println("✓ Successfully created dimension key: " + dimensionKey.location());
    }
    
    @Test
    @Order(2)
    @DisplayName("Test Dimension Type Key Creation")
    void testDimensionTypeKeyCreation() {
        ResourceKey<DimensionType> dimTypeKey = ResourceKey.create(
            net.minecraft.core.registries.Registries.DIMENSION_TYPE,
            new ResourceLocation(Brecher_Dim.MODID, "exploration_test")
        );
        
        Assertions.assertNotNull(dimTypeKey, "Dimension type key should not be null");
        Assertions.assertEquals(Brecher_Dim.MODID, dimTypeKey.location().getNamespace());
        
        System.out.println("✓ Successfully created dimension type key: " + dimTypeKey.location());
    }
    
    @Test
    @Order(3)
    @DisplayName("Test Mod ID Constants")
    void testModConstants() {
        Assertions.assertNotNull(Brecher_Dim.MODID, "Mod ID should not be null");
        Assertions.assertEquals("brecher_dim", Brecher_Dim.MODID);
        
        System.out.println("✓ Mod ID constant verified: " + Brecher_Dim.MODID);
    }
    
    @Test
    @Order(4)
    @DisplayName("Test Async Operations")
    void testAsyncOperations() {
        // Test that we can handle async operations properly
        CompletableFuture<Boolean> asyncTest = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(100); // Simulate some work
                return true;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        });
        
        try {
            Boolean result = asyncTest.get(5, TimeUnit.SECONDS);
            Assertions.assertTrue(result, "Async operation should complete successfully");
            System.out.println("✓ Async operations working correctly");
        } catch (Exception e) {
            Assertions.fail("Async operation failed: " + e.getMessage());
        }
    }
    
    @Test
    @Order(5)
    @DisplayName("Test Registry Helper Static Methods")
    void testRegistryHelperMethods() {
        // Test that we can call static methods without server instance
        try {
            String stats = net.tinkstav.brecher_dim.util.RegistryHelper.getRegistryStats(null);
            Assertions.assertTrue(stats.contains("unavailable") || stats.contains("Registry Statistics"));
            System.out.println("✓ RegistryHelper static methods accessible");
        } catch (Exception e) {
            Assertions.fail("RegistryHelper methods should be accessible: " + e.getMessage());
        }
    }
    
    @Test
    @Order(6)
    @DisplayName("Test Dimension Registrar Static Access")
    void testDimensionRegistrarAccess() {
        // Test that we can access the dimension registrar
        try {
            var registrar = net.tinkstav.brecher_dim.dimension.DimensionRegistrar.getInstance();
            Assertions.assertNotNull(registrar, "Dimension registrar should be accessible");
            System.out.println("✓ DimensionRegistrar singleton accessible");
        } catch (Exception e) {
            Assertions.fail("DimensionRegistrar should be accessible: " + e.getMessage());
        }
    }
    
    @Test
    @Order(7)
    @DisplayName("Test Configuration Access")
    void testConfigurationAccess() {
        // Test that configuration is accessible
        try {
            // Test that config classes can be loaded
            Class.forName("net.tinkstav.brecher_dim.config.BrecherConfig");
            System.out.println("✓ Configuration classes loadable");
        } catch (ClassNotFoundException e) {
            Assertions.fail("Configuration classes should be loadable: " + e.getMessage());
        }
    }
    
    @Test
    @Order(8)
    @DisplayName("Test Networking Classes")
    void testNetworkingAccess() {
        // Test that networking classes are accessible
        try {
            Class.forName("net.tinkstav.brecher_dim.network.BrecherNetworking");
            System.out.println("✓ Networking classes loadable");
        } catch (ClassNotFoundException e) {
            Assertions.fail("Networking classes should be loadable: " + e.getMessage());
        }
    }
    
    @Test
    @Order(9)
    @DisplayName("Test Mixin Accessor Interfaces")
    void testMixinAccessors() {
        // Test that mixin accessor interfaces are loadable
        try {
            Class.forName("net.tinkstav.brecher_dim.mixin.accessor.IRegistryAccessor");
            Class.forName("net.tinkstav.brecher_dim.mixin.accessor.IServerDimensionAccessor");
            System.out.println("✓ Mixin accessor interfaces loadable");
        } catch (ClassNotFoundException e) {
            Assertions.fail("Mixin accessor interfaces should be loadable: " + e.getMessage());
        }
    }
    
    @Test
    @Order(10)
    @DisplayName("Test Core Classes Compilation")
    void testCoreClassesCompiled() {
        // Verify that all our core classes compiled successfully
        String[] coreClasses = {
            "net.tinkstav.brecher_dim.Brecher_Dim",
            "net.tinkstav.brecher_dim.mixin.MixinRegistry",
            "net.tinkstav.brecher_dim.mixin.MixinMinecraftServer",
            "net.tinkstav.brecher_dim.mixin.MixinClientPacketListener",
            "net.tinkstav.brecher_dim.util.RegistryHelper",
            "net.tinkstav.brecher_dim.dimension.DimensionRegistrar"
        };
        
        for (String className : coreClasses) {
            try {
                Class.forName(className);
                System.out.println("✓ Core class compiled: " + className);
            } catch (ClassNotFoundException e) {
                Assertions.fail("Core class should be compiled: " + className + " - " + e.getMessage());
            }
        }
    }
}