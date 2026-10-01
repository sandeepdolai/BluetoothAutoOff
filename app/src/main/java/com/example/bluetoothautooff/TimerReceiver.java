package com.example.bluetoothautooff;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

public class TimerReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "bluetooth_timer";

    @Override
    public void onReceive(Context context, Intent intent) {
        BluetoothManager manager = context.getSystemService(BluetoothManager.class);
        BluetoothAdapter adapter = manager != null ? manager.getAdapter() : null;

        boolean permissionDenied = false;
        boolean securityException = false;
        boolean requestStarted = false;
        boolean alreadyOff = false;
        boolean noAdapter = adapter == null;

        if (Build.VERSION.SDK_INT >= 31
                && context.checkSelfPermission("android.permission.BLUETOOTH_CONNECT")
                != PackageManager.PERMISSION_GRANTED) {
            permissionDenied = true;
        } else if (adapter != null) {
            try {
                if (adapter.isEnabled()) {
                    // For targetSdk <= 32, Android still permits this legacy API.
                    requestStarted = adapter.disable();
                } else {
                    alreadyOff = true;
                }
            } catch (SecurityException e) {
                // Keep this separate from an actual permission denial.
                securityException = true;
            }
        }

        String message;
        if (noAdapter) {
            message = "Bluetooth adapter is unavailable.";
        } else if (permissionDenied) {
            message = "Android reports BLUETOOTH_CONNECT is denied.";
        } else if (securityException) {
            message = "Android threw SecurityException while disabling Bluetooth.";
        } else if (alreadyOff) {
            message = "Bluetooth was already off.";
        } else if (requestStarted) {
            message = "Bluetooth shutdown request sent.";
        } else {
            message = "Android rejected the Bluetooth shutdown request.";
        }

        NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(new NotificationChannel(
                    CHANNEL_ID,
                    "Bluetooth timer",
                    NotificationManager.IMPORTANCE_DEFAULT
            ));
        }

        Notification.Builder builder =
                Build.VERSION.SDK_INT >= 26
                        ? new Notification.Builder(context, CHANNEL_ID)
                        : new Notification.Builder(context);

        builder.setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
                .setContentTitle("Bluetooth Auto-Off")
                .setContentText(message)
                .setAutoCancel(true);

        if (Build.VERSION.SDK_INT >= 33
                && context.checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        nm.notify(42, builder.build());
    }
}
