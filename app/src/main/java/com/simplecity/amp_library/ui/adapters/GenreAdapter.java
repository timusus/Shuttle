package com.simplecity.amp_library.ui.adapters;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;

import com.simplecity.amp_library.model.Genre;
import com.simplecity.amp_library.ui.modelviews.GenreView;
import com.simplecity.amp_library.utils.StringUtils;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

public class GenreAdapter extends ItemAdapter implements
        FastScrollRecyclerView.SectionedAdapter {

    private GenreListener mListener;

    public interface GenreListener {

        void onItemClick(View v, int position, Genre genre);
    }

    public void setListener(GenreListener listener) {
        mListener = listener;
    }

    public Genre getGenre(int position) {
        return ((GenreView) items.get(position)).genre;
    }

    @Override
    protected void attachListeners(final RecyclerView.ViewHolder viewHolder) {

        if (viewHolder instanceof GenreView.ViewHolder) {
            viewHolder.itemView.setOnClickListener(v -> {
                if (mListener != null && viewHolder.getAdapterPosition() != -1) {
                    mListener.onItemClick(v, viewHolder.getAdapterPosition(), getGenre(viewHolder.getAdapterPosition()));
                }
            });
        }
    }

    @NonNull
    @Override
    public String getSectionName(int position) {

        if (items.get(position) == null) {
            return "";
        }

        String string = StringUtils.keyFor(getGenre(position).name);
        if (!TextUtils.isEmpty(string)) {
            string = string.substring(0, 1).toUpperCase();
        } else {
            string = " ";
        }

        return string;
    }
}