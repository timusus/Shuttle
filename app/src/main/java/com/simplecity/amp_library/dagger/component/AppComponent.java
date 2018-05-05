package com.simplecity.amp_library.dagger.component;

import com.simplecity.amp_library.dagger.module.*;
import com.simplecity.amp_library.ui.views.multisheet.CustomMultiSheetView;
import dagger.Component;

import javax.inject.Singleton;

@Singleton
@Component(modules = {
        AppModule.class,
        ModelsModule.class,
        DrawerModule.class
})

public interface AppComponent {

    ActivityComponent plus(ActivityModule module);

    void inject(CustomMultiSheetView target);

}
