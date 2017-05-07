package com.simplecity.amp_library.ui.modelviews;

import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.simplecity.amp_library.model.CategoryItem;
import com.simplecityapps.recycler_adapter.model.BaseViewModel;
import com.simplecityapps.recycler_adapter.recyclerview.BaseViewHolder;

import static com.simplecity.amp_library.R.id.checkBox1;
import static com.simplecity.amp_library.R.id.drag_handle;
import static com.simplecity.amp_library.R.id.line_one;
import static com.simplecity.amp_library.R.layout.list_item_reorder_tabs;
import static com.simplecity.amp_library.R.string.folders_title;
import static com.simplecity.amp_library.ui.adapters.ViewType.TAB;
import static com.simplecity.amp_library.utils.ShuttleUtils.isUpgraded;

public class TabView extends BaseViewModel<TabView.ViewHolder> {

    public CategoryItem categoryItem;

    public TabView(CategoryItem categoryItem) {
        this.categoryItem = categoryItem;
    }

    @Override
    public int getViewType() {
        return TAB;
    }

    @Override
    public int getLayoutResId() {
        return list_item_reorder_tabs;
    }

    @Override
    public void bindView(ViewHolder holder) {

        holder.textView.setText(categoryItem.title);
        holder.checkBox.setChecked(categoryItem.isChecked());

        if (!isUpgraded()) {
            if (categoryItem.title.equals(holder.itemView.getContext().getString(folders_title))) {
                holder.checkBox.setEnabled(false);
            } else {
                holder.checkBox.setEnabled(true);
            }
        }
    }

    @Override
    public ViewHolder createViewHolder(ViewGroup parent) {
        return new ViewHolder(createView(parent));
    }

    public static class ViewHolder extends BaseViewHolder {

        public final TextView textView;
        public final CheckBox checkBox;
        public final View dragHandle;

        ViewHolder(View itemView) {
            super(itemView);

            textView = (TextView) itemView.findViewById(line_one);
            checkBox = (CheckBox) itemView.findViewById(checkBox1);
            dragHandle = itemView.findViewById(drag_handle);
        }

        @Override
        public String toString() {
            return "TabView.ViewHolder";
        }
    }
}
