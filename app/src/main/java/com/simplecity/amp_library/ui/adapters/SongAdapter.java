package com.simplecity.amp_library.ui.adapters;

import android.support.annotation.NonNull;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;

import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.ui.modelviews.ShuffleView;
import com.simplecity.amp_library.ui.modelviews.SongView;
import com.simplecity.amp_library.utils.SortManager;
import com.simplecity.amp_library.utils.StringUtils;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

public class SongAdapter extends ItemAdapter implements FastScrollRecyclerView.SectionedAdapter {

    private static final String TAG = "SongAdapter";

    private SongListener listener;

    public interface SongListener {

        void onItemClick(View v, int position, Song song);

        void onOverflowClick(View v, int position, Song song);

        void onLongClick(View v, int position, Song song);

        void onShuffleClick();

        void onStartDrag(RecyclerView.ViewHolder viewHolder);
    }

    public void setListener(SongListener listener) {
        this.listener = listener;
    }

    public Song getSong(int position) {
        return ((SongView) items.get(position)).song;
    }

    @Override
    protected void attachListeners(final RecyclerView.ViewHolder viewHolder) {
        super.attachListeners(viewHolder);

        if (viewHolder instanceof SongView.ViewHolder) {
            viewHolder.itemView.setOnClickListener(v -> {
                if (listener != null && viewHolder.getAdapterPosition() != -1) {
                    listener.onItemClick(v, viewHolder.getAdapterPosition(), getSong(viewHolder.getAdapterPosition()));
                }
            });

            ((SongView.ViewHolder) viewHolder).overflowButton.setOnClickListener(v -> {
                if (listener != null && viewHolder.getAdapterPosition() != -1) {
                    listener.onOverflowClick(v, viewHolder.getAdapterPosition(), getSong(viewHolder.getAdapterPosition()));
                }
            });

            ((SongView.ViewHolder) viewHolder).itemView.setOnLongClickListener(v -> {
                if (listener != null && viewHolder.getAdapterPosition() != -1) {
                    listener.onLongClick(v, viewHolder.getAdapterPosition(), getSong(viewHolder.getAdapterPosition()));
                }
                return true;
            });

            if (((SongView.ViewHolder) viewHolder).dragHandle != null) {
                ((SongView.ViewHolder) viewHolder).dragHandle.setOnTouchListener((v, event) -> {
                    if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
                        listener.onStartDrag(viewHolder);
                    }
                    return false;
                });
            }
        } else if (viewHolder instanceof ShuffleView.ViewHolder) {
            viewHolder.itemView.setOnClickListener(v -> {
                if (listener != null && viewHolder.getAdapterPosition() != -1) {
                    listener.onShuffleClick();
                }
            });
        }
    }

    @NonNull
    @Override
    public String getSectionName(int position) {

        if (!(items.get(position) instanceof SongView)) {
            return "";
        }

        int sortOrder = SortManager.getInstance().getSongsSortOrder();

        if (sortOrder != SortManager.SongSort.DATE
                && sortOrder != SortManager.SongSort.DURATION
                && sortOrder != SortManager.SongSort.TRACK_NUMBER) {

            Song song = ((SongView) items.get(position)).song;

            String string = null;
            boolean requiresSubstring = true;
            switch (sortOrder) {
                case SortManager.SongSort.DEFAULT:
                    string = StringUtils.keyFor(song.name);
                    break;
                case SortManager.SongSort.NAME:
                    string = song.name;
                    break;
                case SortManager.SongSort.YEAR:
                    string = String.valueOf(song.year);
                    if (string.length() != 4) {
                        string = "-";
                    } else {
                        string = string.substring(2, 4);
                    }
                    requiresSubstring = false;
                    break;
                case SortManager.SongSort.ALBUM_NAME:
                    string = StringUtils.keyFor(song.albumName);
                    break;
                case SortManager.SongSort.ARTIST_NAME:
                    string = StringUtils.keyFor(song.artistName);
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
        return "";
    }
}