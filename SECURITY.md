# Security

## No internet access

Pascaline has no internet permission. Every calculation, unit conversion and history entry stays
on your device, and the app cannot send anything anywhere.

## Permissions

### `android.permission.VIBRATE`
Haptic feedback when you press a key. It only works when **Touch feedback** is enabled in your
device settings.

### `android.permission.WAKE_LOCK`, `android.permission.RECEIVE_BOOT_COMPLETED`, `android.permission.FOREGROUND_SERVICE`
Not used by Pascaline itself. Android's home screen widget library adds them automatically.

### `io.github.calculatorpascaline.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`
Added automatically by Android since version 14. It stops other apps from sending events to
Pascaline's internal receivers. See
[the Android 14 behavior changes](https://developer.android.com/about/versions/14/behavior-changes-14#runtime-receivers-exported).

## Reporting a vulnerability

Please report security issues privately through
[GitHub's private vulnerability reporting](https://github.com/Calculator-Pascaline/Pascaline/security/advisories/new),
not in a public issue.
