/*
 * Copyright (C) 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.libraries.cast.companionlibrary.utils;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * An AsyncTask to fetch an image over HTTP and scale it to the desired size. Clients need to extend
 * this and implement their own {@code onPostExecute(Bitmap bitmap)} method. It provides a uniform
 * treatment of ThreadPool across various versions of Android.
 */
public abstract class FetchBitmapTask extends AsyncTask<Uri, Void, Bitmap> {
    private final int mPreferredWidth;
    private final int mPreferredHeight;
    private final boolean mAllowedToScale;

    /**
     * Constructs a new FetchBitmapTask that will do in-sampling and scaling, if needed.
     *
     * @param preferredWidth The preferred image width.
     * @param preferredHeight The preferred image height.
     * @param allowedToScale If {@code true}, the resulting bitmap will be scaled to match the
     * preferred dimensions while keeping the aspect ratio. Otherwise, no additional scaling will
     * be performed.
     */
    public FetchBitmapTask(int preferredWidth, int preferredHeight, boolean allowedToScale) {
        mPreferredWidth = preferredWidth;
        mPreferredHeight = preferredHeight;
        mAllowedToScale = allowedToScale;
    }

    /**
     * Constructs a new FetchBitmapTask that will do in-sampling but no scaling.
     *
     * @param preferredWidth The preferred image width.
     * @param preferredHeight The preferred image height.
     *
     * @see FetchBitmapTask#FetchBitmapTask(int, int, boolean)
     */
    public FetchBitmapTask(int preferredWidth, int preferredHeight) {
        this(preferredWidth, preferredHeight, false);
    }

    /**
     * Constructs a new FetchBitmapTask. No scaling or in-sampling will be performed if you use this
     * constructor.
     *
     * @see FetchBitmapTask#FetchBitmapTask(int, int)
     * @see FetchBitmapTask#FetchBitmapTask(int, int, boolean)
     */
    public FetchBitmapTask() {
        this(0, 0);
    }

    @Override
    protected Bitmap doInBackground(Uri... uris) {
        if (uris.length != 1 || uris[0] == null) {
            return null;
        }

        Bitmap bitmap = null;
        URL url;
        try {
            url = new URL(uris[0].toString());
        } catch (MalformedURLException e) {
            return null;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = false;
        options.inSampleSize = 1;
        if ((mPreferredWidth > 0) && (mPreferredHeight > 0)) {
            // This is done to do appropriate resampling when the image is too large for the
            // desired target size; instead of downloading the original image and resizing that
            // (which can run into OOM exception), we find an appropriate in-sample-size and
            // only adjust the options to download the resized version.
            Point originalSize = calculateOriginalDimensions(url);
            if (originalSize.x > 0 && originalSize.y > 0) {
                options.inSampleSize = calculateInSampleSize(originalSize.x, originalSize.y,
                        mPreferredWidth, mPreferredHeight);
            }
        }
        HttpURLConnection urlConnection = null;
        InputStream stream = null;
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setDoInput(true);

            if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                stream = new BufferedInputStream(urlConnection.getInputStream());
                bitmap = BitmapFactory.decodeStream(stream, null, options);
                if ((mPreferredWidth > 0) && (mPreferredHeight > 0) && mAllowedToScale) {
                    bitmap = scaleBitmap(bitmap);
                }
            }
        } catch (IOException e) { /* ignore */
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) { /* ignore */
                }
            }
        }

        return bitmap;
    }

    /**
     * Executes the task.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void execute(Uri uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, uri);
        } else {
            execute(new Uri[] {uri});
        }
    }

    /*
     * Scales the bitmap to the preferred width and height.
     *
     * @param bitmap The bitmap to scale.
     * @return The scaled bitmap.
     */
    private Bitmap scaleBitmap(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        // Calculate deltas.
        int dw = width - mPreferredWidth;
        int dh = height - mPreferredHeight;

        if ((dw == 0) && (dh == 0)) {
            return bitmap;
        }

        float scaleWidth = (float) mPreferredWidth / width;
        float scaleHeight = (float) mPreferredHeight / height;
        float scaleFactor = Math.min(scaleHeight, scaleWidth);

        int finalWidth = (int) ((width * scaleFactor) + 0.5f);
        int finalHeight = (int) ((height * scaleFactor) + 0.5f);

        return Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, false);
    }

    /**
     * Returns the original size of the image.
     */
    private Point calculateOriginalDimensions(URL url) {
        int inSampleSize = 0;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        options.inSampleSize = inSampleSize;
        HttpURLConnection connection = null;
        InputStream stream = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            stream = connection.getInputStream();
            BitmapFactory.decodeStream(stream, null, options);
            return new Point(options.outWidth, options.outHeight);
        } catch (IOException e) {
             /* ignore */
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) { /* ignore */
                }
            }
        }
        return new Point(0, 0);
    }

    /**
     * Find the appropriate in-sample-size (as an inverse power of 2) to help reduce the size of
     * downloaded image.
     */
    private int calculateInSampleSize(int originalWidth, int originalHeight,
            int reqWidth, int reqHeight) {
        // Raw height and width of image
        int inSampleSize = 1;

        if (originalHeight > reqHeight || originalWidth > reqWidth) {

            final int halfHeight = originalHeight / 2;
            final int halfWidth = originalWidth / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
}
