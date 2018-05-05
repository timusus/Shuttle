package com.simplecity.amp_library.glide.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.StreamBitmapDecoder;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public class BitmapAndSizeDecoder implements ResourceDecoder<InputStream, BitmapAndSize> {
    private final ResourceDecoder<InputStream, Bitmap> bitmapDecoder;

    private BitmapPool pool;

    public BitmapAndSizeDecoder(Context context) {
        this(context, new StreamBitmapDecoder(context));
    }

    public BitmapAndSizeDecoder(Context context, ResourceDecoder<InputStream, Bitmap> bitmapDecoder) {
        this.bitmapDecoder = bitmapDecoder;
        pool = Glide.get(context).getBitmapPool();
    }

    @Override
    public Resource<BitmapAndSize> decode(InputStream source, int width, int height) throws IOException {
        if (!source.markSupported()) {
            source = new BufferedInputStream(source);
        }

        //Cap the size of the decoded bitmap to a max dimension of 2048px. Last.fm can return some pretty
        //massive images, so we just downsize the image here before it is disk cached, saving disk cache space
        //and speeding up future down-sampling.

        //The just decode bounds options only needs to determine the dimensions of the image from the EXIF
        //data at the top of the file. We set the stream mark at 100kb, meaning we're going to remember 100kb
        //of data and reset the stream when we've finished. 100kb should be enough to allow for the case where
        //thumbnail data is stored before EXIF data in JPEG images.
        source.mark(100 * 2048);
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(source, null, opt);
        source.reset();

        Resource<Bitmap> bitmap = bitmapDecoder.decode(source, width, height);

        return BitmapAndSizeResource.obtain(bitmap.get(), new Size(opt.outWidth, opt.outHeight), pool);
    }

    @Override
    public String getId() {
        return getClass().getName();
    }
}