package com.simplecity.amp_library.dagger.module;

import android.app.Activity;
import com.simplecity.amp_library.dagger.scope.ActivityScope;
import com.simplecity.amp_library.playback.MediaManager;
import com.simplecity.amp_library.ui.activities.BaseActivity;
import dagger.Module;
import dagger.Provides;

@Module
public class ActivityModule {

    private Activity activity;

    public ActivityModule(Activity activity) {
        this.activity = activity;
    }

    @Provides
    @ActivityScope
    Activity provideActivity() {
        return activity;
    }

    @Provides
    @ActivityScope
    MediaManager provideMediaManager() {
        return ((BaseActivity) activity).getMusicUtils();
    }
}