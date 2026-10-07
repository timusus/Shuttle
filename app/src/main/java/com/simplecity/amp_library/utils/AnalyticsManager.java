package com.simplecity.amp_library.utils;

import android.util.Log;

public class AnalyticsManager {

    public static void dropBreadcrumb(String tag, String breadCrumb) {
        Log.i(tag, breadCrumb);
    }
}
