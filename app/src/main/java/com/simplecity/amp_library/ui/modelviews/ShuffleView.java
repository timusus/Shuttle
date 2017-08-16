package com.simplecity.amp_library.ui.modelviews;

import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;

import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ui.adapters.ViewType;
import com.simplecityapps.recycler_adapter.model.BaseViewModel;
import com.simplecityapps.recycler_adapter.recyclerview.BaseViewHolder;

public class ShuffleView extends BaseViewModel<ShuffleView.ViewHolder> {

    public interface ShuffleClickListener {
        void onShuffleItemClick();
    }

    @Nullable
    private ShuffleClickListener listener;

    public void setClickListener(@Nullable ShuffleClickListener listener) {
        this.listener = listener;
    }

    @Override
    public int getViewType() {
        return ViewType.SHUFFLE;
    }

    @Override
    public int getLayoutResId() {
        return R.layout.list_item_shuffle;
    }

    @Override
    public ViewHolder createViewHolder(ViewGroup parent) {
        return new ViewHolder(createView(parent));
    }

    @Override
    public void bindView(ViewHolder holder) {
        super.bindView(holder);

        holder.bind(this);
    }

    private void onItemClick() {
        if (listener != null) {
            listener.onShuffleItemClick();
        }
    }

    public static class ViewHolder extends BaseViewHolder<ShuffleView> {

        public ViewHolder(View itemView) {
            super(itemView);

            itemView.setOnClickListener(v -> viewModel.onItemClick());
        }

        @Override
        public String toString() {
            return "ShuffleView.ViewHolder";
        }
    }
}