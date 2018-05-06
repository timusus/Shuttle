package com.simplecity.amp_library.glide.palette;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.graphics.Palette;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;

public class PaletteBitmapTranscoder implements ResourceTranscoder<Bitmap, PaletteBitmap> {
    private final BitmapPool bitmapPool;

    public PaletteBitmapTranscoder(@NonNull Context context) {
        this.bitmapPool = Glide.get(context).getBitmapPool();
    }

    @Nullable
    @Override
    public Resource<PaletteBitmap> transcode(@NonNull Resource<Bitmap> toTranscode, @NonNull Options options) {
        Bitmap bitmap = toTranscode.get();
        Palette palette = new Palette.Builder(bitmap).generate();
        PaletteBitmap result = new PaletteBitmap(bitmap, palette);
        return new PaletteBitmapResource(result, bitmapPool);
    }


    public String getId() {
        return PaletteBitmapTranscoder.class.getName();
    }
}