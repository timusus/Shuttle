package com.simplecity.amp_library.glide.palette;

import android.content.Context;
import android.graphics.Bitmap;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;

public class ColorSetTranscoder implements ResourceTranscoder<Bitmap, ColorSet> {

    private Context context;

    public ColorSetTranscoder(Context context) {
        this.context = context;
    }

    @Override
    public Resource<ColorSet> transcode(Resource<Bitmap> toTranscode) {
        Bitmap bitmap = toTranscode.get();

        return new ColorSetResource(ColorSet.Companion.fromBitmap(context, bitmap));
    }

    @Override
    public String getId() {
        return ColorSetTranscoder.class.getName();
    }
}