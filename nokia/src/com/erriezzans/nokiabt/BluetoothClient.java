package com.erriezzans.nokiabt;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;
import javax.bluetooth.*;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;

/** Bluetooth Classic inquiry, explicit target selection and RFCOMM service connection. */
final class BluetoothClient implements DiscoveryListener {
    private final NokiaBTController app;
    private final Object lock=new Object();
    private final Vector devices=new Vector();

    private DiscoveryAgent agent;
    private StreamConnection connection;
    private DataOutputStream output;
    private boolean inquiryDone,serviceDone,connected;
    private volatile boolean scanning,connecting;
    private String serviceUrl;
    private int activeSearch=-1;

    BluetoothClient(NokiaBTController owner){app=owner;}
    synchronized boolean isConnected(){return connected;}

    void discover(){
        synchronized(this){if(scanning||connecting||connected)return;scanning=true;}
        synchronized(lock){devices.removeAllElements();inquiryDone=false;}
        app.updateDeviceBrowser(copyDevices(),DeviceBrowserCanvas.SEARCHING,
                "Searching paired and nearby Bluetooth devices...");

        new Thread(new Runnable(){public void run(){
            String failure=null;
            boolean supersededByConnect=false;
            try{
                LocalDevice local=LocalDevice.getLocalDevice();
                agent=local.getDiscoveryAgent();

                // PREKNOWN contains trusted devices; CACHED adds devices seen by earlier inquiries.
                addDevices(agent.retrieveDevices(DiscoveryAgent.PREKNOWN));
                addDevices(agent.retrieveDevices(DiscoveryAgent.CACHED));

                if(!agent.startInquiry(DiscoveryAgent.GIAC,BluetoothClient.this))
                    throw new IOException("Bluetooth inquiry could not start. Check that Bluetooth is on.");

                long deadline=System.currentTimeMillis()+20000L;
                synchronized(lock){
                    while(!inquiryDone&&!isConnecting()&&System.currentTimeMillis()<deadline){
                        long remaining=deadline-System.currentTimeMillis();
                        if(remaining>0)lock.wait(remaining);
                    }
                }
                try{agent.cancelInquiry(BluetoothClient.this);}catch(Exception ignored){}
                supersededByConnect=isConnecting();
            }catch(Exception e){failure=message(e);}
            finally{
                synchronized(BluetoothClient.this){scanning=false;}
            }

            if(supersededByConnect||isConnecting())return;
            Vector copy=copyDevices();
            if(failure!=null){
                app.updateDeviceBrowser(copy,DeviceBrowserCanvas.ERROR,
                        "Search failed: "+failure);
            }else if(copy.size()==0){
                app.updateDeviceBrowser(copy,DeviceBrowserCanvas.READY,
                        "No devices found. Make Android discoverable, then Scan again.");
            }else{
                app.updateDeviceBrowser(copy,DeviceBrowserCanvas.READY,
                        copy.size()+" device(s) found. Select the Android receiver and press Connect.");
            }
        }}).start();
    }

    private boolean isConnecting(){synchronized(this){return connecting;}}
    private Vector copyDevices(){
        Vector copy=new Vector();
        synchronized(lock){for(int i=0;i<devices.size();i++)copy.addElement(devices.elementAt(i));}
        return copy;
    }

    private void addDevices(RemoteDevice[] found){
        if(found==null)return;
        for(int i=0;i<found.length;i++)addDevice(found[i]);
    }

    private void addDevice(RemoteDevice device){
        if(device==null)return;
        boolean added=false;
        synchronized(lock){
            String address=device.getBluetoothAddress();
            for(int i=0;i<devices.size();i++){
                RemoteDevice existing=(RemoteDevice)devices.elementAt(i);
                if(existing.getBluetoothAddress().equalsIgnoreCase(address))return;
            }
            devices.addElement(device);
            added=true;
        }
        if(added){
            app.updateDeviceBrowser(copyDevices(),DeviceBrowserCanvas.SEARCHING,
                    "Found device. Searching continues...");
        }
    }

    void connectTo(final RemoteDevice device){
        if(device==null)return;
        synchronized(this){
            if(connecting||connected)return;
            connecting=true;
        }

        // Stop inquiry before SDP lookup; Bluetooth stacks often reject overlapping procedures.
        if(scanning&&agent!=null){
            try{agent.cancelInquiry(this);}catch(Exception ignored){}
            synchronized(lock){
                long deadline=System.currentTimeMillis()+2500L;
                while(!inquiryDone&&System.currentTimeMillis()<deadline){
                    try{lock.wait(100L);}catch(InterruptedException ignored){}
                }
            }
        }

        new Thread(new Runnable(){public void run(){
            String name=device.getBluetoothAddress();
            try{
                try{
                    String friendly=device.getFriendlyName(false);
                    if(friendly!=null&&friendly.length()>0)name=friendly;
                }catch(Exception ignored){}

                if(agent==null)agent=LocalDevice.getLocalDevice().getDiscoveryAgent();
                app.updateDeviceBrowser(copyDevices(),DeviceBrowserCanvas.CONNECTING,
                        "Looking for Nokia BT Controller on "+name+"...");

                synchronized(lock){serviceDone=false;serviceUrl=null;activeSearch=-1;}
                int search=agent.searchServices(new int[]{0x0001,0x0004},
                        new UUID[]{new UUID(NokiaBTController.SERVICE_UUID,false)},
                        device,BluetoothClient.this);
                synchronized(lock){
                    activeSearch=search;
                    long deadline=System.currentTimeMillis()+15000L;
                    while(!serviceDone&&System.currentTimeMillis()<deadline){
                        long remaining=deadline-System.currentTimeMillis();
                        if(remaining>0)lock.wait(remaining);
                    }
                }

                String url;
                synchronized(lock){url=serviceUrl;}
                if(url==null){
                    try{agent.cancelServiceSearch(search);}catch(Exception ignored){}
                    throw new IOException("Receiver service not found. On Android, start the receiver and keep it listening.");
                }

                app.updateDeviceBrowser(copyDevices(),DeviceBrowserCanvas.CONNECTING,
                        "Connecting securely to "+name+"...");
                connection=(StreamConnection)Connector.open(url,Connector.WRITE,true);
                output=connection.openDataOutputStream();
                synchronized(this){connected=true;}
                send(NokiaBTController.PKT_MODE,NokiaBTController.MODE_REMOTE,0);
                if(!isConnected())throw new IOException("Bluetooth output stream closed during setup.");
                app.connectionSucceeded(name);
            }catch(Exception e){
                closeConnection();
                app.connectionFailed("Could not connect to "+name+": "+message(e));
            }finally{
                synchronized(BluetoothClient.this){connecting=false;}
            }
        }}).start();
    }

    public void deviceDiscovered(RemoteDevice device,DeviceClass deviceClass){addDevice(device);}

    public void inquiryCompleted(int type){
        synchronized(lock){inquiryDone=true;lock.notifyAll();}
    }

    public void servicesDiscovered(int id,ServiceRecord[] records){
        if(records==null)return;
        synchronized(lock){
            if(activeSearch!=-1&&id!=activeSearch)return;
            for(int i=0;i<records.length;i++){
                String url=records[i].getConnectionURL(ServiceRecord.AUTHENTICATE_ENCRYPT,false);
                if(url!=null){serviceUrl=url;break;}
            }
        }
    }

    public void serviceSearchCompleted(int id,int response){
        synchronized(lock){
            if(activeSearch!=-1&&id!=activeSearch)return;
            serviceDone=true;
            lock.notifyAll();
        }
    }

    synchronized void send(byte type,int code,int value){
        if(!connected||output==null)return;
        try{
            output.writeByte(0x4e);
            output.writeByte(0x52);
            output.writeByte(NokiaBTController.PROTOCOL_VERSION);
            output.writeByte(type);
            output.writeShort(code);
            output.writeShort(value);
            output.flush();
        }catch(IOException e){
            app.setStatus("Bluetooth link lost");
            closeConnection();
        }
    }

    void disconnect(){
        send(NokiaBTController.PKT_RESET,0,0);
        closeConnection();
        try{if(agent!=null)agent.cancelInquiry(this);}catch(Exception ignored){}
        try{if(agent!=null&&activeSearch!=-1)agent.cancelServiceSearch(activeSearch);}catch(Exception ignored){}
    }

    private synchronized void closeConnection(){
        connected=false;
        try{if(output!=null)output.close();}catch(Exception ignored){}
        try{if(connection!=null)connection.close();}catch(Exception ignored){}
        output=null;
        connection=null;
    }

    private String message(Exception e){
        return e.getMessage()==null?e.toString():e.getMessage();
    }
}
