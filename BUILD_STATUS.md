# Build status

## Nokia C5-00 Java ME MIDlet

- [x] MIDlet source compiled against CLDC 1.1, MIDP 2.0 API and JSR-82 API libraries.
- [x] CLDC preverification completed with ProGuard microedition mode.
- [x] JAR generated: `dist/NokiaBTController-1.0.0.jar`.
- [x] JAD generated: `dist/NokiaBTController-1.0.0.jad`.
- [x] Bluetooth client, device selection, RemoteCanvas, KeyMap and KeyRemapForm classes are included.
- [x] The key-remap form covers Cross, Circle, Square, Triangle, L1, R1, L2, R2, Select, Start, L3 and R3.
- [x] Duplicate key assignments are rejected; accepted mappings are stored in MIDP RecordStore.
- [x] JAR archive integrity check passed.
- [x] JAD `MIDlet-Jar-Size` matches the generated JAR: 18,109 bytes.
- [x] JAR manifest contains one MIDlet manifest and all required MIDlet attributes.
- [ ] Installation and Bluetooth testing on a physical Nokia C5-00.

Latest successful GitHub Actions run: https://github.com/Erriezzans/NokiaBTController/actions/runs/45203696264

Workflow artifact: `NokiaBTController-JavaME-1.0.0` (JAR + JAD; artifact retention 30 days).

## Android receiver

- [x] Android Java source and native C source are present in the repository.
- [ ] Installable APK build.
- [ ] Root/uinput testing on the target Android device.
- [ ] Pairing and end-to-end remote/mouse/gamepad/typing tests.

The Java ME build does not build the Android APK. The Android app requires its own Android SDK/NDK build and hardware validation.

## Device requirements

The C5-00 app requires its JSR-82 Bluetooth implementation and Bluetooth client permission. Native gamepad/mouse input on Android requires a rooted build (or equivalent privileged helper), kernel uinput support, and working SELinux access. Actual Bluetooth, physical keypad, on-device layout and gamepad compatibility still need testing on the phone and target Android device.
