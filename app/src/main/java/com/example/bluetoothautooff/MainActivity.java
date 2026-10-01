package com.example.bluetoothautooff;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends Activity {
    private static final int REQUEST_BLUETOOTH_PERMISSION = 1001;

    private EditText minutes;
    private TextView status;
    private TextView bluetoothStatus;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            updateScreen();
            handler.postDelayed(this, 500);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(createUi());
        requestRequiredPermissions();
        updateScreen();
        handler.post(ticker);
    }

    private TextView text(String value, float size) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setPadding(16, 16, 16, 16);
        return view;
    }

    private View createUi() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(32, 48, 32, 32);

        TextView title = text("Bluetooth Auto-Off", 28);
        box.addView(title);

        bluetoothStatus = text("", 18);
        box.addView(bluetoothStatus);

        minutes = new EditText(this);
        minutes.setInputType(InputType.TYPE_CLASS_NUMBER);
        minutes.setHint("Minutes");
        minutes.setText("30");
        box.addView(minutes);

        Button start = new Button(this);
        start.setText("START TIMER");
        start.setOnClickListener(v -> startTimer());
        box.addView(start);

        Button cancel = new Button(this);
        cancel.setText("CANCEL TIMER");
        cancel.setOnClickListener(v -> cancelTimer());
        box.addView(cancel);

        status = text("", 18);
        box.addView(status);

        TextView note = text(
                "Requires Nearby devices / Bluetooth permission. Exact alarms are also required for the timer.",
                13
        );
        box.addView(note);

        return box;
    }

    private void requestRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= 31
                && checkSelfPermission("android.permission.BLUETOOTH_CONNECT")
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{"android.permission.BLUETOOTH_CONNECT"},
                    REQUEST_BLUETOOTH_PERMISSION
            );
            return;
        }

        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{"android.permission.POST_NOTIFICATIONS"},
                    REQUEST_BLUETOOTH_PERMISSION + 1
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_BLUETOOTH_PERMISSION) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(
                        this,
                        "Bluetooth permission granted",
                        Toast.LENGTH_SHORT
                ).show();

                if (Build.VERSION.SDK_INT >= 33
                        && checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                        != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(
                            new String[]{"android.permission.POST_NOTIFICATIONS"},
                            REQUEST_BLUETOOTH_PERMISSION + 1
                    );
                }
            } else {
                Toast.makeText(
                        this,
                        "Bluetooth permission is required for the timer",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    private boolean hasBluetoothPermission() {
        return Build.VERSION.SDK_INT < 31
                || checkSelfPermission("android.permission.BLUETOOTH_CONNECT")
                == PackageManager.PERMISSION_GRANTED;
    }

    private void startTimer() {
        if (!hasBluetoothPermission()) {
            requestRequiredPermissions();
            Toast.makeText(
                    this,
                    "Allow Nearby devices / Bluetooth permission first",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        long mins;

        try {
            mins = Long.parseLong(minutes.getText().toString().trim());
        } catch (Exception e) {
            Toast.makeText(this, "Enter minutes", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mins < 1) {
            Toast.makeText(this, "Minimum 1 minute", Toast.LENGTH_SHORT).show();
            return;
        }

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        if (Build.VERSION.SDK_INT >= 31 && !alarmManager.canScheduleExactAlarms()) {
            startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM));
            Toast.makeText(
                    this,
                    "Allow exact alarms, then press START again",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        long end = System.currentTimeMillis() + mins * 60_000L;

        getPreferences(MODE_PRIVATE)
                .edit()
                .putLong("end", end)
                .apply();

        PendingIntent pendingIntent = getTimerPendingIntent();
        alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                end,
                pendingIntent
        );

        Toast.makeText(
                this,
                "Bluetooth will turn off when the timer ends",
                Toast.LENGTH_SHORT
        ).show();

        updateScreen();
    }

    private void cancelTimer() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.cancel(getTimerPendingIntent());

        getPreferences(MODE_PRIVATE)
                .edit()
                .remove("end")
                .apply();

        updateScreen();
    }

    private PendingIntent getTimerPendingIntent() {
        Intent intent = new Intent(this, TimerReceiver.class);

        return PendingIntent.getBroadcast(
                this,
                7,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private BluetoothAdapter getBluetoothAdapter() {
        BluetoothManager manager = getSystemService(BluetoothManager.class);
        return manager != null ? manager.getAdapter() : null;
    }

    private void updateScreen() {
        if (bluetoothStatus != null) {
            BluetoothAdapter adapter = getBluetoothAdapter();
            boolean enabled = false;

            if (adapter != null && hasBluetoothPermission()) {
                try {
                    enabled = adapter.isEnabled();
                } catch (SecurityException ignored) {
                }
            }

            bluetoothStatus.setText("Bluetooth: " + (enabled ? "ON" : "OFF"));
        }

        if (status == null) {
            return;
        }

        long end = getPreferences(MODE_PRIVATE)
                .getLong("end", 0);

        if (end <= 0) {
            status.setText("No active timer");
            return;
        }

        long left = Math.max(0, end - System.currentTimeMillis());
        long seconds = left / 1000;

        status.setText(String.format(
                Locale.US,
                "Time remaining: %02d:%02d:%02d",
                seconds / 3600,
                (seconds / 60) % 60,
                seconds % 60
        ));

        if (left == 0) {
            getPreferences(MODE_PRIVATE)
                    .edit()
                    .remove("end")
                    .apply();
        }
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(ticker);
        super.onDestroy();
    }
}
