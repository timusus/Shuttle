package com.simplecity.amp_library.dagger.component;

import com.simplecity.amp_library.dagger.module.ActivityModule;
import com.simplecity.amp_library.dagger.module.AppModule;
import com.simplecity.amp_library.dagger.module.DrawerModule;
import com.simplecity.amp_library.dagger.module.ModelsModule;
import com.simplecity.amp_library.dagger.module.MultiSheetModule;
import dagger.Component;
import javax.inject.Singleton;

@Singleton
@Component(modules = {
        AppModule.class,
        ModelsModule.class,
        MultiSheetModule.class,
        DrawerModule.class
})

public interface AppComponent {

    ActivityComponent plus(ActivityModule module);
}
