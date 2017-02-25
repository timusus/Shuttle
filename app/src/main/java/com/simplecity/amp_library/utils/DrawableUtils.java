package com.simplecity.amp_library.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.StateSet;

import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ui.views.FilterableStateListDrawable;

public class DrawableUtils {

    /**
     * Returns a {@link FilterableStateListDrawable}, coloring the passed in
     * drawable according to the theme and the passed in highlight color
     *
     * @param baseDrawableResId the drawable to use
     * @return an {@link FilterableStateListDrawable}, coloring the passed in
     * drawable according to the theme and the passed in highlight color
     */
    public static Drawable getColoredStateListDrawable(Context context, int baseDrawableResId) {
        Drawable baseDrawable = context.getResources().getDrawable(baseDrawableResId);
        Drawable highlightDrawable = baseDrawable.getConstantState().newDrawable();
        int baseColor;
        if ((ThemeUtils.getInstance().themeType == ThemeUtils.ThemeType.TYPE_DARK)
                || (ThemeUtils.getInstance().themeType == ThemeUtils.ThemeType.TYPE_SOLID_DARK)
                || (ThemeUtils.getInstance().themeType == ThemeUtils.ThemeType.TYPE_SOLID_BLACK)) {
            baseColor = context.getResources().getColor(R.color.drawable_base_color_dark);
        } else {
            baseColor = context.getResources().getColor(R.color.drawable_base_color_light);
        }
        ColorFilter baseColorFilter = new LightingColorFilter(baseColor, 0);
        ColorFilter highlightColorFilter = new LightingColorFilter(ColorUtils.getAccentColor(), 0);
        FilterableStateListDrawable filterableStateListDrawable = new FilterableStateListDrawable();
        filterableStateListDrawable.addState(new int[]{android.R.attr.state_pressed}, baseDrawable, highlightColorFilter);
        filterableStateListDrawable.addState(StateSet.WILD_CARD, highlightDrawable, baseColorFilter);

        return filterableStateListDrawable;
    }

    /**
     * Returns a {@link FilterableStateListDrawable}, coloring the passed in
     * drawable according to the theme and the passed in highlight color
     *
     * @param baseDrawableResId the drawable to use
     * @return an {@link FilterableStateListDrawable}, coloring the passed in
     * drawable according to the theme and the passed in highlight color
     */
    public static Drawable getColoredStateListDrawableWithThemeColor(Context context, int baseDrawableResId, @ThemeUtils.ThemeColor int color) {
        if (context == null) {
            return null;
        }
        Drawable baseDrawable = context.getResources().getDrawable(baseDrawableResId);
        Drawable highlightDrawable = baseDrawable.getConstantState().newDrawable();
        int baseColor;
        if (color == ThemeUtils.WHITE) {
            baseColor = context.getResources().getColor(R.color.drawable_base_color_dark);
        } else {
            baseColor = context.getResources().getColor(R.color.drawable_base_color_light);
        }
        ColorFilter baseColorFilter = new LightingColorFilter(baseColor, 0);
        int accentColor = ColorUtils.getAccentColor();
        if (accentColor == ColorUtils.getPrimaryColor()) {
            accentColor = Color.WHITE;
        }
        ColorFilter highlightColorFilter = new LightingColorFilter(accentColor, 0);
        FilterableStateListDrawable filterableStateListDrawable = new FilterableStateListDrawable();
        filterableStateListDrawable.addState(new int[]{android.R.attr.state_pressed}, baseDrawable, highlightColorFilter);
        filterableStateListDrawable.addState(StateSet.WILD_CARD, highlightDrawable, baseColorFilter);

        return filterableStateListDrawable;
    }

    /**
     * Returns a {@link FilterableStateListDrawable}, coloring the passed in
     * drawable according to the theme and the passed in highlight color
     *
     * @param baseDrawableResId the drawable to use
     * @return an {@link FilterableStateListDrawable}, coloring the passed in
     * drawable according to the theme and the passed in highlight color
     */
    public static Drawable getColoredStateListDrawable(Context context, int baseDrawableResId, int baseColor) {
        Drawable baseDrawable = context.getResources().getDrawable(baseDrawableResId);
        Drawable highlightDrawable = baseDrawable.getConstantState().newDrawable();
        ColorFilter baseColorFilter = new LightingColorFilter(baseColor, 0);
        ColorFilter highlightColorFilter = new LightingColorFilter(ColorUtils.getAccentColor(), 0);
        FilterableStateListDrawable filterableStateListDrawable = new FilterableStateListDrawable();
        filterableStateListDrawable.addState(new int[]{android.R.attr.state_pressed}, baseDrawable, highlightColorFilter);
        filterableStateListDrawable.addState(StateSet.WILD_CARD, highlightDrawable, baseColorFilter);

        return filterableStateListDrawable;
    }

    /**
     * Sets a drawable to the appropriate base color(light or dark) according to the theme
     *
     * @param drawable the drawable to theme
     * @return a colored drawable
     */
    public static Drawable themeLightOrDark(Context context, Drawable drawable) {
        int baseColor;
        if ((ThemeUtils.getInstance().themeType == ThemeUtils.ThemeType.TYPE_DARK)
                || (ThemeUtils.getInstance().themeType == ThemeUtils.ThemeType.TYPE_SOLID_DARK)
                || (ThemeUtils.getInstance().themeType == ThemeUtils.ThemeType.TYPE_SOLID_BLACK)) {
            baseColor = context.getResources().getColor(R.color.drawable_base_color_dark);
        } else {
            baseColor = context.getResources().getColor(R.color.drawable_base_color_light);
        }
        ColorFilter baseColorFilter = new LightingColorFilter(baseColor, 0);
        drawable.setColorFilter(baseColorFilter);
        return drawable;
    }


    /**
     * Returns a {@link FilterableStateListDrawable}, coloring the passed in
     * drawable according to the theme and the passed in highlight color
     *
     * @param baseDrawable the drawable to use
     * @return an {@link FilterableStateListDrawable}, coloring the passed in
     * drawable according to the theme and the passed in highlight color
     */
    public static Drawable getColoredStateListDrawable(Context context, Drawable baseDrawable) {
        if (baseDrawable == null) {
            return null;
        }
        Drawable highlightDrawable = baseDrawable.getConstantState().newDrawable();
        int baseColor;
        if ((ThemeUtils.getInstance().themeType == ThemeUtils.ThemeType.TYPE_DARK)
                || (ThemeUtils.getInstance().themeType == ThemeUtils.ThemeType.TYPE_SOLID_DARK)
                || (ThemeUtils.getInstance().themeType == ThemeUtils.ThemeType.TYPE_SOLID_BLACK)) {
            baseColor = context.getResources().getColor(R.color.drawable_base_color_dark);
        } else {
            baseColor = context.getResources().getColor(R.color.drawable_base_color_light);
        }
        ColorFilter baseColorFilter = new LightingColorFilter(baseColor, 0);
        ColorFilter highlightColorFilter = new LightingColorFilter(ColorUtils.getAccentColor(), 0);
        FilterableStateListDrawable filterableStateListDrawable = new FilterableStateListDrawable();
        filterableStateListDrawable.addState(new int[]{android.R.attr.state_pressed}, baseDrawable, highlightColorFilter);
        filterableStateListDrawable.addState(StateSet.WILD_CARD, highlightDrawable, baseColorFilter);

        return filterableStateListDrawable;
    }

    /**
     * Returns a {@link FilterableStateListDrawable}, coloring the passed in
     * drawable according to the theme and the passed in highlight color
     *
     * @param baseDrawable the drawable to use
     * @return an {@link FilterableStateListDrawable}, coloring the passed in
     * drawable according to the theme and the passed in highlight color
     */
    public static Drawable getColoredStateListDrawableWithThemeColor(Context context, Drawable baseDrawable, @ThemeUtils.ThemeColor int color) {
        if (baseDrawable == null) {
            return null;
        }
        Drawable highlightDrawable = baseDrawable.getConstantState().newDrawable();
        int baseColor;
        if (color == ThemeUtils.WHITE) {
            baseColor = context.getResources().getColor(R.color.drawable_base_color_dark);
        } else {
            baseColor = context.getResources().getColor(R.color.drawable_base_color_light);
        }
        ColorFilter baseColorFilter = new LightingColorFilter(baseColor, 0);
        int accentColor = ColorUtils.getAccentColor();
        if (accentColor == ColorUtils.getPrimaryColor()) {
            accentColor = Color.WHITE;
        }
        ColorFilter highlightColorFilter = new LightingColorFilter(accentColor, 0);
        FilterableStateListDrawable filterableStateListDrawable = new FilterableStateListDrawable();
        filterableStateListDrawable.addState(new int[]{android.R.attr.state_pressed}, baseDrawable, highlightColorFilter);
        filterableStateListDrawable.addState(StateSet.WILD_CARD, highlightDrawable, baseColorFilter);

        return filterableStateListDrawable;
    }

    /**
     * Returns a {@link FilterableStateListDrawable}, coloring the passed in
     * drawable according to the theme and the passed in highlight color
     *
     * @param baseDrawable the drawable to use
     * @return an {@link FilterableStateListDrawable}, coloring the passed in
     * drawable according to the theme and the passed in highlight color
     */
    public static Drawable getColoredStateListDrawable(Context context, Drawable baseDrawable, int highlightColor) {
        if (baseDrawable == null) {
            return null;
        }
        Drawable highlightDrawable = baseDrawable.getConstantState().newDrawable();
        ColorFilter baseColorFilter = new LightingColorFilter(highlightColor, 0);
        ColorFilter highlightColorFilter = new LightingColorFilter(ColorUtils.getAccentColor(), 0);
        FilterableStateListDrawable filterableStateListDrawable = new FilterableStateListDrawable();
        filterableStateListDrawable.addState(new int[]{android.R.attr.state_pressed}, baseDrawable, highlightColorFilter);
        filterableStateListDrawable.addState(StateSet.WILD_CARD, highlightDrawable, baseColorFilter);

        return filterableStateListDrawable;
    }

    /**
     * Returns a {@link FilterableStateListDrawable}, coloring the passed in
     * drawable according to the theme and the passed in highlight color
     *
     * @param baseDrawableResId the drawable to use
     * @return an {@link FilterableStateListDrawable}, coloring the passed in
     * drawable according to the theme and the passed in highlight color
     */
    public static Drawable getColoredStateListDrawableWithThemeType(Context context, int baseDrawableResId, @ThemeUtils.ThemeType int themeType) {
        Drawable baseDrawable = context.getResources().getDrawable(baseDrawableResId);
        Drawable highlightDrawable = baseDrawable.getConstantState().newDrawable();
        int baseColor;
        if ((themeType == ThemeUtils.ThemeType.TYPE_DARK)
                || (themeType == ThemeUtils.ThemeType.TYPE_SOLID_DARK)
                || (themeType == ThemeUtils.ThemeType.TYPE_SOLID_BLACK)) {
            baseColor = context.getResources().getColor(R.color.drawable_base_color_dark);
        } else {
            baseColor = context.getResources().getColor(R.color.drawable_base_color_light);
        }
        ColorFilter baseColorFilter = new LightingColorFilter(baseColor, 0);
        ColorFilter highlightColorFilter = new LightingColorFilter(ColorUtils.getPrimaryColor(), 0);
        FilterableStateListDrawable filterableStateListDrawable = new FilterableStateListDrawable();
        filterableStateListDrawable.addState(new int[]{android.R.attr.state_pressed}, baseDrawable, highlightColorFilter);
        filterableStateListDrawable.addState(StateSet.WILD_CARD, highlightDrawable, baseColorFilter);

        return filterableStateListDrawable;
    }

    /**
     * Takes a drawable and applies the current theme highlight color to it
     *
     * @param baseDrawable the drawable to theme
     * @return a themed {@link android.graphics.drawable.Drawable}
     */
    public static Drawable getColoredDrawable(Context context, Drawable baseDrawable) {
        if (baseDrawable == null) {
            return null;
        }
        baseDrawable = baseDrawable.getConstantState().newDrawable();
        ColorFilter highlightColorFilter = new LightingColorFilter(ColorUtils.getPrimaryColor(), 0);
        baseDrawable.mutate().setColorFilter(highlightColorFilter);
        return baseDrawable;
    }

    /**
     * Takes a drawable and applies the current theme accent color to it
     *
     * @param baseDrawable the drawable to theme
     * @return a themed {@link android.graphics.drawable.Drawable}
     */
    public static Drawable getColoredAccentDrawableNonWhite(Context context, Drawable baseDrawable) {
        return getColoredAccentDrawable(context, baseDrawable, false, false);
    }

    /**
     * Takes a drawable and applies the current theme accent color to it
     *
     * @param baseDrawable the drawable to theme
     * @return a themed {@link android.graphics.drawable.Drawable}
     */
    public static Drawable getColoredAccentDrawable(Context context, Drawable baseDrawable) {
        return getColoredAccentDrawable(context, baseDrawable, true);
    }

    /**
     * Takes a drawable and applies the current theme accent color to it
     *
     * @param baseDrawableResId the drawable to theme
     * @return a themed {@link android.graphics.drawable.Drawable}
     */
    public static Drawable getColoredAccentDrawable(Context context, int baseDrawableResId, boolean canFallBackToWhite) {
        Drawable drawable = context.getResources().getDrawable(baseDrawableResId);
        return getColoredAccentDrawable(context, drawable, canFallBackToWhite);
    }

    /**
     * Takes a drawable and applies the current theme accent color to it
     *
     * @param baseDrawable the drawable to theme
     * @return a themed {@link android.graphics.drawable.Drawable}
     */
    public static Drawable getColoredAccentDrawable(Context context, Drawable baseDrawable, boolean canFallBackToWhite) {
        return getColoredAccentDrawable(context, baseDrawable, canFallBackToWhite, true);
    }

    /**
     * Takes a drawable and applies the current theme accent color to it
     *
     * @param baseDrawable the drawable to theme
     * @return a themed {@link android.graphics.drawable.Drawable}
     */
    public static Drawable getColoredAccentDrawable(Context context, Drawable baseDrawable, boolean canFallBackToWhite, boolean usePrimary) {
        if (baseDrawable == null) {
            return null;
        }
        baseDrawable = baseDrawable.getConstantState().newDrawable();
        int accentColor = ColorUtils.getAccentColor();
        if (accentColor == ColorUtils.getPrimaryColor() && canFallBackToWhite) {
            accentColor = Color.WHITE;
        }
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (!canFallBackToWhite && sharedPreferences.getBoolean("pref_theme_white_accent", false)) {
            accentColor = ColorUtils.getAccentColor(false, true);
        }
        if (!canFallBackToWhite && usePrimary && sharedPreferences.getBoolean("pref_theme_white_accent", false)) {
            accentColor = ColorUtils.getPrimaryColor();
        }
        ColorFilter highlightColorFilter = new LightingColorFilter(accentColor, 0);
        baseDrawable.mutate().setColorFilter(highlightColorFilter);
        return baseDrawable;
    }

    /**
     * Takes a drawable and applies the current theme accent color to it
     *
     * @param baseDrawable the drawable to theme
     * @return a themed {@link android.graphics.drawable.Drawable}
     */
    public static Drawable getColoredAccentFABDrawable(Context context, Drawable baseDrawable) {
        if (baseDrawable == null) {
            return null;
        }
        baseDrawable = baseDrawable.getConstantState().newDrawable();
        ColorFilter highlightColorFilter = new LightingColorFilter(ColorUtils.getFloatingActionIconColor(context), 0);
        baseDrawable.mutate().setColorFilter(highlightColorFilter);
        return baseDrawable;
    }

    /**
     * Returns a {@link FilterableStateListDrawable}, coloring the passed in
     * drawable according to the theme and the passed in highlight color
     *
     * @param baseDrawable the drawable to use
     * @return an {@link FilterableStateListDrawable}, coloring the passed in
     * drawable according to the theme and the passed in highlight color
     */
    public static Drawable getColoredStateListDrawable(Context context, Drawable baseDrawable, boolean inverted) {
        if (baseDrawable == null) {
            return null;
        }
        Drawable highlightDrawable = baseDrawable.getConstantState().newDrawable();
        int baseColor;
        if (!inverted) {
            baseColor = context.getResources().getColor(R.color.drawable_base_color_dark);
        } else {
            baseColor = context.getResources().getColor(R.color.drawable_base_color_light);
        }
        ColorFilter baseColorFilter = new LightingColorFilter(baseColor, 0);
        ColorFilter highlightColorFilter = new LightingColorFilter(ColorUtils.getPrimaryColor(), 0);
        FilterableStateListDrawable filterableStateListDrawable = new FilterableStateListDrawable();
        filterableStateListDrawable.addState(new int[]{android.R.attr.state_pressed}, baseDrawable, highlightColorFilter);
        filterableStateListDrawable.addState(StateSet.WILD_CARD, highlightDrawable, baseColorFilter);

        return filterableStateListDrawable;
    }

    /**
     * Takes a drawable and applies the passed in color to it
     *
     * @param baseDrawable the drawable to theme
     * @return a themed {@link android.graphics.drawable.Drawable}
     */
    public static Drawable getColoredDrawable(Drawable baseDrawable, int color) {
        if (baseDrawable == null) {
            return null;
        }
        ColorFilter colorFilter = new LightingColorFilter(color, 0);
        baseDrawable.mutate().setColorFilter(colorFilter);
        return baseDrawable;
    }


    /**
     * Takes a drawable resource and applies the current theme highlight color to it
     *
     * @param baseDrawableResId the resource id of the drawable to theme
     * @return a themed {@link android.graphics.drawable.Drawable}
     */
    public static Drawable getColoredDrawable(Context context, int baseDrawableResId) {
        Drawable baseDrawable = context.getResources().getDrawable(baseDrawableResId).getConstantState().newDrawable();
        ColorFilter highlightColorFilter = new LightingColorFilter(ColorUtils.getPrimaryColor(), 0);
        baseDrawable.mutate().setColorFilter(highlightColorFilter);
        return baseDrawable;
    }

    /**
     * Takes a drawable resource and applies the current theme highlight color to it
     *
     * @param baseDrawableResId the resource id of the drawable to theme
     * @return a themed {@link android.graphics.drawable.Drawable}
     */
    public static Bitmap getColoredBitmap(Context context, int baseDrawableResId) {
        Drawable baseDrawable = context.getResources().getDrawable(baseDrawableResId).getConstantState().newDrawable();
        ColorFilter highlightColorFilter = new LightingColorFilter(ColorUtils.getPrimaryColor(), 0);
        baseDrawable.mutate().setColorFilter(highlightColorFilter);

        Bitmap bitmap = Bitmap.createBitmap(baseDrawable.getIntrinsicWidth(), baseDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        baseDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        baseDrawable.draw(canvas);
        return bitmap;
    }

    /**
     * Takes a drawable resource and turns it black
     *
     * @param baseDrawableResId the resource id of the drawable to theme
     * @return a themed {@link android.graphics.drawable.Drawable}
     */
    public static Bitmap getBlackBitmap(Context context, int baseDrawableResId) {
        Drawable baseDrawable = context.getResources().getDrawable(baseDrawableResId).getConstantState().newDrawable();
        ColorFilter colorFilter = new LightingColorFilter(context.getResources().getColor(R.color.black), 0);
        baseDrawable.mutate().setColorFilter(colorFilter);

        Bitmap bitmap = Bitmap.createBitmap(baseDrawable.getIntrinsicWidth(), baseDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        baseDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        baseDrawable.draw(canvas);
        return bitmap;
    }

    /**
     * Takes a drawable resource and turns it black
     *
     * @param baseDrawableResId the resource id of the drawable to theme
     * @return a themed {@link android.graphics.drawable.Drawable}
     */
    public static Bitmap getTintedNotificationDrawable(Context context, int baseDrawableResId) {

        boolean inverse = SettingsManager.getInstance().invertNotificationIcons();

        int colorResId = inverse ? R.color.notification_control_tint_inverse : R.color.notification_control_tint;
        if (ShuttleUtils.hasNougat()) {
            colorResId = inverse ? R.color.notification_control_tint_v24_inverse : R.color.notification_control_tint_v24;
        }
        int tintColor = context.getResources().getColor(colorResId);

        Drawable baseDrawable = context.getResources().getDrawable(baseDrawableResId);
        baseDrawable = DrawableCompat.wrap(baseDrawable);
        DrawableCompat.setTint(baseDrawable, tintColor);

        Bitmap bitmap = Bitmap.createBitmap(baseDrawable.getIntrinsicWidth(), baseDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        baseDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        baseDrawable.draw(canvas);
        return bitmap;
    }

    /**
     * Takes a drawable resource and turns it black
     *
     * @param baseDrawableResId the resource id of the drawable to theme
     * @return a themed {@link android.graphics.drawable.Drawable}
     */
    public static Drawable getBlackDrawable(Context context, int baseDrawableResId) {
        Drawable baseDrawable = context.getResources().getDrawable(baseDrawableResId).getConstantState().newDrawable();
        ColorFilter colorFilter = new LightingColorFilter(context.getResources().getColor(R.color.black), 0);
        baseDrawable.mutate().setColorFilter(colorFilter);
        return baseDrawable;
    }

    /**
     * Takes a drawable resource and turns it white
     *
     * @param baseDrawableResId the resource id of the drawable to theme
     * @return a themed {@link android.graphics.drawable.Drawable}
     */
    public static Drawable getWhiteDrawable(Context context, int baseDrawableResId) {
        if (context == null) {
            return null;
        }
        Drawable baseDrawable = context.getResources().getDrawable(baseDrawableResId).getConstantState().newDrawable();
        ColorFilter colorFilter = new LightingColorFilter(context.getResources().getColor(R.color.white), 0);
        baseDrawable.mutate().setColorFilter(colorFilter);
        return baseDrawable;
    }

    /**
     * Takes a drawable resource and colors it according to the base color of the theme
     *
     * @param baseDrawableResId the resource id of the drawable to theme
     * @return a themed {@link android.graphics.drawable.Drawable}
     */
    public static Drawable getBaseDrawable(Context context, int baseDrawableResId) {
        Drawable baseDrawable = context.getResources().getDrawable(baseDrawableResId).getConstantState().newDrawable();
        ColorFilter colorFilter = new LightingColorFilter(ThemeUtils.getBaseColor(context), 0);
        baseDrawable.mutate().setColorFilter(colorFilter);
        return baseDrawable;
    }

    /**
     * Takes a drawable resource and colors it according to the base color of the theme
     *
     * @param baseDrawableResId the resource id of the drawable to theme
     * @return a themed {@link android.graphics.drawable.Drawable}
     */
    public static Drawable getSemiTransparentBaseDrawable(Context context, int baseDrawableResId) {
        Drawable baseDrawable = context.getResources().getDrawable(baseDrawableResId).getConstantState().newDrawable();
        ColorFilter colorFilter = new LightingColorFilter(ThemeUtils.getBaseColor(context), 0);
        baseDrawable.mutate().setColorFilter(colorFilter);
        baseDrawable.setAlpha(155);
        return baseDrawable;
    }


    /**
     * @return an {@link FilterableStateListDrawable} representing the themed FastScroll thumb
     */
    public static Drawable getColoredFastScrollDrawable(Context context, boolean dontTint) {
        ColorFilter highlightColorFilter;
        if (dontTint) {
            highlightColorFilter = new LightingColorFilter(context.getResources().getColor(R.color.white), 0);
        } else {
            highlightColorFilter = new LightingColorFilter(ColorUtils.getAccentColor(), 0);
        }
        FilterableStateListDrawable stateListDrawable = new FilterableStateListDrawable();

        if (ShuttleUtils.hasLollipopMR1()) {
            stateListDrawable.addState(new int[]{android.R.attr.state_pressed}, CompatUtils.getDrawableCompat(context, R.drawable.fastscroll_thumb_material), highlightColorFilter);
            stateListDrawable.addState(StateSet.WILD_CARD, CompatUtils.getDrawableCompat(context, (R.drawable.fastscroll_thumb_material)), highlightColorFilter);
        } else if (ShuttleUtils.hasLollipop()) {
            stateListDrawable.addState(new int[]{android.R.attr.state_pressed}, context.getResources().getDrawable(R.drawable.fastscroll_thumb_mtrl_alpha), highlightColorFilter);
            stateListDrawable.addState(StateSet.WILD_CARD, CompatUtils.getDrawableCompat(context, (R.drawable.fastscroll_thumb_mtrl_alpha)), highlightColorFilter);
        } else {
            stateListDrawable.addState(new int[]{android.R.attr.state_pressed}, context.getResources().getDrawable(R.drawable.fastscroll_thumb_pressed), highlightColorFilter);
            stateListDrawable.addState(StateSet.WILD_CARD, CompatUtils.getDrawableCompat(context, (R.drawable.fastscroll_thumb_default)), highlightColorFilter);
        }

        return stateListDrawable;
    }


    public static Drawable getProgressDrawable(Context context, LayerDrawable layerDrawable) {
        layerDrawable.setDrawableByLayerId(android.R.id.progress, getColoredAccentDrawable(context, layerDrawable.findDrawableByLayerId(android.R.id.progress)));
        return layerDrawable;
    }

    public static Drawable getBackgroundDrawable(Context context) {
        if (ThemeUtils.getInstance().themeType == ThemeUtils.ThemeType.TYPE_LIGHT
                || ThemeUtils.getInstance().themeType == ThemeUtils.ThemeType.TYPE_SOLID_LIGHT) {
            return context.getResources().getDrawable(R.color.bg_light);
        } else if (ThemeUtils.getInstance().themeType == ThemeUtils.ThemeType.TYPE_DARK
                || ThemeUtils.getInstance().themeType == ThemeUtils.ThemeType.TYPE_SOLID_DARK) {
            return context.getResources().getDrawable(R.color.bg_dark);
        } else if (ThemeUtils.getInstance().themeType == ThemeUtils.ThemeType.TYPE_BLACK
                || ThemeUtils.getInstance().themeType == ThemeUtils.ThemeType.TYPE_SOLID_BLACK) {
            return context.getResources().getDrawable(R.color.bg_black);
        }
        return context.getResources().getDrawable(R.color.bg_light);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static Drawable getSwitchTrackDrawable(Context context) {

        // if (ShuttleUtils.hasAndroidLPreview()) {
        //Todo: Add color states (need method to determine accent color)

        Drawable offState = context.getResources().getDrawable(R.drawable.switch_track_mtrl_alpha);
        offState.setAlpha(1);

        Drawable onState = context.getResources().getDrawable(R.drawable.switch_track_mtrl_alpha);
        onState.setAlpha(1);

        Drawable otherState = context.getResources().getDrawable(R.drawable.switch_track_mtrl_alpha);
        offState.setAlpha(1);

        FilterableStateListDrawable filterableStateListDrawable = new FilterableStateListDrawable();
        filterableStateListDrawable.addState(new int[]{-android.R.attr.enabled}, offState, null);
        filterableStateListDrawable.addState(new int[]{android.R.attr.state_checked}, onState, null);
        filterableStateListDrawable.addState(StateSet.WILD_CARD, otherState, null);


        //Todo: Make filterableStateListDrawable take alpha values for individual drawables
        filterableStateListDrawable.setAlpha(255 / 3);

        return filterableStateListDrawable;

    }
}
