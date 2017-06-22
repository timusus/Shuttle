package com.simplecity.amp_library.dagger.component;


import com.simplecity.amp_library.dagger.module.AppModule;
import com.simplecity.amp_library.dagger.module.DrawerModule;
import com.simplecity.amp_library.dagger.module.FragmentModule;
import com.simplecity.amp_library.dagger.module.ModelsModule;
import com.simplecity.amp_library.dagger.module.MultiSheetModule;
import com.simplecity.amp_library.ui.fragments.LibraryController;
import com.simplecity.amp_library.ui.fragments.MainController;
import com.simplecity.amp_library.ui.views.UpNextView;
import com.simplecity.amp_library.ui.views.multisheet.CustomMultiSheetView;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {
        AppModule.class,
        ModelsModule.class,
        MultiSheetModule.class,
        DrawerModule.class})

public interface AppComponent {

    FragmentComponent plus(FragmentModule module);

    void inject(CustomMultiSheetView target);

    void inject(MainController target);

    void inject(LibraryController target);

    void inject(UpNextView target);
}