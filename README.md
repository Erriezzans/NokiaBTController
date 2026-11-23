# Nokia BT Controller — source project

A two-part Bluetooth controller project for a **Nokia C5-00 (240×320, Java ME)** and an Android phone / Android TV box.

## Features in this source snapshot

- Nokia Java ME client using MIDP 2.1, CLDC 1.1 and JSR-82 Bluetooth service discovery. Device search opens a dedicated browser page with a live result list, selection, Connect, Scan again and Back actions.
- Android RFCOMM receiver with the same private Bluetooth service UUID. Its setup screen includes a user-approved five-minute Bluetooth discoverability action for cases where Nokia inquiry cannot see the phone.
- TV remote mode: navigation, Select, Back, Home and volume keys.
- Remote mouse ON/OFF: `*` toggles pointer mode; D-pad moves the pointer; center clicks.
- Gamepad mode: native Android virtual gamepad created via `/dev/uinput`, including D-pad, four face buttons, L1/R1, L2/R2, Select/Start and L3/R3.
- Gamepad key remapping: the Nokia MIDlet has an Options → Key remap screen. The user can assign physical keys `0–9`, `*` and `#` to each gamepad function; duplicate assignments are rejected and valid mappings are saved in the phone's MIDP RecordStore.
- Native Android virtual keyboard and mouse are separate devices from the gamepad.
- Disconnect cleanup releases held keys/buttons and destroys the uinput devices.

## Build outputs

The Nokia Java ME MIDlet is built by GitHub Actions using Eclipse ECJ, CLDC/MIDP/JSR-82 API libraries, and ProGuard CLDC preverification. The latest successful run produces `NokiaBTController-1.0.0.jar` and `NokiaBTController-1.0.0.jad` as the `NokiaBTController-JavaME-1.0.0` workflow artifact. Download the JAR and JAD from the latest successful build: https://github.com/Erriezzans/NokiaBTController/actions/runs/45292864740

The generated JAR contains the Bluetooth client, dedicated `DeviceBrowserCanvas`, responsive remote Canvas, persistent `KeyMap` and `KeyRemapForm` classes. It uses Java 1.1 class-file bytecode (major version 45); the JAD size is checked against the final JAR by the build script. The release JAR is 27,706 bytes and includes the separate device browser. It has not yet been installed or tested on a physical Nokia C5-00.

The Android companion APK is built separately by [the Android receiver workflow](https://github.com/Erriezzans/NokiaBTController/actions/runs/45206084311). That debug APK contains native helpers for arm64-v8a, armeabi-v7a and x86_64, and includes the Make discoverable (5 minutes) control.

Bluetooth pairing, actual C5-00 JSR-82 behavior, Android kernel uinput availability, root authorization and SELinux policy still require device testing.

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
6. Choose Remote or Gamepad from the MIDlet Options menu. Use Options → Key remap to customise gamepad keypad assignments; use Options → Send text to type into Android text fields.

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


## Nokia C5-00 controls (revised keypad UI)

1. Start the Android receiver and leave it listening.
2. Pair the Nokia C5-00 and Android device in their Bluetooth settings.
3. On Nokia, choose **Find devices**. The app lists nearby/paired Bluetooth devices; select the Android receiver from that list.
4. Choose **Remote** or **Gamepad** from the Options menu.

### TV remote mode
- D-pad and centre: navigate/select; when Mouse is enabled, D-pad moves the pointer and centre clicks.
- `*`: toggle mouse mode.
- `#`: Back; `0`: Home; `7/9`: volume down/up.
- `1`: Menu; `2`: Play/Pause; `3`: Mute; `4`: Rewind; `6`: Fast-forward; `8`: Stop.
- **Send text** opens the Nokia text editor. Typed basic ASCII text is sent as keyboard events; remote shortcuts are not active while this editor is open.

### Gamepad mode
- D-pad: D-pad and digital left-stick axes.
- `1/2/3/4`: Square/Triangle/Circle/Cross.
- `7/9`: L1/R1; `*/#`: L2/R2.
- `6/8`: L3/R3; `5/0`: Select/Start.

The 240x320 drawing uses the actual Canvas dimensions and scales its control spacing to keep the control groups within the screen. The physical keypad remains the input device; the graphics are a labelled key map, not touch controls.


## Bluetooth discovery troubleshooting

Android intentionally does not advertise itself as discoverable by default. In Nokia BT Receiver, use **Make discoverable (5 minutes)** and accept the Android system prompt, then return to the Nokia device browser and choose **Scan again**. You can also pair both devices in Android Bluetooth settings first; paired devices may appear in the Nokia browser even while discoverability is off.

The Nokia search page stays open during scanning and while a connection is being attempted. It shows live discoveries, keeps the selected target visible, and reports a service/connection error on that same page so you can rescan or choose another device. The page uses the phone's physical D-pad and center key; its Connect, Scan again and Back actions are also available as softkey/menu commands.
