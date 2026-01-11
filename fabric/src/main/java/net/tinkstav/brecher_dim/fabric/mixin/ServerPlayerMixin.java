/*
 * Brecher's Dimensions - Temporary resettable dimensions for exploration
 * Copyright (C) 2025 Einbrecher. All rights reserved.
 */

package net.tinkstav.brecher_dim.fabric.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.DimensionTransition;
import net.tinkstav.brecher_dim.event.BrecherEventHandlers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    
    @Shadow public abstract ServerLevel serverLevel();
    
    @Inject(method = "changeDimension", at = @At("HEAD"))
    private void onChangeDimension(DimensionTransition transition, CallbackInfoReturnable<Entity> cir) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        ServerLevel from = player.serverLevel();
        
        // DimensionTransition contains: newLevel, pos, speed, yRot, xRot, postDimensionTransition
        // The first parameter is the destination ServerLevel
        ServerLevel destination = transition.newLevel();
        
        // Handle the dimension change event
        BrecherEventHandlers.onPlayerChangeDimension(player, from, destination);
    }
}