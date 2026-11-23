package com.erriezzans.nokiabt;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

/** Layout is calculated from the actual Canvas size to stay inside the C5-00 screen. */
final class RemoteCanvas extends Canvas {
    private final NokiaBTController app;
    private int mode=NokiaBTController.MODE_REMOTE;
    private boolean mouseEnabled=false,up,down,left,right;
    private String status="Not connected";
    RemoteCanvas(NokiaBTController owner){app=owner;setFullScreenMode(false);}
    void refresh(){repaint();}
    void setConnectionStatus(String s){status=s;repaint();}
    void setMode(int m){mode=m;releaseAll();app.send(NokiaBTController.PKT_MODE,m==0&&mouseEnabled?1:m,0);repaint();}
    void toggleMouse(){mouseEnabled=!mouseEnabled;mode=NokiaBTController.MODE_REMOTE;releaseAll();
        app.send(NokiaBTController.PKT_MODE,mouseEnabled?1:0,0);repaint();}
    void openTextEntry(){app.setStatus("Use Options > Send text to type on Android.");}
    private void releaseAll(){app.send(NokiaBTController.PKT_RESET,0,0);up=down=left=right=false;}
    protected void paint(Graphics g){
        final int w=getWidth(),h=getHeight(),header=36,footer=22;
        g.setColor(13,18,28);g.fillRect(0,0,w,h);
        g.setColor(30,42,62);g.fillRect(0,0,w,header);
        g.setColor(255,255,255);g.setFont(Font.getFont(Font.FACE_SYSTEM,Font.STYLE_BOLD,Font.SIZE_SMALL));
        g.drawString("NOKIA BT  "+(mode==2?"GAMEPAD":"TV REMOTE"),5,4,Graphics.TOP|Graphics.LEFT);
        g.setFont(Font.getFont(Font.FACE_SYSTEM,Font.STYLE_PLAIN,Font.SIZE_SMALL));
        g.setColor(175,194,218);g.drawString(status,5,20,Graphics.TOP|Graphics.LEFT);
        if(mode==NokiaBTController.MODE_GAMEPAD)drawGamepad(g,w,h,header,footer);
        else drawRemote(g,w,h,header,footer);
        g.setColor(35,48,68);g.fillRect(0,h-footer,w,footer);
        g.setColor(235,240,248);g.drawString("Menu",5,h-footer+4,Graphics.TOP|Graphics.LEFT);
        g.drawString(mouseEnabled?"Mouse ON":"Mouse OFF",w-72,h-footer+4,Graphics.TOP|Graphics.LEFT);
    }
    private void drawRemote(Graphics g,int w,int h,int top,int foot){
        int usable=h-top-foot,cx=w/2,cy=top+usable/2-8;
        int unit=Math.min(usable/3, w/3);
        int b=Math.max(25,Math.min(42,unit));
        g.setColor(255,255,255);g.setFont(Font.getFont(Font.FACE_SYSTEM,Font.STYLE_BOLD,Font.SIZE_MEDIUM));
        g.drawString("TV REMOTE",cx,top+5,Graphics.TOP|Graphics.HCENTER);
        g.setColor(mouseEnabled?35:76,mouseEnabled?160:100,mouseEnabled?100:125);
        g.fillRoundRect(cx-43,top+25,86,19,9,9);
        g.setColor(255,255,255);g.setFont(Font.getFont(Font.FACE_SYSTEM,Font.STYLE_BOLD,Font.SIZE_SMALL));
        g.drawString(mouseEnabled?"MOUSE ENABLED":"MOUSE DISABLED",cx,top+29,Graphics.TOP|Graphics.HCENTER);
        cy=Math.min(cy,top+usable/2+8);
        g.setColor(44,59,82);
        g.fillRoundRect(cx-b/2,cy-b/2,b,b,8,8);
        g.fillRoundRect(cx-b/2,cy-b*3/2-3,b,b,8,8);
        g.fillRoundRect(cx-b/2,cy+b/2+3,b,b,8,8);
        g.fillRoundRect(cx-b*3/2-3,cy-b/2,b,b,8,8);
        g.fillRoundRect(cx+b/2+3,cy-b/2,b,b,8,8);
        g.setColor(245,248,255);
        g.drawString("OK",cx,cy-6,Graphics.TOP|Graphics.HCENTER);
        g.drawString("^",cx,cy-b*3/2+5,Graphics.TOP|Graphics.HCENTER);
        g.drawString("v",cx,cy+b/2+8,Graphics.TOP|Graphics.HCENTER);
        g.drawString("<",cx-b*3/2+8,cy-6,Graphics.TOP|Graphics.HCENTER);
        g.drawString(">",cx+b/2+8,cy-6,Graphics.TOP|Graphics.HCENTER);
        int y=Math.min(h-foot-13,cy+b*3/2+12);
        g.setFont(Font.getFont(Font.FACE_SYSTEM,Font.STYLE_PLAIN,Font.SIZE_SMALL));
        g.setColor(170,190,215);
        g.drawString("1 Menu  2 Play/Pause  3 Mute",cx,y,Graphics.BOTTOM|Graphics.HCENTER);
        g.drawString("4 Rewind  6 Forward  8 Stop",cx,y+14,Graphics.BOTTOM|Graphics.HCENTER);
        g.drawString("7/9 Volume  # Back  0 Home",cx,y+28,Graphics.BOTTOM|Graphics.HCENTER);
        g.drawString("* Mouse toggle  |  Send text in Menu",cx,y+42,Graphics.BOTTOM|Graphics.HCENTER);
    }
    private void drawGamepad(Graphics g,int w,int h,int top,int foot){
        int usable=h-top-foot,cx=w/2,cy=top+usable/2+2;
        int d=Math.min(usable/4,w/3);
        int x=w/4,y=cy,bx=w*3/4,by=cy;
        g.setColor(255,255,255);g.setFont(Font.getFont(Font.FACE_SYSTEM,Font.STYLE_BOLD,Font.SIZE_MEDIUM));
        g.drawString("PLAYSTATION GAMEPAD",cx,top+3,Graphics.TOP|Graphics.HCENTER);
        g.setColor(52,70,96);
        // Shoulder and trigger buttons are visible across the top.
        g.fillRoundRect(7,top+23,49,19,8,8);g.fillRoundRect(61,top+23,49,19,8,8);
        g.fillRoundRect(w-110,top+23,49,19,8,8);g.fillRoundRect(w-56,top+23,49,19,8,8);
        g.setColor(245,248,255);g.setFont(Font.getFont(Font.FACE_SYSTEM,Font.STYLE_BOLD,Font.SIZE_SMALL));
        g.drawString("L2 (*)",31,top+27,Graphics.TOP|Graphics.HCENTER);
        g.drawString("L1 (7)",85,top+27,Graphics.TOP|Graphics.HCENTER);
        g.drawString("R1 (9)",w-85,top+27,Graphics.TOP|Graphics.HCENTER);
        g.drawString("R2 (#)",w-31,top+27,Graphics.TOP|Graphics.HCENTER);
        // D-pad.
        g.setColor(44,59,82);
        g.fillRoundRect(x-d/2,y-d/2,d,d,8,8);
        g.fillRoundRect(x-d/3,y-d*3/2,d*2/3,d,8,8);
        g.fillRoundRect(x-d/3,y+d/2,d*2/3,d,8,8);
        g.fillRoundRect(x-d*3/2,y-d/3,d,d*2/3,8,8);
        g.fillRoundRect(x+d/2,y-d/3,d,d*2/3,8,8);
        // Four face buttons.
        g.fillRoundRect(bx-11,by-d,26,26,13,13);
        g.fillRoundRect(bx-d/2-11,by-13,26,26,13,13);
        g.fillRoundRect(bx+d/2-11,by-13,26,26,13,13);
        g.fillRoundRect(bx-11,by+d/2,26,26,13,13);
        // L3/R3 stick-click buttons and Select/Start.
        g.fillRoundRect(x-20,y+d*3/2+4,40,18,8,8);
        g.fillRoundRect(bx-20,by+d*3/2+4,40,18,8,8);
        g.fillRoundRect(cx-47,h-foot-20,42,18,8,8);
        g.fillRoundRect(cx+5,h-foot-20,42,18,8,8);
        g.setColor(245,248,255);
        g.drawString("^",x,y-d*3/2+8,Graphics.TOP|Graphics.HCENTER);
        g.drawString("v",x,y+d/2+8,Graphics.TOP|Graphics.HCENTER);
        g.drawString("<",x-d*3/2+8,y-5,Graphics.TOP|Graphics.HCENTER);
        g.drawString(">",x+d/2+8,y-5,Graphics.TOP|Graphics.HCENTER);
        g.drawString("△",bx+2,by-d+4,Graphics.TOP|Graphics.HCENTER);
        g.drawString("□",bx-d/2+2,by-9,Graphics.TOP|Graphics.HCENTER);
        g.drawString("○",bx+d/2+2,by-9,Graphics.TOP|Graphics.HCENTER);
        g.drawString("×",bx+2,by+d/2+4,Graphics.TOP|Graphics.HCENTER);
        g.setFont(Font.getFont(Font.FACE_SYSTEM,Font.STYLE_PLAIN,Font.SIZE_SMALL));
        g.drawString("L3 (6)",x,y+d*3/2+6,Graphics.TOP|Graphics.HCENTER);
        g.drawString("R3 (8)",bx,by+d*3/2+6,Graphics.TOP|Graphics.HCENTER);
        g.drawString("SELECT (5)",cx-26,h-foot-17,Graphics.TOP|Graphics.HCENTER);
        g.drawString("START (0)",cx+26,h-foot-17,Graphics.TOP|Graphics.HCENTER);
    }
    protected void keyPressed(int keyCode){
        int a=getGameAction(keyCode);char c=keyChar(keyCode);
        if(mode==NokiaBTController.MODE_GAMEPAD){gamepad(a,c,true);return;}
        if(c=='*'){toggleMouse();return;}
        if(c=='#'){key(NokiaBTController.KEY_BACK,true);return;}
        if(c=='0'){key(NokiaBTController.KEY_HOME,true);return;}
        if(c=='7'){key(NokiaBTController.KEY_VOLUMEDOWN,true);return;}
        if(c=='9'){key(NokiaBTController.KEY_VOLUMEUP,true);return;}
        if(c=='1'){key(NokiaBTController.KEY_MENU,true);return;}
        if(c=='2'){key(NokiaBTController.KEY_PLAYPAUSE,true);return;}
        if(c=='3'){key(NokiaBTController.KEY_MUTE,true);return;}
        if(c=='4'){key(NokiaBTController.KEY_REWIND,true);return;}
        if(c=='6'){key(NokiaBTController.KEY_FASTFORWARD,true);return;}
        if(c=='8'){key(NokiaBTController.KEY_STOP,true);return;}
        if(c=='5'||a==FIRE){if(mouseEnabled)mouseClick(1);else key(NokiaBTController.KEY_ENTER,true);return;}
        direction(a,true);
    }
    protected void keyRepeated(int keyCode){int a=getGameAction(keyCode);if(mode!=2&&mouseEnabled)moveMouse(a);}
    protected void keyReleased(int keyCode){
        int a=getGameAction(keyCode);char c=keyChar(keyCode);
        if(mode==2){gamepad(a,c,false);return;}
        int k=remoteKey(c);if(k!=0)key(k,false);else if(c=='5'||a==FIRE){if(!mouseEnabled)key(NokiaBTController.KEY_ENTER,false);}
        else if(!mouseEnabled)direction(a,false);
    }
    private int remoteKey(char c){
        if(c=='#')return NokiaBTController.KEY_BACK;if(c=='0')return NokiaBTController.KEY_HOME;
        if(c=='7')return NokiaBTController.KEY_VOLUMEDOWN;if(c=='9')return NokiaBTController.KEY_VOLUMEUP;
        if(c=='1')return NokiaBTController.KEY_MENU;if(c=='2')return NokiaBTController.KEY_PLAYPAUSE;
        if(c=='3')return NokiaBTController.KEY_MUTE;if(c=='4')return NokiaBTController.KEY_REWIND;
        if(c=='6')return NokiaBTController.KEY_FASTFORWARD;if(c=='8')return NokiaBTController.KEY_STOP;return 0;
    }
    private void direction(int a,boolean pressed){
        int k=a==UP?NokiaBTController.KEY_UP:a==DOWN?NokiaBTController.KEY_DOWN:
            a==LEFT?NokiaBTController.KEY_LEFT:a==RIGHT?NokiaBTController.KEY_RIGHT:0;
        if(k==0)return;if(mouseEnabled){if(pressed)moveMouse(a);}else key(k,pressed);
    }
    private void moveMouse(int a){int dx=a==LEFT?-10:a==RIGHT?10:0,dy=a==UP?-10:a==DOWN?10:0;
        if(dx!=0||dy!=0)app.send(NokiaBTController.PKT_MOUSE_MOVE,dx,dy);}
    private void mouseClick(int b){app.send(NokiaBTController.PKT_MOUSE_BUTTON,b,1);app.send(NokiaBTController.PKT_MOUSE_BUTTON,b,0);}
    private void key(int k,boolean down){app.send(NokiaBTController.PKT_KEY,k,down?1:0);}
    private void gamepad(int a,char c,boolean pressed){
        if(a==UP)up=pressed;else if(a==DOWN)down=pressed;else if(a==LEFT)left=pressed;else if(a==RIGHT)right=pressed;
        int hx=(right?1:0)-(left?1:0),hy=(down?1:0)-(up?1:0);
        app.send(NokiaBTController.PKT_PAD_HAT,0,hx);app.send(NokiaBTController.PKT_PAD_HAT,1,hy);
        app.send(NokiaBTController.PKT_PAD_AXIS,0,hx*32767);app.send(NokiaBTController.PKT_PAD_AXIS,1,hy*32767);
        int b=padButton(c);if(b<0&&a==FIRE)b=8;if(b>=0)app.send(NokiaBTController.PKT_PAD_BUTTON,b,pressed?1:0);
    }
    private int padButton(char c){
        if(c=='4')return 0;if(c=='3')return 1;if(c=='1')return 2;if(c=='2')return 3;
        if(c=='7')return 4;if(c=='9')return 5;if(c=='*')return 6;if(c=='#')return 7;
        if(c=='5')return 8;if(c=='0')return 9;if(c=='6')return 10;if(c=='8')return 11;return -1;
    }
    private char keyChar(int keyCode){
        if(keyCode>='0'&&keyCode<='9')return(char)keyCode;
        String n=getKeyName(keyCode);return n!=null&&n.length()==1?n.charAt(0):'\0';
    }
}