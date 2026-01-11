/*
 * Brecher's Dimensions - Temporary resettable dimensions for exploration
 * Copyright (C) 2025 Einbrecher. All rights reserved.
 */

package net.tinkstav.brecher_dim.fabric.mixin.client;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = ClientPacketListener.class)
public abstract class MixinClientPacketListener {
    @Unique
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // Client-side packet handling is now implemented through platform-specific
    // networking handlers (FabricNetworking.initClient() and NeoForgeNetworking)
    // using the CustomPacketPayload system introduced in 1.21.1
}