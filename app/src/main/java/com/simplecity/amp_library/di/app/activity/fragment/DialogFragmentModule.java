package com.simplecity.amp_library.di.app.activity.fragment;

import android.support.v4.app.DialogFragment;
import dagger.Binds;
import dagger.Module;
import javax.inject.Named;

@Module
public abstract class DialogFragmentModule {

    @Binds
    @Named(FragmentModule.FRAGMENT)
    @FragmentScope
    abstract DialogFragment fragment(DialogFragment dialogFragment);
}