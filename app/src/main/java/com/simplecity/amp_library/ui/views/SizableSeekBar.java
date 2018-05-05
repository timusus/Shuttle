package com.simplecity.amp_library.ui.views;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ScaleDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.SeekBar;
import com.afollestad.aesthetic.AestheticSeekBar;

public class SizableSeekBar extends AestheticSeekBar {

    private static final String TAG = "SizableSeekBar";

    private static final float maxThumbSizeRatio = 2.0f;

    float currentThumbSizeRatio = 1.0f;
    OnSeekBarChangeListener seekListener;
    private Drawable pendingThumb;
    Drawable thumb;
    private ValueAnimator thumbGrowAnimator;
    private ValueAnimator thumbShrinkAnimator;
    private AccelerateDecelerateInterpolator interpolator = new AccelerateDecelerateInterpolator();

    public SizableSeekBar(Context context) {
        this(context, null);
    }

    public SizableSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);

        super.setOnSeekBarChangeListener(internalListener);

        setThumb(pendingThumb);
        pendingThumb = null;
    }

    void startThumbGrowAnimation() {
        if (thumbShrinkAnimator != null) {
            thumbShrinkAnimator.cancel();
            thumbShrinkAnimator = null;
        }
        thumbGrowAnimator = ValueAnimator.ofFloat(currentThumbSizeRatio, maxThumbSizeRatio);
        thumbGrowAnimator.setInterpolator(interpolator);
        thumbGrowAnimator.addUpdateListener(mAnimatorListener);
        thumbGrowAnimator.setDuration(300);
        thumbGrowAnimator.start();
    }

    void startThumbShrinkAnimation() {
        if (thumbGrowAnimator != null) {
            thumbGrowAnimator.cancel();
            thumbGrowAnimator = null;
        }
        thumbShrinkAnimator = ValueAnimator.ofFloat(currentThumbSizeRatio, 1.0f);
        thumbShrinkAnimator.setInterpolator(interpolator);
        thumbShrinkAnimator.addUpdateListener(mAnimatorListener);
        thumbShrinkAnimator.setDuration(300);
        thumbShrinkAnimator.start();
    }

    @Override
    public void setOnSeekBarChangeListener(OnSeekBarChangeListener listener) {
        seekListener = listener;
    }

    @Override
    public void setThumb(Drawable thumb) {
        if (thumb == null) {
            return;
        }

        if (!(thumb instanceof ScaleDrawable)) {
            thumb = new ScaleDrawable(thumb, Gravity.CENTER, 1.0F, 1.0F);
        }

        this.thumb = thumb;
        int level = (int) (10000F * (1.0F / maxThumbSizeRatio));
        this.thumb.setLevel(level);
        super.setThumb(this.thumb);
    }

    public Drawable getThumb() {
        return thumb;
    }

    private ValueAnimator.AnimatorUpdateListener mAnimatorListener = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            currentThumbSizeRatio = (Float) valueAnimator.getAnimatedValue();
            int level = (int) (10000F * (currentThumbSizeRatio / maxThumbSizeRatio));
            thumb.setLevel(level);
            SizableSeekBar.this.invalidate();
        }
    };

    private OnSeekBarChangeListener internalListener = new OnSeekBarChangeListener() {

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            startThumbShrinkAnimation();
            if (seekListener != null) {
                seekListener.onStopTrackingTouch(SizableSeekBar.this);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            startThumbGrowAnimation();
            if (seekListener != null) {
                seekListener.onStartTrackingTouch(SizableSeekBar.this);
            }
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (seekListener != null) {
                seekListener.onProgressChanged(SizableSeekBar.this, progress, fromUser);
            }
        }
    };
}
