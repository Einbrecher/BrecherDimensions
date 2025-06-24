package net.tinkstav.brecher_dim;

import com.mojang.logging.LogUtils;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.server.MinecraftServer;
import net.tinkstav.brecher_dim.dimension.BrecherDimensionManager;
import net.tinkstav.brecher_dim.dimension.DimensionRegistrar;
import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * Main class for Brecher's Dimensions mod.
 * Platform-specific implementations are in fabric/neoforge modules.
 */
public class BrecherDimensions {
    public static final String MOD_ID = "brecher_dim";
    private static final Logger LOGGER = LogUtils.getLogger();
    
    private static BrecherDimensionManager dimensionManager;
    
    public static void init() {
        LOGGER.info("Brecher's Dimensions initialized on platform: {}", getPlatformName());
    }
    
    /**
     * Get the platform name (Fabric/NeoForge)
     */
    @ExpectPlatform
    public static String getPlatformName() {
        throw new AssertionError("Platform implementation missing");
    }
    
    /**
     * Get the config directory for the current platform
     */
    @ExpectPlatform
    public static Path getConfigDirectory() {
        throw new AssertionError("Platform implementation missing");
    }
    
    /**
     * Check if we're on the physical client
     */
    @ExpectPlatform
    public static boolean isPhysicalClient() {
        throw new AssertionError("Platform implementation missing");
    }
    
    /**
     * Called when the server is starting
     */
    public static void onServerStarting(MinecraftServer server) {
        LOGGER.info("Brecher's Dimensions server starting");
        
        try {
            // Initialize dimension counter from saved data
            // Note: DimensionCounterUtil will need to be migrated
            // DimensionCounterUtil.initialize(server);
            
            // Validate configuration settings
            // Note: Config system needs to be migrated
            // BrecherConfig.validateConfig();
            
            // Create exploration dimensions at runtime
            DimensionRegistrar.createExplorationDimensionsAtStartup(server);
            
            // Log created exploration dimensions
            LOGGER.info("Created exploration dimensions:");
            DimensionRegistrar.getInstance().getDimensionInfo().forEach(info -> LOGGER.info("  {}", info));
            
            // Initialize dimension manager with runtime dimensions
            dimensionManager = new BrecherDimensionManager(
                server, 
                DimensionRegistrar.getInstance().getRegisteredDimensions()
            );
            
            LOGGER.info("Brecher's Dimensions server setup complete with {} exploration dimensions", 
                DimensionRegistrar.getInstance().getRuntimeDimensions().size());
                
        } catch (Exception e) {
            LOGGER.error("Failed to start Brecher's Dimensions properly", e);
            throw new RuntimeException("Critical failure during Brecher's Dimensions startup", e);
        }
    }
    
    /**
     * Called when the server is stopping
     */
    public static void onServerStopping(MinecraftServer server) {
        LOGGER.info("Brecher's Dimensions server stopping - beginning cleanup sequence");
        
        try {
            if (dimensionManager != null) {
                // Evacuate all players from exploration dimensions
                dimensionManager.evacuateAllPlayers();
                
                // Shutdown dimension manager
                dimensionManager.shutdown();
                LOGGER.info("Dimension manager shutdown complete");
            }
            
            // Clear runtime dimension tracking
            DimensionRegistrar.cleanupOnShutdown();
            
            LOGGER.info("Brecher's Dimensions cleanup complete - exploration dimensions will be regenerated on next start");
            
        } catch (Exception e) {
            LOGGER.error("Error during Brecher's Dimensions shutdown cleanup", e);
        }
    }
    
    public static BrecherDimensionManager getDimensionManager() {
        return dimensionManager;
    }
    
    public static DimensionRegistrar getDimensionRegistrar() {
        return DimensionRegistrar.getInstance();
    }
}