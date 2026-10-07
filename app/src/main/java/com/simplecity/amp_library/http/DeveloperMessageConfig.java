package com.simplecity.amp_library.http;

import androidx.annotation.NonNull;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Remote switch for the once-only developer message. Format: {"enabled": false, "saleEndEpochMillis": 0}
 */
public class DeveloperMessageConfig {

    public static final String URL = "https://shuttlemusicplayer.com/legacy/developer-message.json";

    private static final long TIMEOUT_SECONDS = 5;

    public interface Listener {
        /**
         * Called on a background thread. Not called when the config could not be fetched or parsed.
         */
        void onConfig(@NonNull DeveloperMessageConfig config);

        /**
         * Called on a background thread, including when the call was cancelled.
         */
        void onFailure();
    }

    public boolean enabled = false;

    public long saleEndEpochMillis = 0;

    /**
     * Starts the fetch and returns the call so the caller can cancel it.
     */
    public static Call fetch(Listener listener) {
        OkHttpClient client = HttpClient.getInstance().okHttpClient.newBuilder()
                .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();
        Call call = client.newCall(new Request.Builder().url(URL).build());
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                listener.onFailure();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try (ResponseBody body = response.body()) {
                    if (response.code() != 200 || body == null) {
                        listener.onFailure();
                        return;
                    }
                    DeveloperMessageConfig config = new Gson().fromJson(body.string(), DeveloperMessageConfig.class);
                    if (config == null) {
                        listener.onFailure();
                        return;
                    }
                    listener.onConfig(config);
                } catch (Exception e) {
                    // IO errors and malformed JSON: treat as not enabled, retry on a later launch.
                    listener.onFailure();
                }
            }
        });
        return call;
    }
}
