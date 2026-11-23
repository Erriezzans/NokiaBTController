# Hardware test checklist

1. Confirm that `su -c 'ls -l /dev/uinput'` succeeds on Android (or `/dev/input/uinput`).
2. Confirm that the root shell can open the node; SELinux may still deny the operation.
3. Start the Android receiver and confirm its notification says it is listening.
4. Pair Nokia C5-00 and Android in system Bluetooth settings.
5. Start the MIDlet Bluetooth search and connect to the Android service.
6. Confirm Android logcat reports `READY: virtual keyboard, mouse and gamepad created`.
7. In Remote mode, test Up/Down/Left/Right, Select, Back and Home.
8. Toggle Mouse ON and test pointer movement and left-click; toggle it OFF again.
9. Switch to Gamepad and use an Android gamepad tester to test the D-pad, face buttons, shoulders, triggers, Select and Start.
10. Disconnect Bluetooth while holding a key; confirm the helper releases all state.
11. Test after screen-off, process recreation and Bluetooth reconnection.

Expected known limitations: the Nokia D-pad is digital, so analog axes only have center/full-deflection values. A game that only accepts certain console controller identities may need an adjusted virtual device identity or key layout.
