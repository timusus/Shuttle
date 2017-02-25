package com.simplecity.amp_library.ui.modelviews;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.simplecity.amp_library.R;

public class DiscNumberView extends BaseAdaptableItem<Integer, DiscNumberView.ViewHolder> {

    private int discNumber = 0;

    public DiscNumberView(int discNumber) {
        this.discNumber = discNumber;
    }

    @Override
    public int getViewType() {
        return ViewType.DISC_NUMBER;
    }

    @Override
    public int getLayoutResId() {
        return R.layout.list_item_disc_number;
    }

    @Override
    public void bindView(ViewHolder holder) {
        holder.textView.setText(holder.itemView.getContext().getString(R.string.disc_number_label, discNumber));
    }

    @Override
    public ViewHolder getViewHolder(ViewGroup parent) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(getLayoutResId(), parent, false));
    }

    @Override
    public Integer getItem() {
        return discNumber;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView textView;

        public ViewHolder(View itemView) {
            super(itemView);

            textView = (TextView) itemView.findViewById(R.id.textView);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DiscNumberView that = (DiscNumberView) o;

        return discNumber == that.discNumber;

    }

    @Override
    public int hashCode() {
        return discNumber;
    }

    @Override
    public boolean areContentsEqual(Object other) {
        return equals(other);
    }
}
