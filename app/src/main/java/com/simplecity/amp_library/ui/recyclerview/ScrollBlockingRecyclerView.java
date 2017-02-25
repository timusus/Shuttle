package com.simplecity.amp_library.ui.recyclerview;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;
import com.sothree.slidinguppanel.NestedScrollBlocker;
import com.sothree.slidinguppanel.ScrollableViewHelper;

/**
 * A custom RecyclerView which contains a flag indicating whether a parent SlidingUpPanel
 * is allowed to respond to touch events.
 * <p>
 * Used with {@link ScrollableViewHelper}
 */
public class ScrollBlockingRecyclerView extends FastScrollRecyclerView implements NestedScrollBlocker {

    boolean blockScroll = true;

    public ScrollBlockingRecyclerView(Context context) {
        super(context);
    }

    public ScrollBlockingRecyclerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ScrollBlockingRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void setBlockScroll(boolean blockScroll) {
        this.blockScroll = blockScroll;
    }

    @Override
    public boolean getBlockScroll() {
        return blockScroll;
    }
}
