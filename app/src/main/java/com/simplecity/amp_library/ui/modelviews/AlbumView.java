package com.simplecity.amp_library.ui.modelviews;

import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.github.florent37.glidepalette.GlidePalette;
import com.simplecity.amp_library.format.PrefixHighlighter;
import com.simplecity.amp_library.model.Album;
import com.simplecity.amp_library.ui.adapters.ViewType;
import com.simplecity.amp_library.utils.PlaceholderProvider;
import com.simplecity.amp_library.utils.SettingsManager;
import com.simplecity.amp_library.utils.StringUtils;
import com.simplecity.amp_library.utils.sorting.SortManager;
import java.util.Arrays;
import java.util.List;

public class AlbumView extends MultiItemView<AlbumView.ViewHolder, Album> implements SectionedView {

    public interface ClickListener {

        void onAlbumClick(int position, AlbumView albumView, ViewHolder viewHolder);

        boolean onAlbumLongClick(int position, AlbumView albumView);

        void onAlbumOverflowClicked(View v, Album album);
    }

    private static final String TAG = "AlbumView";

    public Album album;

    private int viewType;

    private RequestManager requestManager;

    private SortManager sortManager;

    private PrefixHighlighter prefixHighlighter;

    private SettingsManager settingsManager;

    private char[] prefix;

    private boolean showYear;

    @Nullable
    private ClickListener listener;

    public AlbumView(Album album, @ViewType int viewType, RequestManager requestManager, SortManager sortManager, SettingsManager settingsManager) {
        this.album = album;
        this.viewType = viewType;
        this.requestManager = requestManager;
        this.sortManager = sortManager;
        this.settingsManager = settingsManager;
    }

    public void setClickListener(@Nullable ClickListener listener) {
        this.listener = listener;
    }

    public void setPrefix(PrefixHighlighter prefixHighlighter, char[] prefix) {
        this.prefixHighlighter = prefixHighlighter;
        this.prefix = prefix;
    }

    void onItemClick(int position, ViewHolder holder) {
        if (listener != null) {
            listener.onAlbumClick(position, this, holder);
        }
    }

    void onOverflowClick(View v) {
        if (listener != null) {
            listener.onAlbumOverflowClicked(v, album);
        }
    }

    boolean onAlbumLongclick(int position) {
        if (listener != null) {
            return listener.onAlbumLongClick(position, this);
        }
        return false;
    }

    public void showYear(boolean showYear) {
        this.showYear = showYear;
    }

    @Override
    public int getViewType() {
        return viewType;
    }

    public void setViewType(int viewType) {
        this.viewType = viewType;
    }

    @Override
    public void bindView(final ViewHolder holder) {

        super.bindView(holder);

        holder.lineOne.setText(album.name);

        holder.lineTwo.setVisibility(View.VISIBLE);

        if (holder.albumCount != null) {
            holder.albumCount.setVisibility(View.GONE);
        }
        if (holder.trackCount != null) {
            holder.trackCount.setVisibility(View.GONE);
        }

        if (showYear) {
            holder.lineTwo.setText(StringUtils.makeYearLabel(holder.itemView.getContext(), album.year));
        } else {
            holder.lineTwo.setText(album.albumArtistName);
        }

        if (getViewType() == ViewType.ALBUM_PALETTE) {
            if (holder.bottomContainer != null) {
                holder.bottomContainer.setBackgroundColor(0x20000000);
            }
        }

        requestManager.load(album)
                .listener(getViewType() == ViewType.ALBUM_PALETTE ? GlidePalette.with(album.getArtworkKey())
                        .use(GlidePalette.Profile.MUTED_DARK)
                        .intoBackground(holder.bottomContainer)
                        .crossfade(true)
                        : null)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(PlaceholderProvider.getInstance(holder.itemView.getContext()).getPlaceHolderDrawable(album.name, false, settingsManager))
                .into(holder.imageOne);

        holder.overflowButton.setContentDescription(holder.itemView.getResources().getString(com.simplecity.amp_library.R.string.btn_options, album.name));

        if (prefixHighlighter != null) {
            prefixHighlighter.setText(holder.lineOne, prefix);
            prefixHighlighter.setText(holder.lineTwo, prefix);
        }

        ViewCompat.setTransitionName(holder.imageOne, album.getArtworkKey());
    }

    @Override
    public void bindView(ViewHolder holder, int position, List payloads) {
        super.bindView(holder, position, payloads);
        //A partial bind. Due to the areContentsEqual implementation, the only reason this is called
        //is because the prefix changed. Update accordingly.
        if (prefixHighlighter != null) {
            prefixHighlighter.setText(holder.lineOne, prefix);
            prefixHighlighter.setText(holder.lineTwo, prefix);
        }
    }

    @Override
    public ViewHolder createViewHolder(ViewGroup parent) {
        return new ViewHolder(createView(parent));
    }

    @Override
    public int getSpanSize(int spanCount) {
        return 1;
    }

    @Override
    public String getSectionName() {

        int sortOrder = sortManager.getAlbumsSortOrder();
        String string = null;
        boolean requiresSubstring = true;
        switch (sortOrder) {
            case SortManager.AlbumSort.DEFAULT:
                string = StringUtils.keyFor(album.name);
                break;
            case SortManager.AlbumSort.NAME:
                string = album.name;
                break;
            case SortManager.AlbumSort.ARTIST_NAME:
                string = album.albumArtistName;
                break;
            case SortManager.AlbumSort.YEAR:
                string = String.valueOf(album.year);
                if (string.length() != 4) {
                    string = "-";
                } else {
                    string = string.substring(2, 4);
                }
                requiresSubstring = false;
                break;
        }

        if (requiresSubstring) {
            if (!TextUtils.isEmpty(string)) {
                string = string.substring(0, 1).toUpperCase();
            } else {
                string = " ";
            }
        }

        return string;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AlbumView albumView = (AlbumView) o;

        if (viewType != albumView.viewType) return false;
        return album != null ? album.equals(albumView.album) : albumView.album == null;
    }

    @Override
    public int hashCode() {
        int result = album != null ? album.hashCode() : 0;
        result = 31 * result + viewType;
        return result;
    }

    @Override
    public boolean areContentsEqual(Object other) {
        if (other instanceof AlbumView) {
            return album.equals(((AlbumView) other).album) && Arrays.equals(prefix, ((AlbumView) other).prefix);
        }
        return false;
    }

    public static class ViewHolder extends MultiItemView.ViewHolder<AlbumView> {

        public ViewHolder(View itemView) {
            super(itemView);

            itemView.setOnClickListener(v -> viewModel.onItemClick(getAdapterPosition(), this));

            itemView.setOnLongClickListener(v -> viewModel.onAlbumLongclick(getAdapterPosition()));

            overflowButton.setOnClickListener(v -> viewModel.onOverflowClick(v));
        }
    }
}
