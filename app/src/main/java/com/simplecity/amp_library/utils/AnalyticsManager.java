package com.simplecity.amp_library.utils;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
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
    }

    public void logUpgrade(@UpgradeType String upgradeType) {
    }

    public void logScreenName(Activity activity, String name) {
    }

    public void setIsUpgraded(boolean isUpgraded) {
    }

    public void logInitialTheme(ThemeUtils.Theme theme) {
    }

    public void logRateShown() {
    }

    public void logRateClicked() {
    }

    public void didSnow() {
    }

    public void dropBreadcrumb(String tag, String breadCrumb) {

        Log.d(tag, breadCrumb);

        if (!analyticsEnabled()) {
            return;
        }
    }
}