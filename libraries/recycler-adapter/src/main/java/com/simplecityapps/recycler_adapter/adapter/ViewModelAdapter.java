package com.simplecityapps.recycler_adapter.adapter;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.util.DiffUtil;
import android.support.v7.util.ListUpdateCallback;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ViewGroup;
import com.simplecityapps.recycler_adapter.BuildConfig;
import com.simplecityapps.recycler_adapter.model.ContentsComparator;
import com.simplecityapps.recycler_adapter.model.ViewModel;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import java.util.ArrayList;
import java.util.List;

/**
 * A custom RecyclerView.Adapter used for adapting {@link ViewModel}'s.
 * <p>
 * To allow the RecyclerView to perform its animations, use {@link #setItems(List)}
 */
public class ViewModelAdapter extends RecyclerView.Adapter {

    private static final String TAG = "ViewModelAdapter";

    private boolean enableLogging = true;

    @Nullable
    private Disposable setItemsDisposable = null;

    /**
     * The dataset for this RecyclerView Adapter
     */
    public List<ViewModel> items = new ArrayList<>();

    @Override
    public int getItemViewType(int position) {
        return items.get(position).getViewType();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        for (ViewModel item : items) {
            if (viewType == item.getViewType()) {
                return item.createViewHolder(parent);
            }
        }
        throw new IllegalStateException("No ViewHolder found for viewType: " + viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        items.get(position).bindView(holder);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, @NonNull List payloads) {
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
     * @param items the new dataset ({@link List<ViewModel>})
     */
    public synchronized Disposable setItems(List<ViewModel> items) {
        return setItems(items, null);
    }

    /**
     * This method is used to transform the current dataset ({@link #items}) into the passed in list of items, performing
     * logic to remove, add and rearrange items in a way that allows the RecyclerView to animate properly.
     *
     * @param items the new dataset ({@link List<ViewModel>})
     * @param callback an optional {@link ListUpdateCallback}
     */
    @Nullable
    public synchronized Disposable setItems(List<ViewModel> items, @Nullable CompletionListUpdateCallback callback) {

        if (this.items == items) {
            return null;
        }

        if (setItemsDisposable != null && !setItemsDisposable.isDisposed()) {
            setItemsDisposable.dispose();
        }

        setItemsDisposable = Single.fromCallable(() -> DiffUtil.calculateDiff(new DiffCallback(this.items, items)))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(diffResult -> {
                    ViewModelAdapter.this.items = items;
                    diffResult.dispatchUpdatesTo(ViewModelAdapter.this);

                    if (BuildConfig.DEBUG) {
                        logDiffResult(diffResult);
                    }

                    if (callback != null) {
                        callback.onComplete();
                        diffResult.dispatchUpdatesTo(callback);
                    }
                });

        return setItemsDisposable;
    }

    private void logDiffResult(DiffUtil.DiffResult diffResult) {
        diffResult.dispatchUpdatesTo(new ListUpdateCallback() {
            @Override
            public void onInserted(int position, int count) {
                if (enableLogging && BuildConfig.DEBUG) {
                    Log.i(TAG, String.format("onInserted: position: %d, count: %d", position, count));
                }
            }

            @Override
            public void onRemoved(int position, int count) {
                if (enableLogging && BuildConfig.DEBUG) {
                    Log.i(TAG, String.format("onRemoved:position: %d, count: %d", position, count));
                }
            }

            @Override
            public void onMoved(int fromPosition, int toPosition) {
                if (enableLogging && BuildConfig.DEBUG) {
                    Log.i(TAG, String.format("onMoved: from: %d, to: %d", fromPosition, fromPosition));
                }
            }

            @Override
            public void onChanged(int position, int count, Object payload) {
                if (enableLogging && BuildConfig.DEBUG) {
                    Log.i(TAG, String.format("onChanged: position: %d, count: %d", position, count));
                }
            }
        });
    }

    /**
     * Add a single item to the dataset ({@link #items}), notifying the adapter of the insert
     *
     * @param position int
     * @param item the {@link ViewModel} to add
     */
    public void addItem(int position, ViewModel item) {
        items.add(position, item);
        notifyItemInserted(position);
    }

    /**
     * Add a single item to the dataset ({@link #items}), notifying the adapter of the insert
     *
     * @param item the {@link ViewModel} to add
     */
    public void addItem(ViewModel item) {
        items.add(item);
        notifyItemInserted(items.size());
    }

    /**
     * Add a list of items to the dataset, notifying the adapter of the insert(s).
     *
     * @param items the {@link List<ViewModel>} to add
     */
    public void addItems(List<ViewModel> items) {
        int previousItemCount = this.items.size();
        this.items.addAll(items);
        notifyItemRangeInserted(previousItemCount, items.size());
    }

    /**
     * Remove & return the item at items[position]
     *
     * @param position int
     * @return the {@link ViewModel} that was removed, or null if it couldn't be removed
     */
    @Nullable
    public ViewModel removeItem(int position) {
        if (getItemCount() == 0 || position < 0 || position >= items.size()) {
            return null;
        }
        final ViewModel model = items.remove(position);
        notifyItemRemoved(position);
        return model;
    }

    /**
     * Remove & return the passed in item
     *
     * @param item the {@link ViewModel} to remove
     * @return the {@link ViewModel} that was removed, or null if it couldn't be removed.
     */
    @Nullable
    public ViewModel removeItem(@Nullable ViewModel item) {
        if (item == null) {
            return null;
        }
        return removeItem(items.indexOf(item));
    }

    /**
     * Moves an item from {@param fromPosition} to {@param toPosition}
     *
     * @param fromPosition int
     * @param toPosition int
     */
    public void moveItem(int fromPosition, int toPosition) {
        final ViewModel model = items.remove(fromPosition);
        items.add(toPosition, model);
        notifyItemMoved(fromPosition, toPosition);
    }

    private static class DiffCallback extends DiffUtil.Callback {

        private List<ViewModel> oldList;
        private List<ViewModel> newList;

        DiffCallback(List<ViewModel> oldList, List<ViewModel> newList) {
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