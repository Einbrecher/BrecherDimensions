# Brecher's Exploration Dimensions

A Minecraft mod that creates rotating Exploration Dimensions that are automatically replaced with new worlds at each server restart. This system ensures that Players consistently have access to "new," untouched world generation - whether it be to experience new features/updates, explore pristine POIs, ravage the landscape for resources, or go mining - without the downsides of ballooning world file sizes or the need to constantly prune chunks. Players can access the Exploration Dimensions via slash commands, and any stragglers are automatically evacuated when the dimensions rotate. 

This mod looks to solve the problems server operators face when setting world borders or managing world size while also trying to give players enough room to explore. It uses mixins to override Minecraft's native restrictions when it comes to world seeds, so each successive Exploration Dimension truly is different from the last. Some basic popular mod compatibility (Xaero's, Corpse/Gravestone mods, Nature's/Exploration Compasses) is already built in, with more on the way.

## Features

- **Runtime Dimension Creation**: Dynamically creates "exploration" variants of existing dimensions (e.g., Overworld, Nether, End) at server startup
- **Rotation System**: Exploration Dimensions are replaced with entirely new worlds each server restart - Normal dimensions are unaffected
- **Slash Command Access**: Players can teleport to/from Exploration Dimensions using slash commands
- **Performance Optimized**: Aggressive chunk unloading and memory management for server stability
- **Inventory Preservation**: Optional keepInventory feature for players in Exploration Dimensions
- **Corpse Mod Compatibility**: Options to cede priority to or override popular corpse mod/gravestone mods with the keepInventory feature
- **Xaero Map Cleanup**: Automatic cleanup of map data for old Exploration Dimensions

## Commands

### Player Commands (`/exploration`)
- `list` - Show available Exploration Dimensions
- `tp <dimension>` - Teleport to Exploration Dimension
- `return` - Return to saved position
- `info` - Display mod information

### Admin Commands (`/explorationadmin`)
- `returnall` - Evacuate all players from Exploration Dimensions
- `info <dimension>` - Detailed dimension statistics
- `stats` - Overall mod statistics
- `debug registry` - Registry diagnostics
- `debug compass` - Compass compatibility diagnostics
- `counter show` - Display current dimension counters
- `counter reset all` - Reset all dimension counters to 0
- `counter reset <dimension>` - Reset specific dimension counter

## Installation

1. Ensure you have Minecraft 1.21.1 with either:
   - Fabric Loader 0.16.9+ and Fabric API
   - NeoForge 21.1.162+
2. Download the appropriate mod JAR file from the releases page:
   - `brecher_dim-fabric-{version}-1.21.1.jar` for Fabric
   - `brecher_dim-neoforge-{version}-1.21.1.jar` for NeoForge
3. Place it in your mods folder
4. Configure the mod (config files are in `config/brecher_exploration/`):
   - Fabric: `config/brecher_exploration/brecher_dimensions.yml`
   - NeoForge: `config/brecher_exploration/brecher_dimensions.yml`

## Configuration

### Core Settings
- `enabledDimensions` - List of dimensions to create exploration copies for
- `explorationBorder` - World border size for Exploration Dimensions (-1 for same as parent)
- `seedStrategy` - Seed generation strategy: "weekly" (default, resets on Thursday), "random", "date-based", or "debug"

### Optional Features
- `preventExplorationSpawnSetting` - Disable spawn setting in Exploration Dimensions (beds/anchors will still explode)
- `keepInventoryInExploration` - keepInventory when dying in Exploration Dimensions
- `deferToCorpseMods` - Let corpse mods handle death instead of built-in keepInventory
- `disableEnderChests` - Block ender chest access in Exploration Dimensions
- `clearInventoryOnReturn` - Clear inventory when returning from Exploration Dimensions
- `cleanupXaeroMapData` - Auto-cleanup Xaero minimap data when dimensions rotate

### Restrictions
- `teleportCooldown` - Cooldown between teleports (in seconds)
- `restrictToCurrentDimension` - Only allow teleport to exploration version of current dimension

### Performance
- `aggressiveChunkUnloading` - Enable aggressive chunk unloading
- `maxChunksPerPlayer` - Maximum chunks loaded per player
- `preventDiskSaves` - Don't save Exploration Dimension chunks to disk; chunks will regenerate when **unloaded** - not just at server restart (performance heavy, disabled by default)
- `preGenerateSpawnChunks` - Pre-generate spawn chunks on dimension creation
- `oldDimensionRetentionCount` - How many, if any, "old" Exploration Dimensions should be kept

## Mod Compatibility

### Built-in Compatibility
- Both the Nature's Compass and Explorer's Compass mods will work in Exploration Dimensions
- Xaero's Map cleanup of old/unavailable Exploration Dimensions
- Corpse/Gravestone mod priority options with built-in keepInventory system

### Tested/Known Compatibilities
- Explorations Dimensions should work as-is with any world generation or structure mods/packs that use vanilla's built-in systems for generation or structure placement
- Xaero's Map / Minimap
- Terralith
- Yung's Structures
- Cobblemon

### Known Issues / WIP
- Mods that utilize custom placement mechanics (e.g., AE2 Meteorites) do not (yet) recognize the updated world seeds for Exploration Dimensions, so these generations will not change from cycle to cycle
- Broader version/loader support
- Modded/datapack dimensions should theoretically work, but are as of yet untested

## License

As of July 2, 2025, Brecher's Dimensions and Brecher's Exploration Dimensions are made available subject to the terms and conditions of the GNU Lesser General Public License 3, the full, unedited text of which may be found in LICENSE.md

## Contributing

To contribute, please contact Einbrecher

## Support

For issues and feature requests, please use the GitHub issue tracker.