package com.simplecity.amp_library.dagger.component;


import com.simplecity.amp_library.dagger.module.AppModule;
import com.simplecity.amp_library.dagger.module.DrawerModule;
import com.simplecity.amp_library.dagger.module.GlideModule;
import com.simplecity.amp_library.dagger.module.ModelsModule;
import com.simplecity.amp_library.dagger.module.MultiSheetModule;
import com.simplecity.amp_library.dagger.module.PresenterModule;
import com.simplecity.amp_library.ui.activities.MainActivity;
import com.simplecity.amp_library.ui.drawer.DrawerFragment;
import com.simplecity.amp_library.ui.drawer.DrawerPresenter;
import com.simplecity.amp_library.ui.fragments.BaseFragment;
import com.simplecity.amp_library.ui.fragments.MainController;
import com.simplecity.amp_library.ui.fragments.MiniPlayerFragment;
import com.simplecity.amp_library.ui.fragments.PlayerFragment;
import com.simplecity.amp_library.ui.fragments.QueueFragment;
import com.simplecity.amp_library.ui.fragments.QueuePagerFragment;
import com.simplecity.amp_library.ui.presenters.PlayerPresenter;
import com.simplecity.amp_library.ui.presenters.QueuePagerPresenter;
import com.simplecity.amp_library.ui.presenters.QueuePresenter;
import com.simplecity.amp_library.ui.settings.SettingsParentFragment;
import com.simplecity.amp_library.ui.views.UpNextView;
import com.simplecity.amp_library.ui.views.multisheet.CustomMultiSheetView;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {
        AppModule.class,
        PresenterModule.class,
        GlideModule.class,
        DrawerModule.class,
        ModelsModule.class,
        MultiSheetModule.class})

public interface AppComponent {

    void inject(MainActivity target);

    void inject(PlayerPresenter target);

    void inject(QueuePagerPresenter target);

    void inject(QueuePresenter target);

    void inject(PlayerFragment target);

    void inject(MiniPlayerFragment target);

    void inject(QueuePagerFragment target);

    void inject(QueueFragment target);

    void inject(UpNextView target);

    void inject(DrawerFragment target);

    void inject(DrawerPresenter target);

    void inject(MainController target);

    void inject(CustomMultiSheetView target);

    void inject(BaseFragment target);

    void inject(SettingsParentFragment.SettingsFragment target);
}