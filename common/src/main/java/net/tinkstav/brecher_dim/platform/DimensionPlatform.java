package net.tinkstav.brecher_dim.platform;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;

/**
 * Platform abstraction for dimension creation and management
 */
public class DimensionPlatform {
    
    /**
     * Creates a runtime dimension
     * @param server The Minecraft server
     * @param dimensionKey The key for the new dimension
     * @param dimensionType The dimension type
     * @param chunkGenerator The chunk generator
     * @param seed The seed for the dimension
     * @return The created ServerLevel, or null if failed
     */
    @ExpectPlatform
    public static ServerLevel createRuntimeDimension(MinecraftServer server,
                                                   ResourceKey<Level> dimensionKey,
                                                   DimensionType dimensionType,
                                                   ChunkGenerator chunkGenerator,
                                                   long seed) {
        throw new AssertionError("Platform implementation missing");
    }
    
    /**
     * Removes a runtime dimension
     * @param server The Minecraft server
     * @param dimensionKey The key of the dimension to remove
     */
    @ExpectPlatform
    public static void removeRuntimeDimension(MinecraftServer server, ResourceKey<Level> dimensionKey) {
        throw new AssertionError("Platform implementation missing");
    }
    
    /**
     * Check if runtime dimension creation is supported on this platform
     */
    @ExpectPlatform
    public static boolean isRuntimeDimensionSupported() {
        throw new AssertionError("Platform implementation missing");
    }
}