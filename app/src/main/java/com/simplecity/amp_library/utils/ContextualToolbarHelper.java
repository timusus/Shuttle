package com.simplecity.amp_library.utils;

import android.support.annotation.NonNull;

import com.annimon.stream.Stream;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.ui.modelviews.SelectableViewModel;
import com.simplecity.amp_library.ui.views.ContextualToolbar;

import java.util.ArrayList;
import java.util.List;

public class ContextualToolbarHelper<T> {

    public interface Callback {
        void notifyItemChanged(int position);

        void notifyDatasetChanged();
    }

    @NonNull
    private ContextualToolbar contextualToolbar;

    private boolean isActive = false;

    private List<SelectableViewModel<T>> items = new ArrayList<>();

    @NonNull
    private Callback callback;

    public ContextualToolbarHelper(@NonNull ContextualToolbar contextualToolbar, @NonNull Callback callback) {
        this.contextualToolbar = contextualToolbar;
        this.callback = callback;
    }

    /**
     * Call to show the Contextual Toolbar, and begin tracking item selections.
     */
    public void start() {
        contextualToolbar.show();
        contextualToolbar.setNavigationOnClickListener(v -> finish());
        isActive = true;
    }

    /**
     * Deselects all current items and notifies the adapter. Hides the Contextual Toolbar, removes callbacks
     * and sets isActive to false.
     */
    public void finish() {
        if (!items.isEmpty()) {
            Stream.of(items).forEach(viewModel -> viewModel.setSelected(false));
            callback.notifyDatasetChanged();
        }

        items.clear();

        contextualToolbar.hide();

        contextualToolbar.setNavigationOnClickListener(null);

        isActive = false;
    }

    /**
     * Called ot update the toolbar's title to reflect the number of selected items.
     */
    private void updateCount() {
        contextualToolbar.setTitle(ShuttleApplication.getInstance().getString(R.string.action_mode_selection_count, items.size()));
    }

    /**
     * If the item is not present in the list, it will be added and set to 'selected', and vise-versa.
     * The selection count is updated each time this is called.
     * <p>
     * If removing the passed in item results in an empty list of selected items, finish() is called.
     *
     * @param item the item to select/deselect.
     */
    private void addOrRemoveItem(SelectableViewModel<T> item) {
        if (items.contains(item)) {
            items.remove(item);
            item.setSelected(false);
        } else {
            items.add(item);
            item.setSelected(true);
        }

        updateCount();

        if (items.isEmpty()) {
            finish();
        }
    }

    /**
     * If the contextual toolbar helper is 'active', then the clicked item will be added/removed to/from
     * the list of items, the adapter will be notified, and the selected count will be updated.
     * <p>
     * If not active, nothing happens, and this method will return false.
     *
     * @param position            the position of the click
     * @param selectableViewModel the selectableViewModel which was clicked
     * @return true if the click was consumed by the ContextualToolbarHelper, else false.
     */
    public boolean handleClick(int position, SelectableViewModel<T> selectableViewModel) {
        if (isActive) {
            addOrRemoveItem(selectableViewModel);
            callback.notifyItemChanged(position);
            return true;
        }
        return false;
    }

    /**
     * If the contextual toolbar helper is not 'active', it will be put into the active state.
     * <p>
     * If already active, nothing happens, and this method will return false.
     *
     * @param position            the position of the long click
     * @param selectableViewModel the selectableViewModel which was clicked
     * @return true if the long press was consumed by the ContextualToolbarHelper, else false.
     */
    public boolean handleLongClick(int position, SelectableViewModel<T> selectableViewModel) {
        if (!isActive) {
            start();
            addOrRemoveItem(selectableViewModel);
            callback.notifyItemChanged(position);
            return true;
        }
        return false;
    }

    /**
     * @return a List of the currently selected SelectableViewModels
     */
    public List<SelectableViewModel<T>> getItems() {
        return items;
    }
}