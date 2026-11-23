package com.erriezzans.nokiabt;

import java.util.Vector;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;
import javax.microedition.lcdui.TextBox;
import javax.microedition.lcdui.TextField;
import javax.microedition.midlet.MIDlet;
import javax.bluetooth.RemoteDevice;

/** Nokia C5-00 Java ME Bluetooth remote and gamepad. */
public final class NokiaBTController extends MIDlet implements CommandListener {
    static final String SERVICE_UUID = "e1f55c5a3a624c939c5c9d8a5fce0016";
    static final byte PROTOCOL_VERSION = 1;
    static final byte PKT_KEY=1, PKT_MOUSE_MOVE=2, PKT_MOUSE_BUTTON=3,
        PKT_PAD_BUTTON=4, PKT_PAD_HAT=5, PKT_PAD_AXIS=6, PKT_RESET=7, PKT_MODE=8, PKT_TEXT=9;
    static final int KEY_ESC=1, KEY_ENTER=28, KEY_HOME=102, KEY_UP=103, KEY_LEFT=105,
        KEY_RIGHT=106, KEY_DOWN=108, KEY_VOLUMEDOWN=114, KEY_VOLUMEUP=115,
        KEY_PLAYPAUSE=164, KEY_BACK=158, KEY_MENU=139, KEY_MUTE=113,
        KEY_CHANNELUP=0, KEY_CHANNELDOWN=0, KEY_STOP=128, KEY_REWIND=168, KEY_FASTFORWARD=208;
    static final int MODE_REMOTE=0, MODE_MOUSE=1, MODE_GAMEPAD=2;
    private Display display;
    private RemoteCanvas canvas;
    private BluetoothClient bluetooth;
    private List deviceList;
    private Vector foundDevices;
    private TextBox textBox;
    private final KeyMap keyMap = new KeyMap();
    private final Command sendTextCommand=new Command("Send",Command.OK,1);
    private final Command cancelTextCommand=new Command("Cancel",Command.CANCEL,2);
    private final Command connectCommand=new Command("Find devices",Command.SCREEN,1);
    private final Command remoteCommand=new Command("Remote",Command.SCREEN,2);
    private final Command gamepadCommand=new Command("Gamepad",Command.SCREEN,3);
    private final Command mouseCommand=new Command("Mouse ON/OFF",Command.SCREEN,4);
    private final Command textCommand=new Command("Send text",Command.SCREEN,5);
    private final Command keyRemapCommand=new Command("Key remap",Command.SCREEN,6);
    private final Command disconnectCommand=new Command("Disconnect",Command.SCREEN,6);
    private final Command exitCommand=new Command("Exit",Command.EXIT,9);
    public NokiaBTController() {
        display=Display.getDisplay(this); canvas=new RemoteCanvas(this);
        canvas.addCommand(connectCommand); canvas.addCommand(remoteCommand);
        canvas.addCommand(gamepadCommand); canvas.addCommand(mouseCommand);
        canvas.addCommand(textCommand); canvas.addCommand(keyRemapCommand); canvas.addCommand(disconnectCommand);
        canvas.addCommand(exitCommand); canvas.setCommandListener(this);
        bluetooth=new BluetoothClient(this);
    }
    public void startApp(){display.setCurrent(canvas);canvas.refresh();}
    public void pauseApp(){}
    public void destroyApp(boolean unconditional){if(bluetooth!=null)bluetooth.disconnect();notifyDestroyed();}
    void connectBluetooth(){if(bluetooth!=null&&!bluetooth.isConnected())bluetooth.discover();}
    void selectDevice(int index){
        if(foundDevices==null||index<0||index>=foundDevices.size())return;
        bluetooth.connectTo((RemoteDevice)foundDevices.elementAt(index));
        display.setCurrent(canvas);
    }
    void showDevices(Vector devices){
        foundDevices=devices;
        if(devices==null||devices.size()==0){setStatus("No devices found. Pair Android first.");return;}
        deviceList=new List("Choose Android device",List.IMPLICIT);
        for(int i=0;i<devices.size();i++){
            RemoteDevice d=(RemoteDevice)devices.elementAt(i);
            String name;
            try{name=d.getFriendlyName(false);}catch(Exception e){name=null;}
            if(name==null||name.length()==0)name=d.getBluetoothAddress();
            deviceList.append(name+"  ["+d.getBluetoothAddress()+"]",null);
        }
        deviceList.setCommandListener(this);
        display.setCurrent(deviceList);
    }
    private void showTextEntry(){
        textBox=new TextBox("Send text to Android","",160,TextField.ANY);
        textBox.addCommand(sendTextCommand);textBox.addCommand(cancelTextCommand);textBox.setCommandListener(this);
        display.setCurrent(textBox);
    }
    void disconnectBluetooth(){if(bluetooth!=null)bluetooth.disconnect();canvas.setConnectionStatus("Disconnected");}
    int findGamepadButton(char key){return keyMap.findButton(key);}
    String gamepadKeyLabel(int button){return keyMap.getKeyLabel(button);}
    void showMainScreen(){display.setCurrent(canvas);}
    void showKeyRemap(){display.setCurrent(new KeyRemapForm(this,keyMap.getMappings()));}
    void saveKeyMappings(int[] values){
        boolean persisted=keyMap.setMappings(values);
        display.setCurrent(canvas);
        if(persisted)setStatus("Gamepad key mapping saved.");
        else setStatus("Mapping applied, but phone could not save it to memory.");
        canvas.refresh();
    }
    boolean isConnected(){return bluetooth!=null&&bluetooth.isConnected();}
    void send(byte type,int code,int value){if(bluetooth!=null)bluetooth.send(type,code,value);}
    void setStatus(String value){canvas.setConnectionStatus(value);}
    public void commandAction(Command command,Displayable d){
        if(d==deviceList){if(command==List.SELECT_COMMAND)selectDevice(deviceList.getSelectedIndex());return;}
        if(command==connectCommand)connectBluetooth();
        else if(command==remoteCommand)canvas.setMode(MODE_REMOTE);
        else if(command==gamepadCommand)canvas.setMode(MODE_GAMEPAD);
        else if(command==mouseCommand)canvas.toggleMouse();
        else if(command==textCommand)showTextEntry();
        else if(command==keyRemapCommand)showKeyRemap();
        else if(d==textBox){
            if(command==sendTextCommand){String value=textBox.getString();for(int i=0;i<value.length();i++)send(PKT_TEXT,value.charAt(i),1);send(PKT_KEY,KEY_ENTER,1);send(PKT_KEY,KEY_ENTER,0);}
            display.setCurrent(canvas);
        }
        else if(command==disconnectCommand)disconnectBluetooth();
        else if(command==exitCommand)destroyApp(true);
    }
}