package com.simplecity.amp_library.ui.modelviews;

import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.simplecityapps.recycler_adapter.model.BaseViewModel;
import com.simplecityapps.recycler_adapter.recyclerview.BaseViewHolder;

import static com.simplecity.amp_library.R.drawable.ic_shuffle_white;
import static com.simplecity.amp_library.R.id.icon;
import static com.simplecity.amp_library.R.layout.list_item_shuffle;
import static com.simplecity.amp_library.ui.adapters.ViewType.SHUFFLE;
import static com.simplecity.amp_library.utils.DrawableUtils.getColoredAccentDrawable;


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
        return SHUFFLE;
    }

    @Override
    public int getLayoutResId() {
        return list_item_shuffle;
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

            ImageView imageView = (ImageView) itemView.findViewById(icon);
            imageView.setImageDrawable(getColoredAccentDrawable(itemView.getContext(), itemView.getResources().getDrawable(ic_shuffle_white)));

            itemView.setOnClickListener(v -> viewModel.onItemClick());
        }

        @Override
        public String toString() {
            return "ShuffleView.ViewHolder";
        }
    }
}
