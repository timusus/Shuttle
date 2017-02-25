package com.simplecity.amp_library.ui.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.simplecity.amp_library.ui.modelviews.AlbumArtistView;
import com.simplecity.amp_library.ui.modelviews.AlbumView;
import com.simplecity.amp_library.ui.modelviews.MultiItemView;
import com.simplecity.amp_library.ui.modelviews.SongView;
import com.simplecity.amp_library.ui.modelviews.SuggestedSongView;
import com.simplecity.amp_library.ui.modelviews.ViewType;

public class SearchAdapter extends ItemAdapter {

    public interface SearchListener {

        void onItemClick(View v, int position, Object item);

        void onOverflowClick(View v, int position, Object item);
    }

    private SearchListener listener;

    public void setListener(SearchListener listener) {
        this.listener = listener;
    }

    public Object getItem(int position) {
        return items.get(position).getItem();
    }

    @Override
    protected void attachListeners(RecyclerView.ViewHolder viewHolder) {
        super.attachListeners(viewHolder);

        if (viewHolder instanceof MultiItemView.ViewHolder) {

            //MultiItemView type is ambiguous, so we have to check the type of item at current viewholder

            viewHolder.itemView.setOnClickListener(v -> {
                if (listener != null && viewHolder.getAdapterPosition() != -1) {

                    switch (items.get(viewHolder.getAdapterPosition()).getViewType()) {
                        case ViewType.ARTIST_CARD:
                        case ViewType.ARTIST_LIST:
                            listener.onItemClick(v, viewHolder.getAdapterPosition(), ((AlbumArtistView) items.get(viewHolder.getAdapterPosition())).albumArtist);
                            break;
                        case ViewType.ALBUM_CARD:
                        case ViewType.ALBUM_CARD_LARGE:
                        case ViewType.ALBUM_LIST_SMALL:
                        case ViewType.ALBUM_LIST:
                            listener.onItemClick(v, viewHolder.getAdapterPosition(), ((AlbumView) items.get(viewHolder.getAdapterPosition())).album);
                            break;
                        case ViewType.SUGGESTED_SONG:
                            listener.onItemClick(v, viewHolder.getAdapterPosition(), ((SuggestedSongView) items.get(viewHolder.getAdapterPosition())).song);
                            break;
                        case ViewType.SONG:
                            listener.onItemClick(v, viewHolder.getAdapterPosition(), ((SongView) items.get(viewHolder.getAdapterPosition())).song);
                            break;
                    }
                }
            });

            ((MultiItemView.ViewHolder) viewHolder).overflowButton.setOnClickListener(v -> {
                if (listener != null && viewHolder.getAdapterPosition() != -1) {
                    switch (items.get(viewHolder.getAdapterPosition()).getViewType()) {
                        case ViewType.ARTIST_CARD:
                        case ViewType.ARTIST_LIST:
                            listener.onOverflowClick(v, viewHolder.getAdapterPosition(), ((AlbumArtistView) items.get(viewHolder.getAdapterPosition())).albumArtist);
                            break;
                        case ViewType.ALBUM_CARD:
                        case ViewType.ALBUM_CARD_LARGE:
                        case ViewType.ALBUM_LIST_SMALL:
                        case ViewType.ALBUM_LIST:
                            listener.onOverflowClick(v, viewHolder.getAdapterPosition(), ((AlbumView) items.get(viewHolder.getAdapterPosition())).album);
                            break;
                        case ViewType.SUGGESTED_SONG:
                            listener.onOverflowClick(v, viewHolder.getAdapterPosition(), ((SuggestedSongView) items.get(viewHolder.getAdapterPosition())).song);
                            break;
                    }
                }
            });
        } else if (viewHolder instanceof SongView.ViewHolder) {
            viewHolder.itemView.setOnClickListener(v -> {
                if (listener != null && viewHolder.getAdapterPosition() != -1) {
                    listener.onItemClick(v, viewHolder.getAdapterPosition(), ((SongView) items.get(viewHolder.getAdapterPosition())).song);
                }
            });

            ((SongView.ViewHolder) viewHolder).overflowButton.setOnClickListener(v -> {
                if (listener != null && viewHolder.getAdapterPosition() != -1) {
                    listener.onOverflowClick(v, viewHolder.getAdapterPosition(), ((SongView) items.get(viewHolder.getAdapterPosition())).song);
                }
            });
        }

    }
}