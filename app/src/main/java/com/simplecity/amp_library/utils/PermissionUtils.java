package com.simplecity.amp_library.utils;

import android.Manifest;

import com.greysonparrelli.permiso.Permiso;

public class PermissionUtils {

    private PermissionUtils() {

    }

    public interface PermissionCallback {
        void onSuccess();
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
        simplePermissionRequest(callback, Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

}
