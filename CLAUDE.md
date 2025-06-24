# Brecher's Dimensions - AI Assistant Guide

## Project Overview

**Brecher's Dimensions** is a sophisticated Minecraft Forge mod that creates temporary, resettable duplicate dimensions at runtime. Built for Minecraft 1.20.1 (Forge 47.4.1), it solves the problem of limited exploration opportunities on servers with world borders by providing fresh, daily-resetting dimensions for players to explore.

## Core Architecture

### Technical Foundation
- **Startup Dimension Creation**: Creates all exploration dimensions at server startup using Mixin-based registry manipulation
- **Registry Manipulation**: Temporarily unfreezes Minecraft's registries during startup to register dimensions without datapacks
- **Memory-Only Design**: Dimensions exist only in memory, are never saved to disk, and reset with new seeds on each restart
- **Thread-Safe Operations**: Uses ConcurrentHashMap and concurrent collections for thread safety
- **Client-Server Sync**: Custom networking packets ensure all clients receive dimension information

### Key Components

#### 1. **Dimension Management** (`dimension/` package)
- `BrecherDimensionManager`: Tracks exploration dimensions and player locations during server runtime
- `DimensionRegistrar`: Creates all dimensions from enabledDimensions config at server startup
- `DynamicDimensionFactory`: Creates dimensions with per-type counters (overworld_0, nether_0, etc.)
- `ExplorationDimensionTypes`: Provides dimension type templates for overworld, nether, and end

#### 2. **Mixin System** (`mixin/` package)
- `MixinMinecraftServer`: Enables dimension creation at startup (removal method exists but unused)
- `MixinRegistryFixed`: Allows modification of frozen registries with adaptive field discovery
- `MixinPlayerList`: Syncs dimension info to players when they join
- `MixinServerLevel`: Prevents exploration dimensions from saving to disk
- `MixinClientPacketListener`: Client-side registry synchronization

#### 3. **Player Features**
- **Commands**: `/brecher` for players, `/brecheradmin` for operators
- **Teleportation**: Safe teleport with position finding, emergency platform creation, and return tracking
- **Safety**: Automatic evacuation on restart/crash, spawn safety validation, invulnerability on arrival

#### 4. **Data & Configuration**
- **Config System**: Controls which dimensions to create, safety features, and performance settings
- **SavedData**: Tracks player positions, return locations, and dimension access history
- **Seed Management**: Random, date-based, or debug seed strategies
- **Counter System**: Per-dimension-type counters for consistent naming (overworld_0, nether_0, etc.)

#### 5. **Performance & Monitoring**
- **Memory Management**: Aggressive chunk unloading and entity cleanup in exploration dimensions
- **Registry Monitoring**: Statistics tracking, validation, emergency cleanup
- **Event System**: Handles portal blocking, death respawning, player tracking, server shutdown

## Design Philosophy

### Core Principles
1. **Temporary by Design**: Everything in exploration dimensions is ephemeral
2. **Server Stability**: Extensive error handling, fallbacks, and emergency cleanup
3. **User Experience**: Clear messages, clickable UI elements, warnings before resets
4. **Performance First**: Aggressive optimization for memory and CPU usage
5. **Compatibility**: Works alongside other mods with configurable restrictions

### Technical Approach
- **Mixin over Reflection**: Direct bytecode modification for reliability and performance
- **Defensive Programming**: Multiple fallback strategies for critical operations
- **Atomic Operations**: Batch updates and transactional registry modifications
- **Resource Management**: Automatic cleanup with scheduled executors

## Development Environment

### Requirements
- **Java 17** (required for Minecraft 1.20.1)
- **Gradle 7.6+** (wrapper included)
- **IDE**: IntelliJ IDEA (recommended) or Eclipse

### Build Configuration
```gradle
minecraft 'net.minecraftforge:forge:1.20.1-47.4.1'
annotationProcessor 'org.spongepowered:mixin:0.8.5:processor'
```

### Key Files
- `build.gradle`: Main build configuration with Mixin support
- `brecher_dim.mixins.json`: Mixin configuration
- `accesstransformer.cfg`: Makes private Minecraft fields accessible
- `mods.toml`: Mod metadata and dependencies

## Project Structure
```
src/main/java/net/tinkstav/brecher_dim/
├── Brecher_Dim.java          # Main mod class
├── accessor/                 # Interfaces for Mixin access
├── commands/                 # Player and admin commands
├── config/                   # Configuration management
├── data/                     # Persistent data handling
├── debug/                    # Diagnostic tools
├── dimension/                # Core dimension management
├── event/                    # Forge event handlers
├── mixin/                    # Mixin implementations
│   └── client/              # Client-side mixins
├── network/                  # Client-server packets
├── performance/              # Memory and chunk management
├── teleport/                 # Teleportation logic
└── util/                     # Helper utilities
```

## Configuration Options

### General Settings
- `explorationBorder`: World border size for exploration dimensions (-1 for same as parent)

### Dimension Settings
- `enabledDimensions`: List of dimensions to create exploration copies for (determines total count)
- `allowModdedDimensions`: Support for modded dimension exploration
- `blacklist`: Dimensions that should never have exploration copies

### Feature Settings
- `preventBedSpawn`: Disable bed spawning in exploration dimensions
- `disableEnderChests`: Block ender chest access
- `clearInventoryOnReturn`: Clear inventory when returning
- `disableModdedPortals`: Prevent modded portal usage

### Gameplay Settings
- `teleportCooldown`: Cooldown in ticks between teleports (20 ticks = 1 second)
- `restrictToCurrentDimension`: When true, players can only teleport to the exploration version of their current dimension

### Performance Settings
- `chunkUnloadDelay`: Aggressive chunk unloading timing
- `maxChunksPerPlayer`: Per-player chunk loading limits
- `preventDiskSaves`: Skip saving exploration dimensions to disk
- `oldDimensionRetentionCount`: Number of old dimension folders to keep on disk

## Command Reference

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
- `counter show` - Display current dimension counters
- `counter reset all` - Reset all dimension counters to 0
- `counter reset <dimension>` - Reset specific dimension counter

## Testing & Deployment

### Development Testing
```bash
gradlew runClient    # Test in client environment
gradlew runServer    # Test dedicated server
gradlew test         # Run unit tests
```

### Building for Release
```bash
gradlew clean build  # Creates JAR in build/libs/
```

### Memory Requirements
- Development: 4GB for Gradle, 2GB for tests
- Runtime: Depends on dimension count and player activity

## Known Limitations

1. **Forge 1.20.1**: No native runtime dimension support, requires Mixin workarounds
2. **Memory Usage**: All enabled dimensions remain loaded for entire server session
3. **Modded Compatibility**: Some mods may not recognize dynamically created dimensions
4. **Startup Only**: Dimensions can only be created at server startup, not during runtime

## Future Enhancements

### Planned Features
- Scheduled dimension resets during runtime
- Per-dimension custom generation settings
- Integration APIs for other mods
- Dynamic dimension loading/unloading

### Optimization Opportunities
- On-demand dimension creation instead of startup-only
- Memory optimization for idle dimensions
- Shared chunk generator caching
- Enhanced modded portal detection

## Troubleshooting

### Common Issues
1. **Registry Corruption**: Use `/brecheradmin debug registry` and emergency cleanup
2. **Memory Leaks**: Check dimension limits and chunk unloading settings
3. **Client Desync**: Ensure all players reconnect after server restart
4. **Portal Issues**: Verify portal blocking configuration

### Debug Tools
- Registry statistics monitoring
- Memory usage tracking
- Dimension access history
- Emergency cleanup commands

## Contributing Guidelines

### Code Standards
- Use existing code style and patterns
- Add comprehensive error handling
- Include debug logging for diagnostics
- Test with multiple players

### Testing Requirements
- Verify dimension creation/removal
- Test player evacuation scenarios
- Check memory usage under load
- Validate client synchronization

### Documentation
- Update CLAUDE.md for significant changes
- Document new configuration options
- Add Javadoc for public APIs
- Include usage examples

## Version History

### v0.1-1.20.1 (Current)
- Startup-based dimension creation using Mixin registry manipulation
- Per-dimension-type counters for consistent naming
- Comprehensive crash recovery and spawn safety features
- Automatic old dimension cleanup on disk

---

*This mod demonstrates advanced Minecraft modding techniques including Mixin-based dimension creation, registry manipulation without datapacks, and comprehensive multiplayer safety features. Each server restart provides fresh exploration dimensions with new seeds, perfect for servers with world borders.*