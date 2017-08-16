package com.simplecity.amp_library.ui.modelviews;

import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.simplecity.amp_library.model.Song;
import com.simplecityapps.recycler_adapter.model.BaseViewModel;
import com.simplecityapps.recycler_adapter.recyclerview.BaseViewHolder;

import butterknife.BindView;
import butterknife.ButterKnife;

import static android.view.View.GONE;
import static com.simplecity.amp_library.R.id.btn_overflow;
import static com.simplecity.amp_library.R.id.line_one;
import static com.simplecity.amp_library.R.id.line_two;
import static com.simplecity.amp_library.R.id.play_count;
import static com.simplecity.amp_library.R.layout.list_item_two_lines;
import static com.simplecity.amp_library.ui.adapters.ViewType.BLACKLIST;
import static java.lang.String.format;

public class BlacklistView extends BaseViewModel<BlacklistView.ViewHolder> {

    public interface ClickListener {
        void onRemove(BlacklistView blacklistView);
    }

    public Song song;

    @Nullable
    private ClickListener listener;

    public BlacklistView(Song song) {
        this.song = song;
    }

    public void setClickListener(@Nullable ClickListener listener) {
        this.listener = listener;
    }

    private void onDeleteClicked() {
        if (listener != null) {
            listener.onRemove(this);
        }
    }

    @Override
    public int getViewType() {
        return BLACKLIST;
    }

    @Override
    public int getLayoutResId() {
        return list_item_two_lines;
    }

    @Override
    public void bindView(ViewHolder holder) {

        super.bindView(holder);

        holder.lineOne.setText(song.name);
        holder.lineTwo.setText(format("%s | %s", song.artistName, song.albumName));
    }

    @Override
    public ViewHolder createViewHolder(ViewGroup parent) {
        return new ViewHolder(createView(parent));
    }

    @Override
    public boolean areContentsEqual(Object other) {
        return equals(other);
    }

    public static class ViewHolder extends BaseViewHolder<BlacklistView> {

        @BindView(line_one)
        public TextView lineOne;

        @BindView(line_two)
        public TextView lineTwo;

        @BindView(play_count)
        public TextView playCount;

        @BindView(btn_overflow)
        public ImageButton overflow;

        public ViewHolder(View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);

            playCount.setVisibility(GONE);

            overflow.setOnClickListener(v -> viewModel.onDeleteClicked());
        }

        @Override
        public String toString() {
            return "BlacklistView.ViewHolder";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BlacklistView that = (BlacklistView) o;

        return song != null ? song.equals(that.song) : that.song == null;
    }

    @Override
    public int hashCode() {
        return song != null ? song.hashCode() : 0;
    }
}
