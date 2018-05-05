package com.simplecity.amp_library.dagger.module;

import com.simplecity.amp_library.ui.views.multisheet.MultiSheetEventRelay;
import com.simplecity.amp_library.ui.views.multisheet.MultiSheetSlideEventRelay;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;

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