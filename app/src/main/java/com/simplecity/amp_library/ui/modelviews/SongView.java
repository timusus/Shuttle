package com.simplecity.amp_library.ui.modelviews;

import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.RequestManager;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.format.PrefixHighlighter;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.ui.views.NonScrollImageButton;
import com.simplecity.amp_library.utils.SortManager;
import com.simplecityapps.recycler_adapter.model.BaseViewModel;
import com.simplecityapps.recycler_adapter.recyclerview.BaseViewHolder;

import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

import static android.support.v4.view.MotionEventCompat.getActionMasked;
import static android.text.TextUtils.isEmpty;
import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.bumptech.glide.Glide.clear;
import static com.bumptech.glide.load.engine.DiskCacheStrategy.ALL;
import static com.simplecity.amp_library.R.drawable.ic_drag_grip;
import static com.simplecity.amp_library.R.drawable.ic_overflow_white;
import static com.simplecity.amp_library.R.id.btn_overflow;
import static com.simplecity.amp_library.R.layout.list_item_edit;
import static com.simplecity.amp_library.R.layout.list_item_two_lines;
import static com.simplecity.amp_library.R.string.btn_options;
import static com.simplecity.amp_library.glide.utils.GlideUtils.getPlaceHolderDrawable;
import static com.simplecity.amp_library.ui.adapters.ViewType.SONG;
import static com.simplecity.amp_library.ui.adapters.ViewType.SONG_EDITABLE;
import static com.simplecity.amp_library.utils.DrawableUtils.getBaseDrawable;
import static com.simplecity.amp_library.utils.DrawableUtils.getColoredAccentDrawable;
import static com.simplecity.amp_library.utils.DrawableUtils.getColoredDrawable;
import static com.simplecity.amp_library.utils.DrawableUtils.getColoredStateListDrawable;
import static com.simplecity.amp_library.utils.SettingsManager.getInstance;
import static com.simplecity.amp_library.utils.SortManager.SongSort.ALBUM_NAME;
import static com.simplecity.amp_library.utils.SortManager.SongSort.ARTIST_NAME;
import static com.simplecity.amp_library.utils.SortManager.SongSort.DATE;
import static com.simplecity.amp_library.utils.SortManager.SongSort.DEFAULT;
import static com.simplecity.amp_library.utils.SortManager.SongSort.DURATION;
import static com.simplecity.amp_library.utils.SortManager.SongSort.NAME;
import static com.simplecity.amp_library.utils.SortManager.SongSort.TRACK_NUMBER;
import static com.simplecity.amp_library.utils.SortManager.SongSort.YEAR;
import static com.simplecity.amp_library.utils.StringUtils.keyFor;
import static java.lang.String.format;
import static java.lang.String.valueOf;

public class SongView extends BaseViewModel<SongView.ViewHolder> implements
        SectionedView {

    public interface ClickListener {

        void onSongClick(Song song, ViewHolder holder);

        boolean onSongLongClick(Song song);

        void onSongOverflowClick(View v, Song song);

        void onStartDrag(ViewHolder holder);
    }

    private static final String TAG = "SongView";

    public Song song;

    private RequestManager requestManager;

    private PrefixHighlighter prefixHighlighter;

    private char[] prefix;

    private boolean editable;

    private boolean showAlbumArt;

    private boolean showTrackNumber;

    private boolean isCurrentTrack;

    @Nullable
    private ClickListener listener;

    public SongView(Song song, RequestManager requestManager) {
        this.song = song;
        this.requestManager = requestManager;
    }

    public void setClickListener(@Nullable ClickListener listener) {
        this.listener = listener;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public void setShowAlbumArt(boolean showAlbumArt) {
        this.showAlbumArt = showAlbumArt;
    }

    public void setPrefix(PrefixHighlighter prefixHighlighter, char[] prefix) {
        this.prefixHighlighter = prefixHighlighter;
        this.prefix = prefix;
    }

    public void setShowTrackNumber(boolean showTrackNumber) {
        this.showTrackNumber = showTrackNumber;
    }

    public void setCurrentTrack(boolean isCurrentTrack) {
        this.isCurrentTrack = isCurrentTrack;
    }

    public boolean isCurrentTrack() {
        return isCurrentTrack;
    }

    private void onItemClick(ViewHolder holder) {
        if (listener != null) {
            listener.onSongClick(song, holder);
        }
    }

    private void onOverflowClick(View v) {
        if (listener != null) {
            listener.onSongOverflowClick(v, song);
        }
    }

    private boolean onItemLongClick() {
        if (listener != null) {
            return listener.onSongLongClick(song);
        }
        return false;
    }

    private void onStartDrag(ViewHolder holder) {
        if (listener != null) {
            listener.onStartDrag(holder);
        }
    }

    @Override
    public int getViewType() {
        return editable ? SONG_EDITABLE : SONG;
    }

    @Override
    public int getLayoutResId() {
        return editable ? list_item_edit : list_item_two_lines;
    }

    @Override
    public void bindView(ViewHolder holder) {
        super.bindView(holder);

        holder.lineOne.setText(song.name);

        if (holder.playCount != null) {
            if (song.playCount > 1) {
                holder.playCount.setVisibility(VISIBLE);
                holder.playCount.setText(valueOf(song.playCount));
            } else {
                holder.playCount.setVisibility(GONE);
            }
        }

        holder.lineTwo.setText(format("%s - %s", song.artistName, song.albumName));
        holder.lineThree.setText(song.getDurationLabel());

        if (holder.dragHandle != null) {
            if (isCurrentTrack) {
                holder.dragHandle.setImageDrawable(getColoredAccentDrawable(holder.itemView.getContext(), holder.itemView.getResources().getDrawable(ic_drag_grip)));
            } else {
                holder.dragHandle.setImageDrawable(getBaseDrawable(holder.itemView.getContext(), ic_drag_grip));
            }
        }

        if (holder.artwork != null) {
            if (showAlbumArt && getInstance().showArtworkInQueue()) {
                holder.artwork.setVisibility(VISIBLE);
                requestManager.load(song)
                        .diskCacheStrategy(ALL)
                        .placeholder(getPlaceHolderDrawable(song.albumName, false))
                        .into(holder.artwork);
            } else {
                holder.artwork.setVisibility(GONE);
            }
        }

        holder.overflowButton.setContentDescription(holder.itemView.getResources().getString(btn_options, song.name));

        if (prefixHighlighter != null) {
            prefixHighlighter.setText(holder.lineOne, prefix);
            prefixHighlighter.setText(holder.lineTwo, prefix);
        }

        if (holder.trackNumber != null) {
            if (showTrackNumber) {
                holder.trackNumber.setVisibility(VISIBLE);
                holder.trackNumber.setText(valueOf(song.track));
            } else {
                holder.trackNumber.setVisibility(GONE);
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

        if (holder.dragHandle != null) {
            if (isCurrentTrack) {
                holder.dragHandle.setImageDrawable(getColoredAccentDrawable(holder.itemView.getContext(), holder.itemView.getResources().getDrawable(ic_drag_grip)));
            } else {
                holder.dragHandle.setImageDrawable(getBaseDrawable(holder.itemView.getContext(), ic_drag_grip));
            }
        }
    }

    @Override
    public ViewHolder createViewHolder(ViewGroup parent) {
        return new ViewHolder(createView(parent));
    }

    @Override
    public String getSectionName() {
        int sortOrder = SortManager.getInstance().getSongsSortOrder();

        if (sortOrder != DATE
                && sortOrder != DURATION
                && sortOrder != TRACK_NUMBER) {

            String string = null;
            boolean requiresSubstring = true;
            switch (sortOrder) {
                case DEFAULT:
                    string = keyFor(song.name);
                    break;
                case NAME:
                    string = song.name;
                    break;
                case YEAR:
                    string = valueOf(song.year);
                    if (string.length() != 4) {
                        string = "-";
                    } else {
                        string = string.substring(2, 4);
                    }
                    requiresSubstring = false;
                    break;
                case ALBUM_NAME:
                    string = keyFor(song.albumName);
                    break;
                case ARTIST_NAME:
                    string = keyFor(song.artistName);
                    break;
            }

            if (requiresSubstring) {
                if (!isEmpty(string)) {
                    string = string.substring(0, 1).toUpperCase();
                } else {
                    string = " ";
                }
            }
            return string;
        }
        return "";
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
        if (showTrackNumber != songView.showTrackNumber) return false;
        if (song != null ? !song.equals(songView.song) : songView.song != null) return false;
        return Arrays.equals(prefix, songView.prefix);

    }

    @Override
    public int hashCode() {
        int result = song != null ? song.hashCode() : 0;
        result = 31 * result + Arrays.hashCode(prefix);
        result = 31 * result + (editable ? 1 : 0);
        result = 31 * result + (showAlbumArt ? 1 : 0);
        result = 31 * result + (showTrackNumber ? 1 : 0);
        return result;
    }

    public static class ViewHolder extends BaseViewHolder<SongView> {

        @BindView(R.id.line_one)
        TextView lineOne;

        @BindView(R.id.line_two)
        TextView lineTwo;

        @BindView(R.id.line_three)
        TextView lineThree;

        @Nullable @BindView(R.id.trackNumber)
        TextView trackNumber;

        @Nullable @BindView(R.id.play_count)
        TextView playCount;

        @BindView(btn_overflow)
        public NonScrollImageButton overflowButton;

        @Nullable @BindView(R.id.drag_handle)
        ImageView dragHandle;

        @Nullable @BindView(R.id.image)
        ImageView artwork;

        ViewHolder(View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);

            if (playCount != null) {
                playCount.setBackground(getColoredDrawable(itemView.getContext(), playCount.getBackground()));
            }

            overflowButton.setImageDrawable(getColoredStateListDrawable(itemView.getContext(), ic_overflow_white));
            if (dragHandle != null) {
                dragHandle.setImageDrawable(getBaseDrawable(itemView.getContext(), ic_drag_grip));
            }

            itemView.setOnClickListener(v -> viewModel.onItemClick(this));
            itemView.setOnLongClickListener(v -> viewModel.onItemLongClick());

            overflowButton.setOnClickListener(v -> viewModel.onOverflowClick(v));

            if (dragHandle != null) {
                dragHandle.setOnTouchListener((v, event) -> {
                    if (getActionMasked(event) == ACTION_DOWN) {
                        viewModel.onStartDrag(this);
                    }
                    return true;
                });
            }
        }

        @Override
        public String toString() {
            return "SongView.ViewHolder";
        }

        @Override
        public void recycle() {
            super.recycle();

            if (artwork != null) {
                clear(artwork);
            }
        }
    }
}