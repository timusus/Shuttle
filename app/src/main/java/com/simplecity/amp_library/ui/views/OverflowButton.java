package com.simplecity.amp_library.ui.views;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.AttributeSet;

import com.afollestad.aesthetic.Aesthetic;
import com.simplecity.amp_library.R;

import io.reactivex.disposables.Disposable;

public class OverflowButton extends NonScrollImageButton {

    Disposable aestheticDisposable;
    private final Drawable drawable;

    public OverflowButton(Context context, AttributeSet attrs) {
        super(context, attrs);

        drawable = DrawableCompat.wrap(getResources().getDrawable(R.drawable.ic_overflow_white)).mutate();
        setImageDrawable(drawable);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        aestheticDisposable = Aesthetic.get()
                .textColorSecondary()
                .subscribe(color -> {
                    DrawableCompat.setTint(drawable, color);
                    setImageDrawable(drawable);
                });
    }

    @Override
    protected void onDetachedFromWindow() {
        aestheticDisposable.dispose();

        super.onDetachedFromWindow();
    }
}