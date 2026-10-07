package com.simplecity.amp_library.utils;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.preference.PreferenceManager;
import androidx.core.content.ContextCompat;
import com.greysonparrelli.permiso.Permiso;

public class PermissionUtils {

    private static final String KEY_NOTIFICATION_PERMISSION_REQUESTED = "notification_permission_requested";

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
        prefs.edit().putBoolean(KEY_NOTIFICATION_PERMISSION_REQUESTED, true).apply();
        Permiso.getInstance().requestPermissions(new Permiso.IOnPermissionResult() {
            @Override
            public void onPermissionResult(Permiso.ResultSet resultSet) {
                // Nothing to do either way
            }

            @Override
            public void onRationaleRequested(Permiso.IOnRationaleProvided callback, String... permissions) {
                callback.onRationaleProvided();
            }
        }, Manifest.permission.POST_NOTIFICATIONS);
    }
}
