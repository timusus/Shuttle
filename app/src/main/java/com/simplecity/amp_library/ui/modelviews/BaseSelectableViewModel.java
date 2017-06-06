package com.simplecity.amp_library.ui.modelviews;

import android.support.annotation.CallSuper;

import com.simplecityapps.recycler_adapter.model.BaseViewModel;
import com.simplecityapps.recycler_adapter.recyclerview.BaseViewHolder;

import java.util.List;

public abstract class BaseSelectableViewModel<VH extends BaseViewHolder, T> extends BaseViewModel<VH> implements SelectableViewModel<T> {

    private boolean isSelected = false;

    @Override
    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    @Override
    public boolean isSelected() {
        return isSelected;
    }

    @Override
    @CallSuper
    public void bindView(VH holder) {
        super.bindView(holder);

        holder.itemView.setActivated(isSelected);
    }

    @Override
    @CallSuper
    public void bindView(VH holder, int position, List payloads) {
        super.bindView(holder, position, payloads);

        holder.itemView.setActivated(isSelected);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BaseSelectableViewModel<?, ?> that = (BaseSelectableViewModel<?, ?>) o;

        return isSelected == that.isSelected;
    }

    @Override
    public int hashCode() {
        return (isSelected ? 1 : 0);
    }
}
