package com.simplecity.amp_library.ui.modelviews;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.simplecity.amp_library.R;
import com.simplecity.amp_library.utils.DrawableUtils;


public class ShuffleView extends BaseAdaptableItem<Object, ShuffleView.ViewHolder> {

    @Override
    public int getViewType() {
        return ViewType.SHUFFLE;
    }

    @Override
    public int getLayoutResId() {
        return R.layout.list_item_shuffle;
    }

    @Override
    public ViewHolder getViewHolder(ViewGroup parent) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(getLayoutResId(), parent, false));
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public ViewHolder(View itemView) {
            super(itemView);

            ImageView imageView = (ImageView) itemView.findViewById(R.id.icon);
            imageView.setImageDrawable(DrawableUtils.getColoredAccentDrawable(itemView.getContext(), itemView.getResources().getDrawable(R.drawable.ic_shuffle_white)));
        }

        @Override
        public String toString() {
            return "ShuffleView.ViewHolder";
        }
    }
}
