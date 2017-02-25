package com.simplecity.amp_library.glide.utils;

import android.graphics.drawable.Drawable;

import com.bumptech.glide.request.animation.*;

public class PaddingAnimationFactory<T extends Drawable> implements GlideAnimationFactory<T> {
    private final DrawableCrossFadeFactory<T> realFactory;

    public PaddingAnimationFactory(DrawableCrossFadeFactory<T> factory) {
        this.realFactory = factory;
    }

    @Override public GlideAnimation<T> build(boolean isFromMemoryCache, boolean isFirstResource) {
        return new PaddingAnimation<>(realFactory.build(isFromMemoryCache, isFirstResource));
    }
}