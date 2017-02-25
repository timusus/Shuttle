package com.simplecity.amp_library.ui.modelviews;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.simplecity.amp_library.R;
import com.simplecity.amp_library.model.SuggestedHeader;
import com.simplecity.amp_library.utils.ColorUtils;
import com.simplecity.amp_library.utils.DrawableUtils;

public class SuggestedHeaderView extends BaseAdaptableItem<SuggestedHeader, SuggestedHeaderView.ViewHolder> {

    public SuggestedHeader suggestedHeader;

    public SuggestedHeaderView(SuggestedHeader suggestedHeader) {
        this.suggestedHeader = suggestedHeader;
    }

    @Override
    public int getViewType() {
        return ViewType.SUGGESTED_HEADER;
    }

    @Override
    public int getLayoutResId() {
        return R.layout.suggested_header;
    }

    @Override
    public void bindView(ViewHolder holder) {
        holder.titleOne.setText(suggestedHeader.title);
        holder.titleTwo.setText(suggestedHeader.subtitle);
        holder.titleThree.setBackground(DrawableUtils.getColoredAccentDrawable((holder.itemView.getContext()), holder.titleThree.getBackground(), false, true));
        holder.titleThree.setTextColor(ColorUtils.getAccentColorSensitiveTextColor(holder.itemView.getContext()));
        if (suggestedHeader.subtitle == null || suggestedHeader.subtitle.length() == 0) {
            holder.titleTwo.setVisibility(View.GONE);
        } else {
            holder.titleTwo.setVisibility(View.VISIBLE);
        }

        holder.itemView.setContentDescription(suggestedHeader.title);
    }

    @Override
    public ViewHolder getViewHolder(ViewGroup parent) {
        return new SuggestedHeaderView.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(getLayoutResId(), parent, false));
    }

    @Override
    public SuggestedHeader getItem() {
        return suggestedHeader;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView titleOne;
        TextView titleTwo;
        TextView titleThree;

        public ViewHolder(View itemView) {
            super(itemView);
            titleOne = (TextView) itemView.findViewById(R.id.text1);
            titleTwo = (TextView) itemView.findViewById(R.id.text2);
            titleThree = (TextView) itemView.findViewById(R.id.text3);
        }

        @Override
        public String toString() {
            return "SuggestedHeaderView.ViewHolder";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SuggestedHeaderView that = (SuggestedHeaderView) o;

        return suggestedHeader != null ? suggestedHeader.equals(that.suggestedHeader) : that.suggestedHeader == null;

    }

    @Override
    public int hashCode() {
        return suggestedHeader != null ? suggestedHeader.hashCode() : 0;
    }
}
