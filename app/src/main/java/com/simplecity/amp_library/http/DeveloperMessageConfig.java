package com.simplecity.amp_library.http;

import android.os.Build;
import androidx.annotation.NonNull;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ShuttleApplication;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
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

    @SerializedName("enabled")
    public boolean enabled = false;

    @SerializedName("saleEndEpochMillis")
    public long saleEndEpochMillis = 0;

    /**
     * Starts the fetch and returns the call so the caller can cancel it.
     */
    public static Call fetch(Listener listener) {
        OkHttpClient.Builder builder = HttpClient.getInstance().okHttpClient.newBuilder()
                .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (Build.VERSION.SDK_INT < 24) {
            // Network security config is ignored before API 24; trust the system roots plus the bundled ISRG roots.
            try {
                X509TrustManager trustManager = createTrustManager();
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, new TrustManager[] { trustManager }, null);
                builder.sslSocketFactory(sslContext.getSocketFactory(), trustManager);
            } catch (Exception e) {
                // Keep the default client (system roots only).
            }
        }
        OkHttpClient client = builder.build();
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

    /**
     * A trust manager for the system roots plus the bundled ISRG Root X2 and Root YE certificates.
     */
    private static X509TrustManager createTrustManager() throws Exception {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        int[] rawResources = { R.raw.isrg_root_x2, R.raw.isrg_root_ye };
        for (int i = 0; i < rawResources.length; i++) {
            try (InputStream in = ShuttleApplication.getInstance().getResources().openRawResource(rawResources[i])) {
                Certificate certificate = certificateFactory.generateCertificate(in);
                keyStore.setCertificateEntry("isrg_root_" + i, certificate);
            }
        }

        X509TrustManager systemTrustManager = trustManagerFor(null);
        X509TrustManager bundledTrustManager = trustManagerFor(keyStore);
        return new X509TrustManager() {
            @Override
            public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws java.security.cert.CertificateException {
                systemTrustManager.checkClientTrusted(chain, authType);
            }

            @Override
            public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws java.security.cert.CertificateException {
                try {
                    systemTrustManager.checkServerTrusted(chain, authType);
                } catch (java.security.cert.CertificateException e) {
                    bundledTrustManager.checkServerTrusted(chain, authType);
                }
            }

            @Override
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return systemTrustManager.getAcceptedIssuers();
            }
        };
    }

    private static X509TrustManager trustManagerFor(KeyStore keyStore) throws Exception {
        TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        factory.init(keyStore);
        for (TrustManager trustManager : factory.getTrustManagers()) {
            if (trustManager instanceof X509TrustManager) {
                return (X509TrustManager) trustManager;
            }
        }
        throw new IllegalStateException("No X509TrustManager");
    }
}
