package com.erriezzans.nokiabt;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public final class MainActivity extends Activity {
    private static final int REQUEST_PERMISSIONS = 41;
    private static final int REQUEST_ADVERTISE_PERMISSION = 42;
    private static final int REQUEST_DISCOVERABLE = 43;

    private TextView status;
    private TextView discoverabilityStatus;
    private boolean receiverRegistered;

    private final BroadcastReceiver statusReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (BluetoothReceiverService.ACTION_STATUS.equals(intent.getAction())) {
                status.setText(intent.getStringExtra(BluetoothReceiverService.EXTRA_STATUS));
            }
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
    }

    @Override protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(BluetoothReceiverService.ACTION_STATUS);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(statusReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(statusReceiver, filter);
        receiverRegistered = true;
    }

    @Override protected void onPause() {
        if (receiverRegistered) {
            unregisterReceiver(statusReceiver);
            receiverRegistered = false;
        }
        super.onPause();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(28), dp(22), dp(24));
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("Nokia BT Receiver");
        title.setTextSize(25);
        title.setTextColor(0xff18243a);
        title.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(title, matchWrap());

        TextView subtitle = new TextView(this);
        subtitle.setText("Android TV remote · Mouse · Native gamepad");
        subtitle.setTextSize(14);
        subtitle.setTextColor(0xff53647b);
        subtitle.setPadding(0, dp(6), 0, dp(18));
        root.addView(subtitle, matchWrap());

        status = new TextView(this);
        status.setText("Receiver stopped");
        status.setTextSize(16);
        status.setTextColor(0xff173b2a);
        status.setBackgroundColor(0xffe7f4ec);
        status.setPadding(dp(14), dp(15), dp(14), dp(15));
        root.addView(status, matchWrap());

        Button start = new Button(this);
        start.setText("Start Bluetooth receiver");
        start.setOnClickListener(v -> ensurePermissionsAndStart());
        root.addView(start, matchWrapWithTop());

        Button discoverable = new Button(this);
        discoverable.setText("Make discoverable (5 minutes)");
        discoverable.setOnClickListener(v -> ensurePermissionAndDiscoverable());
        root.addView(discoverable, matchWrap());

        discoverabilityStatus = new TextView(this);
        discoverabilityStatus.setText("Bluetooth visibility is off. Enable this when the Nokia cannot find this Android device.");
        discoverabilityStatus.setTextSize(13);
        discoverabilityStatus.setTextColor(0xff53647b);
        discoverabilityStatus.setPadding(dp(4), dp(2), dp(4), dp(12));
        root.addView(discoverabilityStatus, matchWrap());

        Button stop = new Button(this);
        stop.setText("Stop receiver");
        stop.setOnClickListener(v -> {
            Intent intent = new Intent(this, BluetoothReceiverService.class);
            intent.setAction(BluetoothReceiverService.ACTION_STOP);
            startService(intent);
            status.setText("Receiver stopped");
        });
        root.addView(stop, matchWrap());

        TextView info = new TextView(this);
        info.setText(
                "SETUP\n" +
                "1. Enable Bluetooth on Android.\n" +
                "2. Pair the Nokia C5-00 with this Android device in Bluetooth settings.\n" +
                "3. Grant Nearby devices permission when requested.\n" +
                "4. Tap Start Bluetooth receiver and leave it running.\n" +
                "5. If Nokia search shows no devices, tap Make discoverable and allow the system prompt.\n" +
                "6. On Nokia, open Find devices, select this Android device, then press Connect.\n\n" +
                "ROOT REQUIRED FOR NATIVE INPUT\n" +
                "The receiver asks su to create virtual keyboard, mouse and gamepad devices using /dev/uinput. Approve the root prompt. The Android kernel must include uinput and SELinux must allow the root process to access it.\n\n" +
                "GAMEPAD\n" +
                "The Nokia D-pad acts as D-pad and left stick. Keys 1–4 are Square, Triangle, Circle and Cross; 7/9 are L1/R1; */# are L2/R2; 5 is Select; 0 is Start.\n\n" +
                "MOUSE\n" +
                "In Remote mode, press * or use Options → Mouse ON/OFF. D-pad moves the pointer; center clicks.\n\n" +
                "Discoverability is temporary (up to five minutes) and controlled by Android's system dialog. A paired device can still connect when visibility is off.\n\n" +
                "This receiver does not emulate a Bluetooth HID device for a separate host. It creates Android's local virtual input devices.");
        info.setTextSize(14);
        info.setTextColor(0xff33445c);
        info.setPadding(0, dp(22), 0, 0);
        root.addView(info, matchWrap());

        setContentView(scroll);
    }

    private void ensurePermissionsAndStart() {
        if (Build.VERSION.SDK_INT >= 31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] { Manifest.permission.BLUETOOTH_CONNECT }, REQUEST_PERMISSIONS);
            return;
        }
        startReceiver();
    }

    private void ensurePermissionAndDiscoverable() {
        if (Build.VERSION.SDK_INT >= 31 && checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] { Manifest.permission.BLUETOOTH_ADVERTISE },
                    REQUEST_ADVERTISE_PERMISSION);
            return;
        }
        requestDiscoverableMode();
    }

    private void requestDiscoverableMode() {
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        try {
            startActivityForResult(intent, REQUEST_DISCOVERABLE);
        } catch (Exception e) {
            discoverabilityStatus.setText("Could not open Android's Bluetooth visibility prompt: " + e.getMessage());
        }
    }

    private void startReceiver() {
        Intent intent = new Intent(this, BluetoothReceiverService.class);
        intent.setAction(BluetoothReceiverService.ACTION_START);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent);
        else startService(intent);
        status.setText("Starting Bluetooth receiver...");
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) startReceiver();
            else status.setText("Nearby devices permission is required.");
        } else if (requestCode == REQUEST_ADVERTISE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestDiscoverableMode();
            } else {
                discoverabilityStatus.setText("Nearby devices permission was denied. Allow Bluetooth advertising to make this phone visible.");
            }
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_DISCOVERABLE) {
            if (resultCode != RESULT_CANCELED && resultCode > 0) {
                discoverabilityStatus.setText("This Android device is discoverable to nearby Bluetooth devices for up to "
                        + resultCode + " seconds. Start Scan again on the Nokia.");
            } else {
                discoverabilityStatus.setText("Visibility was not enabled. Pair the Nokia in Android Bluetooth settings instead.");
            }
        }
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams matchWrapWithTop() {
        LinearLayout.LayoutParams p = matchWrap();
        p.topMargin = dp(18);
        return p;
    }

    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }
}
