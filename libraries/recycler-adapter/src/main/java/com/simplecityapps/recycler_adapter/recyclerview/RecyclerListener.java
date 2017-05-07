package com.simplecityapps.recycler_adapter.recyclerview;

import android.support.v7.widget.RecyclerView;

/**
 * A custom RecyclerListener that calls recycle() on the ViewHolder, if it implements {@link RecyclingViewHolder}
 */
public class RecyclerListener implements RecyclerView.RecyclerListener {

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        if (holder instanceof RecyclingViewHolder) {
            ((RecyclingViewHolder) holder).recycle();
        }
    }
}