package com.simplecity.amp_library.glide.loader;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
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
    }

    @Nullable
    @Override
    public LoadData<InputStream> buildLoadData(@NonNull ArtworkProvider artworkProvider, int width, int height, @NonNull Options options) {
        return new MultiFetcher(model, allowOfflineDownload);
        return null;
    }

    @Override
    public boolean handles(@NonNull ArtworkProvider artworkProvider) {
        return false;
    }

    /**
     * The default factory for {@link ArtworkModelLoader}s.
     */
    public static class Factory implements ModelLoaderFactory<ArtworkProvider, InputStream> {

        @NonNull
        @Override
        public ModelLoader<ArtworkProvider, InputStream> build(@NonNull MultiModelLoaderFactory multiFactory) {
            return new ArtworkModelLoader(false);
        }

        @Override
        public void teardown() {
            // Do nothing.
        }
    }

}
