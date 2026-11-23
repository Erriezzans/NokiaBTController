# Hardware test checklist

1. Confirm that `su -c 'ls -l /dev/uinput'` succeeds on Android (or `/dev/input/uinput`).
2. Confirm that the root shell can open the node; SELinux may still deny the operation.
3. Pair Nokia C5-00 and Android in system Bluetooth settings when possible.
4. Start the Android receiver and confirm its notification says it is listening.
5. If the Nokia inquiry returns no devices, tap **Make discoverable (5 minutes)** in the Android app and approve the Android system dialog.
6. Open **Find devices** on Nokia. Confirm the separate browser page stays visible during the scan and adds devices as they are discovered.
7. Test Up/Down to select a device, Center or **Connect** to select it, **Scan again** to repeat discovery, and **Back** to return to the remote screen.
8. Select the Android receiver. Confirm the browser reports service lookup and connection progress; on success it switches to the remote screen, and on failure it stays on the browser with an error and retry action.
9. Confirm Android logcat reports `READY: virtual keyboard, mouse and gamepad created`.
10. In Remote mode, test Up/Down/Left/Right, Select, Back and Home.
11. Toggle Mouse ON and test pointer movement and left-click; toggle it OFF again.
12. Switch to Gamepad and use an Android gamepad tester to test the D-pad, face buttons, shoulders, triggers, Select and Start.
13. Disconnect Bluetooth while holding a key; confirm the helper releases all state.
14. Test after screen-off, process recreation and Bluetooth reconnection.

Android is not discoverable by default. The temporary visibility request is user-approved and lasts up to five minutes. A previously paired device can still be used when visibility is off.

Expected known limitations: the Nokia D-pad is digital, so analog axes only have center/full-deflection values. A game that only accepts certain console controller identities may need an adjusted virtual device identity or key layout.
