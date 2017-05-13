package com.simplecity.amp_library.ui.modelviews;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.RequestManager;
import com.simplecity.amp_library.model.Song;
import com.simplecityapps.recycler_adapter.model.BaseViewModel;
import com.simplecityapps.recycler_adapter.recyclerview.BaseViewHolder;

import static com.bumptech.glide.Glide.clear;
import static com.simplecity.amp_library.R.layout.list_item_queue_pager;
import static com.simplecity.amp_library.ui.adapters.ViewType.QUEUE_PAGER_ITEM;

public class QueuePagerItemView extends BaseViewModel<QueuePagerItemView.ViewHolder> {

    private Song song;
    private RequestManager requestManager;

    public QueuePagerItemView(Song song, RequestManager requestManager) {
        this.song = song;
        this.requestManager = requestManager;
    }

    @Override
    public int getViewType() {
        return QUEUE_PAGER_ITEM;
    }

    @Override
    public int getLayoutResId() {
        return list_item_queue_pager;
    }

    @Override
    public ViewHolder createViewHolder(ViewGroup parent) {
        return new ViewHolder(createView(parent));
    }

    @Override
    public void bindView(ViewHolder holder) {
        super.bindView(holder);

        requestManager.load(song).into((ImageView) holder.itemView);
    }

    static class ViewHolder extends BaseViewHolder<QueuePagerItemView> {

        public ViewHolder(View itemView) {
            super(itemView);
        }

        @Override
        public void recycle() {
            super.recycle();

            clear(itemView);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        QueuePagerItemView that = (QueuePagerItemView) o;

        return song != null ? song.equals(that.song) : that.song == null;
    }

    @Override
    public int hashCode() {
        return song != null ? song.hashCode() : 0;
    }
}