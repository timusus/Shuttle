package com.simplecity.amp_library.utils;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.preference.PreferenceManager;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import com.greysonparrelli.permiso.Permiso;

public class PermissionUtils {

    private static final String KEY_NOTIFICATION_PERMISSION_REQUESTED = "notification_permission_requested";

    private static boolean notificationRequestPending = false;
    private static boolean bluetoothRequestPending = false;
    private static boolean imagesRequestPending = false;

    private PermissionUtils() {

    }

    public interface PermissionCallback {
        void onSuccess();
    }

    /**
     * The runtime permission needed to read the music library on this device: READ_MEDIA_AUDIO from API 33,
     * READ_EXTERNAL_STORAGE on 30-32, and WRITE_EXTERNAL_STORAGE (which includes read) up to 29, where tag edits and
     * deletes still write to the file directly.
     */
    public static String storagePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return Manifest.permission.READ_MEDIA_AUDIO;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Manifest.permission.READ_EXTERNAL_STORAGE;
        } else {
            return Manifest.permission.WRITE_EXTERNAL_STORAGE;
        }
    }

    public static boolean hasStoragePermission(Context context) {
        return ContextCompat.checkSelfPermission(context, storagePermission()) == PackageManager.PERMISSION_GRANTED;
    }

    private static void simplePermissionRequest(final PermissionCallback callback, String... permissions) {
        Permiso.getInstance().requestPermissions(new Permiso.IOnPermissionResult() {
            @Override
            public void onPermissionResult(Permiso.ResultSet resultSet) {
                if (resultSet.areAllPermissionsGranted()) {
                    callback.onSuccess();
                }
            }

            @Override
            public void onRationaleRequested(Permiso.IOnRationaleProvided callback, String... permissions) {
                callback.onRationaleProvided();
            }
        }, permissions);
    }

    public static void RequestStoragePermissions(final PermissionCallback callback) {
        simplePermissionRequest(callback, storagePermission());
    }

    /**
     * BLUETOOTH_CONNECT is a runtime permission from API 31, and without it the A2DP/headset connection broadcasts
     * behind "resume on Bluetooth connect" are not delivered.
     */
    public static boolean needsBluetoothConnectPermission(Context context) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED;
    }

    public static void requestBluetoothConnect(Runnable onGranted, Runnable onDenied) {
        Permiso.getInstance().requestPermissions(new Permiso.IOnPermissionResult() {
            @Override
            public void onPermissionResult(Permiso.ResultSet resultSet) {
                if (resultSet.areAllPermissionsGranted()) {
                    onGranted.run();
                } else {
                    onDenied.run();
                }
            }

            @Override
            public void onRationaleRequested(Permiso.IOnRationaleProvided callback, String... permissions) {
                callback.onRationaleProvided();
            }
        }, Manifest.permission.BLUETOOTH_CONNECT);
    }

    /**
     * For users who enabled "resume on Bluetooth connect" before it needed BLUETOOTH_CONNECT: asks once, and switches
     * the setting off if refused. Waits for the storage and notification prompts so they never stack.
     */
    public static void requestBluetoothConnectForExistingUsersOnce(Context context) {
        SettingsManager settings = SettingsManager.getInstance();
        if (!needsBluetoothConnectPermission(context)
                || !settings.getBluetoothResumeConnect()
                || settings.getBluetoothConnectPermissionAsked()
                || bluetoothRequestPending
                || !hasStoragePermission(context)) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && !PreferenceManager.getDefaultSharedPreferences(context).getBoolean(KEY_NOTIFICATION_PERMISSION_REQUESTED, false)
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        bluetoothRequestPending = true;
        requestBluetoothConnect(() -> {
            bluetoothRequestPending = false;
            settings.setBluetoothConnectPermissionAsked();
        }, () -> {
            bluetoothRequestPending = false;
            settings.setBluetoothConnectPermissionAsked();
            settings.setBluetoothResumeConnect(false);
        });
    }

    /**
     * Asks once for READ_MEDIA_IMAGES (API 33+), which folder artwork needs to read folder.jpg and friends. Optional:
     * the library loads without it. Waits for the storage, notification and Bluetooth prompts so they never stack.
     */
    public static void requestImagesPermissionOnce(Context context) {
        SettingsManager settings = SettingsManager.getInstance();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || settings.ignoreFolderArtwork()
                || settings.getImagesPermissionAsked()
                || imagesRequestPending
                || notificationRequestPending
                || bluetoothRequestPending
                || !hasStoragePermission(context)
                || ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean(KEY_NOTIFICATION_PERMISSION_REQUESTED, false)
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (needsBluetoothConnectPermission(context)
                && settings.getBluetoothResumeConnect()
                && !settings.getBluetoothConnectPermissionAsked()) {
            return;
        }
        imagesRequestPending = true;
        Context appContext = context.getApplicationContext();
        Permiso.getInstance().requestPermissions(new Permiso.IOnPermissionResult() {
            @Override
            public void onPermissionResult(Permiso.ResultSet resultSet) {
                imagesRequestPending = false;
                settings.setImagesPermissionAsked();
                if (resultSet.areAllPermissionsGranted()) {
                    // Drops the placeholders cached while the folder artwork was unreadable
                    Glide.get(appContext).clearMemory();
                }
            }

            @Override
            public void onRationaleRequested(Permiso.IOnRationaleProvided callback, String... permissions) {
                callback.onRationaleProvided();
            }
        }, Manifest.permission.READ_MEDIA_IMAGES);
    }

    /**
     * Asks for POST_NOTIFICATIONS (API 33+) the first time only. Playback works without it; the media notification is
     * just not shown.
     */
    public static void requestNotificationPermissionOnce(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefs.getBoolean(KEY_NOTIFICATION_PERMISSION_REQUESTED, false)
                || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (notificationRequestPending) {
            return;
        }
        notificationRequestPending = true;
        Permiso.getInstance().requestPermissions(new Permiso.IOnPermissionResult() {
            @Override
            public void onPermissionResult(Permiso.ResultSet resultSet) {
                // Answered (granted or denied): don't ask again
                notificationRequestPending = false;
                prefs.edit().putBoolean(KEY_NOTIFICATION_PERMISSION_REQUESTED, true).apply();
            }

            @Override
            public void onRationaleRequested(Permiso.IOnRationaleProvided callback, String... permissions) {
                callback.onRationaleProvided();
            }
        }, Manifest.permission.POST_NOTIFICATIONS);
    }
}
