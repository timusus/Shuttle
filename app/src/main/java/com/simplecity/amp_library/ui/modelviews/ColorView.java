package com.simplecity.amp_library.ui.modelviews;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ui.views.CircleView;

public class ColorView extends BaseAdaptableItem<Integer, ColorView.ViewHolder> {

    public int color;

    public boolean selected;

    public ColorView(int color) {
        this.color = color;
    }

    @Override
    public int getViewType() {
        return ViewType.COLOR;
    }

    @Override
    public int getLayoutResId() {
        return R.layout.list_item_color;
    }

    @Override
    public void bindView(ViewHolder holder) {
        holder.circleView.setColor(color);
        holder.circleView.setActivated(selected);
    }

    @Override
    public ViewHolder getViewHolder(ViewGroup parent) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(getLayoutResId(), parent, false));
    }

    @Override
    public Integer getItem() {
        return color;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        CircleView circleView;

        public ViewHolder(final View itemView) {
            super(itemView);
            circleView = (CircleView) itemView.findViewById(R.id.image);
        }

        @Override
        public String toString() {
            return "ColorView.ViewHolder";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ColorView colorView = (ColorView) o;

        return color == colorView.color && selected == colorView.selected;

    }

    @Override
    public int hashCode() {
        int result = color;
        result = 31 * result + (selected ? 1 : 0);
        return result;
    }
}
