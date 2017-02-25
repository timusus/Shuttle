package com.simplecity.amp_library.model;

import android.support.annotation.LayoutRes;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import com.simplecity.amp_library.ui.modelviews.ViewType;

import java.util.List;

public interface AdaptableItem<T, H extends RecyclerView.ViewHolder> extends ContentsComparator {

    @ViewType
    int getViewType();

    @LayoutRes
    int getLayoutResId();

    void bindView(H holder);

    void bindView(H holder, int position, List payloads);

    H getViewHolder(ViewGroup parent);

    void recycle(H holder);

    T getItem();
}