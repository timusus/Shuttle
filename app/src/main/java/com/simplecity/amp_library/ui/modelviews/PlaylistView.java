package com.simplecity.amp_library.ui.modelviews;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.simplecity.amp_library.R;
import com.simplecity.amp_library.model.Playlist;
import com.simplecity.amp_library.ui.views.NonScrollImageButton;
import com.simplecity.amp_library.utils.DrawableUtils;

public class PlaylistView extends BaseAdaptableItem<Playlist, PlaylistView.ViewHolder> {

    public Playlist playlist;

    public PlaylistView(Playlist playlist) {
        this.playlist = playlist;
    }

    @Override
    public int getViewType() {
        return ViewType.PLAYLIST;
    }

    @Override
    public int getLayoutResId() {
        return R.layout.list_item_one_line;
    }

    @Override
    public void bindView(ViewHolder holder) {
        holder.lineOne.setText(playlist.name);
        holder.overflowButton.setContentDescription(holder.itemView.getResources().getString(R.string.btn_options, playlist.name));
    }

    @Override
    public ViewHolder getViewHolder(ViewGroup parent) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(getLayoutResId(), parent, false));
    }

    @Override
    public Playlist getItem() {
        return playlist;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public TextView lineOne;
        public NonScrollImageButton overflowButton;

        public ViewHolder(View itemView) {
            super(itemView);

            lineOne = (TextView) itemView.findViewById(R.id.line_one);
            overflowButton = (NonScrollImageButton) itemView.findViewById(R.id.btn_overflow);
            overflowButton.setImageDrawable(DrawableUtils.getColoredStateListDrawable(itemView.getContext(), R.drawable.ic_overflow_white));
        }

        @Override
        public String toString() {
            return "PlaylistView.ViewHolder";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PlaylistView that = (PlaylistView) o;

        return playlist != null ? playlist.equals(that.playlist) : that.playlist == null;

    }

    @Override
    public int hashCode() {
        return playlist != null ? playlist.hashCode() : 0;
    }

    @Override
    public boolean areContentsEqual(Object other) {
        return equals(other);
    }
}
