/*
 * Copyright (C) 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.libraries.cast.companionlibrary.widgets;

import com.google.android.libraries.cast.companionlibrary.R;
import com.google.android.libraries.cast.companionlibrary.utils.Utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.ColorRes;
import android.support.annotation.DimenRes;
import android.support.annotation.StringRes;
import android.support.v7.app.MediaRouteButton;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * A simple overlay view that can be used to bring user's attention to the cast button. To use
 * this overlay, build an instance of this class and call {@link #show()}:
 * <pre>
 *     IntroductoryOverlay overlay = new IntroductoryOverlay.Builder(activity)
 *         .setMenuItem(mMediaRouteMenuItem)
 *         .setTitleText(R.string.intro_overlay_text)
 *         .setOnDismissed(onOverlayDismissedListener)
 *         .setSingleSTime()
 *         .build();
 *     overlay.show();
 * </pre>
 * Here, {@code mMediaRouteMenuItem} is the {@link MenuItem} reference to the cast button (returned
 * when you call
 * {@link com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager#addMediaRouterButton(Menu, int)}).
 * You can also point to an instance of {@link MediaRouteButton} by calling
 * {@link Builder#setMediaRouteButton(MediaRouteButton)}.
 * <p>To customize the layout of this view, copy {@code res/layout/ccl_intro_overlay.xml} into your
 * project and update that copy (keep the component ids the same). To customize the style, take a
 * look at the {@code res/values/intro_overlay.xml}.
 * <p>In order to show this overlay at the right time, clients can register to
 * {@link com.google.android.libraries.cast.companionlibrary.cast.callbacks.VideoCastConsumer#onCastAvailabilityChanged(boolean)}.
 * Management of how often this overlay should be shown is left to the client application.
 */
public class IntroductoryOverlay extends RelativeLayout {

    private static final long FADE_OUT_LENGTH_MS = 400;
    public static final String FTU_SHOWN_KEY = "ccl_ftu_shown";
    private boolean mIsSingleTime;
    private TextView mTitleText;
    private TextView mSubtitleText;
    private Button mButton;
    private float mFocusRadius;
    private int mOverlayColorId;
    private int mCenterY;
    private int mCenterX;
    private Paint mHolePaint;
    private Bitmap mBitmap;
    private boolean mIsOverlayVisible;
    private static final String ALPHA_PROPERTY = "alpha";
    private static final float INVISIBLE_VALUE = 0f;
    private OnOverlayDismissedListener mListener;

    private IntroductoryOverlay(Builder builder) {
        this(builder, null, R.styleable.CustomTheme_CCLIntroOverlayStyle);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public IntroductoryOverlay(Builder builder, AttributeSet attrs, int defStyleAttr) {
        super(builder.mContext, attrs, defStyleAttr);
        mIsSingleTime = builder.mSingleTime;
        LayoutInflater inflater = LayoutInflater.from(getContext());
        inflater.inflate(R.layout.ccl_intro_overlay, this);
        mButton = (Button) findViewById(R.id.button);
        mTitleText = (TextView) findViewById(R.id.textTitle);
        mSubtitleText = (TextView) findViewById(R.id.textSubtitle);
        TypedArray typedArray = getContext().getTheme()
                .obtainStyledAttributes(attrs, R.styleable.CCLIntroOverlay,
                        R.attr.CCLIntroOverlayStyle, R.style.CCLIntroOverlay);
        if (builder.mOverlayColor != 0) {
            mOverlayColorId = builder.mOverlayColor;
        } else {
            mOverlayColorId = typedArray
                    .getColor(R.styleable.CCLIntroOverlay_ccl_IntroBackgroundColor,
                            Color.argb(0, 0, 0, 0));
        }
        mFocusRadius = builder.mRadius;
        mListener = builder.mListener;
        if (mFocusRadius == 0) {
            mFocusRadius = typedArray
                    .getDimension(R.styleable.CCLIntroOverlay_ccl_IntroFocusRadius, 0);
        }
        View view = builder.mView;
        Rect rect = new Rect();
        view.getGlobalVisibleRect(rect);
        mCenterX = rect.centerX();
        mCenterY = rect.centerY();
        setFitsSystemWindows(true);
        setupHolePaint();
        setText(builder.mTitleText, builder.mSubtitleText);

        setButton(builder.mButtonText, typedArray);
        typedArray.recycle();
    }

    /**
     * Shows the overlay if it is not visible already.
     */
    public void show() {
        if (mIsSingleTime && isFtuShown()) {
            // we are exceeding the max number
            mListener = null;
            return;
        }
        if (!mIsOverlayVisible) {
            mIsOverlayVisible = true;
            ((ViewGroup) ((Activity) getContext()).getWindow().getDecorView()).addView(this);
        }
    }

    /**
     * Removes the overlay and frees the resources used. It also removes the reference to the
     * activity that was used to build this overlay to avoid any leaks. When user taps on the
     * button in this overlay, this method will be called automatically. After calling this method,
     * for all practical purposes, this component cannot be re-used.
     */
    public void remove() {
        if (getContext() != null) {
            ((ViewGroup) ((Activity) getContext()).getWindow().getDecorView()).removeView(this);
        }
        if (mBitmap != null && !mBitmap.isRecycled()) {
            mBitmap.recycle();
        }
        mBitmap = null;
        mListener = null;
    }

    private void setButton(String text, TypedArray typedArray) {
        String buttonText = text;
        if (TextUtils.isEmpty(text)) {
            buttonText = typedArray.getString(R.styleable.CCLIntroOverlay_ccl_IntroButtonText);
        }
        int buttonColor = typedArray
                    .getColor(R.styleable.CCLIntroOverlay_ccl_IntroButtonBackgroundColor,
                            Color.argb(0, 0, 0, 0));

        mButton.setText(buttonText);
        mButton.getBackground().setColorFilter(buttonColor, PorterDuff.Mode.MULTIPLY);
        mButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                fadeOut(FADE_OUT_LENGTH_MS);
            }
        });
    }

    private void setText(CharSequence titleText, CharSequence detailText) {

        if (!TextUtils.isEmpty(titleText)) {
            mTitleText.setText(titleText);
        }

        if (!TextUtils.isEmpty(detailText)) {
            mSubtitleText.append(detailText);
        }
    }

    private void setupHolePaint() {
        PorterDuffXfermode xfermode = new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY);
        mHolePaint = new Paint();
        mHolePaint.setColor(0xFFFFFF);
        mHolePaint.setAlpha(0);
        mHolePaint.setXfermode(xfermode);
        mHolePaint.setAntiAlias(true);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        mBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas2 = new Canvas(mBitmap);
        canvas2.drawColor(mOverlayColorId);
        canvas2.drawCircle(mCenterX, mCenterY, mFocusRadius, mHolePaint);
        canvas.drawBitmap(mBitmap, 0, 0, null);
        super.dispatchDraw(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return true;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void fadeOut(long duration) {
        ObjectAnimator oa = ObjectAnimator.ofFloat(this, ALPHA_PROPERTY, INVISIBLE_VALUE);
        oa.setDuration(duration).addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationEnd(Animator animator) {
                setFtuShown();
                if(mListener != null) {
                    mListener.onOverlayDismissed();
                    mListener = null;
                }
                remove();
            }

        });
        oa.start();
    }

    private void setFtuShown() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        sharedPref.edit().putBoolean(FTU_SHOWN_KEY, true).apply();
    }

    private boolean isFtuShown() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        return sharedPref.getBoolean(FTU_SHOWN_KEY, false);
    }

    /**
     * The builder class that is used to instantiate an instance of {@link IntroductoryOverlay}
     */
    public static class Builder {
        private Context mContext;

        @ColorRes
        private float mRadius;
        private String mButtonText;
        private String mTitleText;
        private String mSubtitleText;
        private int mOverlayColor;
        private OnOverlayDismissedListener mListener;
        private boolean mSingleTime;
        private View mView;

        /**
         * The constructor for the Builder class. Note that the context passed here must be an
         * activity context.
         */
        public Builder(Context activityContext) {
            mContext = activityContext;
        }

        public IntroductoryOverlay build() {
            Utils.assertNotNull(mView, "MenuItem or MediaRouteButton");
            return new IntroductoryOverlay(this);
        }

        /**
         * Set the {@link MenuItem} referencing the cast button.
         */
        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        public Builder setMenuItem(MenuItem menuItem) {
            mView = menuItem.getActionView();
            return this;
        }

        /**
         * Set the {@link MediaRouteButton} that the ovelay should focus on.
         */
        public Builder setMediaRouteButton(MediaRouteButton button) {
            mView = button;
            return this;
        }

        /**
         * Sets the background color of the overlay view. This will override the value that is
         * provided in the style resource file. This is optional.
         */
        public Builder setOverlayColor(@ColorRes int colorId) {
            mOverlayColor = mContext.getResources().getColor(colorId);
            return this;
        }

        /**
         * Sets the radius of the "hole" around the cast button. This will override the value
         * specified in the resource file and is optional.
         */
        public Builder setFocusRadiusId(@DimenRes int radiusId) {
            mRadius = mContext.getResources().getDimension(radiusId);
            return this;
        }

        /**
         * Sets the radius of the "hole" around the cast button. This will override the value
         * specified in the resource file and is optional.
         */
        public Builder setFocusRadius(float radius) {
            mRadius = radius;
            return this;
        }

        /**
         * Sets the text used in the confirmation button on this overlay. This will override the
         * value specified in the resource file and is optional.
         */
        public Builder setButtonText(String text) {
            mButtonText = text;
            return this;
        }

        /**
         * Sets the text used in the confirmation button on this overlay. This will override the
         * value specified in the resource file and is optional.
         */
        public Builder setButtonText(@StringRes int stringId) {
            mButtonText = mContext.getResources().getString(stringId);
            return this;
        }

        /**
         * Sets the text used in the Title area on the overlay. This will override the
         * value specified in the resource file and is optional.
         */
        public Builder setTitleText(@StringRes int stringId) {
            mTitleText = mContext.getResources().getString(stringId);
            return this;
        }

        /**
         * Sets the text used in the Title area on the overlay. This will override the
         * value specified in the resource file and is optional.
         */
        public Builder setTitleText(String text) {
            mTitleText = text;
            return this;
        }

        /**
         * Sets the text used in the Subtitle area on the overlay. This is optional.
         */
        public Builder setSubtitleText(@StringRes int stringId) {
            mSubtitleText = mContext.getResources().getString(stringId);
            return this;
        }

        /**
         * Sets the text used in the Subtitle area on the overlay. This is optional.
         */
        public Builder setSubtitleText(String text) {
            mSubtitleText = text;
            return this;
        }

        /**
         * Sets an {@link OnOverlayDismissedListener} listener that will be notified when the
         * overlay is dismissed by pressing on the confirmation button.
         */
        public Builder setOnDismissed(OnOverlayDismissedListener listener) {
            mListener = listener;
            return this;
        }

        /**
         * Sets the maximum number of times that this overlay should be shown to 1. If it is needed
         * to show this more than once, management of the counter has to be done by the client.
         */
        public Builder setSingleTime() {
            mSingleTime = true;
            return this;
        }
    }

    /**
     * An interface to notify the clients when the ovelay is dismissed explicitly when user taps
     * on the confirmation button.
     */
    public interface OnOverlayDismissedListener {
        void onOverlayDismissed();
    }
}
