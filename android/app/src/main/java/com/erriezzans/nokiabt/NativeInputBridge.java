package com.erriezzans.nokiabt;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Starts the native /dev/uinput worker through su and streams protocol frames to it. */
final class NativeInputBridge {
    private static final String TAG = "NokiaBT";
    private java.lang.Process rootProcess;
    private OutputStream rootStdin;
    private volatile String lastOutput = "";
    private volatile boolean helperFailed;
    private CountDownLatch readyLatch;

    synchronized void start(Context context) throws Exception {
        if (rootProcess != null) {
            try {
                rootProcess.exitValue();
                rootProcess = null;
                rootStdin = null;
            } catch (IllegalThreadStateException stillRunning) {
                return;
            }
        }
        String abi = chooseAbi();
        if (abi == null) throw new IllegalStateException("Unsupported Android CPU ABI");

        File helper = new File(context.getFilesDir(), "nrgpad-" + abi);
        copyAsset(context, "bin/" + abi + "/nrgpad", helper);

        helperFailed = false;
        lastOutput = "";
        readyLatch = new CountDownLatch(1);
        java.lang.Process process = Runtime.getRuntime().exec("su");
        OutputStream shellIn = process.getOutputStream();
        String target = "/data/local/tmp/nrgpad_" + android.os.Process.myUid();
        String command = "cp '" + helper.getAbsolutePath() + "' '" + target + "' && "
                + "chmod 755 '" + target + "' && exec '" + target + "' --stdio\n";
        shellIn.write(command.getBytes(StandardCharsets.UTF_8));
        shellIn.flush();
        rootProcess = process;
        rootStdin = shellIn;

        Thread stdout = new Thread(() -> drain(process.getInputStream(), "stdout"), "nrgpad-stdout");
        Thread stderr = new Thread(() -> drain(process.getErrorStream(), "stderr"), "nrgpad-stderr");
        stdout.setDaemon(true);
        stderr.setDaemon(true);
        stdout.start();
        stderr.start();
        if (!readyLatch.await(20, TimeUnit.SECONDS) || helperFailed) {
            String reason = lastOutput;
            stop();
            throw new IOException("Could not create Android virtual input devices. "
                    + (reason == null || reason.length() == 0 ? "Check root permission and /dev/uinput." : reason));
        }
    }

    private String chooseAbi() {
        for (String supported : Build.SUPPORTED_ABIS) {
            if (supported.equals("arm64-v8a") || supported.equals("armeabi-v7a") || supported.equals("x86_64")) {
                return supported;
            }
        }
        return null;
    }

    private void copyAsset(Context context, String name, File target) throws Exception {
        try (InputStream in = context.getAssets().open(name);
             FileOutputStream out = new FileOutputStream(target)) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = in.read(buffer)) >= 0) out.write(buffer, 0, count);
            out.flush();
        }
        if (!target.setReadable(true, true)) {
            Log.w(TAG, "Could not set helper file read permission; root will copy it before execution");
        }
    }

    private void drain(InputStream stream, String source) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lastOutput = line;
                Log.i(TAG, "uinput " + source + ": " + line);
                if ("stdout".equals(source) && line.startsWith("READY:")) {
                    CountDownLatch latch = readyLatch;
                    if (latch != null) latch.countDown();
                } else if ("stderr".equals(source)) {
                    helperFailed = true;
                    CountDownLatch latch = readyLatch;
                    if (latch != null) latch.countDown();
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "uinput " + source + " closed", e);
        }
    }

    synchronized void send(Protocol.Frame frame) throws Exception {
        if (rootStdin == null) throw new IllegalStateException("Root input helper is not running");
        rootStdin.write(Protocol.encode(frame));
        rootStdin.flush();
    }

    String getLastOutput() { return lastOutput; }

    synchronized void stop() {
        java.lang.Process process = rootProcess;
        try {
            if (rootStdin != null) rootStdin.close();
        } catch (Exception ignored) { }
        try {
            if (process != null) {
                Thread.sleep(300);
                process.destroy();
            }
        } catch (Exception ignored) { }
        rootStdin = null;
        rootProcess = null;
        readyLatch = null;
    }
}
