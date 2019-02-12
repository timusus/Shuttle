package com.simplecity.amp_library.ui.widgets;

import android.support.v4.app.Fragment;
import com.simplecity.amp_library.di.app.activity.fragment.FragmentModule;
import com.simplecity.amp_library.di.app.activity.fragment.FragmentScope;
import com.simplecity.amp_library.ui.screens.main.LibraryController;
import com.simplecity.amp_library.ui.screens.widgets.WidgetFragment;
import dagger.Binds;
import dagger.Module;
import javax.inject.Named;

@Module(includes = FragmentModule.class)
public abstract class WidgetFragmentModule {

    @Binds
    @Named(FragmentModule.FRAGMENT)
    @FragmentScope
    abstract Fragment fragment(WidgetFragment widgetFragment);
}