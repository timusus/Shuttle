package com.simplecity.amp_library.ui.modelviews;

import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.simplecity.amp_library.model.Playlist;
import com.simplecity.amp_library.ui.views.NonScrollImageButton;
import com.simplecityapps.recycler_adapter.model.BaseViewModel;
import com.simplecityapps.recycler_adapter.recyclerview.BaseViewHolder;

import static com.simplecity.amp_library.R.id.btn_overflow;
import static com.simplecity.amp_library.R.id.line_one;
import static com.simplecity.amp_library.R.layout.list_item_one_line;
import static com.simplecity.amp_library.R.string.btn_options;
import static com.simplecity.amp_library.ui.adapters.ViewType.PLAYLIST;
import static com.simplecity.amp_library.ui.screens.playlist.list.PlaylistListFragment.PlaylistClickListener;

public class PlaylistView extends BaseViewModel<PlaylistView.ViewHolder> {

    public interface OnClickListener {

        void onPlaylistClick(int position, PlaylistView playlistView);

        void onPlaylistOverflowClick(int position, View v, Playlist playlist);
    }

    public Playlist playlist;

    @Nullable
    private OnClickListener listener;

    public PlaylistView(Playlist playlist) {
        this.playlist = playlist;
    }

    public void setListener(@Nullable OnClickListener listener) {
        this.listener = listener;
    }

    @Override
    public int getViewType() {
        return PLAYLIST;
    }

    @Override
    public int getLayoutResId() {
        return list_item_one_line;
    }

    @Override
    public void bindView(ViewHolder holder) {
        super.bindView(holder);

        holder.lineOne.setText(playlist.name);
        holder.overflowButton.setContentDescription(holder.itemView.getResources().getString(btn_options, playlist.name));
    }

    void onPlaylistClicked(int position) {
        if (listener != null) {
            listener.onPlaylistClick(position, this);
        }
    }

    void onOverflowClicked(int position, View v) {
        if (listener != null) {
            listener.onPlaylistOverflowClick(position, v, playlist);
        }
    }

    @Override
    public ViewHolder createViewHolder(ViewGroup parent) {
        return new ViewHolder(createView(parent));
    }

    public static class ViewHolder extends BaseViewHolder<PlaylistView> {

        public TextView lineOne;
        public NonScrollImageButton overflowButton;
        public PlaylistClickListener listener;

        public ViewHolder(View itemView) {
            super(itemView);

            lineOne = itemView.findViewById(line_one);
            overflowButton = itemView.findViewById(btn_overflow);

            itemView.setOnClickListener(v -> viewModel.onPlaylistClicked(getAdapterPosition()));
            overflowButton.setOnClickListener(v -> viewModel.onOverflowClicked(getAdapterPosition(), v));
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
