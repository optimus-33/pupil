#!/bin/bash
set -e

ANDROID_SDK_DIR="/home/codespace/android-sdk"

echo "========================================"
echo "  Android SDK Setup Script"
echo "========================================"

# Check if Android SDK already exists in persistent home directory
if [ -d "$ANDROID_SDK_DIR/platforms/android-34" ] && [ -d "$ANDROID_SDK_DIR/build-tools/34.0.0" ]; then
    echo "✅ Android SDK already installed at $ANDROID_SDK_DIR"
    exit 0
fi

echo "Installing Android SDK to $ANDROID_SDK_DIR..."

# Create SDK directory
mkdir -p "$ANDROID_SDK_DIR"
cd "$ANDROID_SDK_DIR"

# Download command-line tools
echo "Downloading Android SDK command-line tools..."
CMD_LINE_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
curl -fsSL -o cmdline-tools.zip "$CMD_LINE_TOOLS_URL"

# Extract and restructure
echo "Extracting..."
unzip -q cmdline-tools.zip
mkdir -p cmdline-tools/latest
mv cmdline-tools/bin cmdline-tools/lib cmdline-tools/NOTICE.txt cmdline-tools/source.properties cmdline-tools/latest/
rm cmdline-tools.zip

# Export ANDROID_HOME for sdkmanager
export ANDROID_HOME="$ANDROID_SDK_DIR"

# Accept all licenses
echo "Accepting SDK licenses..."
yes | "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --licenses > /dev/null 2>&1

# Install required components
echo "Installing platforms;android-34..."
"$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" "platforms;android-34" > /dev/null 2>&1

echo "Installing build-tools;34.0.0..."
"$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" "build-tools;34.0.0" > /dev/null 2>&1

echo ""
echo "========================================"
echo "  Android SDK Setup Complete!"
echo "  Location: $ANDROID_SDK_DIR"
echo "========================================"
