package com.simplecity.amp_library.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import com.afollestad.aesthetic.Aesthetic;
import com.simplecity.amp_library.R;

public class DrawableUtils {

    /**
     * Takes a drawable resource and applies the current theme highlight color to it
     *
     * @param baseDrawableResId the resource id of the drawable to theme
     * @return a themed {@link android.graphics.drawable.Drawable}
     */
    public static Bitmap getColoredBitmap(Context context, int baseDrawableResId) {
        Drawable baseDrawable = ContextCompat.getDrawable(context, baseDrawableResId).getConstantState().newDrawable();
        ColorFilter highlightColorFilter = new LightingColorFilter(Aesthetic.get(context).colorPrimary().blockingFirst(), 0);
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
        Drawable baseDrawable = ContextCompat.getDrawable(context, baseDrawableResId).getConstantState().newDrawable();
        ColorFilter colorFilter = new LightingColorFilter(ContextCompat.getColor(context, R.color.black), 0);
        baseDrawable.mutate().setColorFilter(colorFilter);

        Bitmap bitmap = Bitmap.createBitmap(baseDrawable.getIntrinsicWidth(), baseDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        baseDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        baseDrawable.draw(canvas);
        return bitmap;
    }
}