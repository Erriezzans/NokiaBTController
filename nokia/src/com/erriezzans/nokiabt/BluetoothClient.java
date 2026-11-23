package com.erriezzans.nokiabt;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;
import javax.bluetooth.*;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;

/** Bluetooth inquiry, user-selectable target, then exact UUID service discovery. */
final class BluetoothClient implements DiscoveryListener {
    private final NokiaBTController app;
    private final Object lock=new Object();
    private final Vector devices=new Vector();
    private DiscoveryAgent agent;
    private StreamConnection connection;
    private DataOutputStream output;
    private boolean inquiryDone,serviceDone,connected,connecting;
    private String serviceUrl;
    private int activeSearch;
    BluetoothClient(NokiaBTController owner){app=owner;}
    boolean isConnected(){return connected;}
    void discover(){
        if(connecting||connected)return;
        connecting=true;
        new Thread(new Runnable(){public void run(){
            try{
                app.setStatus("Searching Bluetooth devices...");
                LocalDevice local=LocalDevice.getLocalDevice();
                agent=local.getDiscoveryAgent();
                synchronized(lock){devices.removeAllElements();inquiryDone=false;}
                RemoteDevice[] paired=agent.retrieveDevices(DiscoveryAgent.PREKNOWN);
                if(paired!=null)for(int i=0;i<paired.length;i++)addDevice(paired[i]);
                if(!agent.startInquiry(DiscoveryAgent.GIAC,BluetoothClient.this))
                    throw new IOException("Bluetooth search could not start");
                synchronized(lock){if(!inquiryDone)lock.wait(25000L);}
                try{agent.cancelInquiry(BluetoothClient.this);}catch(Exception ignored){}
                Vector copy=new Vector();
                synchronized(lock){for(int i=0;i<devices.size();i++)copy.addElement(devices.elementAt(i));}
                connecting=false;
                app.showDevices(copy);
                app.setStatus(copy.size()+" device(s) found. Select the Android receiver.");
            }catch(Exception e){connecting=false;app.setStatus("Search failed: "+message(e));}
        }}).start();
    }
    private void addDevice(RemoteDevice d){synchronized(lock){if(!devices.contains(d))devices.addElement(d);}}
    void connectTo(final RemoteDevice device){
        if(connecting||connected)return;
        connecting=true;
        new Thread(new Runnable(){public void run(){
            try{
                String name=device.getFriendlyName(false);
                app.setStatus("Finding Nokia receiver service on "+name+"...");
                serviceDone=false;serviceUrl=null;
                UUID[] uuids={new UUID(NokiaBTController.SERVICE_UUID,false)};
                activeSearch=agent.searchServices(new int[]{0x0001,0x0004},uuids,device,BluetoothClient.this);
                synchronized(lock){if(!serviceDone)lock.wait(15000L);}
                if(serviceUrl==null)throw new IOException("Receiver service not found. Start the Android receiver first.");
                app.setStatus("Connecting to "+name+"...");
                connection=(StreamConnection)Connector.open(serviceUrl,Connector.WRITE,true);
                output=connection.openDataOutputStream();connected=true;
                app.setStatus("Connected: "+name);
                send(NokiaBTController.PKT_MODE,NokiaBTController.MODE_REMOTE,0);
            }catch(Exception e){closeConnection();app.setStatus("Connection failed: "+message(e));}
            finally{connecting=false;}
        }}).start();
    }
    public void deviceDiscovered(RemoteDevice d,DeviceClass c){addDevice(d);}
    public void inquiryCompleted(int type){synchronized(lock){inquiryDone=true;lock.notifyAll();}}
    public void servicesDiscovered(int id,ServiceRecord[] records){
        if(id!=activeSearch||records==null)return;
        for(int i=0;i<records.length;i++){
            String url=records[i].getConnectionURL(ServiceRecord.AUTHENTICATE_ENCRYPT,false);
            if(url!=null){synchronized(lock){serviceUrl=url;}break;}
        }
    }
    public void serviceSearchCompleted(int id,int response){if(id==activeSearch)synchronized(lock){serviceDone=true;lock.notifyAll();}}
    synchronized void send(byte type,int code,int value){
        if(!connected||output==null)return;
        try{output.writeByte(0x4e);output.writeByte(0x52);output.writeByte(NokiaBTController.PROTOCOL_VERSION);
            output.writeByte(type);output.writeShort(code);output.writeShort(value);output.flush();}
        catch(IOException e){app.setStatus("Bluetooth link lost");closeConnection();}
    }
    void disconnect(){send(NokiaBTController.PKT_RESET,0,0);closeConnection();try{if(agent!=null)agent.cancelInquiry(this);}catch(Exception ignored){}}
    private synchronized void closeConnection(){connected=false;try{if(output!=null)output.close();}catch(Exception ignored){}
        try{if(connection!=null)connection.close();}catch(Exception ignored){}output=null;connection=null;}
    private String message(Exception e){return e.getMessage()==null?e.toString():e.getMessage();}
}