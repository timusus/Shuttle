package com.simplecity.amp_library.glide.utils;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.view.View;
import com.bumptech.glide.request.animation.GlideAnimation.ViewAdapter;

public class WrappingViewAdapter implements ViewAdapter {
    protected final ViewAdapter adapter;

    public WrappingViewAdapter(@NonNull ViewAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public View getView() {
        return adapter.getView();
    }

    @Override
    public Drawable getCurrentDrawable() {
        return adapter.getCurrentDrawable();
    }

    @Override
    public void setDrawable(Drawable drawable) {
        adapter.setDrawable(drawable);
    }
}