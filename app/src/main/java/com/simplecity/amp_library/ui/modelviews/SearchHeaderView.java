package com.simplecity.amp_library.ui.modelviews;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.simplecity.amp_library.R;
import com.simplecity.amp_library.model.Header;

public class SearchHeaderView extends BaseAdaptableItem<Header, SearchHeaderView.ViewHolder> {

    private Header header;

    public SearchHeaderView(Header header) {
        this.header = header;
    }

    @Override
    public int getViewType() {
        return ViewType.SEARCH_HEADER;
    }

    @Override
    public int getLayoutResId() {
        return R.layout.list_item_section_separator;
    }

    @Override
    public void bindView(ViewHolder holder) {
        holder.lineOne.setText(header.title);
    }

    @Override
    public ViewHolder getViewHolder(ViewGroup parent) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(getLayoutResId(), parent, false));
    }

    @Override
    public Header getItem() {
        return header;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView lineOne;

        public ViewHolder(View itemView) {
            super(itemView);

            lineOne = (TextView) itemView.findViewById(R.id.line_one);
        }

        @Override
        public String toString() {
            return "SearchHeaderView.ViewHolder";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SearchHeaderView that = (SearchHeaderView) o;

        return header != null ? header.equals(that.header) : that.header == null;

    }

    @Override
    public int hashCode() {
        return header != null ? header.hashCode() : 0;
    }
}
