package com.simplecity.amp_library.ui.modelviews;

import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.bumptech.glide.Glide;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ui.adapters.ViewType;
import com.simplecity.amp_library.ui.views.NonScrollImageButton;
import com.simplecityapps.recycler_adapter.model.ViewModel;
import com.simplecityapps.recycler_adapter.recyclerview.BaseViewHolder;
import java.util.List;

public abstract class MultiItemView<VH extends MultiItemView.ViewHolder, T> extends BaseSelectableViewModel<VH> {

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

    public static class ViewHolder<T extends ViewModel> extends BaseViewHolder<T> {

        @BindView(R.id.line_one)
        public TextView lineOne;

        @BindView(R.id.line_two)
        public TextView lineTwo;

        @Nullable
        @BindView(R.id.albumCount)
        public TextView albumCount;

        @Nullable
        @BindView(R.id.trackCount)
        public TextView trackCount;

        @BindView(R.id.image)
        public ImageView imageOne;

        @BindView(R.id.btn_overflow)
        public NonScrollImageButton overflowButton;

        @Nullable
        @BindView(R.id.bottom_container)
        View bottomContainer;

        @Nullable
        @BindView(R.id.tickImage)
        ImageView tickImageView;

        public ViewHolder(View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);
        }

        @Override
        public String toString() {
            return "MultiItemView.ViewHolder";
        }

        @Override
        public void recycle() {
            super.recycle();

            Glide.clear(imageOne);
        }
    }

    @Override
    public void bindView(VH holder) {
        super.bindView(holder);

        if (holder.tickImageView != null) {
            holder.tickImageView.setVisibility(isSelected() ? View.VISIBLE : View.GONE);
        }

        int viewType = getViewType();
        if (viewType == ViewType.ARTIST_GRID || viewType == ViewType.ALBUM_GRID) {
            if (holder.bottomContainer != null) {
                holder.bottomContainer.setBackgroundColor(0x90000000);
            }
        }
    }

    @Override
    public void bindView(VH holder, int position, List payloads) {
        super.bindView(holder, position, payloads);

        if (holder.tickImageView != null) {
            holder.tickImageView.setVisibility(isSelected() ? View.VISIBLE : View.GONE);
        }
    }
}
