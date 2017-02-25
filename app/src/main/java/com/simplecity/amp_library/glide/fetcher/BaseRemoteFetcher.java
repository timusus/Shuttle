package com.simplecity.amp_library.glide.fetcher;

import android.text.TextUtils;
import android.util.Log;

import com.bumptech.glide.util.ContentLengthInputStream;
import com.simplecity.amp_library.http.HttpClient;
import com.simplecity.amp_library.model.ArtworkProvider;

import java.io.IOException;
import java.io.InputStream;

import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Call;

abstract class BaseRemoteFetcher extends BaseFetcher {

    Call retrofitCall;

    private okhttp3.Call okHttpCall;

    private Response response;

    BaseRemoteFetcher(ArtworkProvider artworkProvider) {
        super(artworkProvider);
    }

    abstract String getUrl() throws IOException;

    @Override
    protected InputStream getStream() throws IOException {
        try {
            if (!TextUtils.isEmpty(getUrl())) {
                Request request = new Request.Builder().url(getUrl()).build();
                okHttpCall = HttpClient.getInstance().okHttpClient.newCall(request);

                response = okHttpCall.execute();
                if (response.isSuccessful()) {
                    long contentLength = response.body().contentLength();
                    stream = ContentLengthInputStream.obtain(response.body().byteStream(), contentLength);
                    return stream;
                } else {
                    response.close();
                }
            }
        } catch (IOException e) {
            Log.e(getTag(), "getStream() failed: " + e.toString());
        }

        return stream;
    }

    @Override
    public void cleanup() {
        super.cleanup();
        if (response != null) {
            response.close();
        }
    }

    @Override
    public void cancel() {
        super.cancel();
        if (retrofitCall != null) {
            retrofitCall.cancel();
        }
        if (okHttpCall != null) {
            okHttpCall.cancel();
        }
    }
}
