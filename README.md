# Bluetooth Auto-Off

Personal Android app: set a countdown and switch Bluetooth off when the timer expires.

## Build

GitHub Actions builds a debug APK automatically on pushes to `main`.

Open **Actions → Build APK** and download the `BluetoothAutoOff-debug-apk` artifact.

## Important Android limitation

This personal sideload build targets Android API 32 because Android 13+ restricts Bluetooth enable/disable APIs for apps targeting API 33 or later.

The app is intended for personal sideloading, not Play Store distribution.

## Function

1. Enter the number of minutes.
2. Tap **START TIMER**.
3. The app schedules an exact alarm.
4. When it expires, the receiver requests Bluetooth OFF.
5. A notification reports the result.
