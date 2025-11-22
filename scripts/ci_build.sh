#!/usr/bin/env sh

# ENV PATH
export ANDROID_SDK_ROOT=$HOME/android
export ANDROID_HOME=$HOME/android
export PATH=$ANDROID_SDK_ROOT/tools/bin:$ANDROID_SDK_ROOT/tools:$PATH

cmdtool_version=7302050
uname_s="$(uname -s | tr '[:upper:]' '[:lower:]')"

type wget || {
    echo "you need to have wget installed" &&
        exit 1
}

usage() {
    echo "Script for building the Android app. Must be called from the root of the repo:"
    echo ""
    echo "Must be called with the following arguments (one per call):"
    echo "    dep: installs the basic dependencies for compiling the app."
    echo "    optionals: installs extra dependencies for Android development (not needed for compilation)."
    echo "    ndk: installs the Android NDK. Needed if you want to rebuild the Skycoin native library."
    echo "    build_native: installs gomobile, rebuilds the Skycoin native library and saves it to the /app/libs directory. NOTE: you need to have git installed in the system."
    echo "    build: creates an app bundle for release. It is saved in /app/build/outputs/bundle/release/app-release.aab."
    echo "    build debug: creates a debug APK of the app. It is saved in /app/build/outputs/apk/debug/app-debug.apk."
    echo "    build apk: creates an unsigned release APK of the app. It is saved in /app/build/outputs/apk/release/app-release-unsigned.apk."
    echo "    sign_release: signs the app bundle saved in /app/build/outputs/bundle/release/app-release.aab using jarsigner. It uses the file called skycoin-playstore-keystore as signing keys."
    echo "    install debug: installs the debug APK in the connected Android device."
}

install_dep() {
    mkdir -p "$HOME"/android
		if [ ! -f /tmp/commandlinetools-"${uname_s}"-latest.zip ]; then
				wget https://dl.google.com/android/repository/commandlinetools-"${uname_s}"-${cmdtool_version}_latest.zip -O /tmp/commandlinetools-"${uname_s}"-latest.zip
		fi
    unzip -d "$HOME"/android /tmp/commandlinetools-"${uname_s}"-latest.zip

    yes | "$ANDROID_SDK_ROOT"/cmdline-tools/bin/sdkmanager --sdk_root="$ANDROID_SDK_ROOT" \
				"build-tools;30.0.3" "platforms;android-30"

    yes | sdkmanager --licenses
}

install_optionals() {
    yes | "$ANDROID_SDK_ROOT"/cmdline-tools/bin/sdkmanager --sdk_root="$ANDROID_SDK_ROOT" \
        emulator platform-tools "extras;google;m2repository" "extras;android;m2repository"

    yes | sdkmanager --licenses
}

install_ndk() {
    yes | "$ANDROID_SDK_ROOT"/cmdline-tools/bin/sdkmanager --sdk_root="$ANDROID_SDK_ROOT" --install "ndk;22.1.7171670"
}

build_native() {
    cwd=$PWD

    if [ ! -d ${cwd}/app/libs ]; then
        printf "Error: you are running this script from an unknown location, it will not be possible to move the compiled files to the correct location."
        exit 1
    fi

    GO111MODULE=off go get golang.org/x/mobile/cmd/gomobile
    GO111MODULE=off gomobile init

    mkdir -p "$GOPATH"/src/github.com/skycoin
    cd "$GOPATH"/src/github.com/skycoin

    git clone https://github.com/skycoin/skycoin-lite.git
    cd skycoin-lite

    ANDROID_NDK_HOME="$ANDROID_SDK_ROOT"/ndk/22.1.7171670 ANDROID_HOME="$ANDROID_SDK_ROOT" gomobile bind -target=android github.com/skycoin/skycoin-lite/mobile

    mv mobile.aar ${cwd}/app/libs/mobile.aar
    mv mobile-sources.jar ${cwd}/app/libs/mobile-sources.jar
}

build_release() {
    ./gradlew bundleRelease
}

build_release_apk() {
    ./gradlew assembleRelease
}

sign_release_build() {
    if [ ! -f skycoin-playstore-keystore ]; then
        printf "Error: skycoin-playstore-keystore not found in current directory.\n"
        exit 1
    fi

    # Sign the AAB using jarsigner
    jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 \
        -keystore skycoin-playstore-keystore \
        $PWD/app/build/outputs/bundle/release/app-release.aab \
        skycoin-key

    echo "Signed bundle saved at: $PWD/app/build/outputs/bundle/release/app-release.aab"
}

install_release() {
    echo "Error: App bundles cannot be directly installed. Use 'build apk' to create an APK for local installation."
    exit 1
}

build_debug() {
    ./gradlew assembleDebug
}

install_debug() {
    adb -d install ./app/build/outputs/apk/debug/app-debug.apk
}

cmd="$1"
if [ "$1" != "" ]; then
    shift 1
fi

case "$cmd" in
build)
    if [ "$1" = "debug" ]; then
        build_debug
    elif [ "$1" = "apk" ]; then
        build_release_apk
    else
        build_release
    fi
    ;;
sign_release)
    sign_release_build
    ;;
install)
    if [ "$1" = "debug" ]; then
        install_debug
    else
        install_release
    fi
    ;;
dep)
    install_dep
    ;;
optionals)
    install_optionals
    ;;
ndk)
    install_ndk
    ;;
build_native)
    build_native
    ;;
*)
    usage
    ;;
esac
