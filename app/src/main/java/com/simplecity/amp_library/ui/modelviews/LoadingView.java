package com.simplecity.amp_library.ui.modelviews;

import android.view.View;
import android.view.ViewGroup;
import com.simplecityapps.recycler_adapter.model.BaseViewModel;
import com.simplecityapps.recycler_adapter.recyclerview.BaseViewHolder;

import static com.simplecity.amp_library.R.layout.list_item_loading;
import static com.simplecity.amp_library.ui.adapters.ViewType.LOADING;

public class LoadingView extends BaseViewModel<LoadingView.ViewHolder> {

    @Override
    public int getViewType() {
        return LOADING;
    }

    @Override
    public int getLayoutResId() {
        return list_item_loading;
    }

    @Override
    public ViewHolder createViewHolder(ViewGroup parent) {
        return new ViewHolder(createView(parent));
    }

    public static class ViewHolder extends BaseViewHolder {

        public ViewHolder(View itemView) {
            super(itemView);
        }

        @Override
        public String toString() {
            return "LoadingView.ViewHolder";
        }
    }
}
