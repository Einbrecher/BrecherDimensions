package net.tinkstav.brecher_dim.fabric.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.tinkstav.brecher_dim.fabric.FabricEventHandlersImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    
    @Inject(method = "changeDimension", at = @At("HEAD"))
    private void onChangeDimension(ServerLevel destination, CallbackInfoReturnable<Entity> cir) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        ServerLevel from = player.serverLevel();
        
        // Note: This is called before the actual dimension change
        // We might need to handle this differently or use a different injection point
        FabricEventHandlersImpl.handlePlayerChangeDimension(player, from, destination);
    }
}