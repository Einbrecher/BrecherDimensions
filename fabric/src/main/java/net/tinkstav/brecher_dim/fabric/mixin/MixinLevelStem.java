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

package net.tinkstav.brecher_dim.fabric.mixin;

import com.mojang.logging.LogUtils;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

/**
 * Mixin to access LevelStem internals
 * This helps us understand how chunk generators are stored
 */
@Mixin(LevelStem.class)
public class MixinLevelStem {
    @Unique
    private static final Logger LOGGER = LogUtils.getLogger();
    
    @Shadow @Final private ChunkGenerator generator;
    
    /**
     * Log when a LevelStem is created to understand the flow
     */
    public void brecher_dim$logCreation() {
        LOGGER.debug("LevelStem created with generator: {}", generator.getClass().getSimpleName());
    }
}