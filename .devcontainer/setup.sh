#!/bin/bash
set -e

echo "Installing Android SDK..."

# Install dependencies
sudo apt-get update -qq
sudo apt-get install -y -qq wget unzip zipalign

# Download Android command-line tools
ANDROID_SDK_ROOT="/usr/local/android-sdk"
CMDLINE_TOOLS_VERSION="11076708"

sudo mkdir -p $ANDROID_SDK_ROOT/cmdline-tools
cd /tmp
wget -q "https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip" -O cmdline-tools.zip
sudo unzip -q cmdline-tools.zip -d $ANDROID_SDK_ROOT/cmdline-tools
sudo mv $ANDROID_SDK_ROOT/cmdline-tools/cmdline-tools $ANDROID_SDK_ROOT/cmdline-tools/latest
rm cmdline-tools.zip

# Accept licenses and install SDK components
export ANDROID_HOME=$ANDROID_SDK_ROOT
yes | sudo $ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager --licenses > /dev/null 2>&1
sudo $ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager \
  "platform-tools" \
  "platforms;android-34" \
  "build-tools;34.0.0"

# Fix permissions
sudo chown -R vscode:vscode $ANDROID_SDK_ROOT

echo "Android SDK setup complete."

echo "sdk.dir=/usr/local/android-sdk" > /workspaces/pupil/local.properties