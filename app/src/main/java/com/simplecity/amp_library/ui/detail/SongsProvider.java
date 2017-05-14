package com.simplecity.amp_library.ui.detail;

import android.support.annotation.NonNull;

import com.simplecity.amp_library.model.Song;
import com.simplecityapps.recycler_adapter.model.ViewModel;

import java.util.List;

import rx.Observable;

public interface SongsProvider {

    @NonNull
    Observable<List<Song>> getSongs();

    @NonNull
    List<ViewModel> getSongViewModels(List<Song> songs);

}
