package com.simplecity.amp_library.ui.modelviews;

import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.simplecity.amp_library.model.WhitelistFolder;
import com.simplecityapps.recycler_adapter.model.BaseViewModel;
import com.simplecityapps.recycler_adapter.recyclerview.BaseViewHolder;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.simplecity.amp_library.R.id.btn_overflow;
import static com.simplecity.amp_library.R.id.line_one;
import static com.simplecity.amp_library.R.layout.list_item_one_line;
import static com.simplecity.amp_library.ui.adapters.ViewType.BLACKLIST;

public class WhitelistView extends BaseViewModel<WhitelistView.ViewHolder> {

    public interface ClickListener {
        void onRemove(WhitelistView WhitelistView);
    }

    public WhitelistFolder whitelistFolder;

    public WhitelistView(WhitelistFolder whitelistFolder) {
        this.whitelistFolder = whitelistFolder;
    }

    @Nullable ClickListener listener;

    public void setClickListener(@Nullable ClickListener listener) {
        this.listener = listener;
    }

    private void onRemove() {
        if (listener != null) {
            listener.onRemove(this);
        }
    }

    @Override
    public int getViewType() {
        return BLACKLIST;
    }

    @Override
    public int getLayoutResId() {
        return list_item_one_line;
    }

    @Override
    public void bindView(ViewHolder holder) {
        super.bindView(holder);

        holder.lineOne.setText(whitelistFolder.folder);
    }

    @Override
    public ViewHolder createViewHolder(ViewGroup parent) {
        return new ViewHolder(createView(parent));
    }

    public static class ViewHolder extends BaseViewHolder<WhitelistView> {

        @BindView(line_one)
        public TextView lineOne;

        @BindView(btn_overflow)
        public ImageButton overflow;

        public ViewHolder(View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);

            lineOne.setSingleLine(false);
            overflow.setOnClickListener(v -> viewModel.onRemove());
        }

        @Override
        public String toString() {
            return "WhitelistView.ViewHolder";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WhitelistView that = (WhitelistView) o;

        return whitelistFolder != null ? whitelistFolder.equals(that.whitelistFolder) : that.whitelistFolder == null;
    }

    @Override
    public int hashCode() {
        return whitelistFolder != null ? whitelistFolder.hashCode() : 0;
    }

    @Override
    public boolean areContentsEqual(Object other) {
        return false;
    }
}
