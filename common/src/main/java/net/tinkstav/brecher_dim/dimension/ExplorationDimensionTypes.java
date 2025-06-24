package net.tinkstav.brecher_dim.dimension;

import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.tags.BlockTags;
import net.minecraft.resources.ResourceLocation;
import java.util.OptionalLong;

public class ExplorationDimensionTypes {
    
    public static DimensionType createOverworldExploration() {
        return new DimensionType(
            OptionalLong.empty(), // fixed time (empty = normal day/night)
            true,  // hasSkyLight
            false, // hasCeiling
            false, // ultraWarm
            true,  // natural
            1.0D,  // coordinateScale
            true,  // bedWorks
            false, // respawnAnchorWorks
            -64,   // minY
            384,   // height
            384,   // logicalHeight
            BlockTags.INFINIBURN_OVERWORLD,
            ResourceLocation.parse("minecraft:overworld"),
            0.0F,  // ambientLight
            new DimensionType.MonsterSettings(false, true, UniformInt.of(0, 7), 0)
        );
    }
    
    public static DimensionType createNetherExploration() {
        return new DimensionType(
            OptionalLong.of(18000L), // fixed time (always dark)
            false, // hasSkyLight
            true,  // hasCeiling
            true,  // ultraWarm
            false, // natural
            8.0D,  // coordinateScale
            false, // bedWorks
            true,  // respawnAnchorWorks
            0,     // minY
            256,   // height
            128,   // logicalHeight
            BlockTags.INFINIBURN_NETHER,
            ResourceLocation.parse("minecraft:the_nether"),
            0.1F,  // ambientLight
            new DimensionType.MonsterSettings(true, false, UniformInt.of(0, 11), 15)
        );
    }
    
    public static DimensionType createEndExploration() {
        return new DimensionType(
            OptionalLong.of(6000L), // fixed time
            false, // hasSkyLight
            false, // hasCeiling
            false, // ultraWarm
            false, // natural
            1.0D,  // coordinateScale
            false, // bedWorks
            false, // respawnAnchorWorks
            0,     // minY
            256,   // height
            256,   // logicalHeight
            BlockTags.INFINIBURN_END,
            ResourceLocation.parse("minecraft:the_end"),
            0.0F,  // ambientLight
            new DimensionType.MonsterSettings(false, true, UniformInt.of(0, 7), 0)
        );
    }
}