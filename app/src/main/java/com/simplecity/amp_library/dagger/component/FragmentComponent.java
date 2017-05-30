package com.simplecity.amp_library.dagger.component;

import com.simplecity.amp_library.dagger.module.FragmentModule;
import com.simplecity.amp_library.dagger.module.PresenterModule;
import com.simplecity.amp_library.dagger.scope.FragmentScope;
import com.simplecity.amp_library.search.SearchFragment;
import com.simplecity.amp_library.ui.drawer.DrawerFragment;
import com.simplecity.amp_library.ui.fragments.AlbumArtistFragment;
import com.simplecity.amp_library.ui.fragments.AlbumFragment;
import com.simplecity.amp_library.ui.fragments.BaseFragment;
import com.simplecity.amp_library.ui.fragments.MiniPlayerFragment;
import com.simplecity.amp_library.ui.fragments.PlayerFragment;
import com.simplecity.amp_library.ui.fragments.QueueFragment;
import com.simplecity.amp_library.ui.fragments.QueuePagerFragment;
import com.simplecity.amp_library.ui.fragments.SuggestedFragment;
import com.simplecity.amp_library.ui.presenters.PlayerPresenter;
import com.simplecity.amp_library.ui.settings.SettingsParentFragment;

import dagger.Subcomponent;

@FragmentScope
@Subcomponent(modules = {
        FragmentModule.class,
        PresenterModule.class})

public interface FragmentComponent {

    void inject(BaseFragment target);

    void inject(PlayerFragment target);

    void inject(MiniPlayerFragment target);

    void inject(PlayerPresenter target);

    void inject(QueuePagerFragment target);

    void inject(QueueFragment target);

    void inject(DrawerFragment target);

    void inject(SettingsParentFragment.SettingsFragment target);

    void inject(AlbumArtistFragment target);

    void inject(AlbumFragment target);

    void inject(SuggestedFragment target);

    void inject(SearchFragment target);
}