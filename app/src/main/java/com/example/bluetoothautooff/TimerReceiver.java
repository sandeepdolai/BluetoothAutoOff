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
        boolean noAdapter = adapter == null;
        String securityMessage = "";

        if (Build.VERSION.SDK_INT >= 31
                && context.checkSelfPermission("android.permission.BLUETOOTH_CONNECT")
                != PackageManager.PERMISSION_GRANTED) {
            permissionDenied = true;
        } else if (adapter != null) {
            /*
             * Do not call isEnabled() here.  disable() itself reports false when
             * Bluetooth is already off, and keeping the permission-sensitive call
             * in one place makes the failure unambiguous.
             */
            try {
                // targetSdk 32 intentionally keeps the legacy disable() API available.
                requestStarted = adapter.disable();
            } catch (SecurityException e) {
                securityException = true;
                securityMessage = e.getMessage() == null
                        ? "No exception details were provided."
                        : e.getMessage();
            }
        }

        String message;
        if (noAdapter) {
            message = "Bluetooth adapter is unavailable.";
        } else if (permissionDenied) {
            message = "BLUETOOTH_CONNECT is actually denied.";
        } else if (securityException) {
            message = "Bluetooth disable was blocked by Android: " + securityMessage;
        } else if (requestStarted) {
            message = "Bluetooth shutdown request sent.";
        } else {
            message = "Bluetooth was already off or Android rejected the request.";
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
