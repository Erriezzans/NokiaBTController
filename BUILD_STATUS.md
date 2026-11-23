# Build status

## Nokia C5-00 Java ME MIDlet

- [x] MIDlet source compiled against CLDC 1.1, MIDP 2.0 API and JSR-82 API libraries.
- [x] CLDC preverification run with ProGuard microedition mode.
- [x] JAR generated: `dist/NokiaBTController-1.0.0.jar`.
- [x] JAD generated: `dist/NokiaBTController-1.0.0.jad`.
- [x] Final JAR contains `NokiaBTController.class`, `BluetoothClient.class` and `RemoteCanvas.class`.
- [x] All three classes use class-file major version 45 (Java 1.1 bytecode).
- [x] JAR archive integrity check passed.
- [x] JAD `MIDlet-Jar-Size` matches the generated JAR (9,886 bytes).
- [ ] Installation and Bluetooth testing on a physical Nokia C5-00.

Latest successful GitHub Actions run: https://github.com/Erriezzans/NokiaBTController/actions/runs/45196452711

Artifact: `NokiaBTController-JavaME-1.0.0` (contains the JAR and JAD; Actions retention 30 days).

## Android receiver

- [x] Android Java source and native C source are present in the repository.
- [ ] Installable APK build.
- [ ] Root/uinput testing on the target Android device.
- [ ] Pairing and end-to-end remote/mouse/gamepad testing.

The Java ME build does not build the Android APK. The Android app requires its own Android SDK/NDK build and hardware validation.

## Known device requirements

The C5-00 app requires the device's JSR-82 Bluetooth implementation and the Bluetooth client permission. Native gamepad/mouse input on Android requires a rooted build (or equivalent privileged helper), kernel uinput support, and working SELinux access.


## Revised Nokia UI and pairing

- [x] Bluetooth inquiry includes paired devices and newly discovered devices.
- [x] Nokia displays a selectable Bluetooth device list instead of silently choosing the first device.
- [x] Selected-device service discovery requests the receiver's exact RFCOMM UUID.
- [x] Remote layout uses runtime screen dimensions; TV remote adds menu, media, mute, volume, home, back and mouse controls.
- [x] Separate MIDP text-entry screen sends basic ASCII keystrokes; keypad shortcuts are suspended while typing.
- [x] Gamepad layout labels and draws D-pad, face buttons, L1/L2/R1/R2, L3/R3, Select and Start.
- [x] Latest Java ME JAR/JAD build passed GitHub Actions and archive/manifest/JAD-size checks.
- [ ] Nokia C5-00 physical screen, Bluetooth discovery and key behaviour test.

## Android helper changes

- [x] Native helper source expanded for text entry, media keys and L3/R3.
- [ ] Android APK build and ABI asset verification (GitHub Actions run: https://github.com/Erriezzans/NokiaBTController/actions/runs/45201894352).
- [ ] Root/uinput runtime test on Android hardware.
