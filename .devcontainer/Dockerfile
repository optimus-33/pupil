FROM mcr.microsoft.com/devcontainers/java:17-bullseye

ENV ANDROID_SDK_ROOT=/usr/local/android-sdk
ENV ANDROID_HOME=/usr/local/android-sdk
ENV PATH=$PATH:/usr/local/android-sdk/cmdline-tools/latest/bin:/usr/local/android-sdk/platform-tools:/usr/local/android-sdk/build-tools/34.0.0

RUN apt-get update && apt-get install -y wget unzip && rm -rf /var/lib/apt/lists/*

RUN mkdir -p $ANDROID_SDK_ROOT/cmdline-tools && \
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O /tmp/cmdline-tools.zip && \
    unzip -q /tmp/cmdline-tools.zip -d $ANDROID_SDK_ROOT/cmdline-tools && \
    mv $ANDROID_SDK_ROOT/cmdline-tools/cmdline-tools $ANDROID_SDK_ROOT/cmdline-tools/latest && \
    rm /tmp/cmdline-tools.zip

RUN yes | $ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager --licenses > /dev/null 2>&1 && \
    $ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager \
      "platform-tools" \
      "platforms;android-34" \
      "build-tools;34.0.0"

RUN chown -R vscode:vscode $ANDROID_SDK_ROOT