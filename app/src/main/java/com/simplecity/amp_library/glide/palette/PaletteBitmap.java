package com.simplecity.amp_library.glide.palette;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.v7.graphics.Palette;

public class PaletteBitmap {
    public final Palette palette;
    public final Bitmap bitmap;

    public PaletteBitmap(@NonNull Bitmap bitmap, @NonNull Palette palette) {
        this.bitmap = bitmap;
        this.palette = palette;
    }
}