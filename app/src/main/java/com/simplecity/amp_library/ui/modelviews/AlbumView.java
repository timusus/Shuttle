package com.simplecity.amp_library.ui.modelviews;

import android.support.v4.view.ViewCompat;

import com.bignerdranch.android.multiselector.MultiSelector;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.github.florent37.glidepalette.GlidePalette;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.format.PrefixHighlighter;
import com.simplecity.amp_library.glide.utils.GlideUtils;
import com.simplecity.amp_library.model.Album;
import com.simplecity.amp_library.model.ContentsComparator;
import com.simplecity.amp_library.utils.StringUtils;

import java.util.Arrays;
import java.util.List;

public class AlbumView extends MultiItemView<Album> implements ContentsComparator {

    private static final String TAG = "AlbumView";

    public Album album;

    private int viewType;

    private RequestManager requestManager;

    private PrefixHighlighter prefixHighlighter;

    private char[] prefix;

    public AlbumView(Album album, @ViewType int viewType, RequestManager requestManager) {
        this.album = album;
        this.viewType = viewType;
        this.requestManager = requestManager;
    }

    public AlbumView(Album album, @ViewType int viewType, RequestManager requestManager, MultiSelector multiSelector) {
        this.album = album;
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

        holder.lineOne.setText(album.name);
        holder.lineTwo.setText(String.format("%s | %s", album.albumArtistName, StringUtils.makeAlbumAndSongsLabel(holder.itemView.getContext(), -1, album.numSongs)));

        if (getViewType() == ViewType.ALBUM_PALETTE) {
            holder.bottomContainer.setBackgroundColor(0x20000000);
        }

        requestManager.load(album)
                .listener(getViewType() == ViewType.ALBUM_PALETTE ? GlidePalette.with(album.getArtworkKey())
                        .use(GlidePalette.Profile.MUTED_DARK)
                        .intoBackground(holder.bottomContainer)
                        .crossfade(true)
                        : null)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(GlideUtils.getPlaceHolderDrawable(album.name, false))
                .into(holder.imageOne);

        holder.overflowButton.setContentDescription(holder.itemView.getResources().getString(R.string.btn_options, album.name));

        if (prefixHighlighter != null) {
            prefixHighlighter.setText(holder.lineOne, prefix);
            prefixHighlighter.setText(holder.lineTwo, prefix);
        }

        ViewCompat.setTransitionName(holder.imageOne, album.getArtworkKey());
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
    public Album getItem() {
        return album;
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
        return this.equals(other) && Arrays.equals(prefix, ((AlbumView) other).prefix);

    }
}
