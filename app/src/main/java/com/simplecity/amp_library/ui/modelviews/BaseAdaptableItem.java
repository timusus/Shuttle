package com.simplecity.amp_library.ui.modelviews;

import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;

import com.simplecity.amp_library.model.AdaptableItem;
import com.simplecity.amp_library.model.ContentsComparator;

import java.util.List;


public abstract class BaseAdaptableItem<T, H extends RecyclerView.ViewHolder> implements
        AdaptableItem<T, H>,
        ContentsComparator {

    @Override
    public void bindView(H holder) {

    }

    @Override
    public void bindView(H holder, int position, List payloads) {
        if (payloads.isEmpty()) {
            bindView(holder);
        }
    }

    @Override
    public void recycle(H holder) {

    }

    @Override
    public boolean areContentsEqual(Object other) {
        return equals(other);
    }

    @Nullable
    @Override
    public T getItem() {
        return null;
    }
}
