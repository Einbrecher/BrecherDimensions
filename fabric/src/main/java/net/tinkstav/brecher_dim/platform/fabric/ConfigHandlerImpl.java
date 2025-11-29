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

package net.tinkstav.brecher_dim.platform.fabric;

import net.tinkstav.brecher_dim.platform.ConfigHandler;
import net.tinkstav.brecher_dim.config.fabric.BrecherConfigImpl;
import net.fabricmc.loader.api.FabricLoader;

public class ConfigHandlerImpl implements ConfigHandler {
    @Override
    public void init() {
        BrecherConfigImpl.init();
    }
    
    @Override
    public void reload() {
        BrecherConfigImpl.reload();
    }
    
    @Override
    public String getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("brecher_exploration").resolve("brecher_dimensions.yml").toString();
    }
}