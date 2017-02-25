package com.simplecity.amp_library.ui.modelviews;

import com.bumptech.glide.RequestManager;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.model.Album;

public class HorizontalAlbumView extends AlbumView {

    public HorizontalAlbumView(Album album, RequestManager requestManager) {
        super(album, ViewType.ALBUM_CARD, requestManager);
    }

    @Override
    public int getLayoutResId() {
        return R.layout.grid_item_horizontal;
    }
}