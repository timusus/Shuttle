package com.doomonafireball.betterpickers.hmspicker;

import com.simplecity.amp_library.R;
import com.simplecity.amp_library.utils.TypefaceManager;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class HmsView extends LinearLayout {

    private ZeroTopPaddingTextView mHoursOnes;
    private ZeroTopPaddingTextView mMinutesOnes, mMinutesTens;
    private Typeface mAndroidClockMonoThin;

    private ColorStateList mTextColor;

    public HmsView(Context context) {
        this(context, null);
    }

    public HmsView(Context context, AttributeSet attrs) {
        super(context, attrs);

        if (isInEditMode()) {
            mAndroidClockMonoThin = Typeface.createFromAsset(getContext().getAssets(), "fonts/" + TypefaceManager.ANDROID_CLOCK_MONO_THIN);
        } else {
            mAndroidClockMonoThin = TypefaceManager.getInstance().getTypeface(TypefaceManager.ANDROID_CLOCK_MONO_THIN);
        }

        // Init defaults
        mTextColor = getResources().getColorStateList(
                R.color.dialog_text_color_holo_dark);
    }

    public void setTheme(int themeResId) {
        if (themeResId != -1) {
            TypedArray a = getContext().obtainStyledAttributes(themeResId, R.styleable.BetterPickersDialogFragment);
            mTextColor = a.getColorStateList(R.styleable.BetterPickersDialogFragment_bpTextColor);
            a.recycle();
        }

        restyleViews();
    }

    private void restyleViews() {
        if (mHoursOnes != null) {
            mHoursOnes.setTextColor(mTextColor);
        }
        if (mMinutesOnes != null) {
            mMinutesOnes.setTextColor(mTextColor);
        }
        if (mMinutesTens != null) {
            mMinutesTens.setTextColor(mTextColor);
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mHoursOnes = (ZeroTopPaddingTextView) findViewById(R.id.hours_ones);
        mMinutesTens = (ZeroTopPaddingTextView) findViewById(R.id.minutes_tens);
        mMinutesOnes = (ZeroTopPaddingTextView) findViewById(R.id.minutes_ones);

        if (mHoursOnes != null) {
            mHoursOnes.updatePaddingForBoldDate();
        }
        // Set the lowest time unit with thin font (excluding hundredths)
        if (mMinutesTens != null) {
            mMinutesTens.setTypeface(mAndroidClockMonoThin);
            mMinutesTens.updatePadding();
        }
        if (mMinutesOnes != null) {
            mMinutesOnes.setTypeface(mAndroidClockMonoThin);
            mMinutesOnes.updatePadding();
        }
    }

    public void setTime(int hoursOnesDigit, int minutesTensDigit, int minutesOnesDigit) {
        if (mHoursOnes != null) {
            mHoursOnes.setText(String.format("%d", hoursOnesDigit));
        }
        if (mMinutesTens != null) {
            mMinutesTens.setText(String.format("%d", minutesTensDigit));
        }
        if (mMinutesOnes != null) {
            mMinutesOnes.setText(String.format("%d", minutesOnesDigit));
        }
    }
}
