# Brecher's Dimensions

A sophisticated Minecraft mod that creates temporary, resettable duplicate dimensions at runtime. Built for Minecraft 1.21.1 with support for both Fabric and NeoForge, it solves the problem of limited exploration opportunities on servers with world borders by providing fresh, daily-resetting dimensions for players to explore.

## Features

- **Runtime Dimension Creation**: Creates exploration dimensions dynamically at server startup without datapacks
- **Daily Reset System**: Dimensions reset with new seeds each server restart (random or date-based)
- **Memory-Only Design**: Exploration dimensions exist only in memory, never saved to disk
- **Safe Teleportation**: Comprehensive safety features including spawn validation and emergency platforms
- **Multi-Dimension Support**: Create exploration copies of Overworld, Nether, End, and modded dimensions
- **Performance Optimized**: Aggressive chunk unloading and memory management for server stability
- **Inventory Preservation**: Optional keep inventory feature for exploration dimensions
- **Corpse Mod Compatibility**: Smart integration with popular corpse/gravestone mods
- **Xaero Map Cleanup**: Automatic cleanup of minimap data for resetting dimensions

## Technical Highlights

- **Manual Multi-Loader Architecture**: Supports both Fabric and NeoForge without Architectury
- **Mixin-Based Registry Manipulation**: Advanced registry manipulation without datapacks
- **Thread-Safe Operations**: Concurrent data structures and locking for multiplayer safety
- **Client-Server Synchronization**: Custom networking for seamless dimension information sync
- **Comprehensive Error Handling**: Multiple fallback strategies and emergency cleanup procedures
- **Per-Dimension-Type Counters**: Consistent naming system (overworld_0, nether_0, etc.)
- **Service Loader Pattern**: Clean platform abstraction using Java's ServiceLoader

## Commands

### Player Commands (`/exploration`)
- `list` - Show available exploration dimensions
- `tp <dimension>` - Teleport to exploration dimension
- `return` - Return to saved position
- `info` - Display mod information

### Admin Commands (`/explorationadmin`)
- `returnall` - Evacuate all players from exploration dimensions
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
4. Configure the mod:
   - Fabric: `config/brecher_exploration.properties`
   - NeoForge: `config/brecher_dim-common.toml`

## Configuration

### Core Settings
- `enabledDimensions` - List of dimensions to create exploration copies for
- `explorationBorder` - World border size for exploration dimensions (-1 for same as parent)
- `seedStrategy` - Seed generation strategy: "random", "date-based", or "debug"
- `allowModdedDimensions` - Allow exploration copies of modded dimensions

### Safety & Features
- `preventExplorationSpawnSetting` - Disable bed spawning in exploration dimensions
- `keepInventoryInExploration` - Keep inventory when dying in exploration dimensions
- `deferToCorpseMods` - Let corpse mods handle death instead of keeping inventory
- `disableEnderChests` - Block ender chest access in exploration dimensions
- `disableEndGateways` - Prevent end gateway usage in exploration dimensions
- `cleanupXaeroMapData` - Auto-cleanup Xaero minimap data on dimension reset

### Gameplay
- `teleportCooldown` - Cooldown between teleports (in seconds)
- `restrictToCurrentDimension` - Only allow teleport to exploration version of current dimension
- `clearInventoryOnReturn` - Clear inventory when returning from exploration

### Performance
- `aggressiveChunkUnloading` - Enable aggressive chunk unloading
- `maxChunksPerPlayer` - Maximum chunks loaded per player
- `preventDiskSaves` - Skip saving exploration dimensions to disk
- `preGenerateSpawnChunks` - Pre-generate spawn chunks on dimension creation

## Compass Mod Compatibility

### Nature's Compass
Nature's Compass works correctly in exploration dimensions because biomes are placed using climate parameters that remain consistent regardless of the dimension's seed.

### Explorer's Compass  
Explorer's Compass currently has limited functionality in exploration dimensions. Structures generate at different locations due to seed modifications, but the compass calculates positions using the original world seed. This mismatch means the compass cannot reliably find most structures.

**Debug Command**: Use `/explorationadmin debug compass` to verify dimension seed information.

## License

As of July 2, 2025, Brecher's Dimensions and Brecher's Exploration Dimensions are made available subject to the terms and conditions of the GNU Lesser General Public License 3, the full, unedited text of which may be found in the LICENSE.md file in this repository.

## Contributing

To contribute, please contact Einbrecher

## Support

For issues and feature requests, please use the GitHub issue tracker.