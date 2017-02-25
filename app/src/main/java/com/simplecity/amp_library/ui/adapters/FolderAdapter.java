package com.simplecity.amp_library.ui.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.simplecity.amp_library.model.BaseFileObject;
import com.simplecity.amp_library.ui.modelviews.BreadcrumbsView;
import com.simplecity.amp_library.ui.modelviews.FolderView;
import com.simplecity.amp_library.ui.views.BreadcrumbItem;

public class FolderAdapter extends ItemAdapter {

    public interface Listener {

        void onItemClick(View v, int position, BaseFileObject fileObject);

        void onOverflowClick(View v, int position, BaseFileObject fileObject);

        void onBreadcrumbItemClick(BreadcrumbItem item);

        void onCheckedChange(FolderView folderView, boolean isChecked);
    }

    private Listener listener;

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public BaseFileObject getFileObject(int position) {
        return (BaseFileObject) items.get(position).getItem();
    }

    @Override
    protected void attachListeners(RecyclerView.ViewHolder viewHolder) {
        super.attachListeners(viewHolder);

        if (viewHolder instanceof BreadcrumbsView.ViewHolder) {
            ((BreadcrumbsView.ViewHolder) viewHolder).breadcrumbView.addBreadcrumbListener(item -> {
                if (viewHolder.getAdapterPosition() != -1 && listener != null) {
                    listener.onBreadcrumbItemClick(item);
                }
            });
        } else if (viewHolder instanceof FolderView.ViewHolder) {
            ((FolderView.ViewHolder) viewHolder).itemView.setOnClickListener(v -> {
                if (viewHolder.getAdapterPosition() != -1 && listener != null) {
                    listener.onItemClick(v, viewHolder.getAdapterPosition(), getFileObject(viewHolder.getAdapterPosition()));
                }
            });
            ((FolderView.ViewHolder) viewHolder).overflow.setOnClickListener(v -> {
                if (viewHolder.getAdapterPosition() != -1 && listener != null) {
                    listener.onOverflowClick(v, viewHolder.getAdapterPosition(), getFileObject(viewHolder.getAdapterPosition()));
                }
            });
            ((FolderView.ViewHolder) viewHolder).checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (viewHolder.getAdapterPosition() != -1 && listener != null) {
                    listener.onCheckedChange((FolderView) items.get(viewHolder.getAdapterPosition()), isChecked);
                }
            });
        }
    }
}