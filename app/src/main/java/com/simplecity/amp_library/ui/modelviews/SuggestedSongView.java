package com.simplecity.amp_library.ui.modelviews;

import android.support.v7.widget.RecyclerView;

import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.glide.utils.GlideUtils;
import com.simplecity.amp_library.model.Song;

public class SuggestedSongView extends MultiItemView {

    public Song song;

    private RequestManager requestManager;

    public SuggestedSongView(Song song, RequestManager requestManager) {
        this.song = song;
        this.requestManager = requestManager;
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
    public void bindView(final RecyclerView.ViewHolder holder) {

        ((ViewHolder) holder).lineOne.setText(song.name);
        ((ViewHolder) holder).lineTwo.setText(song.artistName);

        requestManager.load(song)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(GlideUtils.getPlaceHolderDrawable(song.albumName, false))
                .into(((ViewHolder) holder).imageOne);

        ((ViewHolder) holder).overflowButton.setContentDescription(holder.itemView.getResources().getString(R.string.btn_options, song.name));
    }

    @Override
    public Song getItem() {
        return song;
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
}
