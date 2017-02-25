package com.simplecity.amp_library.utils;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;

import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ShuttleApplication;


public class PlaceholderProvider {

    private static PlaceholderProvider sInstance;

    private final TextPaint paint = new TextPaint();
    private final TypedArray colors;

    public static PlaceholderProvider getInstance() {
        if (sInstance == null) {
            sInstance = new PlaceholderProvider();
        }
        return sInstance;
    }

    private PlaceholderProvider() {
        final Resources res = ShuttleApplication.getInstance().getResources();
        paint.setTypeface(TypefaceManager.getInstance().getTypeface(TypefaceManager.SANS_SERIF_LIGHT));
        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setAntiAlias(true);
        colors = res.obtainTypedArray(R.array.pastel_colors);
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
}
