package com.simplecity.amp_library.ui.adapters;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;

import com.annimon.stream.Stream;
import com.simplecity.amp_library.model.AlbumArtist;
import com.simplecity.amp_library.ui.modelviews.AlbumArtistView;
import com.simplecity.amp_library.utils.SettingsManager;
import com.simplecity.amp_library.utils.SortManager;
import com.simplecity.amp_library.utils.StringUtils;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

public class AlbumArtistAdapter extends ItemAdapter implements FastScrollRecyclerView.SectionedAdapter {

    private AlbumArtistListener mListener;

    public interface AlbumArtistListener {

        void onItemClick(View v, int position, AlbumArtist albumArtist);

        void onOverflowClick(View v, int position, AlbumArtist albumArtist);

        void onLongClick(View v, int position, AlbumArtist albumArtist);
    }

    public void setListener(AlbumArtistListener listener) {
        mListener = listener;
    }

    public AlbumArtist getAlbumArtist(int position) {
        return ((AlbumArtistView) items.get(position)).albumArtist;
    }

    @Override
    protected void attachListeners(final RecyclerView.ViewHolder viewHolder) {
        super.attachListeners(viewHolder);

        if (viewHolder instanceof AlbumArtistView.ViewHolder) {

            viewHolder.itemView.setOnClickListener(v -> {
                if (mListener != null && viewHolder.getAdapterPosition() != -1) {
                    mListener.onItemClick(v, viewHolder.getAdapterPosition(), getAlbumArtist(viewHolder.getAdapterPosition()));
                }
            });

            ((AlbumArtistView.ViewHolder) viewHolder).overflowButton.setOnClickListener(v -> {
                if (mListener != null && viewHolder.getAdapterPosition() != -1) {
                    mListener.onOverflowClick(v, viewHolder.getAdapterPosition(), getAlbumArtist(viewHolder.getAdapterPosition()));
                }
            });

            ((AlbumArtistView.ViewHolder) viewHolder).itemView.setOnLongClickListener(v -> {
                if (mListener != null && viewHolder.getAdapterPosition() != -1) {
                    mListener.onLongClick(v, viewHolder.getAdapterPosition(), getAlbumArtist(viewHolder.getAdapterPosition()));
                }
                return true;
            });
        }
    }

    public void updateItemViewType() {

        int viewType = SettingsManager.getInstance().getArtistDisplayType();

        Stream.of(items)
                .filter(item -> item instanceof AlbumArtistView)
                .forEach(adaptableItem -> ((AlbumArtistView) adaptableItem).setViewType(viewType));
    }

    @NonNull
    @Override
    public String getSectionName(int position) {

        if (!(items.get(position) instanceof AlbumArtistView)) {
            return "";
        }

        int sortOrder = SortManager.getInstance().getArtistsSortOrder();

        AlbumArtist albumArtist = ((AlbumArtistView) items.get(position)).albumArtist;
        String string = null;
        switch (sortOrder) {
            case SortManager.ArtistSort.DEFAULT:
                string = StringUtils.keyFor(albumArtist.name);
                break;
            case SortManager.AlbumSort.ARTIST_NAME:
                string = albumArtist.name;
                break;
        }

        if (!TextUtils.isEmpty(string)) {
            string = string.substring(0, 1).toUpperCase();
        } else {
            string = " ";
        }

        return string;
    }
}