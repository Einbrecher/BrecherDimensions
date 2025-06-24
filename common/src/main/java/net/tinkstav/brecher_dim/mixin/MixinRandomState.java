package net.tinkstav.brecher_dim.mixin;

import com.mojang.logging.LogUtils;
import net.minecraft.core.HolderGetter;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.tinkstav.brecher_dim.dimension.ExplorationSeedManager;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Mixin to intercept RandomState creation and modify the seed for exploration dimensions
 */
@Mixin(RandomState.class)
public class MixinRandomState {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    /**
     * Modify the seed argument when creating RandomState for exploration dimensions
     */
    @ModifyArg(
        method = "create(Lnet/minecraft/world/level/levelgen/NoiseGeneratorSettings;Lnet/minecraft/core/HolderGetter;J)Lnet/minecraft/world/level/levelgen/RandomState;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/RandomState;<init>(Lnet/minecraft/world/level/levelgen/NoiseGeneratorSettings;Lnet/minecraft/core/HolderGetter;J)V"),
        index = 2
    )
    private static long brecher_dim$modifySeedForExploration(long seed) {
        // Check if we're creating RandomState for an exploration dimension
        ResourceKey<?> currentDimension = ExplorationSeedManager.getCurrentDimension();
        if (currentDimension != null && currentDimension instanceof ResourceKey<?>) {
            @SuppressWarnings("unchecked")
            ResourceKey<Level> levelKey = (ResourceKey<Level>) currentDimension;
            if (ExplorationSeedManager.isExplorationDimension(levelKey)) {
                long modifiedSeed = ExplorationSeedManager.modifySeed(levelKey, seed);
                LOGGER.info("Modifying seed for exploration dimension {}: {} -> {}", 
                           currentDimension.location(), seed, modifiedSeed);
                return modifiedSeed;
            }
        }
        return seed;
    }
    
    /**
     * Modify the seed argument for the Provider-based create method
     */
    @ModifyArg(
        method = "create(Lnet/minecraft/core/HolderGetter$Provider;Lnet/minecraft/resources/ResourceKey;J)Lnet/minecraft/world/level/levelgen/RandomState;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/RandomState;create(Lnet/minecraft/world/level/levelgen/NoiseGeneratorSettings;Lnet/minecraft/core/HolderGetter;J)Lnet/minecraft/world/level/levelgen/RandomState;"),
        index = 2
    )
    private static long brecher_dim$modifySeedForExplorationProvider(long seed) {
        // Check if we're creating RandomState for an exploration dimension
        ResourceKey<?> currentDimension = ExplorationSeedManager.getCurrentDimension();
        if (currentDimension != null && currentDimension instanceof ResourceKey<?>) {
            @SuppressWarnings("unchecked")
            ResourceKey<Level> levelKey = (ResourceKey<Level>) currentDimension;
            if (ExplorationSeedManager.isExplorationDimension(levelKey)) {
                long modifiedSeed = ExplorationSeedManager.modifySeed(levelKey, seed);
                LOGGER.info("Modifying seed for exploration dimension {}: {} -> {}", 
                           currentDimension.location(), seed, modifiedSeed);
                return modifiedSeed;
            }
        }
        return seed;
    }
}