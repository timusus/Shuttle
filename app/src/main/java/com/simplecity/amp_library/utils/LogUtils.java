package com.simplecity.amp_library.utils;

import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.simplecity.amp_library.BuildConfig;

public class LogUtils {

    private LogUtils() {
        //no instance
    }

    public static void logException(String tag, String message, Throwable throwable) {
        if (BuildConfig.DEBUG) {
            Log.e(tag, message + "\nThrowable: " + throwable.getMessage());
            throwable.printStackTrace();
        } else {
            Crashlytics.log(Log.ERROR, tag, message + "\nThrowable: " + throwable.getMessage());
        }
    }

    public static void logError(String tag, String message) {
        Crashlytics.log(Log.ERROR, tag, message);
    }
}