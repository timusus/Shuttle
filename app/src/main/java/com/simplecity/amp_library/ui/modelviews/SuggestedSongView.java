package com.simplecity.amp_library.ui.modelviews;

import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.ui.adapters.ViewType;
import com.simplecity.amp_library.utils.PlaceholderProvider;
import com.simplecity.amp_library.utils.SettingsManager;

public class SuggestedSongView extends MultiItemView<SuggestedSongView.ViewHolder, Song> {

    public interface ClickListener {

        void onSongClick(Song song, ViewHolder holder);

        void onSongOverflowClicked(View v, int position, Song song);
    }

    public Song song;

    private RequestManager requestManager;

    private SettingsManager settingsManager;

    @Nullable
    private ClickListener listener;

    public SuggestedSongView(Song song, RequestManager requestManager, SettingsManager settingsManager) {
        this.song = song;
        this.requestManager = requestManager;
        this.settingsManager = settingsManager;
    }

    void onItemClick(ViewHolder holder) {
        if (listener != null) {
            listener.onSongClick(song, holder);
        }
    }

    void onOverflowClick(View v, ViewHolder viewHolder) {
        if (listener != null) {
            listener.onSongOverflowClicked(v, viewHolder.getAdapterPosition(), song);
        }
    }

    public void setClickListener(@Nullable ClickListener listener) {
        this.listener = listener;
    }

    @Override
    public int getLayoutResId() {
        return R.layout.grid_item_horizontal;
    }

    @Override
    public int getViewType() {
        return ViewType.SUGGESTED_SONG;
    }

    @Override
    public void bindView(final ViewHolder holder) {
        super.bindView(holder);

        holder.lineOne.setText(song.name);
        holder.lineTwo.setText(song.artistName);
        holder.lineTwo.setVisibility(View.VISIBLE);
        if (holder.albumCount != null) {
            holder.albumCount.setVisibility(View.GONE);
        }
        if (holder.trackCount != null) {
            holder.trackCount.setVisibility(View.GONE);
        }

        requestManager.load(song)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(PlaceholderProvider.getInstance(holder.imageOne.getContext()).getPlaceHolderDrawable(song.albumName, false, settingsManager))
                .into(holder.imageOne);

        holder.overflowButton.setContentDescription(holder.itemView.getResources().getString(R.string.btn_options, song.name));
    }

    @Override
    public ViewHolder createViewHolder(ViewGroup parent) {
        return new ViewHolder(createView(parent));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SuggestedSongView that = (SuggestedSongView) o;

        return song != null ? song.equals(that.song) : that.song == null;
    }

    @Override
    public int hashCode() {
        return song != null ? song.hashCode() : 0;
    }

    public static class ViewHolder extends MultiItemView.ViewHolder<SuggestedSongView> {

        public ViewHolder(View itemView) {
            super(itemView);

            itemView.setOnClickListener(v -> viewModel.onItemClick(this));

            overflowButton.setOnClickListener(v -> viewModel.onOverflowClick(v, this));
        }
    }
}
