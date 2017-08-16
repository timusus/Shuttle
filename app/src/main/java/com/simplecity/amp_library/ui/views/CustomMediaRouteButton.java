package com.simplecity.amp_library.ui.views;

import android.content.Context;
import android.support.v7.app.MediaRouteButton;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;

import com.afollestad.aesthetic.Aesthetic;
import com.simplecity.amp_library.R;

public class CustomMediaRouteButton extends MediaRouteButton {

    public CustomMediaRouteButton(Context context) {
        this(context, null);
    }

    public CustomMediaRouteButton(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.mediaRouteButtonStyle);
    }

    public CustomMediaRouteButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(getThemedContext(context), attrs, defStyleAttr);
    }

    private static Context getThemedContext(Context context) {
        return Aesthetic.get(context).isDark().blockingFirst() ?
                new ContextThemeWrapper(new ContextThemeWrapper(context, R.style.Theme_AppCompat), R.style.Theme_MediaRouter) :
                new ContextThemeWrapper(new ContextThemeWrapper(context, R.style.Theme_AppCompat_Light), R.style.Theme_MediaRouter);
    }
}