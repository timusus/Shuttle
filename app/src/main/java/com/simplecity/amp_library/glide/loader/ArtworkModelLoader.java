package com.simplecity.amp_library.glide.loader;

import android.content.Context;
import android.preference.PreferenceManager;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.GenericLoaderFactory;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.simplecity.amp_library.glide.fetcher.MultiFetcher;
import com.simplecity.amp_library.model.ArtworkProvider;
import com.simplecity.amp_library.utils.SettingsManager;
import java.io.InputStream;

public class ArtworkModelLoader implements ModelLoader<ArtworkProvider, InputStream> {

    private boolean allowOfflineDownload;

    private Context applicationContext;

    private SettingsManager settingsManager;

    public ArtworkModelLoader(Context context, boolean allowOfflineDownload) {
        this.applicationContext = context.getApplicationContext();
        this.allowOfflineDownload = allowOfflineDownload;
        this.settingsManager = new SettingsManager(PreferenceManager.getDefaultSharedPreferences(context));
    }

    private static final String TAG = "ArtworkModelLoader";

    @Override
    public DataFetcher<InputStream> getResourceFetcher(ArtworkProvider model, int width, int height) {
        return new MultiFetcher(applicationContext, model, settingsManager, allowOfflineDownload);
    }

    /**
     * The default factory for {@link ArtworkModelLoader}s.
     */
    public static class Factory implements ModelLoaderFactory<ArtworkProvider, InputStream> {

        @Override
        public ModelLoader<ArtworkProvider, InputStream> build(Context context, GenericLoaderFactory factories) {
            return new ArtworkModelLoader(context, false);
        }

        @Override
        public void teardown() {
            // Do nothing.
        }
    }
}
