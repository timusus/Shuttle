package com.simplecity.amp_library.ui.views;

import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.AppCompatEditText;
import android.util.AttributeSet;

import com.simplecity.amp_library.utils.ColorUtils;
import com.simplecity.amp_library.utils.ThemeUtils;

public class CustomEditText extends AppCompatEditText implements Themable {

    public CustomEditText(Context context) {
        super(context);
        updateTheme();
    }

    public CustomEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        updateTheme();
    }

    public CustomEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        updateTheme();
    }

    @Override
    public void updateTheme() {
        if (!isInEditMode()) {
            ViewCompat.setBackgroundTintList(this, ColorUtils.getDefaultColorStateList());
            setHintTextColor(ColorUtils.getContrastAwareColorAccent(getContext()));
            ThemeUtils.setEditTextDrawablesColor(this, ColorUtils.getContrastAwareColorAccent(getContext()));
        }
    }
}