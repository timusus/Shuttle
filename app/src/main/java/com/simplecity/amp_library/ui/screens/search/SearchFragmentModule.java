package com.simplecity.amp_library.ui.screens.search;

import android.support.v4.app.Fragment;

import com.simplecity.amp_library.di.app.activity.fragment.FragmentModule;
import com.simplecity.amp_library.di.app.activity.fragment.FragmentScope;

import javax.inject.Named;

import dagger.Binds;
import dagger.Module;

@Module(includes = FragmentModule.class)
public abstract class SearchFragmentModule {

    @Binds
    @Named(FragmentModule.FRAGMENT)
    @FragmentScope
    abstract Fragment fragment(SearchFragment searchFragment);
}
