# Brecher's Dimensions Test Tracker
# Mark tests as they are completed: ✓ Pass, ✗ Fail, ⚠ Issue, - Not Tested

## Test Environment
- Tester Name: ________________
- Date: ________________
- Mod Version: 0.1-1.20.1
- Minecraft: 1.20.1
- Forge: 47.4.1
- Environment: [ ] Single-Player [ ] Dedicated Server
- Other Mods: ________________

## Quick Test Results

### 1. Build & Load
- [ ] Mod builds without errors
- [ ] Mod loads without crashes
- [ ] Config file generates correctly
- [ ] All dimensions register in logs

### 2. Basic Commands (Single Player)
- [ ] `/explore help` displays help
- [ ] `/explore list` shows dimensions
- [ ] `/explore status` shows current status
- [ ] `/explore minecraft:overworld` teleports successfully
- [ ] `/explore return` returns to saved position

### 3. Dimension Features
- [ ] Safe spawn location found
- [ ] Emergency platform created when needed
- [ ] Brief invulnerability works (3 seconds)
- [ ] Welcome message displayed
- [ ] Return position saved correctly

### 4. Reset System
- [ ] Warning at 30 seconds: ___________
- [ ] Warning at 20 seconds: ___________
- [ ] Warning at 10 seconds: ___________
- [ ] Final countdown works: ___________
- [ ] Auto-teleport before reset: ___________
- [ ] Dimension properly cleared: ___________
- [ ] Next reset scheduled: ___________

### 5. Safety Features
- [ ] Cannot portal to normal dimensions
- [ ] Death respawns in overworld
- [ ] Beds show warning (if configured)
- [ ] Login warning in exploration dimension

### 6. Admin Commands
- [ ] `/exploreadmin schedule` shows reset time
- [ ] `/exploreadmin info <dim>` shows details
- [ ] `/exploreadmin reset <dim>` works
- [ ] `/exploreadmin resetall` resets all
- [ ] `/exploreadmin returnall` returns players
- [ ] `/exploreadmin cleanup` clears memory
- [ ] `/exploreadmin forcereset` forces immediate reset

### 7. Multiplayer (if applicable)
- [ ] Multiple players can explore same dimension
- [ ] Each player has own return position
- [ ] All players see reset warnings
- [ ] New players receive dimension sync
- [ ] No desync issues observed

### 8. Performance
- [ ] No TPS drops during normal use
- [ ] Memory usage reasonable (< 100MB additional)
- [ ] Reset completes quickly (< 5 seconds)
- [ ] Chunk unloading works properly
- [ ] No memory leaks observed

## Detailed Test Notes

### Issues Found
Issue #1:
- Test: ________________
- Description: ________________
- Steps to Reproduce: ________________
- Severity: [ ] Critical [ ] High [ ] Medium [ ] Low

Issue #2:
- Test: ________________
- Description: ________________
- Steps to Reproduce: ________________
- Severity: [ ] Critical [ ] High [ ] Medium [ ] Low

### Performance Metrics
- Baseline TPS: ____
- TPS with 1 exploration dimension: ____
- TPS with 3 exploration dimensions: ____
- Memory at start: ____MB
- Memory with dimensions: ____MB
- Memory after reset: ____MB

### Additional Notes
_________________________________________________
_________________________________________________
_________________________________________________

## Test Summary
- Total Tests Run: ____
- Tests Passed: ____
- Tests Failed: ____
- Issues Found: ____
- Ready for Release: [ ] Yes [ ] No

Tester Signature: ________________