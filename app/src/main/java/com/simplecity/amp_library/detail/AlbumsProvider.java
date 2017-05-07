package com.simplecity.amp_library.detail;

import android.support.annotation.NonNull;

import com.simplecity.amp_library.model.Song;
import com.simplecityapps.recycler_adapter.model.ViewModel;

import java.util.List;

public interface AlbumsProvider {

    @NonNull
    List<ViewModel> getAdaptableItems(List<Song> songs);

}
