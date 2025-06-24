# Exploration Dimension Hopping Feature

## Overview
Players can now teleport between exploration dimensions without returning to their original dimension first when `restrict_to_current_dimension` is set to `false` in the config.

## How It Works

### Teleportation Flow
1. **Initial Teleport**: Player uses `/brecher tp <dimension>` from a normal dimension
   - Return position is saved (e.g., Overworld coordinates)
   - Player enters exploration dimension

2. **Exploration Hopping**: While in an exploration dimension, player can use `/brecher tp <other_dimension>`
   - Original return position is preserved
   - Player moves directly between exploration dimensions
   - No need to return to normal dimension first

3. **Return Home**: Player uses `/brecher return` from any exploration dimension
   - Returns to the original saved position
   - Works regardless of how many exploration dimensions were visited

### Example Sequence
```
Overworld (100, 64, 200) → /brecher tp minecraft:the_nether
↓ (saves return position: Overworld 100, 64, 200)
Exploration_Nether → /brecher tp minecraft:the_end  
↓ (preserves return position)
Exploration_End → /brecher tp minecraft:overworld
↓ (preserves return position)
Exploration_Overworld → /brecher return
↓
Overworld (100, 64, 200) ← Returns to original position
```

## Configuration

In `brecher_dim-common.toml`:
```toml
[gameplay]
    # When false: allows exploration-to-exploration teleportation
    # When true: requires returning to normal dimension first
    restrict_to_current_dimension = false
```

## Messages

### When teleporting between exploration dimensions:
- "Teleported between exploration dimensions!"
- "Your return point remains in: [dimension_name]"

### When returning:
- "Returned to the main world."
- "Returned to [dimension_name]"

## Safety Features

1. **Return Position Preservation**: The original return position is never overwritten when hopping between exploration dimensions

2. **Emergency Fallback**: If a player somehow ends up in an exploration dimension without a return position, the system saves the overworld spawn as an emergency fallback

3. **Clear Messaging**: Players are informed about their return point status when hopping between dimensions

## Technical Implementation

The feature works by:
1. Checking if the player is teleporting FROM an exploration dimension
2. If yes, skipping the return position save to preserve the original
3. Allowing the teleportation if `restrict_to_current_dimension` is false
4. Maintaining all other safety features (cooldowns, invulnerability, etc.)

This allows for seamless exploration of multiple dimensions while always maintaining a clear path home.