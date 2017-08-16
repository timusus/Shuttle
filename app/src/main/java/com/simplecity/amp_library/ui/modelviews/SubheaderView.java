package com.simplecity.amp_library.ui.modelviews;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.simplecityapps.recycler_adapter.model.BaseViewModel;
import com.simplecityapps.recycler_adapter.recyclerview.BaseViewHolder;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.simplecity.amp_library.R.id;
import static com.simplecity.amp_library.R.layout.list_item_subheader;
import static com.simplecity.amp_library.ui.adapters.ViewType.SUBHEADER;

public class SubheaderView extends BaseViewModel<SubheaderView.ViewHolder> {

    private String title;

    public SubheaderView(String title) {
        this.title = title;
    }

    @Override
    public int getViewType() {
        return SUBHEADER;
    }

    @Override
    public int getLayoutResId() {
        return list_item_subheader;
    }

    @Override
    public ViewHolder createViewHolder(ViewGroup parent) {
        return new ViewHolder(createView(parent));
    }

    @Override
    public void bindView(ViewHolder holder) {
        super.bindView(holder);

        holder.textView.setText(title);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SubheaderView that = (SubheaderView) o;

        return title != null ? title.equals(that.title) : that.title == null;
    }

    @Override
    public int hashCode() {
        return title != null ? title.hashCode() : 0;
    }

    public static class ViewHolder extends BaseViewHolder {

        @BindView(id.textView)
        TextView textView;

        public ViewHolder(View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);
        }
    }
}
