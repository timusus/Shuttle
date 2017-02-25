package com.simplecity.amp_library.ui.modelviews;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.simplecity.amp_library.R;
import com.simplecity.amp_library.model.AdaptableItem;
import com.simplecity.amp_library.ui.adapters.ItemAdapter;
import com.simplecity.amp_library.ui.recyclerview.CustomSwappingHolder;
import com.simplecity.amp_library.utils.ThemeUtils;

import java.util.List;

public class HorizontalRecyclerView extends BaseAdaptableItem<Object, HorizontalRecyclerView.ViewHolder> {

    public HorizontalAdapter itemAdapter;

    public HorizontalRecyclerView() {
        this.itemAdapter = new HorizontalAdapter();
    }

    public void setItems(List<AdaptableItem> items) {
        itemAdapter.setItems(items);
    }

    public void setListener(HorizontalAdapter.ItemListener listener) {
        itemAdapter.setListener(listener);
    }

    public int getCount() {
        return itemAdapter.getItemCount();
    }

    @Override
    public int getViewType() {
        return ViewType.HORIZONTAL_RECYCLERVIEW;
    }

    @Override
    public int getLayoutResId() {
        return R.layout.recycler_header;
    }

    @Override
    public void bindView(ViewHolder holder) {
        ((RecyclerView) holder.itemView).setAdapter(itemAdapter);
    }

    @Override
    public ViewHolder getViewHolder(ViewGroup parent) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(getLayoutResId(), parent, false));
    }

    public static class ViewHolder extends CustomSwappingHolder {

        public ViewHolder(View itemView) {
            super(itemView);

            LinearLayoutManager layoutManager = new LinearLayoutManager(itemView.getContext(), LinearLayoutManager.HORIZONTAL, false);
            //Todo: Reinstate when updating support lib
//            layoutManager.setInitialPrefetchItemCount(4);
            ((RecyclerView) itemView).setLayoutManager(layoutManager);
            //noinspection RedundantCast
            ((RecyclerView) itemView).setNestedScrollingEnabled(false);
            ThemeUtils.themeRecyclerView(((RecyclerView) itemView));

            ((RecyclerView) itemView).addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    ThemeUtils.themeRecyclerView(recyclerView);
                    super.onScrollStateChanged(recyclerView, newState);
                }
            });
        }

        @Override
        public String toString() {
            return "HorizontalRecyclerView.ViewHolder";
        }
    }

    public static class HorizontalAdapter extends ItemAdapter {

        public interface ItemListener {

            void onItemClick(ItemAdapter adapter, View v, int position, Object item);

            void onOverflowClick(View v, int position, Object item);
        }

        private ItemListener listener;

        public void setListener(ItemListener listener) {
            this.listener = listener;
        }

        public Object getItem(int position) {
            return (items.get(position)).getItem();
        }

        @Override
        protected void attachListeners(RecyclerView.ViewHolder viewHolder) {
            super.attachListeners(viewHolder);

            viewHolder.itemView.setOnClickListener(v -> {
                if (listener != null && viewHolder.getAdapterPosition() != -1) {
                    listener.onItemClick(this, v, viewHolder.getAdapterPosition(), getItem(viewHolder.getAdapterPosition()));
                }
            });

            ((MultiItemView.ViewHolder) viewHolder).overflowButton.setOnClickListener(v -> {
                if (listener != null && viewHolder.getAdapterPosition() != -1) {
                    listener.onOverflowClick(v, viewHolder.getAdapterPosition(), getItem(viewHolder.getAdapterPosition()));
                }
            });
        }
    }
}