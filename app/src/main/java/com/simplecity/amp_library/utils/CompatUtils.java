package com.simplecity.amp_library.utils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;

public class CompatUtils {

    @SuppressWarnings("deprecation")
    public static Drawable getDrawableCompat(Context context, int resId) {
        if (ShuttleUtils.hasLollipop()) {
            return context.getResources().getDrawable(resId, context.getTheme());
        } else {
            return context.getResources().getDrawable(resId);
        }
    }

    @SuppressWarnings("deprecation")
    public static void setDrawableCompat(View view, Drawable drawable) {
        if (ShuttleUtils.hasLollipop()) {
            view.setBackground(drawable);
        } else {
            view.setBackgroundDrawable(drawable);
        }
    }
}
