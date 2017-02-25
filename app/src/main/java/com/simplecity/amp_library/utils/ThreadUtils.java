package com.simplecity.amp_library.utils;

import android.os.Looper;
import android.support.v4.preferencefragment.BuildConfig;

import com.crashlytics.android.core.CrashlyticsCore;

public class ThreadUtils {

    private ThreadUtils() {

    }

    public static void ensureNotOnMainThread() {
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            if (BuildConfig.DEBUG) {
                throw new IllegalStateException("ensureNotOnMainThread failed.");
            } else {
                CrashlyticsCore.getInstance().log("ThreadUtils ensureNotOnMainThread() failed");
            }
        }
    }

}
