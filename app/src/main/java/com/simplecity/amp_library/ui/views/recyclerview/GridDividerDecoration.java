package com.simplecity.amp_library.ui.views.recyclerview;

import android.content.res.Resources;
import android.graphics.Rect;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import com.simplecity.amp_library.utils.ResourceUtils;
import com.simplecityapps.recycler_adapter.adapter.ViewModelAdapter;
import com.simplecityapps.recycler_adapter.model.ViewModel;

public class GridDividerDecoration extends RecyclerView.ItemDecoration {

    private int spacing;
    private boolean includeEdge;

    public GridDividerDecoration(Resources res, int spacingDp, boolean includeEdge) {
        this.spacing = ResourceUtils.toPixels(spacingDp);
        this.includeEdge = includeEdge;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        if (!(parent.getLayoutManager() instanceof GridLayoutManager)) {
            throw new IllegalStateException("GridDividerDecoration can only be used with GridLayoutManager");
        }

        int spanCount = ((GridLayoutManager) parent.getLayoutManager()).getSpanCount();

        int position = parent.getChildAdapterPosition(view);
        if (position < 0) {
            return;
        }

        int spanIndex = ((GridLayoutManager) parent.getLayoutManager()).getSpanSizeLookup().getSpanIndex(position, spanCount);
        int groupIndex = ((GridLayoutManager) parent.getLayoutManager()).getSpanSizeLookup().getSpanGroupIndex(position, spanCount);

        RecyclerView.Adapter adapter = parent.getAdapter();
        if (adapter instanceof ViewModelAdapter) {
            ViewModel viewModel = ((ViewModelAdapter) adapter).items.get(position);
            // IF we have a full-span item, don't appy any decoration (unless it's the first group,
            // in which case we add bottom spacing if includeEdge is true)
            if (viewModel.getSpanSize(spanCount) == spanCount) {
                if (includeEdge && groupIndex == 0) {
                    outRect.bottom = spacing;
                }
                return;
            }
        }

        if (includeEdge) {
            outRect.left = spacing - spanIndex * spacing / spanCount;
            outRect.right = (spanIndex + 1) * spacing / spanCount;

            if (groupIndex == 0) {
                outRect.top = spacing;
            }
            outRect.bottom = spacing;
        } else {
            outRect.left = spanIndex * spacing / spanCount;
            outRect.right = spacing - (spanIndex + 1) * spacing / spanCount;
            if (groupIndex > 0) {
                outRect.top = spacing;
            }
        }
    }
}