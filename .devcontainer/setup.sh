#!/bin/sh
set -e

echo "Installing Android SDK..."

ANDROID_SDK_ROOT="/usr/local/android-sdk"
CMDLINE_TOOLS_VERSION="11076708"

apt-get update -qq
apt-get install -y -qq wget unzip

mkdir -p $ANDROID_SDK_ROOT/cmdline-tools
cd /tmp
wget -q "https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip" -O cmdline-tools.zip
unzip -q cmdline-tools.zip -d $ANDROID_SDK_ROOT/cmdline-tools
mv $ANDROID_SDK_ROOT/cmdline-tools/cmdline-tools $ANDROID_SDK_ROOT/cmdline-tools/latest
rm cmdline-tools.zip

export ANDROID_HOME=$ANDROID_SDK_ROOT
yes | $ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager --licenses > /dev/null 2>&1
$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager \
  "platform-tools" \
  "platforms;android-34" \
  "build-tools;34.0.0"

chown -R vscode:vscode $ANDROID_SDK_ROOT

echo "sdk.dir=/usr/local/android-sdk" > /workspaces/pupil/local.properties

echo "Android SDK setup complete."