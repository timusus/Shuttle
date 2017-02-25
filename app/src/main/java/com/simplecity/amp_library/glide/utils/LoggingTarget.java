package com.simplecity.amp_library.glide.utils;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.util.Log;

import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SizeReadyCallback;
import com.bumptech.glide.request.target.Target;

/**
 * Usage:
 * <ul>
 *     <li>{@code Glide.load.....into(imageView)}<br>
 *         {@code Glide.load.....into(new LoggingTarget<>(new GlideDrawableImageViewTarget(imageView)))}<br>
 *         {@code Glide.load.....into(new LoggingTarget<GlideDrawable>(new GlideDrawableImageViewTarget(imageView)).addToString())}</li>
 *     <li>{@code Glide.load.asBitmap()....into(imageView)}<br>
 *         {@code Glide.load.asBitmap()....into(new LoggingTarget<>(new BitmapImageViewTarget(imageView)))}</li>
 *     <li>{@code Glide.load.asGif()....into(imageView)}<br>
 *         {@code Glide.load.asGif()....into(new LoggingTarget<GifDrawable>(new GlideDrawableImageViewTarget(imageView)))}</li>
 * </ul>
 */
public class LoggingTarget<Z> extends WrappingTarget<Z> {
    private final String tag;
    private final int level;
    protected boolean enableToString = false;
    public LoggingTarget(@NonNull Target<? super Z> target) {
        this("LoggingTarget", Log.VERBOSE, target);
    }
    public LoggingTarget(@NonNull String tag, int logLevel, @NonNull Target<? super Z> target) {
        super(target);
        this.tag = tag;
        this.level = logLevel;
    }

    public LoggingTarget<Z> addToString() {
        enableToString = true;
        return this;
    }

    void log(@NonNull String method, Object... args) {
        log(method, null, args);
    }
    private void log(@NonNull String method, Throwable error, Object... args) {
        StringBuilder sb = new StringBuilder()
                .append(target.getClass().getSimpleName()).append('@').append(Integer.toHexString(target.hashCode()));
        sb.append('.').append(method);
        sb.append('(');
        boolean first = true;
        for (Object arg : args) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            sb.append(arg);
        }
        sb.append(')');
        if (enableToString) {
            sb.append('\n').append(target);
        }
        if (error != null) {
            sb.append('\n');
            sb.append(Log.getStackTraceString(error));
        }
        Log.println(level, tag, sb.toString());
    }

    @Override public void getSize(final SizeReadyCallback cb) {
        log("getSize", cb);
        super.getSize(new SizeReadyCallback() {
            @Override public void onSizeReady(int width, int height) {
                log("onSizeReady", cb, width, height);
                cb.onSizeReady(width, height);
            }
        });
    }

    @Override public void onLoadStarted(Drawable placeholder) {
        log("onLoadStarted", placeholder);
        super.onLoadStarted(placeholder);
    }
    @Override public void onLoadFailed(Exception e, Drawable errorDrawable) {
        log("onLoadFailed", e, e, errorDrawable);
        super.onLoadFailed(e, errorDrawable);
    }
    @Override public void onResourceReady(Z resource, GlideAnimation<? super Z> glideAnimation) {
        log("onResourceReady", resource, glideAnimation);
        super.onResourceReady(resource, glideAnimation);
    }
    @Override public void onLoadCleared(Drawable placeholder) {
        log("onLoadCleared", placeholder);
        super.onLoadCleared(placeholder);
    }

    @Override public Request getRequest() {
        log("getRequest");
        return super.getRequest();
    }
    @Override public void setRequest(Request request) {
        log("setRequest", request);
        super.setRequest(request);
    }

    @Override public void onStart() {
        log("onStart");
        super.onStart();
    }
    @Override public void onStop() {
        log("onStop");
        super.onStop();
    }
    @Override public void onDestroy() {
        log("onDestroy");
        super.onDestroy();
    }
}