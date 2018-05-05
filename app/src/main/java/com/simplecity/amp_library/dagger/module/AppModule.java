package com.simplecity.amp_library.dagger.module;

import android.content.Context;
import com.simplecity.amp_library.ShuttleApplication;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;

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
