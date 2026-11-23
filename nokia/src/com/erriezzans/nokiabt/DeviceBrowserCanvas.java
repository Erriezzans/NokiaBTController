package com.erriezzans.nokiabt;

import java.util.Vector;
import javax.bluetooth.RemoteDevice;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

/** Dedicated 240x320-friendly Bluetooth browser with explicit selection and actions. */
final class DeviceBrowserCanvas extends Canvas {
    static final int SEARCHING=0, READY=1, CONNECTING=2, ERROR=3;

    private final NokiaBTController app;
    private final Command connectCommand=new Command("Connect",Command.OK,1);
    private final Command scanCommand=new Command("Scan again",Command.SCREEN,2);
    private final Command backCommand=new Command("Back",Command.BACK,3);
    private Vector devices=new Vector();
    private int selected;
    private int state=SEARCHING;
    private String status="Preparing Bluetooth search...";

    DeviceBrowserCanvas(NokiaBTController owner) {
        app=owner;
        setFullScreenMode(false);
        addCommand(connectCommand);
        addCommand(scanCommand);
        addCommand(backCommand);
        setCommandListener(owner);
    }

    Command getConnectCommand(){return connectCommand;}
    Command getScanCommand(){return scanCommand;}
    Command getBackCommand(){return backCommand;}
    int getSelectedIndex(){return selected;}
    int getDeviceCount(){return devices.size();}

    void setDevices(Vector list) {
        devices=list==null?new Vector():list;
        if(selected>=devices.size())selected=devices.size()-1;
        if(selected<0)selected=0;
        repaint();
    }

    void setPageState(int newState,String message) {
        state=newState;
        if(message!=null)status=message;
        repaint();
    }

    private RemoteDevice selectedDevice() {
        if(selected<0||selected>=devices.size())return null;
        return (RemoteDevice)devices.elementAt(selected);
    }

    protected void paint(Graphics g) {
        int w=getWidth(),h=getHeight();
        int footer=23;
        g.setColor(12,18,29);
        g.fillRect(0,0,w,h);
        g.setColor(24,38,58);
        g.fillRect(0,0,w,45);
        g.setColor(91,208,190);
        g.fillRect(0,44,w,2);

        g.setFont(Font.getFont(Font.FACE_SYSTEM,Font.STYLE_BOLD,Font.SIZE_MEDIUM));
        g.setColor(250,252,255);
        g.drawString("CHOOSE ANDROID DEVICE",8,5,Graphics.TOP|Graphics.LEFT);
        g.setFont(Font.getFont(Font.FACE_SYSTEM,Font.STYLE_PLAIN,Font.SIZE_SMALL));
        g.setColor(174,193,216);
        g.drawString("Bluetooth connection",8,25,Graphics.TOP|Graphics.LEFT);

        int cardY=53;
        g.setColor(26,38,56);
        g.fillRoundRect(7,cardY,w-14,52,9,9);
        g.setColor(state==CONNECTING?255:state==ERROR?255:91,
                   state==CONNECTING?190:state==ERROR?145:208,
                   state==CONNECTING?92:state==ERROR?105:190);
        g.fillRoundRect(7,cardY,4,52,4,4);
        g.setFont(Font.getFont(Font.FACE_SYSTEM,Font.STYLE_BOLD,Font.SIZE_SMALL));
        g.setColor(245,248,253);
        String label=state==SEARCHING?"SEARCHING":state==CONNECTING?"CONNECTING":
                     state==ERROR?"CONNECTION ISSUE":"DEVICES";
        g.drawString(label,17,cardY+6,Graphics.TOP|Graphics.LEFT);
        g.setFont(Font.getFont(Font.FACE_SYSTEM,Font.STYLE_PLAIN,Font.SIZE_SMALL));
        g.setColor(185,201,221);
        drawStatus(g,17,cardY+21,w-34);

        g.setFont(Font.getFont(Font.FACE_SYSTEM,Font.STYLE_BOLD,Font.SIZE_SMALL));
        g.setColor(228,236,247);
        g.drawString("AVAILABLE DEVICES",9,111,Graphics.TOP|Graphics.LEFT);
        g.setColor(105,125,150);
        g.drawString(String.valueOf(devices.size()),w-11,111,Graphics.TOP|Graphics.RIGHT);

        int listTop=129;
        int listBottom=h-footer-5;
        int rowHeight=39;
        int visible=(listBottom-listTop)/rowHeight;
        if(visible<1)visible=1;
        int first=0;
        if(selected>=visible)first=selected-visible+1;

        if(devices.size()==0) {
            drawEmptyState(g,w,listTop,listBottom);
        } else {
            for(int i=first;i<devices.size()&&i<first+visible;i++) {
                int y=listTop+(i-first)*rowHeight;
                boolean active=i==selected;
                g.setColor(active?38:25,active?74:38,active?89:56);
                g.fillRoundRect(7,y,w-14,rowHeight-4,8,8);
                if(active) {
                    g.setColor(91,208,190);
                    g.fillRoundRect(7,y,4,rowHeight-4,4,4);
                }
                g.setColor(active?248:220,active?252:231,active?255:242);
                g.setFont(Font.getFont(Font.FACE_SYSTEM,Font.STYLE_BOLD,Font.SIZE_SMALL));
                String name=deviceName((RemoteDevice)devices.elementAt(i));
                drawFit(g,name,16,y+5,w-38,Graphics.TOP|Graphics.LEFT);
                g.setColor(active?185:145,active?211:166,active?221:188);
                g.setFont(Font.getFont(Font.FACE_SYSTEM,Font.STYLE_PLAIN,Font.SIZE_SMALL));
                String address=deviceAddress((RemoteDevice)devices.elementAt(i));
                drawFit(g,address,16,y+20,w-38,Graphics.TOP|Graphics.LEFT);
                g.setColor(active?91:105,active?208:126,active?190:150);
                g.drawString(active?">":"·",w-15,y+11,Graphics.TOP|Graphics.RIGHT);
            }
        }

        g.setColor(31,45,64);
        g.fillRect(0,h-footer,w,footer);
        g.setColor(212,224,240);
        g.setFont(Font.getFont(Font.FACE_SYSTEM,Font.STYLE_PLAIN,Font.SIZE_SMALL));
        g.drawString("UP/DOWN SELECT   CENTER CONNECT",w/2,h-footer+5,Graphics.TOP|Graphics.HCENTER);
    }

    private void drawEmptyState(Graphics g,int w,int top,int bottom) {
        g.setColor(24,37,55);
        g.fillRoundRect(7,top,w-14,bottom-top,9,9);
        g.setColor(245,248,253);
        g.setFont(Font.getFont(Font.FACE_SYSTEM,Font.STYLE_BOLD,Font.SIZE_MEDIUM));
        String heading=state==SEARCHING?"Looking for devices...":
                       state==CONNECTING?"Connecting...":"No devices found";
        g.drawString(heading,w/2,top+12,Graphics.TOP|Graphics.HCENTER);
        g.setFont(Font.getFont(Font.FACE_SYSTEM,Font.STYLE_PLAIN,Font.SIZE_SMALL));
        g.setColor(184,201,222);
        if(state==SEARCHING) {
            g.drawString("Paired devices appear first.",w/2,top+39,Graphics.TOP|Graphics.HCENTER);
            g.drawString("Nearby devices are added as found.",w/2,top+56,Graphics.TOP|Graphics.HCENTER);
        } else {
            g.drawString("On Android:",w/2,top+39,Graphics.TOP|Graphics.HCENTER);
            g.drawString("1. Open Nokia BT Receiver.",w/2,top+58,Graphics.TOP|Graphics.HCENTER);
            g.drawString("2. Start Bluetooth receiver.",w/2,top+76,Graphics.TOP|Graphics.HCENTER);
            g.drawString("3. Tap Make discoverable.",w/2,top+94,Graphics.TOP|Graphics.HCENTER);
            g.drawString("Then return here and Scan again.",w/2,top+119,Graphics.TOP|Graphics.HCENTER);
        }
    }

    private String deviceName(RemoteDevice device) {
        try {
            String name=device.getFriendlyName(false);
            if(name!=null&&name.length()>0)return name;
        } catch(Exception ignored) { }
        return deviceAddress(device);
    }

    private String deviceAddress(RemoteDevice device) {
        try { return device.getBluetoothAddress(); }
        catch(Exception ignored) { return "Bluetooth device"; }
    }

    private void drawStatus(Graphics g,int x,int y,int maxWidth) {
        String value=status==null?"":status;
        Font font=g.getFont();
        int end=fitEnd(font,value,0,maxWidth);
        if(end<=0)return;
        g.drawString(value.substring(0,end),x,y,Graphics.TOP|Graphics.LEFT);
        int next=end;
        while(next<value.length()&&value.charAt(next)==' ')next++;
        if(next<value.length()){
            int secondEnd=fitEnd(font,value,next,maxWidth);
            String second=value.substring(next,secondEnd);
            if(secondEnd<value.length()&&second.length()>2)
                second=second.substring(0,second.length()-2)+"..";
            g.drawString(second,x,y+11,Graphics.TOP|Graphics.LEFT);
        }
    }

    private int fitEnd(Font font,String value,int start,int maxWidth) {
        int end=start,lastSpace=-1;
        while(end<value.length()&&font.stringWidth(value.substring(start,end+1))<=maxWidth){
            if(value.charAt(end)==' ')lastSpace=end;
            end++;
        }
        if(end<value.length()&&lastSpace>=start)end=lastSpace;
        return end;
    }

    private void drawFit(Graphics g,String value,int x,int y,int maxWidth,int anchor) {
        if(value==null)value="";
        Font font=g.getFont();
        String shown=value;
        while(shown.length()>0&&font.stringWidth(shown)>maxWidth)
            shown=shown.substring(0,shown.length()-1);
        if(shown.length()<value.length()&&shown.length()>2)
            shown=shown.substring(0,shown.length()-2)+"..";
        g.drawString(shown,x,y,anchor);
    }

    protected void keyPressed(int keyCode) {
        int action=getGameAction(keyCode);
        if(action==UP)moveSelection(-1);
        else if(action==DOWN)moveSelection(1);
        else if(action==FIRE)connectSelected();
    }

    protected void keyRepeated(int keyCode) {
        int action=getGameAction(keyCode);
        if(action==UP)moveSelection(-1);
        else if(action==DOWN)moveSelection(1);
    }

    private void moveSelection(int delta) {
        if(devices.size()==0)return;
        selected+=delta;
        if(selected<0)selected=0;
        if(selected>=devices.size())selected=devices.size()-1;
        repaint();
    }

    private void connectSelected() {
        if(devices.size()==0||state==CONNECTING)return;
        app.selectDevice(selected);
    }
}
