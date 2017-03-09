package com.simplecity.amp_library.ui.views;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ScaleDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.SeekBar;

import com.simplecity.amp_library.utils.ResourceUtils;

public class SizableSeekBar extends SeekBar {

    private ValueAnimator.AnimatorUpdateListener mAnimatorListener = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            mCurrentThumbSizeRatio = (Float) valueAnimator.getAnimatedValue();
            mThumb.setLevel((int) (10000F * (mCurrentThumbSizeRatio / mMaxThumbSizeRatio)));
            SizableSeekBar.this.invalidate();
        }
    };

    float mCurrentThumbSizeRatio = 1.0f;

    private OnSeekBarChangeListener mInternalListener = new OnSeekBarChangeListener() {

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            startThumbShrinkAnimation();
            if (mSeekListener != null)
                mSeekListener.onStopTrackingTouch(SizableSeekBar.this);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            startThumbGrowAnimation();
            if (mSeekListener != null)
                mSeekListener.onStartTrackingTouch(SizableSeekBar.this);
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (mSeekListener != null)
                mSeekListener.onProgressChanged(SizableSeekBar.this, progress, fromUser);
        }

    };

    float mMaxThumbSizeRatio = 2.0f;
    OnSeekBarChangeListener mSeekListener;
    private Drawable mPendingThumb;
    Drawable mThumb;
    private ValueAnimator mThumbGrowAnimator;
    private ValueAnimator mThumbShrinkAnimator;
    private AccelerateDecelerateInterpolator mInterpolator = new AccelerateDecelerateInterpolator();

    public SizableSeekBar(Context context) {
        super(context);
        super.setOnSeekBarChangeListener(mInternalListener);

        setThumb(mPendingThumb);
        mPendingThumb = null;
        configureThumbPadding();
    }

    public SizableSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);

        super.setOnSeekBarChangeListener(mInternalListener);

        setThumb(mPendingThumb);
        mPendingThumb = null;
        configureThumbPadding();
    }

    private void configureThumbPadding() {
        setThumbOffset(getThumbOffset() - ResourceUtils.toPixels(4.5f));
    }

    void startThumbGrowAnimation() {
        if (mThumbShrinkAnimator != null) {
            mThumbShrinkAnimator.cancel();
            mThumbShrinkAnimator = null;
        }
        mThumbGrowAnimator = ValueAnimator.ofFloat(mCurrentThumbSizeRatio, mMaxThumbSizeRatio);
        mThumbGrowAnimator.setInterpolator(mInterpolator);
        mThumbGrowAnimator.addUpdateListener(mAnimatorListener);
        mThumbGrowAnimator.setDuration(300);
        mThumbGrowAnimator.start();

    }

    void startThumbShrinkAnimation() {
        if (mThumbGrowAnimator != null) {
            mThumbGrowAnimator.cancel();
            mThumbGrowAnimator = null;
        }
        mThumbShrinkAnimator = ValueAnimator.ofFloat(mCurrentThumbSizeRatio, 1.0f);
        mThumbShrinkAnimator.setInterpolator(mInterpolator);
        mThumbShrinkAnimator.addUpdateListener(mAnimatorListener);
        mThumbShrinkAnimator.setDuration(300);
        mThumbShrinkAnimator.start();
    }

    @Override
    protected void onSizeChanged(int i, int j, int k, int l) {
        super.onSizeChanged(i, j, k, l);
        configureThumbPadding();
    }

    @Override
    public void setOnSeekBarChangeListener(OnSeekBarChangeListener listener) {
        mSeekListener = listener;
    }

    public void setInterpolator(AccelerateDecelerateInterpolator interpolator) {
        mInterpolator = interpolator;
    }

    @Override
    public void setThumb(Drawable thumb) {
        if (thumb == null) {
            return;
        }

        if (!(thumb instanceof ScaleDrawable)) {
            thumb = new ScaleDrawable(thumb, Gravity.CENTER, 1.0F, 1.0F);
        }

        mThumb = thumb;
        mThumb.setLevel((int) (10000F * (1.0F / mMaxThumbSizeRatio)));
        configureThumbPadding();
        super.setThumb(mThumb);
    }

    public Drawable getThumb() {
        return mThumb;
    }
}
