package com.simplecity.amp_library.ui.recyclerview;

import android.support.v7.widget.GridLayoutManager;

import com.simplecity.amp_library.model.AdaptableItem;
import com.simplecity.amp_library.ui.adapters.ItemAdapter;

import java.util.List;

/**
 * A custom {@link GridLayoutManager.SpanSizeLookup} which determines the span size from the {@link AdaptableItem}
 * at the position of the lookup.
 */
public class AdaptableSpanSizeLookup extends GridLayoutManager.SpanSizeLookup {

    private List<AdaptableItem> items;
    private int spanCount;

    public AdaptableSpanSizeLookup(ItemAdapter itemAdapter, int spanCount) {
        this.items = itemAdapter.items;
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