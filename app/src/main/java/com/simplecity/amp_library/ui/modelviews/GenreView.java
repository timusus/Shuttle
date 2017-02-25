package com.simplecity.amp_library.ui.modelviews;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.simplecity.amp_library.R;
import com.simplecity.amp_library.model.Genre;
import com.simplecity.amp_library.ui.views.NonScrollImageButton;
import com.simplecity.amp_library.utils.StringUtils;

public class GenreView extends BaseAdaptableItem<Genre, GenreView.ViewHolder> {

    public Genre genre;

    public GenreView(Genre genre) {
        this.genre = genre;
    }

    @Override
    public int getViewType() {
        return ViewType.GENRE;
    }

    @Override
    public int getLayoutResId() {
        return R.layout.list_item_two_lines;
    }

    @Override
    public void bindView(ViewHolder holder) {
        holder.lineOne.setText(genre.name);
        holder.lineTwo.setText(StringUtils.makeAlbumAndSongsLabel(holder.itemView.getContext(), -1, genre.numSongs));
    }

    @Override
    public ViewHolder getViewHolder(ViewGroup parent) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(getLayoutResId(), parent, false));
    }

    @Override
    public Genre getItem() {
        return genre;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public TextView lineOne;
        public TextView lineTwo;
        public NonScrollImageButton overflowButton;

        public ViewHolder(View itemView) {
            super(itemView);

            lineOne = (TextView) itemView.findViewById(R.id.line_one);
            lineTwo = (TextView) itemView.findViewById(R.id.line_two);
            overflowButton = (NonScrollImageButton) itemView.findViewById(R.id.btn_overflow);
            overflowButton.setVisibility(View.GONE);
        }

        @Override
        public String toString() {
            return "GenreView.ViewHolder";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GenreView genreView = (GenreView) o;

        return genre != null ? genre.equals(genreView.genre) : genreView.genre == null;

    }

    @Override
    public int hashCode() {
        return genre != null ? genre.hashCode() : 0;
    }
}
