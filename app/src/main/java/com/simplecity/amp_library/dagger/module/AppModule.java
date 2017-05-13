package com.simplecity.amp_library.dagger.module;

import android.content.Context;

import com.simplecity.amp_library.ShuttleApplication;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class AppModule {

    private ShuttleApplication application;

    public AppModule(ShuttleApplication application) {
        this.application = application;
    }

    @Provides
    @Singleton
    public Context provideContext() {
        return application;
    }

}