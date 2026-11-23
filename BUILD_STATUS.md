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
