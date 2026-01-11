/*
 * Brecher's Dimensions - Temporary resettable dimensions for exploration
 * Copyright (C) 2025 Einbrecher. All rights reserved.
 */

package net.tinkstav.brecher_dim.platform.neoforge;

import net.tinkstav.brecher_dim.config.neoforge.BrecherConfigImpl;
import net.tinkstav.brecher_dim.platform.ConfigHandler;
import net.neoforged.fml.loading.FMLPaths;

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
        return FMLPaths.CONFIGDIR.get().resolve("brecher_exploration").resolve("brecher_dimensions.yml").toString();
    }
}