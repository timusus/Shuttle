package com.simplecity.amp_library.utils;

import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.simplecity.amp_library.BuildConfig;

public class LogUtils {

    private LogUtils() {
        //no instance
    }

    public static void logException(String message, Throwable throwable) {
        Crashlytics.log(Log.ERROR, "Shuttle Error", message + " Throwable: " + throwable.getMessage());
        if (BuildConfig.DEBUG) {
            throwable.printStackTrace();
        }
    }
}