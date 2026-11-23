# Build status

## Nokia C5-00 Java ME MIDlet

- [x] MIDlet source compiled against CLDC 1.1, MIDP 2.0 API and JSR-82 API libraries.
- [x] CLDC preverification completed with ProGuard microedition mode.
- [x] Dedicated `DeviceBrowserCanvas` compiled and included in the release JAR.
- [x] Bluetooth search browser shows live paired/cached/inquiry results, D-pad selection, Connect, Scan again and Back actions.
- [x] Connection progress and connection errors remain on the device browser; successful connection returns to the remote screen.
- [x] Persistent `KeyMap` and `KeyRemapForm` classes are included.
- [x] JAR archive integrity check passed.
- [x] JAD `MIDlet-Jar-Size` matches the generated JAR: 27,706 bytes.
- [x] JAR manifest contains one MIDlet manifest and all required MIDlet attributes.
- [ ] Installation, on-screen layout and Bluetooth testing on a physical Nokia C5-00.

Latest successful GitHub Actions run: https://github.com/Erriezzans/NokiaBTController/actions/runs/45207317113

Workflow artifact: `NokiaBTController-JavaME-1.0.0` (JAR + JAD; artifact retention 30 days).

JAR SHA-256: `e244da7a4e6000fc3da83ef0c6a20384d5838ec6ead52229b31f85de46c48e78`

## Android receiver

- [x] Android receiver debug APK built successfully.
- [x] Updated Bluetooth discoverability button requests Android's system-controlled visibility for up to five minutes.
- [x] APK contains `nrgpad` helpers for `arm64-v8a`, `armeabi-v7a` and `x86_64`.
- [x] APK archive integrity and native helper presence checks passed.
- [ ] Install and test Bluetooth pairing/discovery on the target Android phone or TV box.
- [ ] Test root authorization, kernel uinput access and SELinux policy on the target device.

Latest successful GitHub Actions run: https://github.com/Erriezzans/NokiaBTController/actions/runs/45206084311

Workflow artifact: `NokiaBTReceiver-debug` (APK; artifact retention 30 days).

APK SHA-256: `5bb59aa2bc20e1fd3a804ca1f456d2fc26d9e9f081ceaa47c2f4d69d337d4054`

## Device requirements and remaining hardware tests

The C5-00 must provide a working JSR-82 Bluetooth implementation and allow the MIDlet Bluetooth client permission. Android Bluetooth must be enabled; when Nokia inquiry cannot see Android, make Android temporarily discoverable using the receiver app's system prompt or pair the devices in Android Bluetooth settings first.

Native gamepad/mouse input on Android requires a rooted build (or equivalent privileged helper), kernel uinput support and working SELinux access. Actual Bluetooth, physical keypad, on-device layout, text entry and gamepad compatibility still require testing on the phone and target Android device. The virtual gamepad is local Android input; it is not a Bluetooth HID gamepad advertised to a second host.
