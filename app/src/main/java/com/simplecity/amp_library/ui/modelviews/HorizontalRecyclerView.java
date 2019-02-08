package com.simplecity.amp_library.ui.modelviews;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import com.simplecityapps.recycler_adapter.adapter.ViewModelAdapter;
import com.simplecityapps.recycler_adapter.model.BaseViewModel;
import com.simplecityapps.recycler_adapter.model.ViewModel;
import com.simplecityapps.recycler_adapter.recyclerview.BaseViewHolder;
import io.reactivex.disposables.Disposable;
import java.util.List;

import static android.support.v7.widget.LinearLayoutManager.HORIZONTAL;
import static com.simplecity.amp_library.R.layout.recycler_header;
import static com.simplecity.amp_library.ui.adapters.ViewType.HORIZONTAL_RECYCLERVIEW;

public class HorizontalRecyclerView extends BaseViewModel<HorizontalRecyclerView.ViewHolder> {

    public ViewModelAdapter viewModelAdapter;

    public HorizontalRecyclerView(String tag) {
        this.viewModelAdapter = new ViewModelAdapter();
    }

    public Disposable setItems(List<ViewModel> items) {
        return viewModelAdapter.setItems(items);
    }

    public int getCount() {
        return viewModelAdapter.getItemCount();
    }

    @Override
    public int getViewType() {
        return HORIZONTAL_RECYCLERVIEW;
    }

    @Override
    public int getLayoutResId() {
        return recycler_header;
    }

    @Override
    public void bindView(ViewHolder holder) {
        super.bindView(holder);

        ((RecyclerView) holder.itemView).setAdapter(viewModelAdapter);
    }

    @Override
    public ViewHolder createViewHolder(ViewGroup parent) {
        return new ViewHolder(createView(parent));
    }

    public static class ViewHolder extends BaseViewHolder {

        public ViewHolder(View itemView) {
            super(itemView);

            LinearLayoutManager layoutManager = new LinearLayoutManager(itemView.getContext(), HORIZONTAL, false);
            layoutManager.setInitialPrefetchItemCount(4);
            ((RecyclerView) itemView).setLayoutManager(layoutManager);
            //noinspection RedundantCast
            ((RecyclerView) itemView).setNestedScrollingEnabled(false);
        }

        @Override
        public String toString() {
            return "HorizontalRecyclerView.ViewHolder";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HorizontalRecyclerView that = (HorizontalRecyclerView) o;

        return viewModelAdapter != null ? viewModelAdapter.equals(that.viewModelAdapter) : that.viewModelAdapter == null;
    }

    @Override
    public int hashCode() {
        return viewModelAdapter != null ? viewModelAdapter.hashCode() : 0;
    }
}