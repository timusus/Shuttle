package com.simplecity.amp_library.ui.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.simplecity.amp_library.ui.modelviews.AlbumArtistView;
import com.simplecity.amp_library.ui.modelviews.AlbumView;
import com.simplecity.amp_library.ui.modelviews.MultiItemView;
import com.simplecity.amp_library.ui.modelviews.SuggestedHeaderView;
import com.simplecity.amp_library.ui.modelviews.SuggestedSongView;
import com.simplecity.amp_library.ui.modelviews.ViewType;

public class SuggestedAdapter extends ItemAdapter {

    private SuggestedListener listener;

    public interface SuggestedListener {

        void onItemClick(ItemAdapter adapter, View v, int position, Object item);

        void onOverflowClick(View v, int position, Object item);
    }

    public void setListener(SuggestedListener listener) {
        this.listener = listener;
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
                            listener.onItemClick(this, v, viewHolder.getAdapterPosition(), ((AlbumArtistView) items.get(viewHolder.getAdapterPosition())).albumArtist);
                            break;
                        case ViewType.ALBUM_CARD:
                        case ViewType.ALBUM_CARD_LARGE:
                        case ViewType.ALBUM_LIST_SMALL:
                            listener.onItemClick(this, v, viewHolder.getAdapterPosition(), ((AlbumView) items.get(viewHolder.getAdapterPosition())).album);
                            break;
                        case ViewType.SUGGESTED_SONG:
                            listener.onItemClick(this, v, viewHolder.getAdapterPosition(), ((SuggestedSongView) items.get(viewHolder.getAdapterPosition())).song);
                            break;
                    }
                }
            });

            ((MultiItemView.ViewHolder) viewHolder).overflowButton.setOnClickListener(v -> {
                if (listener != null && viewHolder.getAdapterPosition() != -1) {
                    switch (items.get(viewHolder.getAdapterPosition()).getViewType()) {
                        case ViewType.ARTIST_CARD:
                            listener.onOverflowClick(v, viewHolder.getAdapterPosition(), ((AlbumArtistView) items.get(viewHolder.getAdapterPosition())).albumArtist);
                            break;
                        case ViewType.ALBUM_CARD:
                        case ViewType.ALBUM_CARD_LARGE:
                        case ViewType.ALBUM_LIST_SMALL:
                            listener.onOverflowClick(v, viewHolder.getAdapterPosition(), ((AlbumView) items.get(viewHolder.getAdapterPosition())).album);
                            break;
                        case ViewType.SUGGESTED_SONG:
                            listener.onOverflowClick(v, viewHolder.getAdapterPosition(), ((SuggestedSongView) items.get(viewHolder.getAdapterPosition())).song);
                            break;
                    }
                }
            });
        } else if (viewHolder instanceof SuggestedHeaderView.ViewHolder) {
            viewHolder.itemView.setOnClickListener(v -> {
                if (listener != null && viewHolder.getAdapterPosition() != -1) {
                    listener.onItemClick(this, v, viewHolder.getAdapterPosition(), ((SuggestedHeaderView) items.get(viewHolder.getAdapterPosition())).suggestedHeader);
                }
            });
        }
    }
}
