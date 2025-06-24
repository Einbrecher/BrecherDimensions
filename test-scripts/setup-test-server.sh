#!/bin/bash
# Brecher's Dimensions Test Server Setup Script

echo "=== Brecher's Dimensions Test Server Setup ==="
echo "This script sets up a test server environment"
echo ""

# Check if Java is available
if ! command -v java &> /dev/null; then
    echo "ERROR: Java is not installed or not in PATH"
    echo "Please install Java 17 and try again"
    exit 1
fi

# Check Java version
java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [[ "$java_version" -lt 17 ]]; then
    echo "ERROR: Java 17 or higher required (found Java $java_version)"
    exit 1
fi

echo "✓ Java $java_version found"

# Create test server directory
TEST_DIR="test-server"
if [ -d "$TEST_DIR" ]; then
    echo "Test server directory already exists. Remove it? (y/n)"
    read -r response
    if [[ "$response" == "y" ]]; then
        rm -rf "$TEST_DIR"
    else
        echo "Aborting setup"
        exit 1
    fi
fi

mkdir -p "$TEST_DIR"
cd "$TEST_DIR" || exit

echo "✓ Created test server directory"

# Download Forge installer
FORGE_VERSION="47.4.1"
MC_VERSION="1.20.1"
FORGE_INSTALLER="forge-${MC_VERSION}-${FORGE_VERSION}-installer.jar"

echo "Downloading Forge installer..."
if command -v wget &> /dev/null; then
    wget -q "https://maven.minecraftforge.net/net/minecraftforge/forge/${MC_VERSION}-${FORGE_VERSION}/forge-${MC_VERSION}-${FORGE_VERSION}-installer.jar" -O "$FORGE_INSTALLER"
elif command -v curl &> /dev/null; then
    curl -sL "https://maven.minecraftforge.net/net/minecraftforge/forge/${MC_VERSION}-${FORGE_VERSION}/forge-${MC_VERSION}-${FORGE_VERSION}-installer.jar" -o "$FORGE_INSTALLER"
else
    echo "ERROR: Neither wget nor curl found. Please install one."
    exit 1
fi

if [ ! -f "$FORGE_INSTALLER" ]; then
    echo "ERROR: Failed to download Forge installer"
    exit 1
fi

echo "✓ Downloaded Forge installer"

# Install Forge server
echo "Installing Forge server..."
java -jar "$FORGE_INSTALLER" --installServer > /dev/null 2>&1

if [ ! -f "forge-${MC_VERSION}-${FORGE_VERSION}.jar" ]; then
    echo "ERROR: Forge installation failed"
    exit 1
fi

echo "✓ Forge server installed"

# Accept EULA
echo "eula=true" > eula.txt
echo "✓ Accepted EULA"

# Copy server properties
cp ../test-scripts/test-server.properties server.properties
echo "✓ Copied test server properties"

# Create mods directory and copy mod
mkdir -p mods
if [ -f "../build/libs/brecher_dim-0.1-1.20.1.jar" ]; then
    cp "../build/libs/brecher_dim-0.1-1.20.1.jar" mods/
    echo "✓ Copied Brecher's Dimensions mod"
else
    echo "⚠ Mod JAR not found at ../build/libs/brecher_dim-0.1-1.20.1.jar"
    echo "  Please build the mod first with: ./gradlew build"
fi

# Create config directory and copy test config
mkdir -p config
cp ../test-scripts/test-config.toml config/brecher_dim-common.toml
echo "✓ Copied test configuration"

# Create run script
cat > run-test-server.sh << 'EOF'
#!/bin/bash
# Brecher's Dimensions Test Server Runner

echo "Starting Brecher's Dimensions test server..."
echo "Use Ctrl+C to stop the server"
echo ""

# Allocate 4GB RAM for testing
java -Xms2G -Xmx4G -jar forge-*.jar nogui
EOF

chmod +x run-test-server.sh
echo "✓ Created run script"

# Create ops.json with test player
cat > ops.json << 'EOF'
[
  {
    "uuid": "test-uuid",
    "name": "TestPlayer",
    "level": 4,
    "bypassesPlayerLimit": false
  }
]
EOF
echo "✓ Created ops.json for testing"

echo ""
echo "=== Setup Complete ==="
echo ""
echo "Test server created in: $(pwd)"
echo ""
echo "To start the test server:"
echo "  cd $TEST_DIR"
echo "  ./run-test-server.sh"
echo ""
echo "Test with the following:"
echo "  - Server IP: localhost:25565"
echo "  - RCON: localhost:25575 (password: testing123)"
echo "  - Test commands available in: ../test-scripts/test-commands.txt"
echo ""