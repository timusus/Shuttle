package com.simplecity.amp_library.ui.modelviews;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.simplecity.amp_library.R;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.utils.DrawableUtils;

public class BlacklistView extends BaseAdaptableItem<Song, BlacklistView.ViewHolder> {

    public Song song;

    public BlacklistView(Song song) {
        this.song = song;
    }

    @Override
    public int getViewType() {
        return ViewType.BLACKLIST;
    }

    @Override
    public int getLayoutResId() {
        return R.layout.list_item_two_lines;
    }

    @Override
    public void bindView(ViewHolder holder) {

        holder.lineOne.setText(song.name);
        holder.lineTwo.setText(String.format("%s | %s", song.artistName, song.albumName));
    }

    @Override
    public ViewHolder getViewHolder(ViewGroup parent) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(getLayoutResId(), parent, false));
    }

    @Override
    public Song getItem() {
        return song;
    }

    @Override
    public boolean areContentsEqual(Object other) {
        return equals(other);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public TextView lineOne;
        public TextView lineTwo;
        public TextView playCount;
        public ImageButton overflow;

        public ViewHolder(View itemView) {
            super(itemView);
            lineOne = (TextView) itemView.findViewById(R.id.line_one);
            lineTwo = (TextView) itemView.findViewById(R.id.line_two);
            playCount = (TextView) itemView.findViewById(R.id.play_count);
            playCount.setVisibility(View.GONE);
            overflow = (ImageButton) itemView.findViewById(R.id.btn_overflow);
            overflow.setImageDrawable(DrawableUtils.getBaseDrawable(itemView.getContext(), R.drawable.ic_cancel));
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
