package com.simplecity.amp_library.ui.views;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.ImageButton;

/**
 * A custom {@link ImageButton} which prevents parent ScrollView scrolling when used as the
 * anchor for a {@link android.support.v7.widget.PopupMenu}
 */
public class NonScrollImageButton extends ImageButton {

    public NonScrollImageButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean requestRectangleOnScreen(Rect rectangle, boolean immediate) {
        return false;
    }
}
