package com.simplecity.amp_library.dagger.module;

import android.content.Context;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.playback.MediaManager;
import com.simplecity.amp_library.utils.MusicUtils;
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

    @Provides
    @Singleton
    public MediaManager provideMediaManager() {
        return new MusicUtils();
    }
}