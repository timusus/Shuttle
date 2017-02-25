package com.simplecity.amp_library.glide.utils;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

import com.bumptech.glide.load.resource.bitmap.GlideBitmapDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.target.ViewTarget;

import java.util.Locale;

public class LoggingListener<A, B> implements RequestListener<A, B> {
    private final int level;
    private final String name;
    private final RequestListener<A, B> delegate;

    public LoggingListener() {
        this("");
    }

    public LoggingListener(@NonNull String name) {
        this(Log.VERBOSE, name);
    }

    public LoggingListener(int level, @NonNull String name) {
        this(level, name, null);
    }

    public LoggingListener(RequestListener<A, B> delegate) {
        this(Log.VERBOSE, "", delegate);
    }

    public LoggingListener(int level, @NonNull String name, RequestListener<A, B> delegate) {
        this.level = level;
        this.name = name;
        this.delegate = delegate == null ? NoOpRequestListener.<A, B>get() : delegate;
    }

    @Override
    public boolean onException(Exception e, A model, Target<B> target, boolean isFirstResource) {
        android.util.Log.println(level, "GLIDE", String.format(Locale.ROOT,
                "%s.onException(%s, %s, %s, %s)\n%s",
                name, e, model, strip(target), isFirst(isFirstResource), android.util.Log.getStackTraceString(e)));
        return delegate.onException(e, model, target, isFirstResource);
    }

    @Override
    public boolean onResourceReady(B resource, A model, Target<B> target, boolean isFromMemoryCache,
                                   boolean isFirstResource) {
        String resourceString = strip(getResourceDescription(resource));
        String targetString = strip(getTargetDescription(target));
        android.util.Log.println(level, "GLIDE", String.format(Locale.ROOT,
                "%s.onResourceReady(%s, %s, %s, %s, %s)",
                name, resourceString, model, targetString, isMem(isFromMemoryCache), isFirst(isFirstResource)));
        return delegate.onResourceReady(resource, model, target, isFromMemoryCache, isFirstResource);
    }

    private String isMem(boolean isFromMemoryCache) {
        return isFromMemoryCache ? "sync" : "async";
    }

    private String isFirst(boolean isFirstResource) {
        return isFirstResource ? "first" : "not first";
    }

    private String getTargetDescription(Target<?> target) {
        String result;
        if (target instanceof WrappingTarget) {
            Target wrapped = ((WrappingTarget) target).getWrappedTarget();
            result = String.format(Locale.ROOT, "%s in %s", getTargetDescription(wrapped), target);
        } else if (target instanceof ViewTarget) {
            View v = ((ViewTarget) target).getView();
            LayoutParams p = v.getLayoutParams();
            result = String.format(Locale.ROOT,
                    "%s(params=%dx%d->size=%dx%d)", target, p.width, p.height, v.getWidth(), v.getHeight());
        } else {
            result = String.valueOf(target);
        }
        return result;
    }

    private String getResourceDescription(B resource) {
        String result;
        if (resource instanceof Bitmap) {
            Bitmap bm = (Bitmap) resource;
            result = String.format(Locale.ROOT,
                    "%s(%dx%d@%s)", resource, bm.getWidth(), bm.getHeight(), bm.getConfig());
        } else if (resource instanceof BitmapDrawable) {
            Bitmap bm = ((BitmapDrawable) resource).getBitmap();
            result = String.format(Locale.ROOT,
                    "%s(%dx%d@%s)", resource, bm.getWidth(), bm.getHeight(), bm.getConfig());
        } else if (resource instanceof GlideBitmapDrawable) {
            Bitmap bm = ((GlideBitmapDrawable) resource).getBitmap();
            result = String.format(Locale.ROOT,
                    "%s(%dx%d@%s)", resource, bm.getWidth(), bm.getHeight(), bm.getConfig());
        } else if (resource instanceof Drawable) {
            Drawable d = (Drawable) resource;
            result = String.format(Locale.ROOT,
                    "%s(%dx%d)", resource, d.getIntrinsicWidth(), d.getIntrinsicHeight());
        } else {
            result = String.valueOf(resource);
        }
        return result;
    }

    private static String strip(Object text) {
        return String.valueOf(text).replaceAll("(com|android|net|org)(\\.[a-z]+)+\\.", "");
    }
}