package com.simplecity.amp_library.utils;

import android.content.Context;
import android.content.res.TypedArray;

/**
 * Helpers for the {@link android.app.ActionBar}
 */
public final class ActionBarUtils {

    /**
     * The attribute depicting the Size of the {@link android.app.ActionBar}
     */
    private static final int[] ACTION_BAR_SIZE = new int[] {
            android.R.attr.actionBarSize
    };

    /**
     * The top system bar / display cutout inset reported by the window, or -1 if not known. Only recorded when the app
     * is forced edge-to-edge (API 35+), where it can differ from the status_bar_height resource.
     */
    private static int statusBarInset = -1;

    public static void setStatusBarInset(int inset) {
        statusBarInset = inset;
    }

    /* This class is never initialized */
    private ActionBarUtils() {
    }

    /**
     * @return The height of the {@link android.app.ActionBar}
     */
    public static float getActionBarHeight(Context context) {
        final TypedArray actionBarSize = context.obtainStyledAttributes(ACTION_BAR_SIZE);
        final int actionBarHeight = actionBarSize.getDimensionPixelSize(0, 0);
        actionBarSize.recycle();
        return actionBarHeight;
    }

    /**
     * @return The height of the StatusBar
     */
    public static float getStatusBarHeight(Context context) {
        if (statusBarInset >= 0) {
            return statusBarInset;
        }
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }
}
