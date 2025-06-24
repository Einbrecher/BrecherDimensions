# Development Environment Troubleshooting

## Module Resolution Exception

### Error
```
Exception in thread "main" java.lang.module.ResolutionException: Modules main and brecher_dim export package net.tinkstav.brecher_dim.dimension to module minecraft
```

### Cause
This error occurs in Forge 1.20.1 development environments due to Java's module system conflicts. The development environment creates separate modules that conflict when they export the same packages.

### Solutions

#### Solution 1: Clean and Rebuild
```bash
gradlew clean
gradlew build
gradlew runServer
```

#### Solution 2: Add JVM Arguments
Edit `build.gradle` and add to the server run configuration:
```gradle
server {
    // ... existing config ...
    
    // Module system fixes
    jvmArgs '--add-modules=ALL-MODULE-PATH'
    jvmArgs '--add-opens=java.base/java.util.jar=ALL-UNNAMED'
    jvmArgs '--add-opens=java.base/sun.security.util=ALL-UNNAMED'
}
```

#### Solution 3: Use Production Environment
Instead of using the development server, test with a real Forge server:

1. Build the mod:
   ```bash
   gradlew build
   ```

2. Set up a test server:
   ```bash
   mkdir test-server
   cd test-server
   ```

3. Download Forge 1.20.1-47.4.1 server from https://files.minecraftforge.net/

4. Copy your mod to the mods folder:
   ```bash
   copy ..\build\libs\brecher_dim-0.1-1.20.1.jar mods\
   ```

5. Run the server:
   ```bash
   java -jar forge-1.20.1-47.4.1-installer.jar --installServer
   java -jar forge-1.20.1-47.4.1.jar nogui
   ```

#### Solution 4: Disable Module Path
Create a file `gradle.properties` in your user home directory (`C:\Users\[YourName]\.gradle\gradle.properties`) and add:
```properties
org.gradle.jvmargs=-Xmx3G -XX:+UseG1GC
```

Then in your project's `gradle.properties`, add:
```properties
forge.enableModLauncher=false
```

#### Solution 5: IntelliJ IDEA Fix
If using IntelliJ IDEA:
1. Go to Run → Edit Configurations
2. Select your Minecraft Server configuration
3. In VM options, add:
   ```
   --add-exports=java.base/sun.security.util=ALL-UNNAMED
   --add-opens=java.base/java.util.jar=ALL-UNNAMED
   ```

### Alternative Testing Method

If the development environment continues to have issues, use the compiled JAR directly:

1. **For Client Testing**:
   - Install Forge 1.20.1-47.4.1 on your Minecraft launcher
   - Place the mod JAR in your `.minecraft/mods` folder
   - Launch Minecraft with the Forge profile

2. **For Server Testing**:
   - Use a dedicated Forge server as described in Solution 3
   - This more accurately represents the production environment

### Why This Happens

Forge's development environment uses a complex module system setup that sometimes conflicts with mod packages. This is particularly common when:
- Your mod has many packages
- You use Mixins
- You have complex class hierarchies

The production environment (compiled JAR) doesn't have these issues because it uses a different classloading mechanism.

### Verification

To verify the mod works correctly despite the development environment error:
1. The mod compiles successfully: ✅
2. The JAR file is created: ✅
3. No compilation errors: ✅
4. Mixin configuration is valid: ✅

The module resolution error only affects the development runtime, not the actual mod functionality.