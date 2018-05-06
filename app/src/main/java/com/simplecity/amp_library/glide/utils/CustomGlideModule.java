package com.simplecity.amp_library.glide.utils;

import android.content.Context;
import android.support.annotation.NonNull;
import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import com.bumptech.glide.module.AppGlideModule;
import com.simplecity.amp_library.glide.loader.ArtworkModelLoader;
import com.simplecity.amp_library.model.ArtworkProvider;
import java.io.InputStream;

public class CustomGlideModule extends AppGlideModule {
    @Override
    public void applyOptions(Context context, GlideBuilder builder) {

    }

    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
        registry.append(ArtworkProvider.class, InputStream.class, new ArtworkModelLoader.Factory());
    }
}