package com.simplecity.amp_library.glide.fetcher;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.simplecity.amp_library.lastfm.LastFmResult;
import com.simplecity.amp_library.model.ArtworkProvider;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

class LastFmFetcher extends BaseRemoteFetcher {

    private static final String TAG = "LastFmFetcher";

    LastFmFetcher(ArtworkProvider artworkProvider) {
        super(artworkProvider);
    }

    @Override
    protected String getTag() {
        return TAG;
    }

    @Override
    String getUrl() throws IOException {
        retrofitCall = artworkProvider.getLastFmArtwork();
        if (retrofitCall == null) return null;
        return ((LastFmResult) retrofitCall.execute().body()).getImageUrl();
    }

    @Override
    protected InputStream getStream() throws IOException {

        InputStream inputStream = super.getStream();

        if (!inputStream.markSupported()) {
            inputStream = new BufferedInputStream(inputStream);
        }

        //The just decode bounds options only needs to determine the dimensions of the image from the EXIF
        //data at the top of the file. We set the stream mark at 100kb, meaning we're going to remember 100kb
        //of data and reset the stream when we've finished. 100kb should be enough to allow for the case where
        //thumbnail data is stored before EXIF data in JPEG images.
        inputStream.mark(100 * 1024);
        
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(inputStream, null, opt);
        inputStream.reset();

        opt.inSampleSize = Math.max(1, Integer.highestOneBit(Math.max(opt.outWidth, opt.outHeight) / 1024));
        
        //If we don't need to do any sampling, just return the input stream.
        if (opt.inSampleSize == 1) return inputStream;

        opt.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, opt);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);
        bitmap.recycle();
        //noinspection UnusedAssignment Allow GC to free up bitmap resources.
        bitmap = null;

        return new ByteArrayInputStream(outputStream.toByteArray());
    }
}