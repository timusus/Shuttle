package com.simplecity.amp_library.ui.modelviews;

import com.bumptech.glide.RequestManager;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.model.Album;
import com.simplecity.amp_library.ui.adapters.ViewType;
import com.simplecity.amp_library.utils.SettingsManager;
import com.simplecity.amp_library.utils.sorting.SortManager;

public class HorizontalAlbumView extends AlbumView {

    public HorizontalAlbumView(Album album, RequestManager requestManager, SortManager sortManager, SettingsManager settingsManager) {
        super(album, ViewType.ALBUM_CARD, requestManager, sortManager, settingsManager);
    }

    @Override
    public int getLayoutResId() {
        return R.layout.grid_item_horizontal;
    }
}