package com.simplecity.amp_library.ui.settings;

        import android.support.v4.app.Fragment;
        import com.simplecity.amp_library.di.app.activity.fragment.FragmentModule;
        import com.simplecity.amp_library.di.app.activity.fragment.FragmentScope;
        import dagger.Binds;
        import dagger.Module;
        import javax.inject.Named;

@Module(includes = FragmentModule.class)
public abstract class SettingsFragmentModule {

    @Binds
    @Named(FragmentModule.FRAGMENT)
    @FragmentScope
    abstract Fragment fragment(SettingsParentFragment.SettingsFragment settingsFragment);
}
