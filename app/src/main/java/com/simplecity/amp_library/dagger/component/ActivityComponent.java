package com.simplecity.amp_library.dagger.component;

import com.simplecity.amp_library.dagger.module.ActivityModule;
import com.simplecity.amp_library.dagger.module.FragmentModule;
import com.simplecity.amp_library.dagger.module.PresenterModule;
import com.simplecity.amp_library.dagger.scope.ActivityScope;
import com.simplecity.amp_library.ui.activities.MainActivity;
import com.simplecity.amp_library.ui.settings.SettingsParentFragment;
import dagger.Subcomponent;

@ActivityScope
@Subcomponent(modules = {
        ActivityModule.class,
        PresenterModule.class
})

public interface ActivityComponent {

    FragmentComponent plus(FragmentModule module);

    void inject(MainActivity target);

    void inject(SettingsParentFragment.SettingsFragment target);
}