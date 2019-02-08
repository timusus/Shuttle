package com.simplecity.amp_library.ui.views;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

/**
 * A button that will repeatedly call a 'listener' method
 * as long as the button is pressed.
 */
public class RepeatingImageButton extends android.support.v7.widget.AppCompatImageButton {

    private long startTime;
    private int repeatCount;
    private RepeatListener listener;
    long interval = 500;

    @NonNull
    Drawable drawable;

    int normalColor = Color.WHITE;

    public RepeatingImageButton(Context context) {
        this(context, null);
    }

    public RepeatingImageButton(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.imageButtonStyle);
    }

    public RepeatingImageButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setFocusable(true);
        setLongClickable(true);

        drawable = DrawableCompat.wrap(getDrawable().mutate());
        DrawableCompat.setTint(drawable, normalColor);
        setImageDrawable(drawable);
    }

    /**
     * Sets the listener to be called while the button is pressed and
     * the interval in milliseconds with which it will be called.
     *
     * @param l The listener that will be called
     */
    public void setRepeatListener(RepeatListener l) {
        listener = l;
    }

    @Override
    public boolean performLongClick() {
        startTime = SystemClock.elapsedRealtime();
        repeatCount = 0;
        post(mRepeater);
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            // remove the repeater, but call the hook one more time
            removeCallbacks(mRepeater);
            if (startTime != 0) {
                doRepeat(true);
                startTime = 0;
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                // need to call super to make long press work, but return
                // true so that the application doesn't get the down event.
                super.onKeyDown(keyCode, event);
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                // remove the repeater, but call the hook one more time
                removeCallbacks(mRepeater);
                if (startTime != 0) {
                    doRepeat(true);
                    startTime = 0;
                }
        }
        return super.onKeyUp(keyCode, event);
    }

    private Runnable mRepeater = new Runnable() {
        public void run() {
            doRepeat(false);
            if (isPressed()) {
                postDelayed(this, interval);
            }
        }
    };

    void doRepeat(boolean last) {
        long now = SystemClock.elapsedRealtime();
        if (listener != null) {
            listener.onRepeat(this, now - startTime, last ? -1 : repeatCount++);
        }
    }

    public interface RepeatListener {
        /**
         * This method will be called repeatedly at roughly the interval
         * specified in setRepeatListener(), for as long as the button
         * is pressed.
         *
         * @param v The button as a View.
         * @param duration The number of milliseconds the button has been pressed so far.
         * @param repeatCount The number of previous calls in this sequence.
         * If this is going to be the last call in this sequence (i.e. the user
         * just stopped pressing the button), the value will be -1.
         */
        void onRepeat(View v, long duration, int repeatCount);
    }

    public void invalidateColors(int normal) {

        this.normalColor = normal;

        DrawableCompat.setTint(drawable, normal);
    }
}
