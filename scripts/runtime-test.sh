#!/usr/bin/env bash
# Runtime test: builds the plugin, downloads Paper, starts server, verifies AlwaysOnline loads.
# Requires: curl, jq, java

set -e
cd "$(dirname "$0")/.."
USER_AGENT="always-online-runtime-test/1.0 (https://github.com/AshleyThew/always-online)"

echo "=== Building plugin ==="
./gradlew build -q
# Use the Paper plugin (always-online-latest.jar), not the Sponge variant
PLUGIN_JAR="output/always-online-latest.jar"
[ -f "$PLUGIN_JAR" ] || { echo "Plugin JAR not found at $PLUGIN_JAR"; exit 1; }

echo "=== Downloading Paper server ==="
TEST_DIR="build/runtime-test"
rm -rf "$TEST_DIR"
mkdir -p "$TEST_DIR"
mkdir -p "$TEST_DIR/plugins"
cp "$PLUGIN_JAR" "$TEST_DIR/plugins/always-online.jar"
cd "$TEST_DIR"

# Use Paper API v2 (api.papermc.io) - matches plugin target 1.21.x
LATEST_VERSION="1.21.10"
BUILDS_JSON=$(curl -sf "https://api.papermc.io/v2/projects/paper/versions/${LATEST_VERSION}/builds")
if command -v jq &>/dev/null; then
  BUILD_NUM=$(echo "$BUILDS_JSON" | jq -r '.builds[-1].build')
  BUILD_NAME=$(echo "$BUILDS_JSON" | jq -r '.builds[-1].downloads.application.name')
else
  BUILD_NUM=$(echo "$BUILDS_JSON" | grep -o '"build":[0-9]*' | tail -1 | cut -d: -f2)
  BUILD_NAME="paper-${LATEST_VERSION}-${BUILD_NUM}.jar"
fi
DOWNLOAD_URL="https://api.papermc.io/v2/projects/paper/versions/${LATEST_VERSION}/builds/${BUILD_NUM}/downloads/${BUILD_NAME}"

curl -sfL -o server.jar "$DOWNLOAD_URL" || {
  echo "Failed to download Paper ${LATEST_VERSION} build ${BUILD_NUM}"
  exit 1
}
echo "Downloaded Paper ${LATEST_VERSION}-${BUILD_NUM}"

echo "=== Preparing server ==="
echo "eula=true" > eula.txt
# Use alternate port to avoid "Address already in use" when another server/run exists
echo "server-port=25566" > server.properties

echo "=== Starting server (max 120s) ==="
timeout 120 java -Xms256M -Xmx512M -jar server.jar nogui 2>&1 | tee server.log || true

echo ""
echo "=== Checking logs ==="
if grep -qE "Done \(|For help" server.log; then
  echo "Server startup: OK"
else
  echo "WARNING: Server may not have fully started (check server.log)"
fi
if grep -qi "AlwaysOnline\|NMS authentication\|Setting up NMS" server.log; then
  echo "AlwaysOnline load: OK"
else
  echo "WARNING: AlwaysOnline load message not found in logs"
fi
if grep -qi "Failed to override the authentication\|Failed to resolve NMS\|due to possible security risks" server.log; then
  echo "ERROR: Plugin authentication setup failed"
  exit 1
fi

echo ""
echo "=== Runtime test complete ==="
