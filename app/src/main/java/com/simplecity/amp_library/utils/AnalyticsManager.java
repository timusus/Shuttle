package com.simplecity.amp_library.utils;

import android.app.Activity;
import android.os.Bundle;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.crashlytics.android.core.CrashlyticsCore;
import com.google.firebase.analytics.FirebaseAnalytics;
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
        String COLORS = "Colors";
        String UPGRADE = "Upgrade";
    }

    public interface ColorType {
        String PRIMARY = "primary";
        String ACCENT = "accent";
    }

    public static void logColorPicked(String colorType, int color) {
        if (!analyticsEnabled()) {
            return;
        }
        switch (colorType) {
            case ColorType.PRIMARY:
                Answers.getInstance().logCustom(new CustomEvent("Primary Color Changed")
                        .putCustomAttribute("Color", String.format("#%06X", (0xFFFFFF & color))));
                break;
            case ColorType.ACCENT:
                Answers.getInstance().logCustom(new CustomEvent("Accent Color Changed")
                        .putCustomAttribute("Color", String.format("#%06X", (0xFFFFFF & color))));
                break;
        }
    }

    public static void logChangelogViewed() {
        if (!analyticsEnabled()) {
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "changelog");
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "0");

        FirebaseAnalytics.getInstance(ShuttleApplication.getInstance())
                .logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
        Answers.getInstance().logCustom(new CustomEvent("Changelog Viewed"));
    }

    public static void logTabVisibilityChanged(boolean visible, String title) {
        if (!analyticsEnabled()) {
            return;
        }

        if (visible) {
            Answers.getInstance().logCustom(new CustomEvent("Tab Enabled")
                    .putCustomAttribute("Tab", title));
        } else {
            Answers.getInstance().logCustom(new CustomEvent("Tab Disabled")
                    .putCustomAttribute("Tab", title));
        }
    }

    public static void logUpgrade(@UpgradeType String upgradeType) {
        if (!analyticsEnabled()) {
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "0");
        bundle.putLong(FirebaseAnalytics.Param.QUANTITY, 0);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, upgradeType);
        bundle.putString(FirebaseAnalytics.Param.ITEM_CATEGORY, "upgrade");

        FirebaseAnalytics.getInstance(ShuttleApplication.getInstance())
                .logEvent(FirebaseAnalytics.Event.PRESENT_OFFER, bundle);
    }

    public static void logScreenName(Activity activity, String name) {
        if (!analyticsEnabled()) {
            return;
        }

        CrashlyticsCore.getInstance().log(String.format("Screen: %s", name));
        FirebaseAnalytics.getInstance(ShuttleApplication.getInstance()).setCurrentScreen(activity, name, null);
    }

    public static void setIsUpgraded() {
        if (!analyticsEnabled()) {
            return;
        }

        FirebaseAnalytics.getInstance(ShuttleApplication.getInstance()).setUserProperty("Upgraded", String.valueOf(ShuttleUtils.isUpgraded()));
    }
}
