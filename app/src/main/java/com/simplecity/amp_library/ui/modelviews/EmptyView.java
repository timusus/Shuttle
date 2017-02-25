package com.simplecity.amp_library.ui.modelviews;

import android.support.annotation.StringRes;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.simplecity.amp_library.R;

public class EmptyView extends BaseAdaptableItem<String, EmptyView.ViewHolder> {

    private String text;

    private int resId = -1;

    public EmptyView(String text) {
        this.text = text;
    }

    public EmptyView(@StringRes int resId) {
        this.resId = resId;
    }

    @Override
    public int getViewType() {
        return ViewType.EMPTY;
    }

    @Override
    public int getLayoutResId() {
        return R.layout.empty_view;
    }

    @Override
    public void bindView(ViewHolder holder) {
        if (resId != -1) {
            text = holder.itemView.getResources().getString(resId);
        }
        ((TextView) holder.itemView).setText(text);
    }

    @Override
    public ViewHolder getViewHolder(ViewGroup parent) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(getLayoutResId(), parent, false));
    }

    @Override
    public String getItem() {
        return text;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public ViewHolder(View itemView) {
            super(itemView);
        }

        @Override
        public String toString() {
            return "EmptyView.ViewHolder";
        }
    }
}
