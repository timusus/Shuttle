package com.simplecity.amp_library.glide.loader;

import android.content.Context;

import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.GenericLoaderFactory;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.simplecity.amp_library.glide.fetcher.MultiFetcher;
import com.simplecity.amp_library.model.ArtworkProvider;

import java.io.InputStream;

public class ArtworkModelLoader implements ModelLoader<ArtworkProvider, InputStream> {

    private boolean allowOfflineDownload;

    public ArtworkModelLoader(boolean allowOfflineDownload){
        this.allowOfflineDownload = allowOfflineDownload;
    }

    private static final String TAG = "ArtworkModelLoader";

    @Override
    public DataFetcher<InputStream> getResourceFetcher(ArtworkProvider model, int width, int height) {
        return new MultiFetcher(model, allowOfflineDownload);
    }

    /**
     * The default factory for {@link ArtworkModelLoader}s.
     */
    public static class Factory implements ModelLoaderFactory<ArtworkProvider, InputStream> {

        @Override
        public ModelLoader<ArtworkProvider, InputStream> build(Context context, GenericLoaderFactory factories) {
            return new ArtworkModelLoader(false);
        }

        @Override
        public void teardown() {
            // Do nothing.
        }
    }

}
