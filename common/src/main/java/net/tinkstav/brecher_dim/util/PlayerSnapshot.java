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

package net.tinkstav.brecher_dim.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.core.BlockPos;
import java.util.*;

public class PlayerSnapshot {
    private final UUID playerId;
    private final BlockPos position;
    private final float health;
    private final int foodLevel;
    private final float saturation;
    private final List<ItemStack> inventory;
    private final List<MobEffectInstance> effects;
    private final int experienceLevel;
    private final float experienceProgress;
    private final long timestamp;
    
    private PlayerSnapshot(ServerPlayer player) {
        this.playerId = player.getUUID();
        this.position = player.blockPosition();
        this.health = player.getHealth();
        this.foodLevel = player.getFoodData().getFoodLevel();
        this.saturation = player.getFoodData().getSaturationLevel();
        this.inventory = new ArrayList<>();
        this.effects = new ArrayList<>(player.getActiveEffects());
        this.experienceLevel = player.experienceLevel;
        this.experienceProgress = player.experienceProgress;
        this.timestamp = System.currentTimeMillis();
        
        // Copy inventory
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                inventory.add(stack.copy());
            }
        }
    }
    
    public static PlayerSnapshot create(ServerPlayer player) {
        return new PlayerSnapshot(player);
    }
    
    public void restore(ServerPlayer player) {
        if (!player.getUUID().equals(playerId)) {
            throw new IllegalArgumentException("Snapshot is for different player!");
        }
        
        // Restore health and food
        player.setHealth(health);
        player.getFoodData().setFoodLevel(foodLevel);
        player.getFoodData().setSaturation(saturation);
        
        // Restore experience
        player.setExperienceLevels(experienceLevel);
        player.experienceProgress = experienceProgress;
        
        // Restore effects
        player.removeAllEffects();
        for (MobEffectInstance effect : effects) {
            player.addEffect(new MobEffectInstance(effect));
        }
        
        // Note: Inventory restoration should be done carefully
        // This is just for emergency recovery
    }
    
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("playerId", playerId);
        tag.putLong("position", position.asLong());
        tag.putFloat("health", health);
        tag.putInt("foodLevel", foodLevel);
        tag.putFloat("saturation", saturation);
        tag.putInt("experienceLevel", experienceLevel);
        tag.putFloat("experienceProgress", experienceProgress);
        
        ListTag effectsList = new ListTag();
        for (MobEffectInstance effect : effects) {
            effectsList.add(effect.save());
        }
        tag.put("effects", effectsList);
        
        return tag;
    }
    
    public long timestamp() {
        return timestamp;
    }
}