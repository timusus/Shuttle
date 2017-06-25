package com.simplecity.amp_library.dagger.module;

import com.simplecity.amp_library.ui.drawer.NavigationEventRelay;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class DrawerModule {

    @Provides
    @Singleton
    NavigationEventRelay provideDrawerEventRelay() {
        return new NavigationEventRelay();
    }

}