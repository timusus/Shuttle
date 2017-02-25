package com.simplecity.amp_library.ui.adapters;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;

import com.annimon.stream.Stream;
import com.simplecity.amp_library.model.Album;
import com.simplecity.amp_library.ui.modelviews.AlbumView;
import com.simplecity.amp_library.utils.SettingsManager;
import com.simplecity.amp_library.utils.SortManager;
import com.simplecity.amp_library.utils.StringUtils;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

public class AlbumAdapter extends ItemAdapter implements FastScrollRecyclerView.SectionedAdapter {

    private AlbumListener mListener;

    public interface AlbumListener {

        void onItemClick(View v, int position, Album album);

        void onOverflowClick(View v, int position, Album album);

        void onLongClick(View v, int position, Album album);
    }

    public void setListener(AlbumListener listener) {
        mListener = listener;
    }

    public Album getAlbum(int position) {
        return ((AlbumView) items.get(position)).album;
    }

    @Override
    protected void attachListeners(final RecyclerView.ViewHolder viewHolder) {
        super.attachListeners(viewHolder);

        if (viewHolder instanceof AlbumView.ViewHolder) {
            viewHolder.itemView.setOnClickListener(v -> {
                if (mListener != null && viewHolder.getAdapterPosition() != -1) {
                    mListener.onItemClick(v, viewHolder.getAdapterPosition(), getAlbum(viewHolder.getAdapterPosition()));
                }
            });

            ((AlbumView.ViewHolder) viewHolder).overflowButton.setOnClickListener(v -> {
                if (mListener != null && viewHolder.getAdapterPosition() != -1) {
                    mListener.onOverflowClick(v, viewHolder.getAdapterPosition(), getAlbum(viewHolder.getAdapterPosition()));
                }
            });

            ((AlbumView.ViewHolder) viewHolder).itemView.setOnLongClickListener(v -> {
                if (mListener != null && viewHolder.getAdapterPosition() != -1) {
                    mListener.onLongClick(v, viewHolder.getAdapterPosition(), getAlbum(viewHolder.getAdapterPosition()));
                }
                return true;
            });
        }
    }

    public void updateItemViewType() {
        int viewType = SettingsManager.getInstance().getAlbumDisplayType();

        Stream.of(items)
                .filter(adaptableItem ->
                        adaptableItem instanceof AlbumView)
                .forEach(adaptableItem ->
                        ((AlbumView) adaptableItem).setViewType(viewType));
    }

    @NonNull
    @Override
    public String getSectionName(int position) {

        if (!(items.get(position) instanceof AlbumView)) {
            return "";
        }

        int sortOrder = SortManager.getInstance().getAlbumsSortOrder();

        Album album = ((AlbumView) items.get(position)).album;
        String string = null;
        boolean requiresSubstring = true;
        switch (sortOrder) {
            case SortManager.AlbumSort.DEFAULT:
                string = StringUtils.keyFor(album.name);
                break;
            case SortManager.AlbumSort.NAME:
                string = album.name;
                break;
            case SortManager.AlbumSort.ARTIST_NAME:
                string = album.albumArtistName;
                break;
            case SortManager.AlbumSort.YEAR:
                string = String.valueOf(album.year);
                if (string.length() != 4) {
                    string = "-";
                } else {
                    string = string.substring(2, 4);
                }
                requiresSubstring = false;
                break;
        }

        if (requiresSubstring) {
            if (!TextUtils.isEmpty(string)) {
                string = string.substring(0, 1).toUpperCase();
            } else {
                string = " ";
            }
        }

        return string;
    }
}