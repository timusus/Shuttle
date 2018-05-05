package com.simplecity.amp_library.ui.modelviews;

import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ui.adapters.ViewType;
import com.simplecityapps.recycler_adapter.model.BaseViewModel;
import com.simplecityapps.recycler_adapter.recyclerview.BaseViewHolder;

public class ShuffleView extends BaseViewModel<ShuffleView.ViewHolder> {

    public interface ShuffleClickListener {
        void onShuffleItemClick();
    }

    @StringRes
    private int titleResId = R.string.shuffle_all;

    public void setTitleResId(int titleResId) {
        this.titleResId = titleResId;
    }

    @Nullable
    private ShuffleClickListener listener;

    public void setClickListener(@Nullable ShuffleClickListener listener) {
        this.listener = listener;
    }

    @Override
    public int getViewType() {
        return ViewType.SHUFFLE;
    }

    @Override
    public int getLayoutResId() {
        return R.layout.list_item_shuffle;
    }

    @Override
    public ViewHolder createViewHolder(ViewGroup parent) {
        return new ViewHolder(createView(parent));
    }

    @Override
    public void bindView(ViewHolder holder) {
        super.bindView(holder);

        holder.title.setText(titleResId);
    }

    void onItemClick() {
        if (listener != null) {
            listener.onShuffleItemClick();
        }
    }

    public static class ViewHolder extends BaseViewHolder<ShuffleView> {

        @BindView(R.id.title)
        TextView title;

        public ViewHolder(View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);

            itemView.setOnClickListener(v -> viewModel.onItemClick());
        }

        @Override
        public String toString() {
            return "ShuffleView.ViewHolder";
        }
    }
}
