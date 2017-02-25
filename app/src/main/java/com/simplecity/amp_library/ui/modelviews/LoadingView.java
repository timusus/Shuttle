package com.simplecity.amp_library.ui.modelviews;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.simplecity.amp_library.R;

public class LoadingView extends BaseAdaptableItem<Object, LoadingView.ViewHolder> {

    @Override
    public int getViewType() {
        return ViewType.LOADING;
    }

    @Override
    public int getLayoutResId() {
        return R.layout.list_item_loading;
    }

    @Override
    public ViewHolder getViewHolder(ViewGroup parent) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(getLayoutResId(), parent, false));
    }

    @Override
    public Object getItem() {
        return null;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public ViewHolder(View itemView) {
            super(itemView);
        }

        @Override
        public String toString() {
            return "LoadingView.ViewHolder";
        }
    }
}
