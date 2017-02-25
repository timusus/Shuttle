package com.simplecity.amp_library.glide.utils;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.view.View;

import com.bumptech.glide.request.animation.GlideAnimation;

public class PaddingViewAdapter implements GlideAnimation.ViewAdapter {
    private final GlideAnimation.ViewAdapter realAdapter;
    private final int targetWidth;
    private final int targetHeight;

    public PaddingViewAdapter(GlideAnimation.ViewAdapter adapter, int targetWidth, int targetHeight) {
        this.realAdapter = adapter;
        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;
    }

    @Override
    public View getView() {
        return realAdapter.getView();
    }

    @Override
    public Drawable getCurrentDrawable() {
        Drawable drawable = realAdapter.getCurrentDrawable();
        if (drawable != null) {
            int padX = Math.max(0, targetWidth - drawable.getIntrinsicWidth()) / 2;
            int padY = Math.max(0, targetHeight - drawable.getIntrinsicHeight()) / 2;
            if (padX > 0 || padY > 0) {
                drawable = new InsetDrawable(drawable, padX, padY, padX, padY);
            }
        }
        return drawable;
    }

    @Override
    public void setDrawable(Drawable drawable) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && drawable instanceof TransitionDrawable) {
            // For some reason padding is taken into account differently on M than before in LayerDrawable
            // PaddingMode was introduced in 21 and gravity in 23, I think NO_GRAVITY default may play
            // a role in this, but didn't have time to dig deeper than this.
            ((TransitionDrawable) drawable).setPaddingMode(TransitionDrawable.PADDING_MODE_STACK);
        }
        realAdapter.setDrawable(drawable);
    }
}