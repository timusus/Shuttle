package com.simplecity.amp_library.ui.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.simplecity.amp_library.model.Album;
import com.simplecity.amp_library.model.AlbumArtist;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.ui.modelviews.AlbumArtistView;
import com.simplecity.amp_library.ui.modelviews.AlbumView;
import com.simplecity.amp_library.ui.modelviews.MultiItemView;
import com.simplecity.amp_library.ui.modelviews.SongView;
import com.simplecity.amp_library.ui.modelviews.SuggestedSongView;
import com.simplecity.amp_library.ui.modelviews.ViewType;

import java.util.List;

public class SearchAdapter extends ItemAdapter {

    public interface SearchListener {

        void onItemClick(AlbumArtist albumArtist);

        void onItemClick(Album album);

        void onItemClick(Song song, List<Song> allSongs);

        void onOverflowClick(View v, AlbumArtist albumArtist);

        void onOverflowClick(View v, Album album);

        void onOverflowClick(View v, Song song);
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
                            listener.onItemClick(((AlbumArtistView) items.get(viewHolder.getAdapterPosition())).albumArtist);
                            break;
                        case ViewType.ALBUM_CARD:
                        case ViewType.ALBUM_CARD_LARGE:
                        case ViewType.ALBUM_LIST_SMALL:
                        case ViewType.ALBUM_LIST:
                            listener.onItemClick(((AlbumView) items.get(viewHolder.getAdapterPosition())).album);
                            break;
                        case ViewType.SUGGESTED_SONG:
                        case ViewType.SONG:
                            listener.onItemClick(((SuggestedSongView) items.get(viewHolder.getAdapterPosition())).song, Stream.of(items)
                                    .filter(adaptableItem -> adaptableItem instanceof SongView)
                                    .map(adaptableItem -> ((SongView) adaptableItem).getItem())
                                    .collect(Collectors.toList()));
                            break;
                    }
                }
            });

            ((MultiItemView.ViewHolder) viewHolder).overflowButton.setOnClickListener(v -> {
                if (listener != null && viewHolder.getAdapterPosition() != -1) {
                    switch (items.get(viewHolder.getAdapterPosition()).getViewType()) {
                        case ViewType.ARTIST_CARD:
                        case ViewType.ARTIST_LIST:
                            listener.onOverflowClick(v, ((AlbumArtistView) items.get(viewHolder.getAdapterPosition())).albumArtist);
                            break;
                        case ViewType.ALBUM_CARD:
                        case ViewType.ALBUM_CARD_LARGE:
                        case ViewType.ALBUM_LIST_SMALL:
                        case ViewType.ALBUM_LIST:
                            listener.onOverflowClick(v, ((AlbumView) items.get(viewHolder.getAdapterPosition())).album);
                            break;
                        case ViewType.SUGGESTED_SONG:
                            listener.onOverflowClick(v, ((SuggestedSongView) items.get(viewHolder.getAdapterPosition())).song);
                            break;
                    }
                }
            });
        } else if (viewHolder instanceof SongView.ViewHolder) {
            viewHolder.itemView.setOnClickListener(v -> {
                if (listener != null && viewHolder.getAdapterPosition() != -1) {
                    listener.onItemClick(((SongView) items.get(viewHolder.getAdapterPosition())).song, Stream.of(items)
                            .filter(adaptableItem -> adaptableItem instanceof SongView)
                            .map(adaptableItem -> ((SongView) adaptableItem).getItem())
                            .collect(Collectors.toList()));
                }
            });

            ((SongView.ViewHolder) viewHolder).overflowButton.setOnClickListener(v -> {
                if (listener != null && viewHolder.getAdapterPosition() != -1) {
                    listener.onOverflowClick(v, ((SongView) items.get(viewHolder.getAdapterPosition())).song);
                }
            });
        }

    }
}