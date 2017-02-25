package com.simplecity.amp_library.ui.modelviews;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.simplecity.amp_library.R;
import com.simplecity.amp_library.model.WhitelistFolder;
import com.simplecity.amp_library.utils.DrawableUtils;

public class WhitelistView extends BaseAdaptableItem<WhitelistFolder, WhitelistView.ViewHolder> {

    public WhitelistFolder whitelistFolder;

    public WhitelistView(WhitelistFolder whitelistFolder) {
        this.whitelistFolder = whitelistFolder;
    }

    @Override
    public int getViewType() {
        return ViewType.BLACKLIST;
    }

    @Override
    public int getLayoutResId() {
        return R.layout.list_item_one_line;
    }

    @Override
    public void bindView(ViewHolder holder) {
        holder.lineOne.setText(whitelistFolder.folder);
    }

    @Override
    public ViewHolder getViewHolder(ViewGroup parent) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(getLayoutResId(), parent, false));
    }

    @Override
    public WhitelistFolder getItem() {
        return whitelistFolder;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public TextView lineOne;
        public ImageButton overflow;

        public ViewHolder(View itemView) {
            super(itemView);
            lineOne = (TextView) itemView.findViewById(R.id.line_one);
            lineOne.setSingleLine(false);
            overflow = (ImageButton) itemView.findViewById(R.id.btn_overflow);
            overflow.setImageDrawable(DrawableUtils.getBaseDrawable(itemView.getContext(), R.drawable.ic_cancel));
        }

        @Override
        public String toString() {
            return "WhitelistView.ViewHolder";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WhitelistView that = (WhitelistView) o;

        return whitelistFolder != null ? whitelistFolder.equals(that.whitelistFolder) : that.whitelistFolder == null;
    }

    @Override
    public int hashCode() {
        return whitelistFolder != null ? whitelistFolder.hashCode() : 0;
    }

    @Override
    public boolean areContentsEqual(Object other) {
        return false;
    }
}
