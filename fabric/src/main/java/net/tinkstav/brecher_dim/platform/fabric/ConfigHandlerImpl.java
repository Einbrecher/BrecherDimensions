/*
 * Brecher's Dimensions - Temporary resettable dimensions for exploration
 * Copyright (C) 2025 Einbrecher. All rights reserved.
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