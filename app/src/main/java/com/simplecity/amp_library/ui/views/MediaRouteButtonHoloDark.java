package com.simplecity.amp_library.ui.views;

import android.content.Context;
import android.support.v7.app.MediaRouteButton;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;

import com.simplecity.amp_library.R;
import com.simplecity.amp_library.utils.ThemeUtils;

public class MediaRouteButtonHoloDark extends MediaRouteButton {

    public MediaRouteButtonHoloDark(Context context) {
        this(context, null);
    }

    public MediaRouteButtonHoloDark(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.mediaRouteButtonStyle);
    }

    public MediaRouteButtonHoloDark(Context context, AttributeSet attrs, int defStyleAttr) {
        super(getThemedContext(context), attrs, defStyleAttr);
    }

    private static Context getThemedContext(Context context) {

        if (ThemeUtils.getThemeType(context) == ThemeUtils.ThemeType.TYPE_LIGHT) {
            context = new ContextThemeWrapper(context, R.style.Theme_AppCompat_Light);
            return new ContextThemeWrapper(context, R.style.Theme_MediaRouter);
        }

        context = new ContextThemeWrapper(context, R.style.Theme_AppCompat);
        return new ContextThemeWrapper(context, R.style.Theme_MediaRouter);

    }
}
