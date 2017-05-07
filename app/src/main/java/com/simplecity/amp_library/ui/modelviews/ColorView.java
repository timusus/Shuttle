package com.simplecity.amp_library.ui.modelviews;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ui.views.CircleView;
import com.simplecityapps.recycler_adapter.model.BaseViewModel;
import com.simplecity.amp_library.ui.adapters.ViewType;
import com.simplecityapps.recycler_adapter.recyclerview.BaseViewHolder;

import static com.simplecity.amp_library.R.id;
import static com.simplecity.amp_library.R.id.image;
import static com.simplecity.amp_library.R.layout;
import static com.simplecity.amp_library.R.layout.list_item_color;
import static com.simplecity.amp_library.ui.adapters.ViewType.COLOR;

public class ColorView extends BaseViewModel<ColorView.ViewHolder> {

    public int color;

    public boolean selected;

    public ColorView(int color) {
        this.color = color;
    }

    @Override
    public int getViewType() {
        return COLOR;
    }

    @Override
    public int getLayoutResId() {
        return list_item_color;
    }

    @Override
    public void bindView(ViewHolder holder) {
        super.bindView(holder);

        holder.circleView.setColor(color);
        holder.circleView.setActivated(selected);
    }

    @Override
    public ViewHolder createViewHolder(ViewGroup parent) {
        return new ViewHolder(createView(parent));
    }

    public class ViewHolder extends BaseViewHolder {

        CircleView circleView;

        public ViewHolder(final View itemView) {
            super(itemView);
            circleView = (CircleView) itemView.findViewById(image);
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
