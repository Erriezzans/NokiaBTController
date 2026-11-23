package com.erriezzans.nokiabt;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.UUID;

public final class BluetoothReceiverService extends Service {
    public static final String ACTION_START = "com.erriezzans.nokiabt.START";
    public static final String ACTION_STOP = "com.erriezzans.nokiabt.STOP";
    public static final String ACTION_STATUS = "com.erriezzans.nokiabt.STATUS";
    public static final String EXTRA_STATUS = "status";

    private static final String TAG = "NokiaBT";
    private static final String CHANNEL_ID = "nokia_bt_receiver";
    private static final UUID SERVICE_UUID = UUID.fromString("e1f55c5a-3a62-4c93-9c5c-9d8a5fce0016");
    private static final String SERVICE_NAME = "Nokia BT Controller";

    private volatile boolean running;
    private BluetoothServerSocket serverSocket;
    private BluetoothSocket clientSocket;
    private Thread acceptThread;
    private NativeInputBridge inputBridge;

    @Override public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        inputBridge = new NativeInputBridge();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_START : intent.getAction();
        if (ACTION_STOP.equals(action)) {
            stopReceiver();
            stopSelf();
            return START_NOT_STICKY;
        }
        Notification notification = buildNotification("Waiting for Nokia C5-00");
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(11, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE);
        } else {
            startForeground(11, notification);
        }
        if (!running) startServer();
        return START_STICKY;
    }

    private void startServer() {
        running = true;
        acceptThread = new Thread(this::acceptLoop, "nokia-bt-accept");
        acceptThread.start();
    }

    private void acceptLoop() {
        try {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter == null || !adapter.isEnabled()) {
                publish("Bluetooth is off. Turn it on in Android settings.");
                running = false;
                stopSelf();
                return;
            }
            serverSocket = adapter.listenUsingRfcommWithServiceRecord(SERVICE_NAME, SERVICE_UUID);
            publish("Listening for Nokia C5-00. Pair the devices first.");
            while (running) {
                BluetoothSocket socket = serverSocket.accept();
                if (!running) {
                    socket.close();
                    break;
                }
                closeClient();
                clientSocket = socket;
                publish("Nokia connected: " + socket.getRemoteDevice().getName());
                handleClient(socket);
                closeClient();
                if (running) publish("Disconnected. Waiting for Nokia...");
            }
        } catch (SecurityException e) {
            publish("Bluetooth permission missing. Reopen the app and grant Nearby devices.");
            Log.e(TAG, "Bluetooth permission", e);
            running = false;
            stopSelf();
        } catch (IOException e) {
            if (running) {
                publish("Bluetooth receiver error: " + e.getMessage());
                Log.e(TAG, "accept loop", e);
                running = false;
                stopSelf();
            }
        } finally {
            closeClient();
            closeServer();
        }
    }

    private void handleClient(BluetoothSocket socket) {
        try {
            inputBridge.start(this);
            publish("Connected. Starting virtual keyboard, mouse and gamepad...");
            DataInputStream input = new DataInputStream(socket.getInputStream());
            while (running && !socket.isClosed()) {
                Protocol.Frame frame = Protocol.read(input);
                if (frame == null) break;
                if (frame.type == Protocol.MODE) {
                    String mode = frame.code == Protocol.MODE_GAMEPAD ? "Gamepad"
                            : frame.code == Protocol.MODE_MOUSE ? "Remote + Mouse" : "TV Remote";
                    publish("Connected · " + mode);
                }
                if (inputBridge != null) inputBridge.send(frame);
            }
        } catch (Exception e) {
            if (running) {
                Log.w(TAG, "client ended", e);
                publish("Native input unavailable or Bluetooth link ended: " + e.getMessage());
            }
        } finally {
            try {
                if (inputBridge != null) inputBridge.send(new Protocol.Frame(Protocol.RESET, 0, 0));
            } catch (Exception ignored) { }
        }
    }

    private void closeClient() {
        try { if (clientSocket != null) clientSocket.close(); } catch (Exception ignored) { }
        clientSocket = null;
    }

    private void closeServer() {
        try { if (serverSocket != null) serverSocket.close(); } catch (Exception ignored) { }
        serverSocket = null;
    }

    private void stopReceiver() {
        running = false;
        closeClient();
        closeServer();
        if (inputBridge != null) inputBridge.stop();
    }

    private void publish(String status) {
        Intent intent = new Intent(ACTION_STATUS);
        intent.setPackage(getPackageName());
        intent.putExtra(EXTRA_STATUS, status);
        sendBroadcast(intent);
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) manager.notify(11, buildNotification(status));
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Nokia Bluetooth receiver", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Bluetooth remote and gamepad connection status");
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private Notification buildNotification(String text) {
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);
        builder.setContentTitle("Nokia BT Receiver")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setOngoing(true);
        return builder.build();
    }

    @Override public void onDestroy() {
        stopReceiver();
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
