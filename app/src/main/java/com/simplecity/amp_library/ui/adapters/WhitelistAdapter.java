package com.simplecity.amp_library.ui.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.simplecity.amp_library.model.WhitelistFolder;
import com.simplecity.amp_library.ui.modelviews.WhitelistView;

public class WhitelistAdapter extends ItemAdapter {

    public interface WhitelistClickListener {
        void onRemove(View v, int position, WhitelistFolder songWhitelist);
    }

    private WhitelistClickListener listener;

    public void setWhitelistListener(WhitelistClickListener listener) {
        this.listener = listener;
    }

    @Override
    protected void attachListeners(RecyclerView.ViewHolder viewHolder) {
        super.attachListeners(viewHolder);

        if (viewHolder instanceof WhitelistView.ViewHolder) {
            ((WhitelistView.ViewHolder) viewHolder).overflow.setOnClickListener(v -> {
                if (viewHolder.getAdapterPosition() != -1) {
                    if (listener != null) {
                        listener.onRemove(v, viewHolder.getAdapterPosition(), ((WhitelistView) items.get(viewHolder.getAdapterPosition())).whitelistFolder);
                    }
                }
            });
        }
    }
}
