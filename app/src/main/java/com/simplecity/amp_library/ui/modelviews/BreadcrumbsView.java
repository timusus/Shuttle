package com.simplecity.amp_library.ui.modelviews;

import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ui.views.BreadcrumbView;
import com.simplecity.amp_library.utils.ColorUtils;

public class BreadcrumbsView extends BaseAdaptableItem<String, BreadcrumbsView.ViewHolder> {

    private String breadcrumbPath;

    public BreadcrumbsView(String breadcrumbPath) {
        this.breadcrumbPath = breadcrumbPath;
    }

    public void setBreadcrumbsPath(String path) {
        breadcrumbPath = path;
    }

    @Override
    public int getViewType() {
        return ViewType.BREADCRUMBS;
    }

    @Override
    public int getLayoutResId() {
        return R.layout.list_item_breadcrumbs;
    }

    @Override
    public void bindView(ViewHolder holder) {
        if (!TextUtils.isEmpty(breadcrumbPath)) {
            holder.breadcrumbView.changeBreadcrumbPath(breadcrumbPath);
        }
    }

    @Override
    public ViewHolder getViewHolder(ViewGroup parent) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(getLayoutResId(), parent, false));
    }

    @Override
    public String getItem() {
        return breadcrumbPath;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public BreadcrumbView breadcrumbView;

        public ViewHolder(View itemView) {
            super(itemView);

            breadcrumbView = (BreadcrumbView) itemView.findViewById(R.id.breadcrumbs);
            breadcrumbView.setTextColor(ColorUtils.getTextColorPrimary());
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
