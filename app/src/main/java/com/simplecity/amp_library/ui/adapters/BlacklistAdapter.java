package com.simplecity.amp_library.ui.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.ui.modelviews.BlacklistView;

public class BlacklistAdapter extends ItemAdapter {

    public interface BlacklistClickListener {
        void onRemove(View v, int position, Song song);
    }

    private BlacklistClickListener listener;

    public void setBlackListListener(BlacklistClickListener listener) {
        this.listener = listener;
    }

    @Override
    protected void attachListeners(RecyclerView.ViewHolder viewHolder) {
        super.attachListeners(viewHolder);

        if (viewHolder instanceof BlacklistView.ViewHolder) {
            ((BlacklistView.ViewHolder) viewHolder).overflow.setOnClickListener(v -> {
                if (viewHolder.getAdapterPosition() != -1) {
                    if (listener != null) {
                        listener.onRemove(v, viewHolder.getAdapterPosition(), ((BlacklistView) items.get(viewHolder.getAdapterPosition())).song);
                    }
                }
            });
        }
    }
}