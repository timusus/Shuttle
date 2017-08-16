package com.simplecityapps.recycler_adapter.recyclerview;

import android.support.v7.widget.RecyclerView;
import android.view.View;

public class ChildAttachStateChangeListener implements RecyclerView.OnChildAttachStateChangeListener {

    private RecyclerView recyclerView;

    public ChildAttachStateChangeListener(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
    }

    @Override
    public void onChildViewAttachedToWindow(View view) {
        RecyclerView.ViewHolder holder = recyclerView.getChildViewHolder(view);
        if (holder instanceof AttachStateViewHolder) {
            ((AttachStateViewHolder) holder).onAttachedToWindow();
        }
    }

    @Override
    public void onChildViewDetachedFromWindow(View view) {
        RecyclerView.ViewHolder holder = recyclerView.getChildViewHolder(view);
        if (holder instanceof AttachStateViewHolder) {
            ((AttachStateViewHolder) holder).onDetachedFromWindow();
        }
    }
}