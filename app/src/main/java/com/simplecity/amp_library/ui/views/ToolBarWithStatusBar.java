package com.simplecity.amp_library.ui.views;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;

public class ToolBarWithStatusBar extends Toolbar {

    public ToolBarWithStatusBar(Context context) {
        this(context, null);
    }

    public ToolBarWithStatusBar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ToolBarWithStatusBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

//        setPadding(getPaddingLeft(), getPaddingTop() + (int) getStatusBarHeight(context), getPaddingRight(), getPaddingBottom());
    }

    float getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }
}