package com.simplecity.amp_library.utils;

import android.content.Context;
import android.content.res.TypedArray;
import android.view.View;
import android.view.ViewGroup;
import java.util.ArrayList;
import java.util.List;

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

    private static final List<Runnable> insetListeners = new ArrayList<>();

    public static void setStatusBarInset(int inset) {
        if (statusBarInset == inset) {
            return;
        }
        statusBarInset = inset;
        for (Runnable listener : new ArrayList<>(insetListeners)) {
            listener.run();
        }
    }

    /**
     * Sizes a toolbar that draws behind the status bar: action bar height plus the status bar inset as height, and the
     * inset added to its top padding. Re-applied whenever the inset changes (e.g. rotation on a cutout device) while
     * the toolbar is attached. Main thread only.
     */
    public static void bindToolbarInset(final View toolbar) {
        final int basePaddingTop = toolbar.getPaddingTop();
        final Runnable apply = () -> {
            int statusBarHeight = (int) getStatusBarHeight(toolbar.getContext());
            ViewGroup.LayoutParams params = toolbar.getLayoutParams();
            if (params != null) {
                params.height = (int) getActionBarHeight(toolbar.getContext()) + statusBarHeight;
            }
            toolbar.setPadding(toolbar.getPaddingLeft(), basePaddingTop + statusBarHeight, toolbar.getPaddingRight(), toolbar.getPaddingBottom());
            toolbar.requestLayout();
        };
        apply.run();
        toolbar.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                insetListeners.add(apply);
                apply.run();
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                insetListeners.remove(apply);
            }
        });
        if (toolbar.isAttachedToWindow()) {
            insetListeners.add(apply);
        }
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
