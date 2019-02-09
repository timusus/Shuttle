package com.simplecity.amp_library.ui.views.recyclerview;

import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;

public class ItemTouchHelperCallback extends ItemTouchHelper.Callback {

    private int startPosition = -1;
    private int endPosition = -1;

    private boolean enabled = false;

    public interface OnItemMoveListener {
        void onItemMove(int fromPosition, int toPosition);
    }

    public interface OnDropListener {
        void onDrop(int fromPosition, int toPosition);
    }

    public interface OnClearListener {
        void onClear();
    }

    public interface OnSwipeListener {
        void onSwipe(int pos);
    }

    private OnItemMoveListener mItemMoveListener;
    private OnDropListener mOnDropListener;

    @Nullable
    private OnClearListener mOnClearListener;
    private OnSwipeListener mOnSwipeListener;

    public ItemTouchHelperCallback(OnItemMoveListener onMoveListener, OnDropListener onDropListener, @Nullable OnClearListener onClearListener, OnSwipeListener onSwipeListener) {
        mItemMoveListener = onMoveListener;
        mOnDropListener = onDropListener;
        mOnClearListener = onClearListener;
        mOnSwipeListener = onSwipeListener;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return false;
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {

        if (startPosition == -1) {
            startPosition = viewHolder.getAdapterPosition();
        }
        endPosition = target.getAdapterPosition();

        mItemMoveListener.onItemMove(viewHolder.getAdapterPosition(), endPosition);
        return true;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        mOnSwipeListener.onSwipe(viewHolder.getAdapterPosition());
    }

    @Override
    public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);

        if (startPosition != -1 && endPosition != -1) {
            mOnDropListener.onDrop(startPosition, endPosition);
        }

        startPosition = -1;
        endPosition = -1;

        if (mOnClearListener != null) {
            mOnClearListener.onClear();
        }
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
        return makeMovementFlags(dragFlags, swipeFlags);
    }
}
