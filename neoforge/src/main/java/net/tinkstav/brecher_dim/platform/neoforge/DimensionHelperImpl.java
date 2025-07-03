/*
 * Brecher's Dimensions - Temporary resettable dimensions for exploration
 * Copyright (C) 2025 Einbrecher
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.tinkstav.brecher_dim.platform.neoforge;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.tinkstav.brecher_dim.platform.DimensionHelper;
import net.tinkstav.brecher_dim.accessor.IServerDimensionAccessor;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import java.util.Map;
import java.util.HashMap;

public class DimensionHelperImpl implements DimensionHelper {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    @Override
    public ServerLevel createDimension(MinecraftServer server, ResourceKey<Level> dimensionKey, LevelStem levelStem, long seed) {
        if (server instanceof IServerDimensionAccessor accessor) {
            // Extract dimension type and chunk generator from level stem
            var dimensionType = levelStem.type().value();
            var chunkGenerator = levelStem.generator();
            
            // Use the mixin accessor to create the dimension
            return accessor.brecher_dim$createRuntimeDimension(
                dimensionKey,
                dimensionType,
                chunkGenerator,
                seed
            );
        }
        
        LOGGER.error("Server doesn't implement IServerDimensionAccessor!");
        return null;
    }
    
    @Override
    public boolean removeDimension(MinecraftServer server, ResourceKey<Level> dimensionKey) {
        if (server instanceof IServerDimensionAccessor accessor) {
            accessor.brecher_dim$removeRuntimeDimension(dimensionKey);
            return true;
        }
        return false;
    }
    
    @Override
    public Map<ResourceKey<Level>, ServerLevel> getLoadedDimensions(MinecraftServer server) {
        // Access levels through the getAllLevels method
        Map<ResourceKey<Level>, ServerLevel> dimensions = new HashMap<>();
        for (ServerLevel level : server.getAllLevels()) {
            dimensions.put(level.dimension(), level);
        }
        return dimensions;
    }
    
    @Override
    public boolean isDimensionLoaded(MinecraftServer server, ResourceKey<Level> dimensionKey) {
        return server.getLevel(dimensionKey) != null;
    }
    
    @Override
    public Registry<DimensionType> getDimensionTypeRegistry(MinecraftServer server) {
        return server.registryAccess().registryOrThrow(Registries.DIMENSION_TYPE);
    }
    
    @Override
    public Registry<LevelStem> getLevelStemRegistry(MinecraftServer server) {
        return server.registryAccess().registryOrThrow(Registries.LEVEL_STEM);
    }
    
    @Override
    public void unloadDimensionChunks(ServerLevel level) {
        // Force unload all chunks
        level.getChunkSource().removeTicketsOnClosing();
        level.getChunkSource().tick(() -> true, false);
    }
    
    @Override
    public boolean supportsRuntimeDimensionCreation() {
        // NeoForge supports runtime dimension creation through our mixin system
        return true;
    }
}