package com.simplecity.amp_library.ui.modelviews;

import android.support.annotation.CallSuper;
import com.simplecityapps.recycler_adapter.model.BaseViewModel;
import com.simplecityapps.recycler_adapter.recyclerview.BaseViewHolder;
import java.util.List;

public abstract class BaseSelectableViewModel<VH extends BaseViewHolder> extends BaseViewModel<VH> implements SelectableViewModel {

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
    public boolean areContentsEqual(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;

        BaseSelectableViewModel that = (BaseSelectableViewModel) other;

        return isSelected == that.isSelected;
    }
}
