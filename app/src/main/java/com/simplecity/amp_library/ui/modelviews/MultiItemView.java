package com.simplecity.amp_library.ui.modelviews;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bignerdranch.android.multiselector.MultiSelector;
import com.bumptech.glide.Glide;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ui.recyclerview.CustomSwappingHolder;
import com.simplecity.amp_library.ui.views.NonScrollImageButton;
import com.simplecity.amp_library.utils.DrawableUtils;

public abstract class MultiItemView<T> extends BaseAdaptableItem<T, MultiItemView.ViewHolder> {

    protected MultiSelector multiSelector;

    @Override
    public int getLayoutResId() {

        switch (getViewType()) {
            case ViewType.ARTIST_LIST:
            case ViewType.ALBUM_LIST:
                return R.layout.list_item_image;
            case ViewType.ARTIST_CARD:
            case ViewType.ARTIST_CARD_LARGE:
            case ViewType.ALBUM_CARD:
            case ViewType.ALBUM_CARD_LARGE:
            case ViewType.SUGGESTED_SONG:
                return R.layout.grid_item_card;
            case ViewType.ARTIST_PALETTE:
            case ViewType.ALBUM_PALETTE:
                return R.layout.grid_item_palette;
            case ViewType.ARTIST_GRID:
            case ViewType.ALBUM_GRID:
                return R.layout.grid_item;
            case ViewType.ARTIST_LIST_SMALL:
            case ViewType.ALBUM_LIST_SMALL:
                return R.layout.list_item_small;
        }
        throw new IllegalStateException("getLayoutResId() invalid ViewType. Class: " + getClass().getSimpleName());
    }

    @Override
    public ViewHolder getViewHolder(ViewGroup parent) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(getLayoutResId(), parent, false), getViewType(), multiSelector);
    }

    @Override
    public void recycle(ViewHolder holder) {
        Glide.clear(holder.imageOne);
    }

    public static class ViewHolder extends CustomSwappingHolder {

        public TextView lineOne;
        public TextView lineTwo;
        public ImageView imageOne;
        public NonScrollImageButton overflowButton;
        public View bottomContainer;

        public ViewHolder(View itemView, @ViewType int viewType, MultiSelector multiSelector) {
            super(itemView, multiSelector);

            lineOne = (TextView) itemView.findViewById(R.id.line_one);
            lineTwo = (TextView) itemView.findViewById(R.id.line_two);
            imageOne = (ImageView) itemView.findViewById(R.id.image);
            overflowButton = (NonScrollImageButton) itemView.findViewById(R.id.btn_overflow);
            bottomContainer = itemView.findViewById(R.id.bottom_container);

            if (viewType == ViewType.ARTIST_CARD
                    || viewType == ViewType.ALBUM_CARD
                    || viewType == ViewType.ARTIST_CARD_LARGE
                    || viewType == ViewType.ALBUM_CARD_LARGE
                    || viewType == ViewType.ARTIST_LIST
                    || viewType == ViewType.ALBUM_LIST
                    || viewType == ViewType.ARTIST_LIST_SMALL
                    || viewType == ViewType.ALBUM_LIST_SMALL
                    || viewType == ViewType.SUGGESTED_SONG) {
                overflowButton.setImageDrawable(DrawableUtils.getBaseDrawable(itemView.getContext(), R.drawable.ic_overflow_white));
            } else {
                overflowButton.setImageDrawable(DrawableUtils.getWhiteDrawable(itemView.getContext(), R.drawable.ic_overflow_white));
            }
            if (viewType == ViewType.ARTIST_GRID
                    || viewType == ViewType.ALBUM_GRID) {
                bottomContainer.setBackgroundColor(0x90000000);
            }
        }

        @Override
        public String toString() {
            return "MultiItemView.ViewHolder";
        }
    }
}
