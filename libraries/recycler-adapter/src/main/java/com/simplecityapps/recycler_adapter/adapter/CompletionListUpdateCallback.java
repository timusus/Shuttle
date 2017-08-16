package com.simplecityapps.recycler_adapter.adapter;

import android.support.v7.util.ListUpdateCallback;

/**
 * A custom {@link ListUpdateCallback} with an additional onComplete() methods, used for notifying when the
 * diff result has been calculated and supplied to the adapter.
 */
public interface CompletionListUpdateCallback extends ListUpdateCallback {

    /**
     * Called once the diff result has been calculated and supplied to the adapter.
     */
    void onComplete();
}
