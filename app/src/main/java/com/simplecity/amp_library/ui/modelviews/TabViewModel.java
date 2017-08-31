package com.simplecity.amp_library.ui.modelviews;

import android.support.annotation.Nullable;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.simplecity.amp_library.R;
import com.simplecity.amp_library.model.CategoryItem;
import com.simplecity.amp_library.ui.adapters.ViewType;
import com.simplecityapps.recycler_adapter.model.BaseViewModel;
import com.simplecityapps.recycler_adapter.recyclerview.BaseViewHolder;

public class TabViewModel extends BaseViewModel<TabViewModel.ViewHolder> {

    public interface Listener {
        void onStartDrag(ViewHolder holder);
    }

    public CategoryItem categoryItem;

    @Nullable
    private Listener listener;

    public TabViewModel(CategoryItem categoryItem) {
        this.categoryItem = categoryItem;
    }

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    @Override
    public int getViewType() {
        return ViewType.TAB;
    }

    @Override
    public int getLayoutResId() {
        return R.layout.list_item_reorder_tabs;
    }

    void onCheckboxClicked(boolean checked) {
        categoryItem.isEnabled = checked;
    }

    void onStartDrag(ViewHolder viewHolder) {
        if (listener != null) {
            listener.onStartDrag(viewHolder);
        }
    }

    @Override
    public void bindView(ViewHolder holder) {
        super.bindView(holder);

        holder.textView.setText(holder.itemView.getContext().getString(categoryItem.getTitleResId()));
        holder.checkBox.setChecked(categoryItem.isEnabled);
    }

    @Override
    public ViewHolder createViewHolder(ViewGroup parent) {
        return new ViewHolder(createView(parent));
    }

    public static class ViewHolder extends BaseViewHolder<TabViewModel> {

        public final TextView textView;
        public final CheckBox checkBox;
        public final View dragHandle;

        ViewHolder(View itemView) {
            super(itemView);

            textView = itemView.findViewById(R.id.line_one);
            checkBox = itemView.findViewById(R.id.checkBox1);
            dragHandle = itemView.findViewById(R.id.drag_handle);

            checkBox.setOnClickListener(view -> viewModel.onCheckboxClicked(((CheckBox) view).isChecked()));

            dragHandle.setOnTouchListener((v, event) -> {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    viewModel.onStartDrag(this);
                }
                return true;
            });
        }

        @Override
        public String toString() {
            return "TabViewModel.ViewHolder";
        }
    }
}
