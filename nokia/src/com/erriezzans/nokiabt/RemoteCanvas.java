package com.erriezzans.nokiabt;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

/** 240x320 keypad-first interface. */
final class RemoteCanvas extends Canvas {
    private final NokiaBTController app;
    private int mode = NokiaBTController.MODE_REMOTE;
    private boolean mouseEnabled = false;
    private String status = "Not connected";
    private int hatX = 0;
    private int hatY = 0;
    private boolean holdUp;
    private boolean holdDown;
    private boolean holdLeft;
    private boolean holdRight;

    RemoteCanvas(NokiaBTController owner) {
        app = owner;
        setFullScreenMode(false);
    }

    void refresh() { repaint(); }

    void setConnectionStatus(String value) {
        status = value;
        repaint();
    }

    void setMode(int newMode) {
        mode = newMode;
        releaseAll();
        int actualMode = (newMode == NokiaBTController.MODE_REMOTE && mouseEnabled)
                ? NokiaBTController.MODE_MOUSE : newMode;
        app.send(NokiaBTController.PKT_MODE, actualMode, 0);
        repaint();
    }

    void toggleMouse() {
        mouseEnabled = !mouseEnabled;
        mode = NokiaBTController.MODE_REMOTE;
        releaseAll();
        app.send(NokiaBTController.PKT_MODE,
                mouseEnabled ? NokiaBTController.MODE_MOUSE : NokiaBTController.MODE_REMOTE, 0);
        repaint();
    }

    private void releaseAll() {
        app.send(NokiaBTController.PKT_RESET, 0, 0);
        hatX = 0;
        hatY = 0;
        holdUp = false; holdDown = false; holdLeft = false; holdRight = false;
    }

    protected void paint(Graphics g) {
        int w = getWidth();
        int h = getHeight();
        g.setColor(13, 18, 28);
        g.fillRect(0, 0, w, h);
        g.setColor(30, 42, 62);
        g.fillRect(0, 0, w, 42);
        g.setColor(255, 255, 255);
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM));
        g.drawString("NOKIA BT CONTROL", 10, 8, Graphics.TOP | Graphics.LEFT);
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL));
        g.setColor(170, 190, 215);
        g.drawString(status, 10, 27, Graphics.TOP | Graphics.LEFT);

        if (mode == NokiaBTController.MODE_GAMEPAD) {
            drawGamepad(g, w, h);
        } else {
            drawRemote(g, w, h);
        }

        g.setColor(35, 48, 68);
        g.fillRect(0, h - 27, w, 27);
        g.setColor(225, 232, 242);
        g.drawString("Options", 8, h - 23, Graphics.TOP | Graphics.LEFT);
        g.drawString("* Mouse", w - 73, h - 23, Graphics.TOP | Graphics.LEFT);
    }

    private void drawRemote(Graphics g, int w, int h) {
        g.setColor(255, 255, 255);
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_LARGE));
        g.drawString("TV REMOTE", w / 2, 52, Graphics.TOP | Graphics.HCENTER);
        g.setColor(mouseEnabled ? 35 : 85, mouseEnabled ? 180 : 100, mouseEnabled ? 110 : 120);
        g.fillRoundRect(55, 80, w - 110, 25, 12, 12);
        g.setColor(255, 255, 255);
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL));
        g.drawString(mouseEnabled ? "MOUSE: ON" : "MOUSE: OFF", w / 2, 86, Graphics.TOP | Graphics.HCENTER);

        int cx = w / 2;
        int cy = 175;
        int d = 54;
        g.setColor(44, 59, 82);
        g.fillRoundRect(cx - d / 2, cy - d / 2, d, d, 12, 12);
        g.fillRoundRect(cx - 23, cy - 69, 46, 43, 10, 10);
        g.fillRoundRect(cx - 23, cy + 26, 46, 43, 10, 10);
        g.fillRoundRect(cx - 69, cy - 23, 43, 46, 10, 10);
        g.fillRoundRect(cx + 26, cy - 23, 43, 46, 10, 10);
        g.setColor(235, 242, 250);
        g.drawString("OK", cx, cy - 7, Graphics.TOP | Graphics.HCENTER);
        g.drawString("▲", cx, cy - 63, Graphics.TOP | Graphics.HCENTER);
        g.drawString("▼", cx, cy + 31, Graphics.TOP | Graphics.HCENTER);
        g.drawString("◀", cx - 63, cy - 7, Graphics.TOP | Graphics.HCENTER);
        g.drawString("▶", cx + 31, cy - 7, Graphics.TOP | Graphics.HCENTER);

        g.setColor(160, 185, 215);
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL));
        g.drawString("D-PAD: Navigate / Move pointer", w / 2, 259, Graphics.TOP | Graphics.HCENTER);
        g.drawString("Center: Select / Click", w / 2, 276, Graphics.TOP | Graphics.HCENTER);
        g.drawString("#: Back     0: Home     7/9: Volume", w / 2, 293, Graphics.TOP | Graphics.HCENTER);
    }

    private void drawGamepad(Graphics g, int w, int h) {
        g.setColor(255, 255, 255);
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_LARGE));
        g.drawString("GAME CONTROLLER", w / 2, 52, Graphics.TOP | Graphics.HCENTER);
        g.setColor(140, 165, 195);
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL));
        g.drawString("Nokia D-pad = D-pad + left stick", w / 2, 78, Graphics.TOP | Graphics.HCENTER);

        // Left-side digital pad illustration.
        int x = 60;
        int y = 177;
        g.setColor(44, 59, 82);
        g.fillRoundRect(x - 21, y - 21, 42, 42, 8, 8);
        g.fillRoundRect(x - 16, y - 60, 32, 40, 8, 8);
        g.fillRoundRect(x - 16, y + 20, 32, 40, 8, 8);
        g.fillRoundRect(x - 60, y - 16, 40, 32, 8, 8);
        g.fillRoundRect(x + 20, y - 16, 40, 32, 8, 8);
        g.setColor(235, 242, 250);
        g.drawString("▲", x, y - 56, Graphics.TOP | Graphics.HCENTER);
        g.drawString("▼", x, y + 25, Graphics.TOP | Graphics.HCENTER);
        g.drawString("◀", x - 55, y - 8, Graphics.TOP | Graphics.HCENTER);
        g.drawString("▶", x + 25, y - 8, Graphics.TOP | Graphics.HCENTER);

        // Face buttons: 1=Square, 2=Triangle, 3=Circle, 4=Cross.
        int bx = 174;
        int by = 177;
        g.setColor(44, 59, 82);
        g.fillRoundRect(bx - 15, by - 56, 34, 34, 16, 16);
        g.fillRoundRect(bx - 52, by - 19, 34, 34, 16, 16);
        g.fillRoundRect(bx + 22, by - 19, 34, 34, 16, 16);
        g.fillRoundRect(bx - 15, by + 18, 34, 34, 16, 16);
        g.setColor(235, 242, 250);
        g.drawString("2", bx + 2, by - 49, Graphics.TOP | Graphics.HCENTER);
        g.drawString("1", bx - 35, by - 12, Graphics.TOP | Graphics.HCENTER);
        g.drawString("3", bx + 39, by - 12, Graphics.TOP | Graphics.HCENTER);
        g.drawString("4", bx + 2, by + 25, Graphics.TOP | Graphics.HCENTER);
        g.setColor(160, 185, 215);
        g.drawString("1 □   2 △   3 ○   4 ×", w / 2, 255, Graphics.TOP | Graphics.HCENTER);
        g.drawString("7/9: L1/R1    */#: L2/R2", w / 2, 273, Graphics.TOP | Graphics.HCENTER);
        g.drawString("5: Select     0: Start", w / 2, 291, Graphics.TOP | Graphics.HCENTER);
    }

    protected void keyPressed(int keyCode) {
        int action = getGameAction(keyCode);
        char ch = keyChar(keyCode);

        if (mode == NokiaBTController.MODE_GAMEPAD) {
            handleGamepadPressed(action, ch);
            return;
        }

        if (ch == '*') {
            toggleMouse();
            return;
        }
        if (ch == '#') {
            sendKey(NokiaBTController.KEY_BACK, true);
            return;
        }
        if (ch == '0') {
            sendKey(NokiaBTController.KEY_HOME, true);
            return;
        }
        if (ch == '7') {
            sendKey(NokiaBTController.KEY_VOLUMEDOWN, true);
            return;
        }
        if (ch == '9') {
            sendKey(NokiaBTController.KEY_VOLUMEUP, true);
            return;
        }
        if (ch == '5' || action == FIRE) {
            if (mouseEnabled) clickMouse(1);
            else sendKey(NokiaBTController.KEY_ENTER, true);
            return;
        }
        handleDirection(action, true);
    }

    protected void keyRepeated(int keyCode) {
        int action = getGameAction(keyCode);
        char ch = keyChar(keyCode);
        if (mode == NokiaBTController.MODE_GAMEPAD) return;
        if (mouseEnabled && (action == UP || action == DOWN || action == LEFT || action == RIGHT)) {
            moveMouse(action);
        }
    }

    protected void keyReleased(int keyCode) {
        int action = getGameAction(keyCode);
        char ch = keyChar(keyCode);
        if (mode == NokiaBTController.MODE_GAMEPAD) {
            handleGamepadReleased(action, ch);
            return;
        }
        if (ch == '#') sendKey(NokiaBTController.KEY_BACK, false);
        else if (ch == '0') sendKey(NokiaBTController.KEY_HOME, false);
        else if (ch == '7') sendKey(NokiaBTController.KEY_VOLUMEDOWN, false);
        else if (ch == '9') sendKey(NokiaBTController.KEY_VOLUMEUP, false);
        else if (ch == '5' || action == FIRE) {
            if (!mouseEnabled) sendKey(NokiaBTController.KEY_ENTER, false);
        } else if (!mouseEnabled) {
            handleDirection(action, false);
        }
    }

    private void handleDirection(int action, boolean down) {
        int code = 0;
        if (action == UP) code = NokiaBTController.KEY_UP;
        else if (action == DOWN) code = NokiaBTController.KEY_DOWN;
        else if (action == LEFT) code = NokiaBTController.KEY_LEFT;
        else if (action == RIGHT) code = NokiaBTController.KEY_RIGHT;
        if (code == 0) return;
        if (mouseEnabled) {
            if (down) moveMouse(action);
        } else {
            sendKey(code, down);
        }
    }

    private void moveMouse(int action) {
        int dx = 0;
        int dy = 0;
        if (action == LEFT) dx = -9;
        else if (action == RIGHT) dx = 9;
        else if (action == UP) dy = -9;
        else if (action == DOWN) dy = 9;
        app.send(NokiaBTController.PKT_MOUSE_MOVE, dx, dy);
    }

    private void clickMouse(int button) {
        app.send(NokiaBTController.PKT_MOUSE_BUTTON, button, 1);
        app.send(NokiaBTController.PKT_MOUSE_BUTTON, button, 0);
    }

    private void sendKey(int linuxKeyCode, boolean down) {
        app.send(NokiaBTController.PKT_KEY, linuxKeyCode, down ? 1 : 0);
    }

    private void handleGamepadPressed(int action, char ch) {
        if (action == UP) holdUp = true;
        else if (action == DOWN) holdDown = true;
        else if (action == LEFT) holdLeft = true;
        else if (action == RIGHT) holdRight = true;
        updateHat();

        int button = gamepadButton(ch);
        if (button < 0 && action == FIRE) button = 8; // Centre navigation key = Select.
        if (button >= 0) app.send(NokiaBTController.PKT_PAD_BUTTON, button, 1);
    }

    private void handleGamepadReleased(int action, char ch) {
        if (action == UP) holdUp = false;
        else if (action == DOWN) holdDown = false;
        else if (action == LEFT) holdLeft = false;
        else if (action == RIGHT) holdRight = false;
        updateHat();
        int button = gamepadButton(ch);
        if (button < 0 && action == FIRE) button = 8;
        if (button >= 0) app.send(NokiaBTController.PKT_PAD_BUTTON, button, 0);
    }

    private void updateHat() {
        int x = (holdRight ? 1 : 0) - (holdLeft ? 1 : 0);
        int y = (holdDown ? 1 : 0) - (holdUp ? 1 : 0);
        hatX = x;
        hatY = y;
        app.send(NokiaBTController.PKT_PAD_HAT, 0, x);
        app.send(NokiaBTController.PKT_PAD_HAT, 1, y);
        app.send(NokiaBTController.PKT_PAD_AXIS, 0, x * 32767);
        app.send(NokiaBTController.PKT_PAD_AXIS, 1, y * 32767);
    }

    private char keyChar(int keyCode) {
        if (keyCode >= '0' && keyCode <= '9') return (char) keyCode;
        String name = getKeyName(keyCode);
        if (name != null && name.length() == 1) return name.charAt(0);
        return '\0';
    }

    private int gamepadButton(char ch) {
        if (ch == '4') return 0; // Cross / South
        if (ch == '3') return 1; // Circle / East
        if (ch == '1') return 2; // Square / West
        if (ch == '2') return 3; // Triangle / North
        if (ch == '7') return 4; // L1
        if (ch == '9') return 5; // R1
        if (ch == '*') return 6; // L2
        if (ch == '#') return 7; // R2
        if (ch == '5') return 8; // Select
        if (ch == '0') return 9; // Start
        return -1;
    }
}
