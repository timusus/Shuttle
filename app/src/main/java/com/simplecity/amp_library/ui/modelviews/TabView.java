package com.simplecity.amp_library.ui.modelviews;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.simplecity.amp_library.R;
import com.simplecity.amp_library.model.CategoryItem;
import com.simplecity.amp_library.utils.ShuttleUtils;

public class TabView extends BaseAdaptableItem<CategoryItem, TabView.ViewHolder> {

    public CategoryItem categoryItem;

    public TabView(CategoryItem categoryItem) {
        this.categoryItem = categoryItem;
    }

    @Override
    public int getViewType() {
        return ViewType.TAB;
    }

    @Override
    public int getLayoutResId() {
        return R.layout.list_item_reorder_tabs;
    }

    @Override
    public void bindView(ViewHolder holder) {

        holder.textView.setText(categoryItem.title);
        holder.checkBox.setChecked(categoryItem.isChecked());

        if (!ShuttleUtils.isUpgraded()) {
            if (categoryItem.title.equals(holder.itemView.getContext().getString(R.string.folders_title))) {
                holder.checkBox.setEnabled(false);
            } else {
                holder.checkBox.setEnabled(true);
            }
        }
    }

    @Override
    public ViewHolder getViewHolder(ViewGroup parent) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(getLayoutResId(), parent, false));
    }

    @Override
    public CategoryItem getItem() {
        return categoryItem;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public final TextView textView;
        public final CheckBox checkBox;
        public final View dragHandle;

        ViewHolder(View itemView) {
            super(itemView);

            textView = (TextView) itemView.findViewById(R.id.line_one);
            checkBox = (CheckBox) itemView.findViewById(R.id.checkBox1);
            dragHandle = itemView.findViewById(R.id.drag_handle);
        }

        @Override
        public String toString() {
            return "TabView.ViewHolder";
        }
    }
}
