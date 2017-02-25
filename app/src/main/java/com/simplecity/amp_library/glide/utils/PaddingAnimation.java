package com.simplecity.amp_library.glide.utils;

import android.graphics.drawable.Drawable;

import com.bumptech.glide.request.animation.GlideAnimation;

class PaddingAnimation<T extends Drawable> implements GlideAnimation<T> {
    private final GlideAnimation<? super T> realAnimation;

    public PaddingAnimation(GlideAnimation<? super T> animation) {
        this.realAnimation = animation;
    }

    @Override
    public boolean animate(T current, final ViewAdapter adapter) {
        int width = current.getIntrinsicWidth();
        int height = current.getIntrinsicHeight();
        return realAnimation.animate(current, new PaddingViewAdapter(adapter, width, height));
    }
}