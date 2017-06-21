package com.simplecity.amp_library.utils;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.text.TextPaint;
import android.text.TextUtils;

import com.afollestad.aesthetic.Aesthetic;
import com.afollestad.aesthetic.Rx;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ShuttleApplication;


public class PlaceholderProvider {

    private static PlaceholderProvider instance;

    private final TextPaint paint = new TextPaint();
    private final TypedArray colors;

    private boolean isDark = false;

    public static PlaceholderProvider getInstance() {
        if (instance == null) {
            instance = new PlaceholderProvider();
        }
        return instance;
    }

    private PlaceholderProvider() {
        final Resources res = ShuttleApplication.getInstance().getResources();
        paint.setTypeface(TypefaceManager.getInstance().getTypeface(TypefaceManager.SANS_SERIF_LIGHT));
        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setAntiAlias(true);
        colors = res.obtainTypedArray(R.array.pastel_colors);

        Aesthetic.get().isDark()
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

    public Drawable getPlaceHolderDrawable(@Nullable String displayName, boolean large) {
        Drawable drawable;
        if (!TextUtils.isEmpty(displayName) && SettingsManager.getInstance().useGmailPlaceholders()) {
            drawable = PlaceholderProvider.getInstance().getLetterTile(displayName);
        } else {
            drawable = ShuttleApplication.getInstance().getResources().getDrawable(large ? getLargePlaceHolderResId() : getMediumPlaceHolderResId());
        }
        return drawable;
    }
}
