package com.simplecity.amp_library.dagger.module;

import com.simplecity.amp_library.ui.drawer.NavigationEventRelay;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;

@Module
public class DrawerModule {

    @Provides
    @Singleton
    NavigationEventRelay provideDrawerEventRelay() {
        return new NavigationEventRelay();
    }
}