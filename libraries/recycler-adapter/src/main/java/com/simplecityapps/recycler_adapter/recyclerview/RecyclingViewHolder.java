package com.simplecityapps.recycler_adapter.recyclerview;

import androidx.recyclerview.widget.RecyclerView;

public interface RecyclingViewHolder {

    /**
     * If a {@link RecyclerView.RecyclerListener} is attached to the RecyclerView and implemented so as to call this method
     * on the ViewHolder to be recycled, we can clean up resources here.
     */
    void recycle();

}