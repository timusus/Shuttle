package com.simplecity.amp_library.ui.adapters;

import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.View;

import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.ui.modelviews.SongView;

public class DetailAdapter extends ItemAdapter {

    private Listener mListener;

    public interface Listener {

        void onItemClick(View v, int position, Song song);

        void onOverflowClick(View v, int position, Song song);

        void onLongClick(View v, int position, Song song);

        void onStartDrag(RecyclerView.ViewHolder viewHolder);
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public Song getSong(int position) {
        return ((SongView) items.get(position)).song;
    }

    @Override
    protected void attachListeners(final RecyclerView.ViewHolder viewHolder) {
        super.attachListeners(viewHolder);

        if (viewHolder instanceof SongView.ViewHolder) {

            viewHolder.itemView.setOnClickListener(v -> {
                if (mListener != null && viewHolder.getAdapterPosition() != -1) {
                    mListener.onItemClick(v, viewHolder.getAdapterPosition(), getSong(viewHolder.getAdapterPosition()));
                }
            });

            ((SongView.ViewHolder) viewHolder).overflowButton.setOnClickListener(v -> {
                if (mListener != null && viewHolder.getAdapterPosition() != -1) {
                    mListener.onOverflowClick(v, viewHolder.getAdapterPosition(), getSong(viewHolder.getAdapterPosition()));
                }
            });

            ((SongView.ViewHolder) viewHolder).itemView.setOnLongClickListener(v -> {
                if (mListener != null && viewHolder.getAdapterPosition() != -1) {
                    mListener.onLongClick(v, viewHolder.getAdapterPosition(), getSong(viewHolder.getAdapterPosition()));
                }
                return true;
            });

            if (((SongView.ViewHolder) viewHolder).dragHandle != null) {
                ((SongView.ViewHolder) viewHolder).dragHandle.setOnTouchListener((v, event) -> {
                    if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
                        mListener.onStartDrag(viewHolder);
                    }
                    return false;
                });
            }
        }
    }
}