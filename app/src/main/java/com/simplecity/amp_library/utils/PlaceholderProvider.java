package com.simplecity.amp_library.utils;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.TextPaint;
import android.text.TextUtils;
import com.afollestad.aesthetic.Aesthetic;
import com.afollestad.aesthetic.Rx;
import com.simplecity.amp_library.R;

public class PlaceholderProvider {

    private static PlaceholderProvider instance;

    private Context applicationContext;

    private final TextPaint paint = new TextPaint();
    private final TypedArray colors;

    private boolean isDark = false;

    public static PlaceholderProvider getInstance(Context context) {
        if (instance == null) {
            instance = new PlaceholderProvider(context);
        }
        return instance;
    }

    private PlaceholderProvider(Context context) {
        this.applicationContext = context.getApplicationContext();
        paint.setTypeface(TypefaceManager.getInstance().getTypeface(applicationContext, TypefaceManager.SANS_SERIF_LIGHT));
        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setAntiAlias(true);
        colors = applicationContext.getResources().obtainTypedArray(R.array.pastel_colors);

        Aesthetic.get(applicationContext).isDark()
                .compose(Rx.distinctToMainThread())
                .subscribe(isDark -> this.isDark = isDark);
    }

    /**
     * @param displayName The name used to create the letter for the tile
     * @return A {@link Drawable} that contains a letter used in the English
     * alphabet or digit, if there is no letter or digit available, a
     * default image is shown instead
     */
    public Drawable getLetterTile(String displayName) {
        return new LetterDrawable(displayName, colors, paint);
    }

    @DrawableRes
    public int getMediumPlaceHolderResId() {
        return isDark ? R.drawable.ic_placeholder_dark_medium : R.drawable.ic_placeholder_light_medium;
    }

    @DrawableRes
    private int getLargePlaceHolderResId() {
        return isDark ? R.drawable.ic_placeholder_dark_large : R.drawable.ic_placeholder_light_large;
    }

    public Drawable getPlaceHolderDrawable(@Nullable String displayName, boolean large, SettingsManager settingsManager) {
        Drawable drawable;
        if (!TextUtils.isEmpty(displayName) && settingsManager.useGmailPlaceholders()) {
            drawable = PlaceholderProvider.getInstance(applicationContext).getLetterTile(displayName);
        } else {
            drawable = ContextCompat.getDrawable(applicationContext, large ? getLargePlaceHolderResId() : getMediumPlaceHolderResId());
        }
        return drawable;
    }
}
