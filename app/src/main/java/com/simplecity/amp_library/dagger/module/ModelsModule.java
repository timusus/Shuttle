package com.simplecity.amp_library.dagger.module;

import android.support.annotation.NonNull;

import com.simplecity.amp_library.model.PlaylistsModel;

import dagger.Module;
import dagger.Provides;

@Module
public class ModelsModule {

    @Provides
    @NonNull
    public PlaylistsModel providePlaylistsModel() {
        return new PlaylistsModel();
    }

}