package com.simplecity.amp_library.ui.modelviews;

import android.view.View;
import android.view.ViewGroup;

import com.simplecity.amp_library.ui.views.BreadcrumbView;
import com.simplecityapps.recycler_adapter.model.BaseViewModel;
import com.simplecityapps.recycler_adapter.recyclerview.BaseViewHolder;

import static android.text.TextUtils.isEmpty;
import static com.simplecity.amp_library.R.id.breadcrumbs;
import static com.simplecity.amp_library.R.layout.list_item_breadcrumbs;
import static com.simplecity.amp_library.ui.adapters.ViewType.BREADCRUMBS;

public class BreadcrumbsView extends BaseViewModel<BreadcrumbsView.ViewHolder> {

    private String breadcrumbPath;

    public BreadcrumbsView(String breadcrumbPath) {
        this.breadcrumbPath = breadcrumbPath;
    }

    public void setBreadcrumbsPath(String path) {
        breadcrumbPath = path;
    }

    @Override
    public int getViewType() {
        return BREADCRUMBS;
    }

    @Override
    public int getLayoutResId() {
        return list_item_breadcrumbs;
    }

    @Override
    public void bindView(ViewHolder holder) {

        super.bindView(holder);

        if (!isEmpty(breadcrumbPath)) {
            holder.breadcrumbView.changeBreadcrumbPath(breadcrumbPath);
        }
    }

    @Override
    public ViewHolder createViewHolder(ViewGroup parent) {
        return new ViewHolder(createView(parent));
    }

    public static class ViewHolder extends BaseViewHolder {

        public BreadcrumbView breadcrumbView;

        public ViewHolder(View itemView) {
            super(itemView);

            breadcrumbView = (BreadcrumbView) itemView.findViewById(breadcrumbs);
        }

        @Override
        public String toString() {
            return "BreadcrumbsView.ViewHolder";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BreadcrumbsView that = (BreadcrumbsView) o;

        return breadcrumbPath != null ? breadcrumbPath.equals(that.breadcrumbPath) : that.breadcrumbPath == null;

    }

    @Override
    public int hashCode() {
        return breadcrumbPath != null ? breadcrumbPath.hashCode() : 0;
    }
}
