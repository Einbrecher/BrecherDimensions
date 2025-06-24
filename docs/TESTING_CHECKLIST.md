# Brecher's Dimensions - Comprehensive Testing Checklist

## Pre-Testing Setup

- [ ] Ensure Java 17+ is installed
- [ ] Run `gradlew clean build` to verify compilation
- [ ] Back up any existing test worlds
- [ ] Review command syntax in USAGE_GUIDE.md

## Phase 1: Single Player Testing

### 1.1 Basic Functionality
- [ ] Start development server: `gradlew runServer`
- [ ] Join server from development client: `gradlew runClient`
- [ ] Verify mod loads without errors (check logs)
- [ ] Confirm `/brecheradmin` command is registered

### 1.2 Dimension Creation
- [ ] Create Overworld exploration dimension:
  ```
  /brecheradmin create overworld
  ```
  - [ ] Verify dimension created successfully
  - [ ] Check server logs for registry operations
  - [ ] Confirm no errors or warnings

- [ ] Create Nether exploration dimension:
  ```
  /brecheradmin create the_nether
  ```
  - [ ] Verify different world generation
  - [ ] Check dimension has unique seed

- [ ] Create End exploration dimension:
  ```
  /brecheradmin create the_end
  ```
  - [ ] Verify End-style generation

### 1.3 Dimension Navigation
- [ ] Teleport to exploration dimension:
  ```
  /brecheradmin tp exploration_overworld_1
  ```
  - [ ] Verify teleportation works
  - [ ] Check player position is safe
  - [ ] Confirm return position is saved

- [ ] Return to original dimension:
  ```
  /brecheradmin return
  ```
  - [ ] Verify return to exact previous location
  - [ ] Check no data loss

### 1.4 Dimension Management
- [ ] List all dimensions:
  ```
  /brecheradmin list
  ```
  - [ ] Verify all created dimensions appear
  - [ ] Check status information is accurate

- [ ] Remove dimension (while empty):
  ```
  /brecheradmin remove exploration_overworld_1
  ```
  - [ ] Verify dimension removed cleanly
  - [ ] Check registry cleanup in logs

## Phase 2: Multiplayer Testing

### 2.1 Client Synchronization
- [ ] Start server with mod
- [ ] Connect first client
- [ ] Create new dimension from server console
- [ ] Verify client receives dimension update
- [ ] Connect second client
- [ ] Verify new client receives all dimensions

### 2.2 Multi-Player Dimensions
- [ ] Player 1: Teleport to exploration dimension
- [ ] Player 2: Teleport to same dimension
- [ ] Verify both players see each other
- [ ] Test player interactions work normally

### 2.3 Dimension Removal with Players
- [ ] Place both players in exploration dimension
- [ ] Attempt dimension removal:
  ```
  /brecheradmin remove exploration_overworld_1 force
  ```
  - [ ] Verify players evacuated safely
  - [ ] Check players returned to spawn
  - [ ] Confirm dimension removed

## Phase 3: Performance Testing

### 3.1 Memory Usage
- [ ] Record baseline memory usage
- [ ] Create 10 exploration dimensions
- [ ] Check memory increase
- [ ] Remove all exploration dimensions
- [ ] Verify memory returns near baseline

### 3.2 Stress Testing
- [ ] Create 50 dimensions rapidly:
  ```bash
  for i in {1..50}; do
    /brecheradmin create overworld
  done
  ```
  - [ ] Monitor server TPS
  - [ ] Check for memory leaks
  - [ ] Verify all dimensions functional

### 3.3 Concurrent Operations
- [ ] Multiple players creating dimensions simultaneously
- [ ] Multiple players teleporting simultaneously
- [ ] Dimension creation during removal
- [ ] Verify no race conditions or crashes

## Phase 4: Edge Cases

### 4.1 Error Handling
- [ ] Create dimension with invalid name
- [ ] Teleport to non-existent dimension
- [ ] Remove dimension multiple times
- [ ] Create duplicate dimension names

### 4.2 Server Restart
- [ ] Create several exploration dimensions
- [ ] Stop server normally
- [ ] Restart server
- [ ] Verify dimensions persist
- [ ] Check player can still teleport

### 4.3 Crash Recovery
- [ ] Create exploration dimensions
- [ ] Force-kill server (simulate crash)
- [ ] Restart server
- [ ] Verify cleanup occurred
- [ ] Check no corrupted data

## Phase 5: Integration Testing

### 5.1 Forge Compatibility
- [ ] Test with Forge 47.4.1
- [ ] Verify mixin integration works
- [ ] Check for Forge event conflicts

### 5.2 Common Mod Compatibility
- [ ] JEI (Just Enough Items)
  - [ ] Verify no rendering issues
  - [ ] Check recipe lookup works

- [ ] JourneyMap
  - [ ] Verify dimension mapping works
  - [ ] Check waypoints in exploration dimensions

- [ ] FTB Chunks
  - [ ] Verify claiming disabled in exploration
  - [ ] Check no chunk protection conflicts

### 5.3 Performance Mods
- [ ] OptiFine/Sodium compatibility
- [ ] Chunk pregenerator compatibility
- [ ] ServerCore optimization compatibility

## Phase 6: Configuration Testing

### 6.1 Config Options
- [ ] Modify dimension limits
- [ ] Change reset schedules
- [ ] Toggle feature restrictions
- [ ] Verify all options work

### 6.2 Permissions
- [ ] Test admin-only commands
- [ ] Verify permission checks work
- [ ] Test with permission mods

## Phase 7: Long-Term Stability

### 7.1 24-Hour Test
- [ ] Run server for 24 hours
- [ ] Create/remove dimensions periodically
- [ ] Monitor memory usage over time
- [ ] Check for memory leaks

### 7.2 Daily Reset Testing
- [ ] Configure daily reset
- [ ] Leave server running overnight
- [ ] Verify dimensions reset properly
- [ ] Check player data preserved

## Post-Testing Checklist

### Documentation Updates
- [ ] Update README with test results
- [ ] Document any discovered limitations
- [ ] Add troubleshooting entries for issues

### Performance Metrics
- [ ] Average dimension creation time: _____ ms
- [ ] Average teleportation time: _____ ms
- [ ] Memory per dimension: _____ MB
- [ ] Maximum tested dimensions: _____

### Issue Tracking
- [ ] Log all discovered bugs
- [ ] Prioritize critical issues
- [ ] Document workarounds

## Sign-Off

- [ ] All critical tests passed
- [ ] Performance acceptable
- [ ] No data loss scenarios
- [ ] Ready for production use

**Tested By**: _________________  
**Date**: _________________  
**Version**: 0.1-1.20.1