package net.tinkstav.brecher_dim.platform;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.tinkstav.brecher_dim.fabric.FabricDimensionPlatform;

/**
 * Fabric implementation of DimensionPlatform
 * This class is discovered by Architectury's @ExpectPlatform system
 */
public class DimensionPlatformImpl {
    
    public static ServerLevel createRuntimeDimension(MinecraftServer server,
                                                   ResourceKey<Level> dimensionKey,
                                                   DimensionType dimensionType,
                                                   ChunkGenerator chunkGenerator,
                                                   long seed) {
        return FabricDimensionPlatform.createRuntimeDimension(server, dimensionKey, dimensionType, chunkGenerator, seed);
    }
    
    public static void removeRuntimeDimension(MinecraftServer server, ResourceKey<Level> dimensionKey) {
        FabricDimensionPlatform.removeRuntimeDimension(server, dimensionKey);
    }
    
    public static boolean isRuntimeDimensionSupported() {
        return FabricDimensionPlatform.isRuntimeDimensionSupported();
    }
}