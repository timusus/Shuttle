package com.simplecity.amp_library.ui.views;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import com.afollestad.aesthetic.Aesthetic;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

public class AestheticTintedImageView extends AppCompatImageView {

    Disposable aestheticDisposable;

    @Nullable
    private Drawable drawable;

    public AestheticTintedImageView(Context context) {
        super(context);
    }

    public AestheticTintedImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AestheticTintedImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setImageDrawable(@Nullable Drawable drawable) {

        if (drawable != null) {
            drawable = DrawableCompat.wrap(drawable).mutate();
        }

        this.drawable = drawable;

        super.setImageDrawable(drawable);
    }

    protected Observable<Integer> getColorObservable() {
        return Aesthetic.get(getContext()).colorAccent();
    }

    void invalidateColors(int color) {
        if (drawable != null) {
            DrawableCompat.setTint(drawable, color);
            setImageDrawable(drawable);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (!isInEditMode()) {
            aestheticDisposable = getColorObservable().subscribe(this::invalidateColors);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        aestheticDisposable.dispose();

        super.onDetachedFromWindow();
    }
}