# Nokia BT Controller — source project

A two-part Bluetooth controller project for a **Nokia C5-00 (240×320, Java ME)** and an Android phone / Android TV box.

## Features in this source snapshot

- Nokia Java ME client using MIDP 2.1, CLDC 1.1 and JSR-82 Bluetooth service discovery.
- Android RFCOMM receiver with the same private Bluetooth service UUID.
- TV remote mode: navigation, Select, Back, Home and volume keys.
- Remote mouse ON/OFF: `*` toggles pointer mode; D-pad moves the pointer; center clicks.
- Gamepad mode: native Android virtual gamepad created via `/dev/uinput`.
- Native Android virtual keyboard and mouse are separate devices from the gamepad.
- Disconnect cleanup releases held keys/buttons and destroys the uinput devices.

## Important: source code, not prebuilt installation files

The current build environment has Java 21 but **does not have the Android SDK/NDK, Gradle, or a Java ME/CLDC preverifier**. Therefore this package contains the implementation and build project, but does not claim to contain a tested APK or a runnable C5-00 JAR. The C5-00 JAR needs a Java ME toolchain with MIDP 2.1, CLDC 1.1 and JSR-82 libraries plus the CLDC preverifier. The Android APK needs Android SDK 35, NDK and CMake 3.22.1.

The files must still be built and tested on the actual Nokia C5-00 and target Android hardware. Bluetooth pairing, kernel uinput availability, root authorization and SELinux policy differ between Android builds.

## Build Android receiver

Requirements: Android SDK Platform 35, NDK, CMake 3.22.1, JDK 17 and Gradle 8.9+.

From `android/` run:

```sh
gradle assembleDebug
```

The CMake target builds `nrgpad` for `arm64-v8a`, `armeabi-v7a` and `x86_64` and places each executable in `android/app/src/main/assets/bin/<ABI>/nrgpad`; the APK includes the matching helper.

## Build Nokia C5-00 MIDlet

Build with a Java ME SDK containing CLDC 1.1, MIDP 2.1, JSR-82 and the CLDC `preverify` tool. Compile the classes in `nokia/src`, run the preverifier, package the classes into a JAR, then set the exact JAR size and URL in the JAD.

## First-time setup

1. Install the Android receiver APK.
2. Pair the Nokia C5-00 with Android in Bluetooth settings.
3. Open Nokia BT Receiver and tap **Start Bluetooth receiver**.
4. Approve the root-manager prompt. Native input devices require root and kernel uinput support.
5. Install the MIDlet on the Nokia and choose **Connect**.
6. Choose Remote or Gamepad from the MIDlet Options menu.

## Nokia key mapping

### Remote
- D-pad: navigate; with Mouse ON, move pointer
- Center / 5: select; with Mouse ON, left click
- `*`: toggle Mouse ON/OFF
- `#`: Back
- `0`: Home
- `7` / `9`: Volume down / up

### Gamepad
- D-pad: D-pad hat and digital left-stick axes
- `1` Square; `2` Triangle; `3` Circle; `4` Cross
- `7` / `9`: L1 / R1
- `*` / `#`: L2 / R2
- `5`: Select; `0`: Start

The C5-00 has no analog sticks, so it cannot provide true analog positions or a right stick. L2/R2 are digital buttons with trigger-axis values.

## Native input requirements

The Android device must be rooted, authorize the receiver's `su` request, and expose `/dev/uinput` (or `/dev/input/uinput`). SELinux must allow the root process to access it. Some games filter controller identities or support only particular layouts. This is a local virtual input device, not a Bluetooth HID gamepad exposed to another host.

See [docs/PROTOCOL.md](docs/PROTOCOL.md) and [docs/TESTING.md](docs/TESTING.md).
