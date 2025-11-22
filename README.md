# skycoin-android
Native light wallet for Skycoin. "Light" wallet means the wallet does not download or store any blockchain data itself, instead it relies on a remote server ("full node") which stores the blockchain. This node is usually a full wallet. The node is queried over the network when needed by the light wallet.

This means that the wallet can not function without network connectivity to a compatible node. Currently the minimum required version of the node software is 0.25.

# Security
The app tries to store as little critical data as possible, preferring to dynamically generate data when it is needed. The only sensitive data stored on device is the cryptographic seed for each wallet stored. This seed is stored locally on disk in an encrypted form. The seed is only decrypted when it is actually needed, to minimize the time the decrypted plaintext resides in device RAM.

The security for this relies on the android device's support for hardware encryption. This is supported in android from 6.0 (Marshmallow) and onwards, but it is up to each device manufacturer to implement this in a secure way. This app uses the AndroidKeyStore provider to generate an AES/GCM key which is then used to encrypt and decrypt the cryptographic seeds.

NOTE: ultimately, you should consider a lost device as compromised. An attacker with free physical access to a device will probably eventually find an exploit. If you lose your device, re-create your wallets from their seeds on a new device, and send your Skycoins from the old wallets to brand new, unused wallets ASAP.

Your decrypted private keys and seeds will never leave the device. All communication between your light wallet and the remote node is only public/insensitive data. Note that while a malicious node will not be able to steal your funds, you will show the node that you own the addresses you are sending from.

## PIN
The app is protected against casual snooping and theft by requiring a PIN to unlock for use. This PIN is on the application level and not enforced by hardened security inside Android. A persistent attacker may be able to bypass it. The real protection for the wallet comes from the device's built in security lock-screen and varies from device to device and your personal settings.

## Meta info

Some extra information is in the wiki, https://github.com/watercompany/skycoin-android/wiki

# Building & Debugging (locally)

## Prequisites

- Make sure that you have installed OpenJDK 8 / 11.0 or use the embedded JDK inside the Android Studio.

Under debian based distro (like ubuntu), you can run:
```bash
$ sudo apt install -y -qq openjdk-8-jdk
```

- You must install Android SDK version 30 via `sdkmanager` (CLI) or Android Studio (SDK Manager)

```bash
# Install it manually via command line (Linux and Darwin only)
$ ./scripts/ci_build.sh dep
# If you want to install additional dev dependencies (not needed for building the app), use
$ ./scripts/ci_build.sh dep dev
```

## Rebuild the native Skycoin library

The native Skycoin library contains the cryptography functions used by the app. It is located in the
/app/libs directory. For recompiling it, you need to have the Android NDK first, you can install it with:

```bash
$ ./scripts/ci_build.sh ndk
```

After that you can recompile it by using this script:

```bash
$ ./scripts/ci_build.sh build_native
```

NOTE: you must have git installed for the script to work.

## Build and run a debug version

To build from the CLI, run:

```bash
$ ./scripts/ci_build.sh build debug
```

The debug apk will be stored in `./app/build/outputs/apk/debug`

To install and run it using a connected android device (have to be connected via USB, and the USB debugging option turned on):

```bash
$ ./scripts/ci_build.sh install debug
```

## Build and run a release version

To build from the CLI, run:

```bash
$ ./scripts/ci_build.sh build
```

The unsigned release apk will be stored in `./app/build/outputs/apk/release`

Before being able to install the app, you must sign the APK file. For that, the signing keys file must
be in the root of this repository, in a file called skycoin-playstore-keystore. When you have the
correct keys, use this script for signing the file:

```bash
$ ./scripts/ci_build.sh sign_release
```

The signed apk will be also stored in `./app/build/outputs/apk/release`

To install and run it using a connected android device (have to be connected via USB, and the USB debugging option turned on):

```bash
$ ./scripts/ci_build.sh install
```

## Using the build script

You can see more info about how to use the build script just by running `./scripts/ci_build.sh` without arguments.|
