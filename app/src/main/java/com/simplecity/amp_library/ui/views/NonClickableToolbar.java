package com.simplecity.amp_library.ui.views;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import com.afollestad.aesthetic.AestheticToolbar;

/**
 * A Toolbar which does not consume touch events.
 */
public class NonClickableToolbar extends AestheticToolbar {
    public NonClickableToolbar(Context context) {
        super(context);
    }

    public NonClickableToolbar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public NonClickableToolbar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return false;
    }
}
