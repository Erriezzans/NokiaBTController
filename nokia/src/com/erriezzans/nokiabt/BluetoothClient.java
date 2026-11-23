package com.erriezzans.nokiabt;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;

/** JSR-82 inquiry, SPP service discovery and framed packet sender. */
final class BluetoothClient implements DiscoveryListener, Runnable {
    private final NokiaBTController app;
    private final Object lock = new Object();
    private final Vector devices = new Vector();
    private DiscoveryAgent agent;
    private StreamConnection connection;
    private volatile DataOutputStream output;
    private boolean inquiryDone;
    private boolean serviceDone;
    private String serviceUrl;
    private int activeSearch;
    private volatile boolean connected;
    private boolean connecting;

    BluetoothClient(NokiaBTController owner) {
        app = owner;
    }

    boolean isConnected() {
        return connected;
    }

    void connect() {
        if (connecting || connected) return;
        connecting = true;
        Thread thread = new Thread(this);
        thread.start();
    }

    public void run() {
        try {
            app.setStatus("Starting Bluetooth inquiry...");
            LocalDevice local = LocalDevice.getLocalDevice();
            agent = local.getDiscoveryAgent();
            synchronized (lock) {
                devices.removeAllElements();
                inquiryDone = false;
            }
            // Include already paired devices even when Android is not discoverable.
            RemoteDevice[] paired = agent.retrieveDevices(DiscoveryAgent.PREKNOWN);
            if (paired != null) {
                for (int p = 0; p < paired.length; p++) {
                    synchronized (lock) {
                        if (!devices.contains(paired[p])) devices.addElement(paired[p]);
                    }
                }
            }
            boolean started = agent.startInquiry(DiscoveryAgent.GIAC, this);
            if (!started) throw new IOException("Bluetooth inquiry did not start");
            synchronized (lock) {
                if (!inquiryDone) lock.wait(25000L);
            }
            try { agent.cancelInquiry(this); } catch (Exception ignored) { }

            int count = devices.size();
            if (count == 0) throw new IOException("No Bluetooth devices found. Pair Android first.");

            UUID[] uuids = new UUID[] { new UUID(NokiaBTController.SERVICE_UUID, false) };
            for (int i = 0; i < count; i++) {
                RemoteDevice device = (RemoteDevice) devices.elementAt(i);
                String name = device.getFriendlyName(false);
                if (name == null || name.length() == 0) name = device.getBluetoothAddress();
                app.setStatus("Checking " + name + "...");
                synchronized (lock) {
                    serviceDone = false;
                    serviceUrl = null;
                }
                activeSearch = agent.searchServices(new int[] { 0x0001, 0x0004 }, uuids, device, this);
                synchronized (lock) {
                    if (!serviceDone) lock.wait(12000L);
                }
                if (serviceUrl != null) {
                    app.setStatus("Connecting to " + name + "...");
                    connection = (StreamConnection) Connector.open(serviceUrl, Connector.WRITE, true);
                    output = connection.openDataOutputStream();
                    connected = true;
                    app.setStatus("Connected: " + name);
                    send(NokiaBTController.PKT_MODE, NokiaBTController.MODE_REMOTE, 0);
                    connecting = false;
                    return;
                }
            }
            throw new IOException("Receiver not found. Start Android receiver and pair devices.");
        } catch (Exception e) {
            app.setStatus("Connection failed: " + safeMessage(e));
            closeConnection();
        } finally {
            connecting = false;
        }
    }

    public void deviceDiscovered(RemoteDevice remoteDevice, DeviceClass deviceClass) {
        synchronized (lock) {
            if (!devices.contains(remoteDevice)) devices.addElement(remoteDevice);
        }
    }

    public void inquiryCompleted(int discType) {
        synchronized (lock) {
            inquiryDone = true;
            lock.notifyAll();
        }
    }

    public void servicesDiscovered(int transID, ServiceRecord[] records) {
        if (transID != activeSearch || records == null) return;
        for (int i = 0; i < records.length; i++) {
            String url = records[i].getConnectionURL(ServiceRecord.AUTHENTICATE_ENCRYPT, false);
            if (url != null) {
                synchronized (lock) { serviceUrl = url; }
                break;
            }
        }
    }

    public void serviceSearchCompleted(int transID, int responseCode) {
        if (transID != activeSearch) return;
        synchronized (lock) {
            serviceDone = true;
            lock.notifyAll();
        }
    }

    void send(byte type, int code, int value) {
        DataOutputStream stream = output;
        if (!connected || stream == null) return;
        synchronized (this) {
            try {
                stream.writeByte(0x4e); // N
                stream.writeByte(0x52); // R
                stream.writeByte(NokiaBTController.PROTOCOL_VERSION);
                stream.writeByte(type);
                stream.writeShort(code);
                stream.writeShort(value);
                stream.flush();
            } catch (IOException e) {
                app.setStatus("Bluetooth link lost");
                closeConnection();
            }
        }
    }

    void disconnect() {
        send(NokiaBTController.PKT_RESET, 0, 0);
        closeConnection();
        try { if (agent != null) agent.cancelInquiry(this); } catch (Exception ignored) { }
    }

    private synchronized void closeConnection() {
        connected = false;
        try { if (output != null) output.close(); } catch (Exception ignored) { }
        try { if (connection != null) connection.close(); } catch (Exception ignored) { }
        output = null;
        connection = null;
    }

    private String safeMessage(Exception e) {
        String message = e.getMessage();
        if (message == null || message.length() == 0) return e.toString();
        return message;
    }
}
