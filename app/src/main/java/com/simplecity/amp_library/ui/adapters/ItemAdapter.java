package com.simplecity.amp_library.ui.adapters;

import android.support.annotation.Nullable;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import com.simplecity.amp_library.model.AdaptableItem;
import com.simplecity.amp_library.model.ContentsComparator;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * A custom RecyclerView.Adapter used for adapting {@link AdaptableItem}'s.
 * <p>
 * To allow the RecyclerView to perform its animations, use {@link #setItems(List)}
 */
public abstract class ItemAdapter extends RecyclerView.Adapter {

    private static final String TAG = "ItemAdapter";

    /**
     * The dataset for this RecyclerView Adapter
     */
    public List<AdaptableItem> items = new ArrayList<>();

    /**
     * This function exposes the viewHolder to allow subclasses to attach custom listeners to
     * viewHolder items. To do so, subclass ItemAdapter and override this function.
     * <p>
     * Note: This should be called prior to setting the data for this adapter, to ensure a listener is attached
     * to all View Holders.
     */
    protected void attachListeners(final RecyclerView.ViewHolder viewHolder) {

    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).getViewType();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        for (AdaptableItem item : items) {
            if (viewType == item.getViewType()) {
                RecyclerView.ViewHolder viewHolder = item.getViewHolder(parent);
                attachListeners(viewHolder);
                return viewHolder;
            }
        }
        throw new IllegalStateException("No ViewHolder found for viewType: " + viewType);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        items.get(position).bindView(holder);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position, List payloads) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position);
        } else {
            items.get(position).bindView(holder, position, payloads);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * This method is used to transform the current dataset ({@link #items}) into the passed in list of items, performing
     * logic to remove, add and rearrange items in a way that allows the RecyclerView to animate properly.
     *
     * @param items the new dataset ({@link List<AdaptableItem>})
     */
    public synchronized Subscription setItems(List<AdaptableItem> items) {
        if (this.items == items) {
            return null;
        }

        return Observable.fromCallable(() -> DiffUtil.calculateDiff(new DiffCallback(this.items, items)))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(diffResult -> {
                    ItemAdapter.this.items = items;
                    diffResult.dispatchUpdatesTo(ItemAdapter.this);
                });
    }

    /**
     * Add a single item to the dataset ({@link #items}), notifying the adapter of the insert
     *
     * @param position int
     * @param item     the {@link AdaptableItem} to add
     */
    public void addItem(int position, AdaptableItem item) {
        items.add(position, item);
        notifyItemInserted(position);
    }

    /**
     * Add a single item to the dataset ({@link #items}), notifying the adapter of the insert
     *
     * @param item the {@link AdaptableItem} to add
     */
    public void addItem(AdaptableItem item) {
        items.add(item);
        notifyItemInserted(items.size());
    }

    /**
     * Remove & return the item at items[position]
     *
     * @param position int
     * @return the {@link AdaptableItem} at items[position[
     */
    public AdaptableItem removeItem(int position) {
        if (getItemCount() == 0 || position < 0 || position >= items.size()) {
            return null;
        }
        final AdaptableItem model = items.remove(position);
        notifyItemRemoved(position);
        return model;
    }

    /**
     * Moves an item from {@param fromPosition} to {@param toPosition}
     *
     * @param fromPosition int
     * @param toPosition   int
     */
    public void moveItem(int fromPosition, int toPosition) {
        final AdaptableItem model = items.remove(fromPosition);
        items.add(toPosition, model);
        notifyItemMoved(fromPosition, toPosition);
    }

    public void setEmpty(AdaptableItem emptyView) {
        List<AdaptableItem> items = new ArrayList<>(1);
        items.add(emptyView);
        setItems(items);
    }

    private static class DiffCallback extends DiffUtil.Callback {

        private List<AdaptableItem> oldList;
        private List<AdaptableItem> newList;

        DiffCallback(List<AdaptableItem> oldList, List<AdaptableItem> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList != null ? oldList.size() : 0;
        }

        @Override
        public int getNewListSize() {
            return newList != null ? newList.size() : 0;
        }

        Object getOldItem(int oldItemPosition) {
            return oldList.get(oldItemPosition);
        }

        Object getNewItem(int newItemPosition) {
            return newList.get(newItemPosition);
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {

            Object oldItem = getOldItem(oldItemPosition);
            Object newItem = getNewItem(newItemPosition);

            return !(oldItem == null || newItem == null) && oldItem.equals(newItem);
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {

            Object oldItem = getOldItem(oldItemPosition);
            Object newItem = getNewItem(newItemPosition);

            if (oldItem instanceof ContentsComparator) {
                return ((ContentsComparator) oldItem).areContentsEqual(newItem);
            } else {
                return areItemsTheSame(oldItemPosition, newItemPosition);
            }
        }

        @Nullable
        @Override
        public Object getChangePayload(int oldItemPosition, int newItemPosition) {
            return 0;
        }
    }
}