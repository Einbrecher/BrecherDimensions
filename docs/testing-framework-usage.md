# Testing Framework Usage Guide

## Overview
This document explains how to use the comprehensive testing framework developed for the Brecher Dimensions registry manipulation system. The framework provides automated testing, performance validation, and system verification capabilities.

## Framework Components

### 1. Test Infrastructure Classes

#### `TestUtilities.java`
Central utility class providing:
- Mock server creation and management
- Test data generation (dimensions, players, chunk generators)
- Reflection-based method testing
- Performance measurement tools
- Memory validation utilities

#### `MockMinecraftServer.java`
Complete mock implementation of MinecraftServer:
- Implements `IServerDimensionAccessor` interface
- Runtime dimension creation/removal
- Player management simulation
- Registry access through MockRegistryAccess

#### `MockRegistryAccess.java`
Mock registry system with mixin support:
- Enhanced registries with `IRegistryAccessor` implementation
- Runtime entry tracking and validation
- Thread-safe registry operations
- Proper cleanup mechanisms

#### `MockRegistry.java`
Base registry implementation providing:
- Complete Registry<T> interface implementation
- Runtime entry management
- State validation methods
- Holder pattern support

### 2. Test Execution Classes

#### `RegistryManipulationTest.java`
JUnit 5 test suite covering:
- Unit tests for individual components
- Integration tests for system workflows
- Performance benchmarks
- Memory leak detection
- Thread safety validation

#### `TestRunner.java`
Standalone test runner demonstrating:
- Complete test suite execution
- Performance measurement
- Stress testing scenarios
- Result aggregation and reporting

### 3. Simulation Classes

#### `TestPlayer.java`
Player simulation for multiplayer testing:
- Dimension teleportation
- Player evacuation scenarios
- Multi-player test scenarios
- Online/offline state management

#### `MockServerLevel.java` & `MockChunkGenerator.java`
World simulation components:
- Simplified level implementation
- Mock chunk generation
- Player tracking within dimensions

## Running Tests

### Option 1: JUnit 5 Integration (Recommended)

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "RegistryManipulationTest"

# Run with verbose output
./gradlew test --info

# Generate test report
./gradlew test jacocoTestReport
```

### Option 2: Standalone Test Runner

```bash
# Compile test classes
./gradlew testClasses

# Run standalone test runner
java -cp "build/classes/java/test:build/classes/java/main" \
     net.tinkstav.brecher_dim.test.TestRunner
```

### Option 3: IDE Integration

1. **IntelliJ IDEA**:
   - Right-click on test class → "Run Tests"
   - Use built-in test runner with coverage analysis

2. **Eclipse**:
   - Right-click → Run As → JUnit Test
   - View results in JUnit view

## Test Categories

### 1. Unit Tests
**Purpose**: Validate individual component functionality

**Coverage**:
- Registry ID allocation algorithms
- Thread-safe registry operations
- Memory leak prevention
- State validation methods

**Example Output**:
```
--- Testing Registry ID Allocation ---
✓ Successfully allocated 100 unique IDs
✓ ID range: 100 to 199

--- Testing Registry Thread Safety ---
✓ Completed 200 operations in 1250ms
✓ Success: 195, Failures: 5
✓ Throughput: 156 operations/second
```

### 2. Integration Tests
**Purpose**: Verify system-wide functionality

**Coverage**:
- End-to-end dimension creation
- Player teleportation workflows
- Registry synchronization
- Cleanup procedures

**Example Output**:
```
--- Testing Dimension Creation Flow ---
Initial registry sizes - DimensionType: 1, LevelStem: 0
✓ Dimension created successfully: brecher_test:integration_test
✓ Registry sizes - DimensionType: 2, LevelStem: 1
✓ Dimension removed successfully
```

### 3. Performance Tests
**Purpose**: Validate system performance characteristics

**Coverage**:
- Operation timing benchmarks
- Memory usage monitoring
- Throughput measurement
- Scalability validation

**Example Output**:
```
--- Performance Benchmark ---
Warming up...
Running benchmark...
✓ Benchmark Results:
  - Successful operations: 198/200
  - Average time per operation: 12.34ms
  - Operations per second: 81
```

### 4. Memory Tests
**Purpose**: Detect memory leaks and validate resource management

**Coverage**:
- Long-running operation cycles
- Garbage collection impact
- Resource cleanup verification
- Memory growth monitoring

**Example Output**:
```
--- Testing Memory Leak Detection ---
Initial memory: 245 MB
Iteration 100: Memory growth = 2 MB
Iteration 200: Memory growth = 3 MB
Iteration 300: Memory growth = 4 MB
Iteration 400: Memory growth = 3 MB
Iteration 500: Memory growth = 4 MB
✓ Completed 500 iterations
✓ Total memory growth: 4 MB
```

### 5. Stress Tests
**Purpose**: Validate system behavior under extreme conditions

**Coverage**:
- Rapid dimension creation/destruction
- High player count scenarios
- Resource exhaustion handling
- Concurrent operation stress

## Test Configuration

### Memory Settings
```bash
# For comprehensive testing
export JAVA_OPTS="-Xmx4G -XX:+UseG1GC -XX:+PrintGC"

# For memory leak detection
export JAVA_OPTS="-Xmx2G -XX:+UseG1GC -XX:+PrintGCDetails -XX:+TrackClassLoading"
```

### Test Properties
Create `test.properties` file:
```properties
# Test configuration
test.dimension.count=10
test.player.count=50
test.memory.threshold=100MB
test.timeout.seconds=60

# Performance thresholds
perf.dimension.creation.max=50ms
perf.registry.operation.max=10ms
perf.memory.growth.max=50MB
```

## Interpreting Results

### Success Criteria

#### ✅ **Functional Tests**
- All unit tests pass (100% success rate)
- Integration workflows complete successfully
- No exceptions or errors during normal operations
- Registry state validation passes

#### ✅ **Performance Tests**
- Registry operations complete within 50ms
- Dimension creation averages under 100ms
- Memory usage remains stable over time
- No significant performance degradation

#### ✅ **Memory Tests**
- Memory growth < 50MB after 500 operations
- No memory leaks in 24-hour testing
- Proper cleanup of all resources
- GC behavior within normal parameters

#### ✅ **Stress Tests**
- System handles 100+ rapid operations
- Supports 50+ concurrent players
- Graceful degradation under load
- Recovery from resource exhaustion

### Failure Analysis

#### Common Issues:
1. **Registry Casting Errors**: May indicate mixin not applied properly
2. **Memory Growth**: Potential resource leaks or inefficient cleanup
3. **Performance Degradation**: May indicate inefficient algorithms
4. **Thread Safety Issues**: Race conditions in concurrent operations

#### Investigation Steps:
1. Check test logs for specific error messages
2. Use memory profiler for detailed analysis
3. Enable debug logging for detailed operation tracing
4. Run individual test methods to isolate issues

## Continuous Integration

### GitHub Actions Workflow
```yaml
name: Registry Testing
on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        java: [17, 19]
        
    steps:
      - uses: actions/checkout@v3
      - name: Setup JDK
        uses: actions/setup-java@v3
        with:
          java-version: ${{ matrix.java }}
          
      - name: Run Unit Tests
        run: ./gradlew test --tests "*RegistryManipulationTest*"
        
      - name: Run Performance Tests
        run: ./gradlew test --tests "*PerformanceTest*"
        
      - name: Memory Leak Detection
        run: |
          ./gradlew testClasses
          java -Xmx1G -XX:+PrintGC \
               net.tinkstav.brecher_dim.test.TestRunner
               
      - name: Generate Reports
        run: ./gradlew jacocoTestReport
        
      - name: Upload Coverage
        uses: codecov/codecov-action@v3
```

## Custom Test Development

### Creating New Tests

1. **Unit Test Example**:
```java
@Test
@DisplayName("Custom Registry Operation")
void testCustomOperation() {
    MockMinecraftServer server = (MockMinecraftServer) TestUtilities.createTestServer();
    
    // Test setup
    Registry<DimensionType> registry = server.registryAccess()
        .registryOrThrow(Registries.DIMENSION_TYPE);
    
    // Test execution
    ResourceKey<DimensionType> key = TestUtilities.createTestDimensionKey("custom_test");
    DimensionType type = TestUtilities.createTestDimensionType();
    
    // Validation
    Assertions.assertDoesNotThrow(() -> {
        ((IRegistryAccessor<DimensionType>) registry)
            .brecher_dim$addMapping(-1, key, type);
    });
    
    // Cleanup
    server.shutdown();
}
```

2. **Performance Test Example**:
```java
@Test
void benchmarkCustomOperation() {
    long startTime = System.nanoTime();
    
    // Operation to benchmark
    performCustomOperation();
    
    long endTime = System.nanoTime();
    long duration = endTime - startTime;
    
    Assertions.assertTrue(duration < 50_000_000, // 50ms
        "Operation too slow: " + (duration / 1_000_000) + "ms");
}
```

### Test Data Generation

```java
// Create test dimensions
ResourceKey<Level> dimension = TestUtilities.createTestLevelKey("my_test");

// Create test players
TestPlayer player = TestUtilities.createTestPlayer(server, "TestPlayer");

// Create test scenarios
TestPlayer.TestScenario scenario = new TestPlayer.TestScenario(server, 10);
```

## Best Practices

### 1. Test Organization
- Group related tests in test classes
- Use descriptive test names
- Implement proper setup/teardown
- Isolate test dependencies

### 2. Resource Management
- Always shutdown mock servers
- Clean up test dimensions
- Disconnect test players
- Force garbage collection in memory tests

### 3. Assertions
- Use specific assertion messages
- Test both positive and negative cases
- Validate state before and after operations
- Check error conditions explicitly

### 4. Performance Testing
- Include warmup phases
- Run multiple iterations
- Measure relevant metrics
- Set realistic thresholds

## Troubleshooting

### Common Test Failures

#### "Registry not accessible for modification"
- **Cause**: Registry doesn't implement IRegistryAccessor
- **Solution**: Verify mixin application or use reflection-based testing

#### "Memory leak detected"
- **Cause**: Resources not properly cleaned up
- **Solution**: Review cleanup methods and force GC

#### "Thread safety violation"
- **Cause**: Race condition in concurrent operations
- **Solution**: Review locking mechanisms and synchronization

#### "Test timeout"
- **Cause**: Operation taking too long or deadlock
- **Solution**: Increase timeout or review algorithm efficiency

### Debug Mode
Enable detailed logging:
```java
System.setProperty("test.debug", "true");
System.setProperty("test.verbose", "true");
```

### Memory Analysis
```bash
# Run with memory profiling
java -XX:+PrintGCDetails -XX:+TrackClassLoading \
     -Xmx2G -jar test-runner.jar
```

## Conclusion

This testing framework provides comprehensive validation of the registry manipulation system, ensuring reliability, performance, and production readiness. Regular execution of these tests during development helps maintain code quality and prevents regressions.

For additional support or custom test development, refer to the example implementations in the test package or contact the development team.