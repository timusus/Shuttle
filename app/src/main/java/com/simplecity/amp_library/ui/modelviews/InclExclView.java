package com.simplecity.amp_library.ui.modelviews;

import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.model.InclExclItem;
import com.simplecity.amp_library.ui.adapters.ViewType;
import com.simplecity.amp_library.ui.views.OverflowButton;
import com.simplecityapps.recycler_adapter.model.BaseViewModel;
import com.simplecityapps.recycler_adapter.recyclerview.BaseViewHolder;

public class InclExclView extends BaseViewModel<InclExclView.ViewHolder> {

    public interface ClickListener {
        void onRemove(InclExclView InclExclView);
    }

    public InclExclItem inclExclItem;

    public InclExclView(InclExclItem inclExclItem) {
        this.inclExclItem = inclExclItem;
    }

    @Nullable
    ClickListener listener;

    public void setClickListener(@Nullable ClickListener listener) {
        this.listener = listener;
    }

    void onRemove() {
        if (listener != null) {
            listener.onRemove(this);
        }
    }

    @Override
    public int getViewType() {
        return ViewType.INCL_EXCL;
    }

    @Override
    public int getLayoutResId() {
        return R.layout.list_item_one_line;
    }

    @Override
    public void bindView(ViewHolder holder) {
        super.bindView(holder);

        holder.lineOne.setText(inclExclItem.path);
    }

    @Override
    public ViewHolder createViewHolder(ViewGroup parent) {
        return new ViewHolder(createView(parent));
    }

    public static class ViewHolder extends BaseViewHolder<InclExclView> {

        @BindView(R.id.line_one)
        public TextView lineOne;

        @BindView(R.id.btn_overflow)
        public OverflowButton overflow;

        public ViewHolder(View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);

            lineOne.setSingleLine(false);

            overflow.drawable = DrawableCompat.wrap(ContextCompat.getDrawable(itemView.getContext(), R.drawable.ic_close_24dp)).mutate();

            overflow.setOnClickListener(v -> viewModel.onRemove());
        }

        @Override
        public String toString() {
            return "InclExclView.ViewHolder";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InclExclView that = (InclExclView) o;

        return inclExclItem != null ? inclExclItem.equals(that.inclExclItem) : that.inclExclItem == null;
    }

    @Override
    public int hashCode() {
        return inclExclItem != null ? inclExclItem.hashCode() : 0;
    }

    @Override
    public boolean areContentsEqual(Object other) {
        return false;
    }
}
