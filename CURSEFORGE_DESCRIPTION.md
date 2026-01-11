Brecher's Exploration Dimensions adds rotating duplicate dimensions to Minecraft servers - providing unlimited exploration opportunities, even with World Borders enabled!


# Brecher's Exploration Dimensions 

## Unlimited Exploration Potential; Itty Bitty Disk Space

Brecher's Explorations Dimensions creates **rotating** duplicates of existing dimensions that are replaced with **entirely new worlds** on each server restart. Perfect for servers with world borders enabled, these dimensions offer untouched exploration, mining, and resource gathering opportunities to players while requiring minimal oversight from server admins. No longer do you need to manually prune chunks, reset worlds, fiddle with seeds or regenerate worlds to keep things fresh and disk usage low - this mod does it for you, automatically.

## Key Features

**Pristine Worlds, Regularly Delivered**
- "Exploration Dimensions" are replaced automatically with new worlds (using incremented IDs) on every server restart
- Seeds are propagated to both terrain generation and structure generation/placement - a "new" world each cycle
- Original world remains untouched and protected
- Stragglers are automatically evacuated from Exploration Dimensions before the dimensions are cycled
- Compatible with all Vanilla dimensions - modded dimensions should theoretically work, but are untested as of yet
- Compatible with many of your favorite world generation mods/datapacks

**Player Interaction**
- Players access the Exploration Dimensions using slash commands and/or clickable chat menus
- Return tracking remembers where players came from
- Safe teleportation with intelligent spawn point finding
- Emergency platform creation if no safe spot is found
- Automatic player evacuation on server restart/crash

**Notable Config Options**
- Specify which dimensions to duplicate
- Option to limit teleports between like dimensions only (e.g., Overworld -> Overworld, Nether -> Nether)
- Option to set world border independent of normal dimensions
- Option to choose how many - if any - "old" Exploration Dimensions to keep
- Option to disable saving Exploration Dimensions to disk entirely (Chunks are wiped once unloaded)
- Automatic cleanup of old map data (Xaero's Compatibility, Journeymap WIP)

## Commands

**Player Commands** (`/exploration`)
- `list` - View available exploration dimensions
- `tp <dimension>` - Teleport to exploration dimension
- `return` - Return to saved position
- `info` - Display mod information

**Admin Commands** (`/explorationadmin`)
- `returnall` - Evacuate all players
- `info <dimension>` - Dimension statistics
- `stats` - Performance statistics
- `debug` - Troubleshooting tools

## Perfect For
- Survival servers with world borders that would otherwise limit exploration, mining, and/or resource gathering opportunities
- Servers updating to newer versions or adding new mods/content - Exploration Dimensions are all "new" chunks
- Server admins conscious of disk usage - 
- SMP servers wanting fresh content on each server restart without affecting main worlds
- Any server wanting unlimited exploration without world bloat

## Known Limitations, Future Plans
- Mods that use custom generation/placement systems (e.g., AE2) do not yet properly receive the updated world seeds
- Support for LOD mods, mapping mods to reduce caching impacts for old/deleted Exploration Dimensions
- Wider version support

## Installation

Drop the mod file into your server's mods folder and configure via `config/brecher_dim-common.toml`. The mod handles all dimension management automatically.

**Give your players infinite exploration opportunities without the headache!**