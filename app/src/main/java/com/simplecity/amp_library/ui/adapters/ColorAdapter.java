package com.simplecity.amp_library.ui.adapters;

import android.support.v7.widget.RecyclerView;

import com.simplecity.amp_library.ui.modelviews.ColorView;

public class ColorAdapter extends ItemAdapter {

    public interface ColorListener {
        void onColorSelected(int position, int color, boolean isSubColor);
    }

    private ColorListener colorListener;

    public boolean isSubColor = false;

    public void setColorListener(ColorListener colorListener) {
        this.colorListener = colorListener;
    }

    public void setSelectedPosition(int position) {

        for (int i = 0, itemsSize = items.size(); i < itemsSize; i++) {
            ColorView item = (ColorView) items.get(i);
            if (item.selected) {
                item.selected = false;
                notifyItemChanged(i);
                break;
            }
        }

        ((ColorView) items.get(position)).selected = true;

        notifyItemChanged(position);
    }

    @Override
    protected void attachListeners(RecyclerView.ViewHolder viewHolder) {
        super.attachListeners(viewHolder);

        viewHolder.itemView.setOnClickListener(v -> {
            if (colorListener != null && viewHolder.getAdapterPosition() != -1) {
                colorListener.onColorSelected(viewHolder.getAdapterPosition(), ((ColorView) items.get(viewHolder.getAdapterPosition())).color, isSubColor);
            }
        });
    }
}
