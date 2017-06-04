package com.simplecity.amp_library.dagger.module;

import android.support.v4.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.simplecity.amp_library.dagger.scope.FragmentScope;
import com.simplecity.amp_library.format.PrefixHighlighter;

import dagger.Module;
import dagger.Provides;

@Module
public class FragmentModule {

    private Fragment fragment;

    public FragmentModule(Fragment fragment) {
        this.fragment = fragment;
    }

    @Provides
    @FragmentScope
    Fragment provideFragment() {
        return fragment;
    }

    @Provides
    @FragmentScope
    RequestManager provideRequestManager() {
        return Glide.with(fragment);
    }

    @Provides
    @FragmentScope
    PrefixHighlighter providePrefixHighlighter() {
        return new PrefixHighlighter();
    }
}