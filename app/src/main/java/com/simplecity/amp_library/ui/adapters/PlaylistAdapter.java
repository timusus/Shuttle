package com.simplecity.amp_library.ui.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.simplecity.amp_library.model.Playlist;
import com.simplecity.amp_library.ui.modelviews.PlaylistView;

public class PlaylistAdapter extends ItemAdapter {

    private static final String TAG = "PlaylistAdapter";

    private PlaylistListener listener;

    public interface PlaylistListener {

        void onItemClick(View v, int position, Playlist playlist);

        void onOverflowClick(View v, int position, Playlist playlist);
    }

    public void setListener(PlaylistListener listener) {
        this.listener = listener;
    }


    public Playlist getPlaylist(int position) {
        return ((PlaylistView) items.get(position)).playlist;
    }

    @Override
    protected void attachListeners(final RecyclerView.ViewHolder viewHolder) {
        super.attachListeners(viewHolder);

        if (viewHolder instanceof PlaylistView.ViewHolder) {

            viewHolder.itemView.setOnClickListener(v -> {
                if (listener != null && viewHolder.getAdapterPosition() != -1) {
                    listener.onItemClick(v, viewHolder.getAdapterPosition(), getPlaylist(viewHolder.getAdapterPosition()));
                }
            });

            ((PlaylistView.ViewHolder) viewHolder).overflowButton.setOnClickListener(v -> {
                if (listener != null && viewHolder.getAdapterPosition() != -1) {
                    listener.onOverflowClick(v, viewHolder.getAdapterPosition(), getPlaylist(viewHolder.getAdapterPosition()));
                }
            });
        }
    }
}