package com.erriezzans.nokiabt;

import java.io.IOException;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.midlet.MIDlet;

/** Nokia C5-00 Java ME Bluetooth remote and gamepad. */
public final class NokiaBTController extends MIDlet implements CommandListener {
    static final String SERVICE_UUID = "e1f55c5a3a624c939c5c9d8a5fce0016";
    static final byte PROTOCOL_VERSION = 1;

    // Wire packet types; shared with Android receiver and native uinput helper.
    static final byte PKT_KEY = 1;
    static final byte PKT_MOUSE_MOVE = 2;
    static final byte PKT_MOUSE_BUTTON = 3;
    static final byte PKT_PAD_BUTTON = 4;
    static final byte PKT_PAD_HAT = 5;
    static final byte PKT_PAD_AXIS = 6;
    static final byte PKT_RESET = 7;
    static final byte PKT_MODE = 8;

    // Linux input event key codes. These values are stable UAPI values.
    static final int KEY_ESC = 1;
    static final int KEY_ENTER = 28;
    static final int KEY_HOME = 102;
    static final int KEY_UP = 103;
    static final int KEY_LEFT = 105;
    static final int KEY_RIGHT = 106;
    static final int KEY_DOWN = 108;
    static final int KEY_VOLUMEDOWN = 114;
    static final int KEY_VOLUMEUP = 115;
    static final int KEY_PLAYPAUSE = 164;
    static final int KEY_BACK = 158;
    static final int KEY_MENU = 139;

    static final int MODE_REMOTE = 0;
    static final int MODE_MOUSE = 1;
    static final int MODE_GAMEPAD = 2;

    private Display display;
    private RemoteCanvas canvas;
    private BluetoothClient bluetooth;

    private final Command connectCommand = new Command("Connect", Command.SCREEN, 1);
    private final Command remoteCommand = new Command("Remote", Command.SCREEN, 2);
    private final Command gamepadCommand = new Command("Gamepad", Command.SCREEN, 3);
    private final Command mouseCommand = new Command("Mouse ON/OFF", Command.SCREEN, 4);
    private final Command disconnectCommand = new Command("Disconnect", Command.SCREEN, 5);
    private final Command exitCommand = new Command("Exit", Command.EXIT, 9);

    public NokiaBTController() {
        display = Display.getDisplay(this);
        canvas = new RemoteCanvas(this);
        canvas.addCommand(connectCommand);
        canvas.addCommand(remoteCommand);
        canvas.addCommand(gamepadCommand);
        canvas.addCommand(mouseCommand);
        canvas.addCommand(disconnectCommand);
        canvas.addCommand(exitCommand);
        canvas.setCommandListener(this);
        bluetooth = new BluetoothClient(this);
    }

    public void startApp() {
        display.setCurrent(canvas);
        canvas.refresh();
    }

    public void pauseApp() {
        // Keep the Bluetooth link alive while the MIDlet is paused by the OS.
    }

    public void destroyApp(boolean unconditional) {
        if (bluetooth != null) bluetooth.disconnect();
        notifyDestroyed();
    }

    void connectBluetooth() {
        if (bluetooth != null && !bluetooth.isConnected()) bluetooth.connect();
    }

    void disconnectBluetooth() {
        if (bluetooth != null) bluetooth.disconnect();
        canvas.setConnectionStatus("Disconnected");
    }

    boolean isConnected() {
        return bluetooth != null && bluetooth.isConnected();
    }

    void send(byte type, int code, int value) {
        if (bluetooth != null) bluetooth.send(type, code, value);
    }

    void setStatus(String value) {
        canvas.setConnectionStatus(value);
    }

    public void commandAction(Command command, Displayable displayable) {
        if (command == connectCommand) {
            connectBluetooth();
        } else if (command == remoteCommand) {
            canvas.setMode(MODE_REMOTE);
        } else if (command == gamepadCommand) {
            canvas.setMode(MODE_GAMEPAD);
        } else if (command == mouseCommand) {
            canvas.toggleMouse();
        } else if (command == disconnectCommand) {
            disconnectBluetooth();
        } else if (command == exitCommand) {
            destroyApp(true);
        }
    }
}
