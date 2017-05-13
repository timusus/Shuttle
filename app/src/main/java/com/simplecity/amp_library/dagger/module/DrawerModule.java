package com.simplecity.amp_library.dagger.module;

import com.simplecity.amp_library.ui.drawer.DrawerEventRelay;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class DrawerModule {

    @Provides
    @Singleton
    DrawerEventRelay provideDrawerEventRelay() {
        return new DrawerEventRelay();
    }

}