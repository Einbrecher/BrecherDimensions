package net.tinkstav.brecher_dim.mixin;

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