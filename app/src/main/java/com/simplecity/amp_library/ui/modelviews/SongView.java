package com.simplecity.amp_library.ui.modelviews;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bignerdranch.android.multiselector.MultiSelector;
import com.bignerdranch.android.multiselector.SwappingHolder;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.format.PrefixHighlighter;
import com.simplecity.amp_library.glide.utils.GlideUtils;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.ui.views.NonScrollImageButton;
import com.simplecity.amp_library.utils.DrawableUtils;
import com.simplecity.amp_library.utils.MusicUtils;
import com.simplecity.amp_library.utils.SettingsManager;

import java.util.Arrays;
import java.util.List;

public class SongView extends BaseAdaptableItem<Song, SongView.ViewHolder> {

    private static final String TAG = "SongView";

    public Song song;

    private MultiSelector multiSelector;

    private RequestManager requestManager;

    private PrefixHighlighter prefixHighlighter;

    private char[] prefix;

    public SongView(Song song, MultiSelector multiSelector, RequestManager requestManager) {
        this.song = song;
        this.multiSelector = multiSelector;
        this.requestManager = requestManager;
    }

    public void setPrefix(PrefixHighlighter prefixHighlighter, char[] prefix) {
        this.prefixHighlighter = prefixHighlighter;
        this.prefix = prefix;
    }

    private boolean editable;

    private boolean showAlbumArt;

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public void setShowAlbumArt(boolean showAlbumArt) {
        this.showAlbumArt = showAlbumArt;
    }

    private boolean showTrackNumber = false;

    public void setShowTrackNumber(boolean showTrackNumber) {
        this.showTrackNumber = showTrackNumber;
    }

    @Override
    public int getViewType() {
        return editable ? ViewType.SONG_EDITABLE : ViewType.SONG;
    }

    @Override
    public int getLayoutResId() {
        return editable ? R.layout.list_item_edit : R.layout.list_item_two_lines;
    }

    @Override
    public void bindView(ViewHolder holder) {

        holder.lineOne.setText(song.name);

        if (holder.playCount != null) {
            if (song.playCount > 1) {
                holder.playCount.setVisibility(View.VISIBLE);
                holder.playCount.setText(String.valueOf(song.playCount));
            } else {
                holder.playCount.setVisibility(View.GONE);
            }
        }

        holder.lineTwo.setText(String.format("%s - %s", song.artistName, song.albumName));
        holder.lineThree.setText(song.getDurationLabel());

        if (holder.dragHandle != null) {
            if (MusicUtils.getSongId() == song.id) {
                holder.dragHandle.setImageDrawable(DrawableUtils.getColoredAccentDrawable(holder.itemView.getContext(), holder.itemView.getResources().getDrawable(R.drawable.ic_drag_grip)));
            } else {
                holder.dragHandle.setImageDrawable(DrawableUtils.getBaseDrawable(holder.itemView.getContext(), R.drawable.ic_drag_grip));
            }
        }

        if (holder.artwork != null) {
            if (showAlbumArt && SettingsManager.getInstance().showArtworkInQueue()) {
                holder.artwork.setVisibility(View.VISIBLE);
                requestManager.load(song)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(GlideUtils.getPlaceHolderDrawable(song.albumName, false))
                        .into(holder.artwork);
            } else {
                holder.artwork.setVisibility(View.GONE);
            }
        }

        holder.overflowButton.setContentDescription(holder.itemView.getResources().getString(R.string.btn_options, song.name));

        if (prefixHighlighter != null) {
            prefixHighlighter.setText(holder.lineOne, prefix);
            prefixHighlighter.setText(holder.lineTwo, prefix);
        }

//        if (((ViewHolder) holder).dragHandle != null) {
//            ((ViewHolder) holder).dragHandle.setVisibility(editable ? View.VISIBLE : View.GONE);
//        }

        if (holder.trackNumber != null) {
            if (showTrackNumber) {
                holder.trackNumber.setVisibility(View.VISIBLE);
                holder.trackNumber.setText(String.valueOf(song.track));
            } else {
                holder.trackNumber.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void bindView(ViewHolder holder, int position, List payloads) {
        //A partial bind. Due to the areContentsEqual implementation, the only reason this is called
        //is because the prefix changed. Update accordingly.
        if (prefixHighlighter != null) {
            prefixHighlighter.setText(holder.lineOne, prefix);
            prefixHighlighter.setText(holder.lineTwo, prefix);
        }
    }

    @Override
    public ViewHolder getViewHolder(ViewGroup parent) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(getLayoutResId(), parent, false), multiSelector);
    }

    @Override
    public void recycle(ViewHolder holder) {
        if (holder.artwork != null) {
            Glide.clear(holder.artwork);
        }
    }

    @Override
    public Song getItem() {
        return song;
    }

    public static class ViewHolder extends SwappingHolder {

        public TextView lineOne;
        public TextView lineTwo;
        public TextView lineThree;
        public TextView trackNumber;
        public TextView playCount;
        public NonScrollImageButton overflowButton;
        public ImageView dragHandle;
        public ImageView artwork;

        public ViewHolder(View itemView, MultiSelector multiSelector) {
            super(itemView, multiSelector);

            lineOne = (TextView) itemView.findViewById(R.id.line_one);
            lineTwo = (TextView) itemView.findViewById(R.id.line_two);
            lineThree = (TextView) itemView.findViewById(R.id.line_three);
            trackNumber = (TextView) itemView.findViewById(R.id.trackNumber);
            overflowButton = (NonScrollImageButton) itemView.findViewById(R.id.btn_overflow);
            playCount = (TextView) itemView.findViewById(R.id.play_count);
            dragHandle = (ImageView) itemView.findViewById(R.id.drag_handle);
            artwork = (ImageView) itemView.findViewById(R.id.image);

            if (playCount != null) {
                playCount.setBackground(DrawableUtils.getColoredDrawable(itemView.getContext(), playCount.getBackground()));
            }

            overflowButton.setImageDrawable(DrawableUtils.getColoredStateListDrawable(itemView.getContext(), R.drawable.ic_overflow_white));
            if (dragHandle != null) {
                dragHandle.setImageDrawable(DrawableUtils.getBaseDrawable(itemView.getContext(), R.drawable.ic_drag_grip));
            }
        }

        @Override
        public String toString() {
            return "SongView.ViewHolder";
        }
    }

    @Override
    public boolean areContentsEqual(Object other) {
        return this.equals(other) && Arrays.equals(prefix, ((SongView) other).prefix);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SongView songView = (SongView) o;

        if (editable != songView.editable) return false;
        if (showAlbumArt != songView.showAlbumArt) return false;
        if (song != null ? !song.equals(songView.song) : songView.song != null) return false;
        return Arrays.equals(prefix, songView.prefix);

    }

    @Override
    public int hashCode() {
        int result = song != null ? song.hashCode() : 0;
        result = 31 * result + Arrays.hashCode(prefix);
        result = 31 * result + (editable ? 1 : 0);
        result = 31 * result + (showAlbumArt ? 1 : 0);
        return result;
    }
}