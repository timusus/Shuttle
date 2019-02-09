package com.simplecity.amp_library.ui.views.recyclerview;

import android.support.v7.widget.GridLayoutManager;
import com.simplecityapps.recycler_adapter.adapter.ViewModelAdapter;
import com.simplecityapps.recycler_adapter.model.ViewModel;
import java.util.List;

/**
 * A custom {@link GridLayoutManager.SpanSizeLookup} which determines the span size from the {@link ViewModel}
 * at the position of the lookup.
 */
public class AdaptableSpanSizeLookup extends GridLayoutManager.SpanSizeLookup {

    private List<ViewModel> items;
    private int spanCount;

    public AdaptableSpanSizeLookup(ViewModelAdapter ViewModelAdapter, int spanCount) {
        this.items = ViewModelAdapter.items;
        this.spanCount = spanCount;
    }

    @Override
    public int getSpanSize(int position) {

        if (position >= 0 && position < items.size()) {
            return items.get(position).getSpanSize(spanCount);
        }
        return 1;
    }
}