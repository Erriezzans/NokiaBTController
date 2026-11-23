package com.erriezzans.nokiabt;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;
import javax.bluetooth.*;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;

/** Serializes Bluetooth inquiry, SDP lookup and RFCOMM connection. */
final class BluetoothClient implements DiscoveryListener {
    private final NokiaBTController app;
    private final Object lock=new Object();
    private final Vector devices=new Vector();

    private DiscoveryAgent agent;
    private StreamConnection connection;
    private DataOutputStream output;
    private boolean inquiryDone,discoveryFinished,inquiryRunning;
    private boolean serviceDone,connected;
    private volatile boolean scanning,connecting;
    private String serviceUrl;
    private String serviceFailure;
    private int activeSearch=-1;

    BluetoothClient(NokiaBTController owner){app=owner;}
    synchronized boolean isConnected(){return connected;}

    void discover(){
        synchronized(this){
            if(scanning||connecting||connected)return;
            scanning=true;
        }
        synchronized(lock){
            devices.removeAllElements();
            inquiryDone=false;
            discoveryFinished=false;
            inquiryRunning=false;
        }
        app.updateDeviceBrowser(copyDevices(),DeviceBrowserCanvas.SEARCHING,
                "Searching paired and nearby Bluetooth devices...");

        new Thread(new Runnable(){public void run(){
            String failure=null;
            try{
                LocalDevice local=LocalDevice.getLocalDevice();
                agent=local.getDiscoveryAgent();

                // Retrieve bonded devices before the radio inquiry so a paired receiver appears immediately.
                addDevices(agent.retrieveDevices(DiscoveryAgent.PREKNOWN));
                addDevices(agent.retrieveDevices(DiscoveryAgent.CACHED));

                // The user may select a bonded device while this thread is preparing.
                if(!isConnecting()){
                    if(!agent.startInquiry(DiscoveryAgent.GIAC,BluetoothClient.this))
                        throw new IOException("Bluetooth inquiry could not start. Check Bluetooth is turned on.");

                    synchronized(lock){inquiryRunning=true;lock.notifyAll();}
                    // Close the race where Connect was pressed just before startInquiry returned.
                    if(isConnecting()){
                        try{agent.cancelInquiry(BluetoothClient.this);}catch(Exception ignored){}
                    }

                    long deadline=System.currentTimeMillis()+20000L;
                    synchronized(lock){
                        while(!inquiryDone&&!isConnecting()&&System.currentTimeMillis()<deadline){
                            long remaining=deadline-System.currentTimeMillis();
                            if(remaining>0)lock.wait(remaining);
                        }
                    }
                    if(isConnecting()){
                        try{agent.cancelInquiry(BluetoothClient.this);}catch(Exception ignored){}
                        synchronized(lock){
                            long cancelDeadline=System.currentTimeMillis()+5000L;
                            while(!inquiryDone&&System.currentTimeMillis()<cancelDeadline)
                                lock.wait(250L);
                        }
                    }else{
                        try{agent.cancelInquiry(BluetoothClient.this);}catch(Exception ignored){}
                    }
                }
            }catch(Exception e){
                failure=message(e);
            }finally{
                synchronized(lock){
                    inquiryRunning=false;
                    discoveryFinished=true;
                    lock.notifyAll();
                }
                synchronized(BluetoothClient.this){scanning=false;}
            }

            // A Connect request owns the next Bluetooth operation; do not overwrite its UI.
            if(isConnecting())return;
            Vector copy=copyDevices();
            if(failure!=null){
                app.updateDeviceBrowser(copy,DeviceBrowserCanvas.ERROR,
                        "Search failed: "+failure);
            }else if(copy.size()==0){
                app.updateDeviceBrowser(copy,DeviceBrowserCanvas.READY,
                        "No devices found. Pair Android or make it discoverable, then Scan again.");
            }else{
                app.updateDeviceBrowser(copy,DeviceBrowserCanvas.READY,
                        copy.size()+" device(s) found. Select Android, then press Connect.");
            }
        }}).start();
    }

    private boolean isConnecting(){return connecting;}

    private Vector copyDevices(){
        Vector copy=new Vector();
        synchronized(lock){
            for(int i=0;i<devices.size();i++)copy.addElement(devices.elementAt(i));
        }
        return copy;
    }

    private void addDevices(RemoteDevice[] found){
        if(found==null)return;
        for(int i=0;i<found.length;i++)addDevice(found[i]);
    }

    private void addDevice(RemoteDevice device){
        if(device==null)return;
        synchronized(lock){
            String address=device.getBluetoothAddress();
            for(int i=0;i<devices.size();i++){
                RemoteDevice existing=(RemoteDevice)devices.elementAt(i);
                if(existing.getBluetoothAddress().equalsIgnoreCase(address))return;
            }
            devices.addElement(device);
        }
        app.updateDeviceBrowser(copyDevices(),DeviceBrowserCanvas.SEARCHING,
                "Found device. Search is still running...");
    }

    void connectTo(final RemoteDevice device){
        if(device==null)return;
        synchronized(this){
            if(connecting||connected)return;
            connecting=true;
        }
        // Wake the inquiry thread so it can cancel promptly and release the radio.
        synchronized(lock){lock.notifyAll();}
        if(agent!=null){
            try{agent.cancelInquiry(this);}catch(Exception ignored){}
        }

        app.updateDeviceBrowser(copyDevices(),DeviceBrowserCanvas.CONNECTING,
                "Stopping device scan before connection...");

        new Thread(new Runnable(){public void run(){
            String name=device.getBluetoothAddress();
            String stage="Preparing connection";
            try{
                // Wait until inquiry has actually stopped. This also covers selecting a
                // paired result while the discovery thread is still starting the inquiry.
                synchronized(lock){
                    long deadline=System.currentTimeMillis()+8000L;
                    while(scanning&&!discoveryFinished&&System.currentTimeMillis()<deadline){
                        lock.wait(200L);
                    }
                    if(scanning&&!discoveryFinished)
                        throw new IOException("Bluetooth scan did not stop. Choose Scan again and wait for it to finish.");
                }

                stage="Reading device name";
                try{
                    String friendly=device.getFriendlyName(false);
                    if(friendly!=null&&friendly.length()>0)name=friendly;
                }catch(Exception ignored){}

                if(agent==null)agent=LocalDevice.getLocalDevice().getDiscoveryAgent();

                stage="Searching for receiver service";
                app.updateDeviceBrowser(copyDevices(),DeviceBrowserCanvas.CONNECTING,
                        "Searching Nokia BT Controller service on "+name+"...");
                synchronized(lock){
                    serviceDone=false;
                    serviceUrl=null;
                    serviceFailure=null;
                    activeSearch=-1;
                }

                int search=agent.searchServices(new int[]{0x0001,0x0004},
                        new UUID[]{new UUID(NokiaBTController.SERVICE_UUID,false)},
                        device,BluetoothClient.this);
                synchronized(lock){
                    activeSearch=search;
                    long deadline=System.currentTimeMillis()+20000L;
                    while(!serviceDone&&System.currentTimeMillis()<deadline){
                        long remaining=deadline-System.currentTimeMillis();
                        if(remaining>0)lock.wait(remaining);
                    }
                }

                String url;
                String sdpError;
                synchronized(lock){url=serviceUrl;sdpError=serviceFailure;}
                if(url==null){
                    try{agent.cancelServiceSearch(search);}catch(Exception ignored){}
                    if(sdpError!=null)throw new IOException(sdpError);
                    throw new IOException("Android receiver service was not found. Keep the receiver running, pair both devices, then retry.");
                }

                stage="Opening Bluetooth connection";
                app.updateDeviceBrowser(copyDevices(),DeviceBrowserCanvas.CONNECTING,
                        "Opening secure Bluetooth link to "+name+"...");
                connection=(StreamConnection)Connector.open(url,Connector.WRITE,true);
                output=connection.openDataOutputStream();

                synchronized(BluetoothClient.this){connected=true;}
                stage="Sending controller handshake";
                send(NokiaBTController.PKT_MODE,NokiaBTController.MODE_REMOTE,0);
                if(!isConnected())
                    throw new IOException("Bluetooth link closed before the controller handshake completed.");

                app.connectionSucceeded(name);
            }catch(Exception e){
                closeConnection();
                String detail=message(e);
                app.connectionFailed(stage+": "+detail);
            }finally{
                synchronized(BluetoothClient.this){connecting=false;}
            }
        }}).start();
    }

    public void deviceDiscovered(RemoteDevice device,DeviceClass deviceClass){
        addDevice(device);
    }

    public void inquiryCompleted(int type){
        synchronized(lock){
            inquiryDone=true;
            inquiryRunning=false;
            lock.notifyAll();
        }
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
            if(serviceUrl==null){
                if(response==ServiceSearchResponse.SERVICE_SEARCH_NO_RECORDS)
                    serviceFailure="No matching RFCOMM service record was advertised by Android.";
                else if(response==ServiceSearchResponse.SERVICE_SEARCH_DEVICE_NOT_REACHABLE)
                    serviceFailure="Android is not reachable. Keep it nearby and check Bluetooth is on.";
                else if(response==ServiceSearchResponse.SERVICE_SEARCH_ERROR)
                    serviceFailure="Bluetooth service discovery failed. Pair the devices and retry.";
                else if(response==ServiceSearchResponse.SERVICE_SEARCH_TERMINATED)
                    serviceFailure="Bluetooth service discovery was cancelled.";
            }
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
            app.setStatus("Bluetooth link lost: "+message(e));
            closeConnection();
        }
    }

    void disconnect(){
        send(NokiaBTController.PKT_RESET,0,0);
        closeConnection();
        try{if(agent!=null)agent.cancelInquiry(this);}catch(Exception ignored){}
        try{if(agent!=null&&activeSearch!=-1)agent.cancelServiceSearch(activeSearch);}catch(Exception ignored){}
        synchronized(lock){inquiryDone=true;discoveryFinished=true;lock.notifyAll();}
    }

    private synchronized void closeConnection(){
        connected=false;
        try{if(output!=null)output.close();}catch(Exception ignored){}
        try{if(connection!=null)connection.close();}catch(Exception ignored){}
        output=null;
        connection=null;
    }

    private String message(Exception e){
        String value=e.getMessage();
        return value==null||value.length()==0?e.toString():value;
    }
}
