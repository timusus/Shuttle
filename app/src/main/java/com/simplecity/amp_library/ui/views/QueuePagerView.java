package com.simplecity.amp_library.ui.views;

import com.simplecityapps.recycler_adapter.model.ViewModel;

import java.util.List;

public interface QueuePagerView {

    void loadData(List<ViewModel> items, int position);

    void updateQueuePosition(int position);
}