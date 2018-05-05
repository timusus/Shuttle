package com.simplecity.amp_library.ui.modelviews;

import android.support.annotation.StringRes;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.simplecityapps.recycler_adapter.model.BaseViewModel;
import com.simplecityapps.recycler_adapter.recyclerview.BaseViewHolder;

import static android.util.Log.i;
import static android.view.ViewGroup.LayoutParams;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static com.simplecity.amp_library.R.layout.empty_view;
import static com.simplecity.amp_library.ui.adapters.ViewType.EMPTY;

public class EmptyView extends BaseViewModel<EmptyView.ViewHolder> {

    private String text;

    private int resId = -1;

    private int height = 0;

    public EmptyView(String text) {
        this.text = text;
    }

    public EmptyView(@StringRes int resId) {
        this.resId = resId;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public int getViewType() {
        return EMPTY;
    }

    @Override
    public int getLayoutResId() {
        return empty_view;
    }

    @Override
    public void bindView(ViewHolder holder) {
        super.bindView(holder);

        if (resId != -1) {
            text = holder.itemView.getResources().getString(resId);
        }

        ((TextView) holder.itemView).setText(text);

        if (height != 0) {
            holder.itemView.setLayoutParams(new LayoutParams(MATCH_PARENT, height));
            i("EmptyView", "Setting height to: " + height);
        }
    }

    @Override
    public ViewHolder createViewHolder(ViewGroup parent) {
        return new ViewHolder(createView(parent));
    }

    public static class ViewHolder extends BaseViewHolder {

        public ViewHolder(View itemView) {
            super(itemView);
        }

        @Override
        public String toString() {
            return "EmptyView.ViewHolder";
        }
    }
}
