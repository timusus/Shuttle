package com.simplecity.amp_library.ui.modelviews;

import android.support.v4.view.ViewCompat;

import com.bignerdranch.android.multiselector.MultiSelector;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.github.florent37.glidepalette.GlidePalette;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.format.PrefixHighlighter;
import com.simplecity.amp_library.glide.utils.GlideUtils;
import com.simplecity.amp_library.model.AlbumArtist;
import com.simplecity.amp_library.model.ContentsComparator;

import java.util.Arrays;
import java.util.List;

public class AlbumArtistView extends MultiItemView<AlbumArtist> implements ContentsComparator {

    private static final String TAG = "AlbumArtistView";

    public AlbumArtist albumArtist;

    private int viewType;

    private RequestManager requestManager;

    private PrefixHighlighter prefixHighlighter;

    private char[] prefix;

    public AlbumArtistView(AlbumArtist albumArtist, @ViewType int viewType, RequestManager requestManager) {
        this.albumArtist = albumArtist;
        this.viewType = viewType;
        this.requestManager = requestManager;
    }

    public AlbumArtistView(AlbumArtist albumArtist, @ViewType int viewType, MultiSelector multiSelector, RequestManager requestManager) {
        this.albumArtist = albumArtist;
        this.viewType = viewType;
        this.requestManager = requestManager;
        this.multiSelector = multiSelector;
    }

    public void setPrefix(PrefixHighlighter prefixHighlighter, char[] prefix) {
        this.prefixHighlighter = prefixHighlighter;
        this.prefix = prefix;
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

        holder.lineOne.setText(albumArtist.name);
        holder.lineTwo.setText(albumArtist.getNumAlbumsSongsLabel());

        if (getViewType() == ViewType.ARTIST_PALETTE) {
            holder.bottomContainer.setBackgroundColor(0x20000000);
        }

        requestManager.load(albumArtist)
                .listener(getViewType() == ViewType.ARTIST_PALETTE ? GlidePalette.with(albumArtist.getArtworkKey())
                        .use(GlidePalette.Profile.MUTED_DARK)
                        .intoBackground(holder.bottomContainer)
                        .crossfade(true)
                        : null)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(GlideUtils.getPlaceHolderDrawable(albumArtist.name, false))
                .into(holder.imageOne);

        holder.overflowButton.setContentDescription(holder.itemView.getResources().getString(R.string.btn_options, albumArtist.name));

        if (prefixHighlighter != null) {
            prefixHighlighter.setText(holder.lineOne, prefix);
        }

        ViewCompat.setTransitionName(holder.imageOne, albumArtist.getArtworkKey());
    }

    @Override
    public void bindView(ViewHolder holder, int position, List payloads) {
        //A partial bind. Due to the areContentsEqual implementation, the only reason this is called
        //is because the prefix changed. Update accordingly.
        if (prefixHighlighter != null) {
            prefixHighlighter.setText(holder.lineOne, prefix);
        }
    }

    @Override
    public AlbumArtist getItem() {
        return albumArtist;
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
        return this.equals(other) && Arrays.equals(prefix, ((AlbumArtistView) other).prefix);
    }
}