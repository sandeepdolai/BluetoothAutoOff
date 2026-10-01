package com.example.bluetoothautooff;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class TimerReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "bluetooth_timer";

    @Override
    public void onReceive(Context context, Intent intent) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        boolean requested = false;

        if (adapter != null && adapter.isEnabled()) {
            requested = adapter.disable();
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
                .setContentText(
                        requested
                                ? "Bluetooth switched off."
                                : "Bluetooth was already off or Android rejected the request."
                )
                .setAutoCancel(true);

        if (Build.VERSION.SDK_INT >= 33
                && context.checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return;
        }

        nm.notify(42, builder.build());
    }
}
