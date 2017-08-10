package com.simplecity.amp_library.dagger.module;

import com.simplecity.amp_library.ui.views.multisheet.MultiSheetEventRelay;
import com.simplecity.amp_library.ui.views.multisheet.MultiSheetSlideEventRelay;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class MultiSheetModule {

    @Provides
    @Singleton
    MultiSheetEventRelay provideMultiSheetEventRelay() {
        return new MultiSheetEventRelay();
    }

    @Provides
    @Singleton
    MultiSheetSlideEventRelay provideMultiSheetSlideEventRelay() {
        return new MultiSheetSlideEventRelay();
    }

}