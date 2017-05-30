package com.simplecity.amp_library.glide.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.utils.PlaceholderProvider;
import com.simplecity.amp_library.utils.SettingsManager;

public class GlideUtils {

    private GlideUtils() {

    }

    @DrawableRes
    public static int getMediumPlaceHolderResId() {
        //Todo: Return correctly themed placeholder
        return  R.drawable.ic_placeholder_dark_medium;
    }

    @DrawableRes
    public static int getLargePlaceHolderResId() {
        //Todo: Return correctly themed placeholder
        return R.drawable.ic_placeholder_dark_large ;
    }

    public static Drawable getPlaceHolderDrawable(@Nullable String displayName, boolean large) {
        Drawable drawable;
        if (!TextUtils.isEmpty(displayName) && SettingsManager.getInstance().useGmailPlaceholders()) {
            drawable = PlaceholderProvider.getInstance().getLetterTile(displayName);
        } else {
            drawable = ShuttleApplication.getInstance().getResources().getDrawable(large ? getLargePlaceHolderResId() : getMediumPlaceHolderResId());
        }
        return drawable;
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }


}