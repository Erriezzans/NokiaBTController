# Bluetooth protocol

RFCOMM server/client service UUID:

`e1f55c5a-3a62-4c93-9c5c-9d8a5fce0016`

Transport frame: 8 bytes, big-endian.

| Byte(s) | Meaning |
|---|---|
| 0 | `0x4E` (`N`) |
| 1 | `0x52` (`R`) |
| 2 | Protocol version, currently `1` |
| 3 | Packet type |
| 4–5 | Signed 16-bit code |
| 6–7 | Signed 16-bit value |

## Packet types

| Type | Name | Code | Value |
|---:|---|---|---|
| 1 | KEY | Linux `KEY_*` code | 1 down, 0 up |
| 2 | MOUSE_MOVE | relative X delta | relative Y delta |
| 3 | MOUSE_BUTTON | 1 left, 2 right, 3 middle | 1 down, 0 up |
| 4 | PAD_BUTTON | button ID 0–9 | 1 down, 0 up |
| 5 | PAD_HAT | 0 X / 1 Y | -1, 0, +1 |
| 6 | PAD_AXIS | 0 ABS_X / 1 ABS_Y | -32767..32767 |
| 7 | RESET | ignored | ignored; release all inputs |
| 8 | MODE | 0 remote / 1 mouse / 2 gamepad | ignored |

Gamepad button IDs: 0 Cross/South, 1 Circle/East, 2 Square/West, 3 Triangle/North, 4 L1, 5 R1, 6 L2, 7 R2, 8 Select, 9 Start.

The Nokia has no analog sticks. Its D-pad generates digital full-deflection left-stick values plus D-pad hat events. L2/R2 are emitted as digital shoulder-trigger buttons and 0/255 trigger-axis values.
