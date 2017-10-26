package com.simplecity.amp_library.ui.modelviews;

import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ui.adapters.ViewType;

public class ShuffleAlbumsView extends ShuffleView {

    @Override
    public int getViewType() {
        return ViewType.SHUFFLE_ALBUMS;
    }


    @Override
    public int getLayoutResId() {
        return R.layout.list_item_shuffle_albums;
    }
}
