package com.simplecity.amp_library.dagger.component;


import com.simplecity.amp_library.dagger.module.*;
import dagger.Component;

import javax.inject.Singleton;

@Singleton
@Component(modules = {
        AppModule.class,
        ModelsModule.class,
        MultiSheetModule.class,
        DrawerModule.class})

public interface AppComponent {

    ActivityComponent plus(ActivityModule module);

}
