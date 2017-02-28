package com.simplecity.amp_library.utils;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.support.annotation.FloatRange;
import android.view.View;

import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ShuttleApplication;

public class ColorUtils {

    /**
     * @return the highlight color for the current theme
     */
    public static int getPrimaryColor() {
        return SettingsManager.getInstance().getPrimaryColor(ShuttleApplication.getInstance().getResources().getColor(R.color.indigo_500));
    }

    public static int getAccentColor() {
        if (SettingsManager.getInstance().isAccentColorWhite()) {
            return getPrimaryColor();
        }
        return SettingsManager.getInstance().getAccentColor(ColorUtils.getDefaultAccentColor(getPrimaryColor()));
    }

    public static int getAccentColor(boolean canBeWhite, boolean useAccent) {
        if (!useAccent && !canBeWhite && SettingsManager.getInstance().isAccentColorWhite()) {
            return getPrimaryColor();
        }
        return SettingsManager.getInstance().getAccentColor(ColorUtils.getDefaultAccentColor(getPrimaryColor()));
    }

    /**
     * @param context Context
     * @return an alternative, visible accent color
     */
    public static int getContrastAwareColorAccent(Context context) {
        Resources res = context.getResources();
        int colorAccent = getAccentColor();
        int themeType = ThemeUtils.getThemeType(context);
        if (isAccentColorHighContrast(context)) {
            if (themeType == ThemeUtils.ThemeType.TYPE_DARK
                    || themeType == ThemeUtils.ThemeType.TYPE_SOLID_DARK
                    || themeType == ThemeUtils.ThemeType.TYPE_SOLID_BLACK) {
                return android.graphics.Color.WHITE;
            } else {
                return res.getColor(R.color.grey_700);
            }
        }
        return colorAccent;
    }

    public static boolean isAccentColorHighContrast(Context context) {
        Resources res = context.getResources();
        int colorAccent = getAccentColor();
        return colorAccent == res.getColor(R.color.yellow_a100)
                || colorAccent == res.getColor(R.color.yellow_a200)
                || colorAccent == res.getColor(R.color.yellow_a400)
                || colorAccent == res.getColor(R.color.light_green_a100)
                || colorAccent == res.getColor(R.color.light_green_a200)
                || colorAccent == res.getColor(R.color.green_a100)
                || colorAccent == res.getColor(R.color.light_green_a400)
                || colorAccent == res.getColor(R.color.cyan_a100)
                || colorAccent == res.getColor(R.color.teal_a100)
                || colorAccent == res.getColor(R.color.lime_a100)
                || colorAccent == res.getColor(R.color.lime_a200)
                || colorAccent == res.getColor(R.color.lime_a400)
                || colorAccent == res.getColor(R.color.orange_a100);
    }

    public static boolean isPrimaryColorLowContrast(Context context) {
        if (ThemeUtils.getInstance().isThemeDark()) {
            Resources res = context.getResources();
            int primaryColor = getPrimaryColor();
            return primaryColor == res.getColor(R.color.blue_grey_900)
                    || primaryColor == res.getColor(R.color.blue_grey_800)
                    || primaryColor == res.getColor(R.color.black)
                    || primaryColor == res.getColor(R.color.grey_900)
                    || primaryColor == res.getColor(R.color.grey_1000)
                    || primaryColor == res.getColor(R.color.grey_1000)
                    || primaryColor == res.getColor(R.color.brown_900)
                    || primaryColor == res.getColor(R.color.teal_900)
                    || primaryColor == res.getColor(R.color.indigo_900)
                    || primaryColor == res.getColor(R.color.deep_purple_900);
        }
        return false;
    }

    public static int getFloatingActionIconColor(Context context) {
        Resources res = context.getResources();
        if (isAccentColorHighContrast(context)) {
            return res.getColor(R.color.grey_700);
        }
        return android.graphics.Color.WHITE;
    }

    public static int getAccentColorSensitiveTextColor(Context context) {
        if (isAccentColorHighContrast(context)) {
            return context.getResources().getColor(R.color.grey_700);
        } else {
            return context.getResources().getColor(android.R.color.primary_text_dark);
        }
    }

    public static int getPrimaryColorDark(Context context) {
        int primaryColor = getPrimaryColor();
        return ColorUtils.getColorDark(primaryColor);
    }

    public static int getTextColorPrimary() {
        if (ThemeUtils.getInstance().themeType == ThemeUtils.ThemeType.TYPE_LIGHT
                || ThemeUtils.getInstance().themeType == ThemeUtils.ThemeType.TYPE_SOLID_LIGHT) {
            return Color.BLACK;
        } else {
            return Color.WHITE;
        }
    }

    @ColorInt
    public static int shiftColor(@ColorInt int color, @FloatRange(from = 0.0f, to = 2.0f) float by) {
        if (by == 1f) return color;
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= by; // value component
        return Color.HSVToColor(hsv);
    }

    public static int getColorDark(int colorPrimary) {

        if (colorPrimary == Color.BLACK) {
            return Color.BLACK;
        }

        return shiftColor(colorPrimary, 0.85f);
    }

    public static int adjustAlpha(int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }

    /**
     * Returns darker version of specified <code>color</code>.
     */
    public static int darkerise(int color, float factor) {
        int a = Color.alpha(color);
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);

        return Color.argb(a,
                Math.max((int) (r * factor), 0),
                Math.max((int) (g * factor), 0),
                Math.max((int) (b * factor), 0));
    }

    public static int getDefaultAccentColor(int color) {
        Resources res = ShuttleApplication.getInstance().getResources();

        if (color == res.getColor(R.color.red_400) ||
                color == res.getColor(R.color.red_500) ||
                color == res.getColor(R.color.red_600)) {
            return res.getColor(R.color.blue_a200);
        }

        if (color == res.getColor(R.color.pink_400) ||
                color == res.getColor(R.color.pink_500) ||
                color == res.getColor(R.color.pink_600)) {
            return res.getColor(R.color.yellow_a400);
        }

        if (color == res.getColor(R.color.purple_400) ||
                color == res.getColor(R.color.purple_500) ||
                color == res.getColor(R.color.purple_600)) {
            return res.getColor(R.color.yellow_a200);
        }

        if (color == res.getColor(R.color.indigo_400) ||
                color == res.getColor(R.color.indigo_500) |
                        color == res.getColor(R.color.indigo_600)) {
            return res.getColor(R.color.pink_a400);
        }

        if (color == res.getColor(R.color.blue_400) ||
                color == res.getColor(R.color.blue_500) ||
                color == res.getColor(R.color.blue_600)) {
            return res.getColor(R.color.light_blue_a400);
        }

        if (color == res.getColor(R.color.light_blue_400) ||
                color == res.getColor(R.color.light_blue_500) ||
                color == res.getColor(R.color.light_blue_600)) {
            return res.getColor(R.color.deep_purple_a200);
        }

        if (color == res.getColor(R.color.cyan_400) ||
                color == res.getColor(R.color.cyan_500) ||
                color == res.getColor(R.color.cyan_600)) {
            return res.getColor(R.color.deep_purple_a200);
        }

        if (color == res.getColor(R.color.teal_400) ||
                color == res.getColor(R.color.teal_500) ||
                color == res.getColor(R.color.teal_600)) {
            return res.getColor(R.color.light_blue_a200);
        }

        if (color == res.getColor(R.color.green_400) ||
                color == res.getColor(R.color.green_500) ||
                color == res.getColor(R.color.green_600)) {
            return res.getColor(R.color.orange_a200);
        }

        if (color == res.getColor(R.color.orange_400) ||
                color == res.getColor(R.color.orange_500) ||
                color == res.getColor(R.color.orange_600)) {
            return res.getColor(R.color.purple_a200);
        }

        if (color == res.getColor(R.color.grey_400) ||
                color == res.getColor(R.color.grey_500) ||
                color == res.getColor(R.color.grey_600)) {

            return res.getColor(R.color.pink_a200);
        }

        if (color == res.getColor(R.color.blue_grey_900) ||
                color == res.getColor(R.color.blue_grey_800
                )) {
            return res.getColor(R.color.cyan_a700);
        }

        return color;
    }

    public static void startBackgroundTransition(final View view, int colorFrom, int colorTo) {
        ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
        colorAnimation.setDuration(350);
        colorAnimation.addUpdateListener(animator -> view.setBackgroundColor((Integer) animator.getAnimatedValue()));
        colorAnimation.start();
    }

    public static ColorStateList getDefaultColorStateList() {

        ColorStateList colorStateList;
        /**
         * Generate the default color state list which uses the colorControl attributes.
         * Order is important here. The default enabled state needs to go at the bottom.
         */
        final int accentColor = ColorUtils.getAccentColor();
        final int[][] states = new int[7][];
        final int[] colors = new int[7];
        int i = 0;
        // Disabled state
        states[i] = new int[]{-android.R.attr.state_enabled};
        colors[i] = getDisabledColor(ColorUtils.getAccentColor(), 0.6f);
        i++;
        states[i] = new int[]{android.R.attr.state_focused};
        colors[i] = accentColor;
        i++;
        states[i] = new int[]{android.R.attr.state_activated};
        colors[i] = accentColor;
        i++;
        states[i] = new int[]{android.R.attr.state_pressed};
        colors[i] = accentColor;
        i++;
        states[i] = new int[]{android.R.attr.state_checked};
        colors[i] = accentColor;
        i++;
        states[i] = new int[]{android.R.attr.state_selected};
        colors[i] = accentColor;
        i++;
        // Default enabled state
        states[i] = new int[0];
        colors[i] = accentColor;
        colorStateList = new ColorStateList(states, colors);
        return colorStateList;
    }

    public static ColorStateList createSwitchTrackColorStateList() {

        final int accentColor = ColorUtils.getAccentColor();

        final int[][] states = new int[3][];
        final int[] colors = new int[3];
        int i = 0;
        // Disabled state
        states[i] = new int[]{-android.R.attr.state_enabled};
        colors[i] = getDisabledColor(accentColor, 0.1f);
        i++;
        states[i] = new int[]{android.R.attr.state_checked};
        colors[i] = getDisabledColor(accentColor, 0.3f);
        i++;
        // Default enabled state
        states[i] = new int[0];
        colors[i] = getDisabledColor(accentColor, 0.3f);
        return new ColorStateList(states, colors);
    }

    public static ColorStateList createSwitchThumbColorStateList() {

        final int accentColor = ColorUtils.getAccentColor();

        final int[][] states = new int[3][];
        final int[] colors = new int[3];
        int i = 0;

        states[i] = new int[]{-android.R.attr.state_enabled};
        colors[i] = getDisabledColor(accentColor, 0.6f);
        i++;
        states[i] = new int[]{android.R.attr.state_checked};
        colors[i] = accentColor;
        i++;
        // Default enabled state
        states[i] = new int[0];
        colors[i] = accentColor;

        return new ColorStateList(states, colors);
    }

    static final int[] DISABLED_STATE_SET = new int[]{-16842910};
    static final int[] FOCUSED_STATE_SET = new int[]{16842908};
    static final int[] ACTIVATED_STATE_SET = new int[]{16843518};
    static final int[] PRESSED_STATE_SET = new int[]{16842919};
    static final int[] CHECKED_STATE_SET = new int[]{16842912};
    static final int[] SELECTED_STATE_SET = new int[]{16842913};
    static final int[] NOT_PRESSED_OR_FOCUSED_STATE_SET = new int[]{-16842919, -16842908};
    static final int[] EMPTY_STATE_SET = new int[0];

    public static ColorStateList createEditTextColorStateList(Context context) {
        final int[][] states = new int[3][];
        final int[] colors = new int[3];
        int i = 0;

        // Disabled state
        states[i] = DISABLED_STATE_SET;
        int disabledColor = getThemeAttrColor(context, android.support.v7.appcompat.R.attr.colorControlNormal);
        colors[i] = getDisabledColor(disabledColor, 0.6f);
        i++;

        states[i] = NOT_PRESSED_OR_FOCUSED_STATE_SET;
        colors[i] = getThemeAttrColor(context, android.support.v7.appcompat.R.attr.colorControlNormal);
        i++;

        // Default enabled state
        states[i] = EMPTY_STATE_SET;
        colors[i] = ColorUtils.getAccentColor();
        return new ColorStateList(states, colors);
    }

    static int getDisabledColor(int color, float alpha) {
        final int originalAlpha = Color.alpha(color);
        return ColorUtils.setAlphaComponent(color, Math.round(originalAlpha * alpha));
    }

    private static final int[] TEMP_ARRAY = new int[1];

    public static int getThemeAttrColor(Context context, int attr) {
        TEMP_ARRAY[0] = attr;
        TypedArray a = context.obtainStyledAttributes(null, TEMP_ARRAY);
        try {
            return a.getColor(0, 0);
        } finally {
            a.recycle();
        }
    }

    /**
     * Set the alpha component of {@code color} to be {@code alpha}.
     */
    public static int setAlphaComponent(int color, int alpha) {
        if (alpha < 0 || alpha > 255) {
            throw new IllegalArgumentException("alpha must be between 0 and 255.");
        }
        return (color & 0x00ffffff) | (alpha << 24);
    }
}
