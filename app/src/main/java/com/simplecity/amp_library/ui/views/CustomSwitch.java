package com.simplecity.amp_library.ui.views;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.SwitchCompat;
import android.util.AttributeSet;

import com.simplecity.amp_library.utils.ColorUtils;

public class CustomSwitch extends SwitchCompat implements Themable {
    public CustomSwitch(Context context) {
        super(context);
        updateTheme();
    }

    public CustomSwitch(Context context, AttributeSet attrs) {
        super(context, attrs);
        updateTheme();
    }

    public CustomSwitch(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        updateTheme();
    }

    @Override
    public void updateTheme() {
        Drawable thumb = DrawableCompat.wrap(getThumbDrawable());
        DrawableCompat.setTintList(thumb, ColorUtils.createSwitchThumbColorStateList());

        Drawable track = DrawableCompat.wrap(getTrackDrawable());
        DrawableCompat.setTintList(track, ColorUtils.createSwitchTrackColorStateList());
    }
}
