package com.simplecity.amp_library.ui.modelviews;

import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.simplecityapps.recycler_adapter.model.BaseViewModel;
import com.simplecityapps.recycler_adapter.recyclerview.BaseViewHolder;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.simplecity.amp_library.R.id;
import static com.simplecity.amp_library.R.layout.list_item_subheader;
import static com.simplecity.amp_library.R.layout.list_item_subheader_lock;
import static com.simplecity.amp_library.ui.adapters.ViewType.SUBHEADER;
import static com.simplecity.amp_library.ui.adapters.ViewType.SUBHEADERLOCK;

public class SubheaderLockView extends BaseViewModel<SubheaderLockView.ViewHolder> {

    public interface ClickListener {
        void onSubheaderLockClick(ImageView subHeaderLock);
    }

    private ImageView subHeaderLock;
    protected String title;

    @Nullable
    private ClickListener listener;

    public SubheaderLockView(String title) {
        this.title = title;
    }

    public void setClickListener(@Nullable ClickListener listener) {
        this.listener = listener;
    }

    void onClick() {
        if (listener != null) {
            listener.onSubheaderLockClick(subHeaderLock);
        }
    }

    @Override
    public int getViewType() {
        return SUBHEADERLOCK;
    }

    @Override
    public int getLayoutResId() {
        return list_item_subheader_lock;
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
    public void bindView(ViewHolder holder, int position, List payloads) {
        super.bindView(holder, position, payloads);

        holder.textView.setText(title);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SubheaderLockView that = (SubheaderLockView) o;

        return title != null ? title.equals(that.title) : that.title == null;
    }

    @Override
    public int hashCode() {
        return title != null ? title.hashCode() : 0;
    }

    @Override
    public boolean areContentsEqual(Object other) {

        if (other instanceof SubheaderLockView) {
            return ((SubheaderLockView) other).title.equals(title);
        }

        return false;
    }

    public static class ViewHolder extends BaseViewHolder<SubheaderLockView> {

        @BindView(id.textView)
        TextView textView;

        public ViewHolder(View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(v -> viewModel.onClick());
        }
    }
}
