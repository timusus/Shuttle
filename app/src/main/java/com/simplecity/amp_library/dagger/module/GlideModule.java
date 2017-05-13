package com.simplecity.amp_library.dagger.module;

import android.content.Context;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;

import dagger.Module;
import dagger.Provides;

@Module
public class GlideModule {

    @Provides
    RequestManager provideRequestManager(Context context) {
        return Glide.with(context);
    }

}