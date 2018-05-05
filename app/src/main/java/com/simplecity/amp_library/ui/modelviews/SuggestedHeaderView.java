package com.simplecity.amp_library.ui.modelviews;

import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.model.SuggestedHeader;
import com.simplecity.amp_library.ui.adapters.ViewType;
import com.simplecityapps.recycler_adapter.model.BaseViewModel;
import com.simplecityapps.recycler_adapter.recyclerview.BaseViewHolder;

public class SuggestedHeaderView extends BaseViewModel<SuggestedHeaderView.ViewHolder> {

    public interface ClickListener {
        void onSuggestedHeaderClick(SuggestedHeader suggestedHeader);
    }

    private SuggestedHeader suggestedHeader;

    @Nullable
    private ClickListener listener;

    public SuggestedHeaderView(SuggestedHeader suggestedHeader) {
        this.suggestedHeader = suggestedHeader;
    }

    public void setClickListener(@Nullable ClickListener listener) {
        this.listener = listener;
    }

    void onClick() {
        if (listener != null) {
            listener.onSuggestedHeaderClick(suggestedHeader);
        }
    }

    @Override
    public int getViewType() {
        return ViewType.SUGGESTED_HEADER;
    }

    @Override
    public int getLayoutResId() {
        return R.layout.suggested_header;
    }

    @Override
    public void bindView(ViewHolder holder) {
        super.bindView(holder);

        holder.titleOne.setText(suggestedHeader.title);
        holder.titleTwo.setText(suggestedHeader.subtitle);
        if (suggestedHeader.subtitle == null || suggestedHeader.subtitle.length() == 0) {
            holder.titleTwo.setVisibility(View.GONE);
        } else {
            holder.titleTwo.setVisibility(View.VISIBLE);
        }

        holder.itemView.setContentDescription(suggestedHeader.title);
    }

    @Override
    public ViewHolder createViewHolder(ViewGroup parent) {
        return new ViewHolder(createView(parent));
    }

    public static class ViewHolder extends BaseViewHolder<SuggestedHeaderView> {

        @BindView(R.id.text1)
        TextView titleOne;

        @BindView(R.id.text2)
        TextView titleTwo;

        @BindView(R.id.button)
        TextView button;

        public ViewHolder(View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);

            itemView.setOnClickListener(v -> viewModel.onClick());
        }

        @Override
        public String toString() {
            return "SuggestedHeaderView.ViewHolder";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SuggestedHeaderView that = (SuggestedHeaderView) o;

        return suggestedHeader != null ? suggestedHeader.equals(that.suggestedHeader) : that.suggestedHeader == null;
    }

    @Override
    public int hashCode() {
        return suggestedHeader != null ? suggestedHeader.hashCode() : 0;
    }
}
