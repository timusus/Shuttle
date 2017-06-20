package com.simplecity.amp_library.ui.modelviews;

import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.RequestManager;
import com.simplecity.amp_library.format.PrefixHighlighter;
import com.simplecity.amp_library.model.AlbumArtist;
import com.simplecity.amp_library.ui.adapters.ViewType;
import com.simplecity.amp_library.utils.PlaceholderProvider;

import java.util.Arrays;
import java.util.List;

import static android.support.v4.view.ViewCompat.setTransitionName;
import static android.text.TextUtils.isEmpty;
import static com.bumptech.glide.load.engine.DiskCacheStrategy.ALL;
import static com.github.florent37.glidepalette.BitmapPalette.Profile.MUTED_DARK;
import static com.github.florent37.glidepalette.GlidePalette.with;
import static com.simplecity.amp_library.R.string.btn_options;
import static com.simplecity.amp_library.ui.adapters.ViewType.ARTIST_PALETTE;
import static com.simplecity.amp_library.utils.SortManager.AlbumSort.ARTIST_NAME;
import static com.simplecity.amp_library.utils.SortManager.ArtistSort.DEFAULT;
import static com.simplecity.amp_library.utils.SortManager.getInstance;
import static com.simplecity.amp_library.utils.StringUtils.keyFor;

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

    private PrefixHighlighter prefixHighlighter;

    private char[] prefix;

    @Nullable
    private ClickListener listener;

    public AlbumArtistView(AlbumArtist albumArtist, @ViewType int viewType, RequestManager requestManager) {
        this.albumArtist = albumArtist;
        this.viewType = viewType;
        this.requestManager = requestManager;
    }

    public void setClickListener(@Nullable ClickListener listener) {
        this.listener = listener;
    }

    public void setPrefix(PrefixHighlighter prefixHighlighter, char[] prefix) {
        this.prefixHighlighter = prefixHighlighter;
        this.prefix = prefix;
    }

    private void onItemClick(int position, ViewHolder holder) {
        if (listener != null) {
            listener.onAlbumArtistClick(position, this, holder);
        }
    }

    private void onOverflowClick(View v) {
        if (listener != null) {
            listener.onAlbumArtistOverflowClicked(v, albumArtist);
        }
    }

    private boolean onItemLongClick(int positon) {
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
        holder.lineTwo.setText(albumArtist.getNumAlbumsSongsLabel());

        if (getViewType() == ARTIST_PALETTE) {
            holder.bottomContainer.setBackgroundColor(0x20000000);
        }

        requestManager.load(albumArtist)
                .listener(getViewType() == ARTIST_PALETTE ? with(albumArtist.getArtworkKey())
                        .use(MUTED_DARK)
                        .intoBackground(holder.bottomContainer)
                        .crossfade(true)
                        : null)
                .diskCacheStrategy(ALL)
                .placeholder(PlaceholderProvider.getInstance().getPlaceHolderDrawable(albumArtist.name, false))
                .into(holder.imageOne);

        holder.overflowButton.setContentDescription(holder.itemView.getResources().getString(btn_options, albumArtist.name));

        if (prefixHighlighter != null) {
            prefixHighlighter.setText(holder.lineOne, prefix);
        }

        setTransitionName(holder.imageOne, albumArtist.getArtworkKey());
    }

    @Override
    public AlbumArtist getItem() {
        return albumArtist;
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
    public String getSectionName() {
        int sortOrder = getInstance().getArtistsSortOrder();

        String string = null;
        switch (sortOrder) {
            case DEFAULT:
                string = keyFor(albumArtist.name);
                break;
            case ARTIST_NAME:
                string = albumArtist.name;
                break;
        }

        if (!isEmpty(string)) {
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