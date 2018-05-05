package com.simplecity.amp_library.ui.modelviews;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.simplecityapps.recycler_adapter.model.BaseViewModel;
import com.simplecityapps.recycler_adapter.recyclerview.BaseViewHolder;

import static com.simplecity.amp_library.R.id;
import static com.simplecity.amp_library.R.layout.list_item_disc_number;
import static com.simplecity.amp_library.R.string.disc_number_label;
import static com.simplecity.amp_library.ui.adapters.ViewType.DISC_NUMBER;

public class DiscNumberView extends BaseViewModel<DiscNumberView.ViewHolder> {

    private int discNumber = 0;

    public DiscNumberView(int discNumber) {
        this.discNumber = discNumber;
    }

    @Override
    public int getViewType() {
        return DISC_NUMBER;
    }

    @Override
    public int getLayoutResId() {
        return list_item_disc_number;
    }

    @Override
    public void bindView(ViewHolder holder) {
        super.bindView(holder);
        holder.textView.setText(holder.itemView.getContext().getString(disc_number_label, discNumber));
    }

    @Override
    public ViewHolder createViewHolder(ViewGroup parent) {
        return new ViewHolder(createView(parent));
    }

    public static class ViewHolder extends BaseViewHolder {

        TextView textView;

        public ViewHolder(View itemView) {
            super(itemView);

            textView = itemView.findViewById(id.textView);
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
