package com.erriezzans.nokiabt;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;

/** Wire format: N R, version, type, signed 16-bit code, signed 16-bit value; big-endian. */
final class Protocol {
    static final int HEADER_N = 0x4e;
    static final int HEADER_R = 0x52;
    static final int VERSION = 1;
    static final int PACKET_SIZE = 8;

    static final int KEY = 1;
    static final int MOUSE_MOVE = 2;
    static final int MOUSE_BUTTON = 3;
    static final int PAD_BUTTON = 4;
    static final int PAD_HAT = 5;
    static final int PAD_AXIS = 6;
    static final int RESET = 7;
    static final int MODE = 8;

    static final int MODE_REMOTE = 0;
    static final int MODE_MOUSE = 1;
    static final int MODE_GAMEPAD = 2;

    private Protocol() { }

    static Frame read(DataInputStream in) throws IOException {
        int first;
        try { first = in.readUnsignedByte(); }
        catch (EOFException eof) { return null; }
        int second = in.readUnsignedByte();
        int version = in.readUnsignedByte();
        int type = in.readUnsignedByte();
        int code = in.readShort();
        int value = in.readShort();
        if (first != HEADER_N || second != HEADER_R || version != VERSION) {
            throw new IOException("Invalid Nokia controller packet");
        }
        return new Frame(type, code, value);
    }

    static byte[] encode(Frame frame) {
        return new byte[] {
                (byte) HEADER_N, (byte) HEADER_R, (byte) VERSION, (byte) frame.type,
                (byte) ((frame.code >>> 8) & 0xff), (byte) (frame.code & 0xff),
                (byte) ((frame.value >>> 8) & 0xff), (byte) (frame.value & 0xff)
        };
    }

    static final class Frame {
        final int type;
        final int code;
        final int value;
        Frame(int t, int c, int v) { type = t; code = c; value = v; }
    }
}
