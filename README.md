# Brecher's Dimensions

A sophisticated Minecraft Forge mod that creates temporary, resettable duplicate dimensions at runtime. Built for Minecraft 1.20.1 (Forge 47.4.1), it solves the problem of limited exploration opportunities on servers with world borders by providing fresh, daily-resetting dimensions for players to explore.

## Features

- **Runtime Dimension Creation**: Creates exploration dimensions dynamically at server startup without datapacks
- **Daily Reset System**: Dimensions reset with new seeds each server restart
- **Memory-Only Design**: Exploration dimensions exist only in memory, never saved to disk
- **Safe Teleportation**: Comprehensive safety features including spawn validation and emergency platforms
- **Multi-Dimension Support**: Create exploration copies of Overworld, Nether, End, and modded dimensions
- **Performance Optimized**: Aggressive chunk unloading and memory management for server stability

## Technical Highlights

- **Mixin-Based Registry Manipulation**: Advanced registry manipulation without datapacks
- **Thread-Safe Operations**: Concurrent data structures and locking for multiplayer safety
- **Client-Server Synchronization**: Custom networking for seamless dimension information sync
- **Comprehensive Error Handling**: Multiple fallback strategies and emergency cleanup procedures
- **Per-Dimension-Type Counters**: Consistent naming system (overworld_0, nether_0, etc.)

## Commands

### Player Commands (`/brecher`)
- `list` - Show available exploration dimensions
- `tp <dimension>` - Teleport to exploration dimension
- `return` - Return to saved position
- `info` - Display mod information

### Admin Commands (`/brecheradmin`)
- `returnall` - Evacuate all players from exploration dimensions
- `info <dimension>` - Detailed dimension statistics
- `stats` - Overall mod statistics
- `debug registry` - Registry diagnostics

## Installation

1. Ensure you have Minecraft 1.20.1 with Forge 47.4.1 installed
2. Download the mod JAR file from the releases page
3. Place it in your mods folder
4. Configure the mod via `config/brecher_dim-common.toml`

## Configuration

Key configuration options:
- `enabledDimensions` - List of dimensions to create exploration copies for
- `explorationBorder` - World border size for exploration dimensions
- `preventBedSpawn` - Disable bed spawning in exploration dimensions
- `teleportCooldown` - Cooldown between teleports (in ticks)

## Development

This mod uses advanced Minecraft modding techniques including:
- Mixin-based dimension creation
- Registry manipulation without datapacks
- Comprehensive multiplayer safety features

### Building from Source

```bash
./gradlew build
```

The built JAR will be in `build/libs/`

## License

[Add your license here]

## Contributing

[Add contributing guidelines here]

## Support

For issues and feature requests, please use the GitHub issue tracker.