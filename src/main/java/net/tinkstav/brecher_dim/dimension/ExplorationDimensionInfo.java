package net.tinkstav.brecher_dim.dimension;

import net.minecraft.nbt.CompoundTag;
import java.util.UUID;

public record ExplorationDimensionInfo(
    String baseDimension,
    long createdTime,
    long lastResetTime,
    int playerCount
) {
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("baseDimension", baseDimension);
        tag.putLong("createdTime", createdTime);
        tag.putLong("lastResetTime", lastResetTime);
        tag.putInt("playerCount", playerCount);
        return tag;
    }
    
    public static ExplorationDimensionInfo fromNBT(CompoundTag tag) {
        return new ExplorationDimensionInfo(
            tag.getString("baseDimension"),
            tag.getLong("createdTime"),
            tag.getLong("lastResetTime"),
            tag.getInt("playerCount")
        );
    }
}