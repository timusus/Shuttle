package com.simplecity.amp_library.utils;

import android.app.Activity;

public class AnalyticsManager {

    private static final String TAG = "AnalyticsManager";

    private static boolean analyticsEnabled() {
        return false;
    }

    public @interface UpgradeType {
        String NAG = "Nag";
        String FOLDER = "Folder";
        String UPGRADE = "Upgrade";
    }

    public static void logChangelogViewed() {
    }

    public static void logUpgrade(@UpgradeType String upgradeType) {

    }

    public static void logScreenName(Activity activity, String name) {

    }

    public static void setIsUpgraded() {

    }

    public static void logInitialTheme(ThemeUtils.Theme theme) {

    }

    public static void logRateShown() {

    }

    public static void logRateClicked() {

    }

    public static void didSnow() {

    }

    public static void dropBreadcrumb(String tag, String breadCrumb) {

    }
}