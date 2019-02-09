package com.simplecity.amp_library.ui.modelviews;

import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.github.florent37.glidepalette.BitmapPalette;
import com.github.florent37.glidepalette.GlidePalette;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.format.PrefixHighlighter;
import com.simplecity.amp_library.model.AlbumArtist;
import com.simplecity.amp_library.ui.adapters.ViewType;
import com.simplecity.amp_library.utils.PlaceholderProvider;
import com.simplecity.amp_library.utils.SettingsManager;
import com.simplecity.amp_library.utils.StringUtils;
import com.simplecity.amp_library.utils.sorting.SortManager;
import java.util.Arrays;
import java.util.List;

public class AlbumArtistView extends MultiItemView<AlbumArtistView.ViewHolder, AlbumArtist> implements
        SectionedView {

    public interface ClickListener {

        void onAlbumArtistClick(int position, AlbumArtistView albumArtistView, ViewHolder viewholder);

        boolean onAlbumArtistLongClick(int position, AlbumArtistView albumArtistView);

        void onAlbumArtistOverflowClicked(View v, AlbumArtist albumArtist);
    }

    private static final String TAG = "AlbumArtistView";

    public AlbumArtist albumArtist;

    private int viewType;

    private RequestManager requestManager;

    private SortManager sortManager;

    private PrefixHighlighter prefixHighlighter;

    private SettingsManager settingsManager;

    private char[] prefix;

    @Nullable
    private ClickListener listener;

    public AlbumArtistView(AlbumArtist albumArtist, @ViewType int viewType, RequestManager requestManager, SortManager sortManager, SettingsManager settingsManager) {
        this.albumArtist = albumArtist;
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
            listener.onAlbumArtistClick(position, this, holder);
        }
    }

    void onOverflowClick(View v) {
        if (listener != null) {
            listener.onAlbumArtistOverflowClicked(v, albumArtist);
        }
    }

    boolean onItemLongClick(int positon) {
        if (listener != null) {
            return listener.onAlbumArtistLongClick(positon, this);
        }
        return false;
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

        holder.lineOne.setText(albumArtist.name);

        if (holder.trackCount != null) {
            holder.lineTwo.setVisibility(View.GONE);
            holder.trackCount.setVisibility(View.VISIBLE);
            holder.trackCount.setText(String.valueOf(albumArtist.getNumSongs()));
        }
        if (holder.albumCount != null) {
            holder.albumCount.setVisibility(View.VISIBLE);
            holder.albumCount.setText(String.valueOf(albumArtist.getNumAlbums()));
        }

        if (getViewType() == ViewType.ARTIST_PALETTE) {
            if (holder.bottomContainer != null) {
                holder.bottomContainer.setBackgroundColor(0x20000000);
            }
        }

        requestManager.load(albumArtist)
                .listener(getViewType() == ViewType.ARTIST_PALETTE ? GlidePalette.with(albumArtist.getArtworkKey())
                        .use(BitmapPalette.Profile.MUTED_DARK)
                        .intoBackground(holder.bottomContainer)
                        .crossfade(true)
                        : null)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(PlaceholderProvider.getInstance(holder.itemView.getContext()).getPlaceHolderDrawable(albumArtist.name, false, settingsManager))
                .into(holder.imageOne);

        holder.overflowButton.setContentDescription(holder.itemView.getResources().getString(R.string.btn_options, albumArtist.name));

        if (prefixHighlighter != null) {
            prefixHighlighter.setText(holder.lineOne, prefix);
        }

        ViewCompat.setTransitionName(holder.imageOne, albumArtist.getArtworkKey());
    }

    @Override
    public void bindView(ViewHolder holder, int position, List payloads) {
        super.bindView(holder, position, payloads);
        //A partial bind. Due to the areContentsEqual implementation, the only reason this is called
        //is because the prefix changed. Update accordingly.
        if (prefixHighlighter != null) {
            prefixHighlighter.setText(holder.lineOne, prefix);
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
        int sortOrder = sortManager.getArtistsSortOrder();

        String string = null;
        switch (sortOrder) {
            case SortManager.ArtistSort.DEFAULT:
                string = StringUtils.keyFor(albumArtist.name);
                break;
            case SortManager.ArtistSort.NAME:
                string = albumArtist.name;
                break;
        }

        if (!TextUtils.isEmpty(string)) {
            string = string.substring(0, 1).toUpperCase();
        } else {
            string = " ";
        }

        return string;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AlbumArtistView that = (AlbumArtistView) o;

        if (viewType != that.viewType) return false;
        return albumArtist != null ? albumArtist.equals(that.albumArtist) : that.albumArtist == null;
    }

    @Override
    public int hashCode() {
        int result = albumArtist != null ? albumArtist.hashCode() : 0;
        result = 31 * result + viewType;
        return result;
    }

    @Override
    public boolean areContentsEqual(Object other) {
        if (other instanceof AlbumArtistView) {
            return albumArtist.equals(((AlbumArtistView) other).albumArtist) && Arrays.equals(prefix, ((AlbumArtistView) other).prefix);
        }
        return false;
    }

    public static class ViewHolder extends MultiItemView.ViewHolder<AlbumArtistView> {

        public ViewHolder(View itemView) {
            super(itemView);

            itemView.setOnClickListener(v -> viewModel.onItemClick(getAdapterPosition(), this));

            itemView.setOnLongClickListener(v -> viewModel.onItemLongClick(getAdapterPosition()));

            overflowButton.setOnClickListener(v -> viewModel.onOverflowClick(v));
        }
    }
}
