package com.simplecity.amp_library.ui.modelviews;

import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.simplecity.amp_library.model.Genre;
import com.simplecity.amp_library.ui.views.NonScrollImageButton;
import com.simplecityapps.recycler_adapter.model.BaseViewModel;
import com.simplecityapps.recycler_adapter.recyclerview.BaseViewHolder;

import butterknife.BindView;
import butterknife.ButterKnife;

import static android.text.TextUtils.isEmpty;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.simplecity.amp_library.R.id.btn_overflow;
import static com.simplecity.amp_library.R.id.line_one;
import static com.simplecity.amp_library.R.id.line_two;
import static com.simplecity.amp_library.R.layout.list_item_two_lines;
import static com.simplecity.amp_library.ui.adapters.ViewType.GENRE;
import static com.simplecity.amp_library.utils.StringUtils.keyFor;
import static com.simplecity.amp_library.utils.StringUtils.makeAlbumAndSongsLabel;

public class GenreView extends BaseViewModel<GenreView.ViewHolder> implements
        SectionedView {

    public interface ClickListener {

        void onItemClick(Genre genre);

        void onOverflowClick(View v, Genre genre);
    }

    public Genre genre;

    @Nullable
    private ClickListener clickListener;

    public GenreView(Genre genre) {
        this.genre = genre;
    }

    public void setClickListener(@Nullable ClickListener clickListener) {
        this.clickListener = clickListener;
    }

    private void onClick() {
        if (clickListener != null) {
            clickListener.onItemClick(genre);
        }
    }

    private void onOverflowClick(View v) {
        if (clickListener != null) {
            clickListener.onOverflowClick(v, genre);
        }
    }

    @Override
    public int getViewType() {
        return GENRE;
    }

    @Override
    public int getLayoutResId() {
        return list_item_two_lines;
    }

    @Override
    public void bindView(ViewHolder holder) {
        super.bindView(holder);

        holder.lineOne.setText(genre.name);
        String albumAndSongsLabel = makeAlbumAndSongsLabel(holder.itemView.getContext(), -1, genre.numSongs);
        if (!isEmpty(albumAndSongsLabel)) {
            holder.lineTwo.setText(albumAndSongsLabel);
            holder.lineTwo.setVisibility(VISIBLE);
        } else {
            holder.lineTwo.setVisibility(GONE);
        }
    }

    @Override
    public ViewHolder createViewHolder(ViewGroup parent) {
        return new ViewHolder(createView(parent));
    }

    @Override
    public String getSectionName() {

        String string = keyFor(genre.name);
        if (!isEmpty(string)) {
            string = string.substring(0, 1).toUpperCase();
        } else {
            string = " ";
        }

        return string;
    }

    public static class ViewHolder extends BaseViewHolder<GenreView> {

        @BindView(line_one)
        public TextView lineOne;

        @BindView(line_two)
        public TextView lineTwo;

        @BindView(btn_overflow)
        public NonScrollImageButton overflowButton;

        public ViewHolder(View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);

            itemView.setOnClickListener(v -> viewModel.onClick());

            overflowButton.setOnClickListener(v -> viewModel.onOverflowClick(v));
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
