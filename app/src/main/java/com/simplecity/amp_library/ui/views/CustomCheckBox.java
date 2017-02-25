package com.simplecity.amp_library.ui.views;

import android.content.Context;
import android.support.v7.widget.AppCompatCheckBox;
import android.util.AttributeSet;

import com.simplecity.amp_library.utils.ColorUtils;

public class CustomCheckBox extends AppCompatCheckBox implements Themable {

    public CustomCheckBox(Context context) {
        super(context);
        updateTheme();
    }

    public CustomCheckBox(Context context, AttributeSet attrs) {
        super(context, attrs);
        updateTheme();
    }

    public CustomCheckBox(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        updateTheme();
    }

    @Override
    public void updateTheme() {
        if (!isInEditMode()) {
            this.setSupportButtonTintList(ColorUtils.getDefaultColorStateList());
        }
    }
}