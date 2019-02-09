package com.simplecity.amp_library.utils;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.crashlytics.android.core.CrashlyticsCore;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.simplecity.amp_library.BuildConfig;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AnalyticsManager {

    private static final String TAG = "AnalyticsManager";

    private Context context;

    @Inject
    public AnalyticsManager(Context context) {
        this.context = context;
    }

    private boolean analyticsEnabled() {
        return !BuildConfig.DEBUG;
    }

    public @interface UpgradeType {
        String NAG = "Nag";
        String FOLDER = "Folder";
        String UPGRADE = "Upgrade";
    }

    public void logChangelogViewed() {
        if (!analyticsEnabled()) {
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "changelog");
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "0");

        FirebaseAnalytics.getInstance(context)
                .logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
        Answers.getInstance().logCustom(new CustomEvent("Changelog Viewed"));
    }

    public void logUpgrade(@UpgradeType String upgradeType) {
        if (!analyticsEnabled()) {
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "0");
        bundle.putLong(FirebaseAnalytics.Param.QUANTITY, 0);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, upgradeType);
        bundle.putString(FirebaseAnalytics.Param.ITEM_CATEGORY, "upgrade");

        FirebaseAnalytics.getInstance(context)
                .logEvent(FirebaseAnalytics.Event.PRESENT_OFFER, bundle);
    }

    public void logScreenName(Activity activity, String name) {
        if (!analyticsEnabled()) {
            return;
        }

        CrashlyticsCore.getInstance().log(String.format("Screen: %s", name));
        FirebaseAnalytics.getInstance(context).setCurrentScreen(activity, name, null);
    }

    public void setIsUpgraded(boolean isUpgraded) {
        if (!analyticsEnabled()) {
            return;
        }

        FirebaseAnalytics.getInstance(context).setUserProperty("Upgraded", String.valueOf(isUpgraded));
    }

    public void logInitialTheme(ThemeUtils.Theme theme) {
        if (!analyticsEnabled()) {
            return;
        }

        Bundle params = new Bundle();
        params.putString(FirebaseAnalytics.Param.ITEM_ID, String.valueOf(theme.id));
        params.putString(FirebaseAnalytics.Param.ITEM_NAME, String.format("%s-%s-%s", theme.primaryColorName, theme.accentColorName, theme.isDark));
        params.putString(FirebaseAnalytics.Param.ITEM_CATEGORY, "themes");
        FirebaseAnalytics.getInstance(context).logEvent(FirebaseAnalytics.Event.VIEW_ITEM, params);
    }

    public void logRateShown() {
        if (!analyticsEnabled()) {
            return;
        }

        Bundle params = new Bundle();
        params.putString(FirebaseAnalytics.Param.ITEM_ID, "show_rate_snackbar");
        params.putString(FirebaseAnalytics.Param.ITEM_NAME, "show_rate_snackbar");
        params.putString(FirebaseAnalytics.Param.ITEM_CATEGORY, "rate_app");

        FirebaseAnalytics.getInstance(context)
                .logEvent(FirebaseAnalytics.Event.VIEW_ITEM, params);
    }

    public void logRateClicked() {
        if (!analyticsEnabled()) {
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "rate_snackbar");
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "0");

        FirebaseAnalytics.getInstance(context)
                .logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    public void didSnow() {
        if (!analyticsEnabled()) {
            return;
        }

        Bundle params = new Bundle();
        params.putString(FirebaseAnalytics.Param.ITEM_ID, "show_snow");
        params.putString(FirebaseAnalytics.Param.ITEM_NAME, "show_snow");
        params.putString(FirebaseAnalytics.Param.ITEM_CATEGORY, "easter_eggs");

        FirebaseAnalytics.getInstance(context)
                .logEvent(FirebaseAnalytics.Event.VIEW_ITEM, params);
    }

    public void dropBreadcrumb(String tag, String breadCrumb) {

        Log.d(tag, breadCrumb);

        if (!analyticsEnabled()) {
            return;
        }

        CrashlyticsCore.getInstance().log(String.format("%s | %s", tag, breadCrumb));
    }
}