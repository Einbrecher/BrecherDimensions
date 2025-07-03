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