package com.simplecity.amp_library.utils;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import com.simplecity.amp_library.BuildConfig;
import com.simplecity.amp_library.ShuttleApplication;

public class AnalyticsManager {

    private static final String TAG = "AnalyticsManager";

    private static boolean analyticsEnabled() {
        return !BuildConfig.DEBUG;
    }

    public @interface UpgradeType {
        String NAG = "Nag";
        String FOLDER = "Folder";
        String UPGRADE = "Upgrade";
    }

    public static void logChangelogViewed() {
        if (!analyticsEnabled()) {
            return;
        }
    }

    public static void logUpgrade(@UpgradeType String upgradeType) {
        if (!analyticsEnabled()) {
            return;
        }
    }

    public static void logScreenName(Activity activity, String name) {
        if (!analyticsEnabled()) {
            return;
        }
    }

    public static void setIsUpgraded() {
        if (!analyticsEnabled()) {
            return;
        }
    }

    public static void logInitialTheme(ThemeUtils.Theme theme) {
        if (!analyticsEnabled()) {
            return;
        }
    }

    public static void logRateShown() {
        if (!analyticsEnabled()) {
            return;
        }
    }

    public static void logRateClicked() {
        if (!analyticsEnabled()) {
            return;
        }
    }

    public static void didSnow() {
        if (!analyticsEnabled()) {
            return;
        }
    }

    public static void dropBreadcrumb(String tag, String breadCrumb) {

        Log.i(tag, breadCrumb);

        if (!analyticsEnabled()) {
            return;
        }

        Log.e(TAG, String.format("%s | %s", tag, breadCrumb));
    }
}