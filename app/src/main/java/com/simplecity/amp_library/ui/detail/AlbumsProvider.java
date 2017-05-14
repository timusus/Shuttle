package com.simplecity.amp_library.ui.detail;

import android.support.annotation.NonNull;

import com.simplecity.amp_library.model.Album;
import com.simplecityapps.recycler_adapter.model.ViewModel;

import java.util.List;

import rx.Observable;

public interface AlbumsProvider {

    @NonNull
    Observable<List<Album>> getAlbums();

    @NonNull
    List<ViewModel> getAlbumViewModels(List<Album> albums);

}
