package com.simplecity.amp_library.utils;

import android.app.Activity;
import android.content.res.TypedArray;

/**
 * Helpers for the {@link android.app.ActionBar}
 */
public final class ActionBarUtils {

    /**
     * The attribute depicting the Size of the {@link android.app.ActionBar}
     */
    private static final int[] ACTION_BAR_SIZE = new int[]{
            android.R.attr.actionBarSize
    };

    /* This class is never initialized */
    private ActionBarUtils() {
    }

    /**
     * @param activity The {@link android.app.Activity} to use
     * @return The height of the {@link android.app.ActionBar}
     */
    public static float getActionBarHeight(Activity activity) {
        final TypedArray actionBarSize = activity.obtainStyledAttributes(ACTION_BAR_SIZE);
        final int actionBarHeight = actionBarSize.getDimensionPixelSize(0, 0);
        actionBarSize.recycle();
        return actionBarHeight;
    }

    /**
     * @param activity The {@link android.app.Activity} to use
     * @return The height of the StatusBar
     */
    public static float getStatusBarHeight(Activity activity) {
        int result = 0;
        int resourceId = activity.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = activity.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

}
