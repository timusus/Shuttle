package com.simplecity.amp_library.ui.adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.View;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.model.CategoryItem;
import com.simplecity.amp_library.ui.modelviews.TabView;

import java.util.ArrayList;
import java.util.List;

public class TabsAdapter extends ItemAdapter {

    private TabListener mListener;

    public interface TabListener {

        void onItemClick(View v, int position, CategoryItem categoryItem);

        void onStartDrag(RecyclerView.ViewHolder viewHolder);
    }

    public void setListener(TabListener listener) {
        mListener = listener;
    }

    private Context mContext;
    private SharedPreferences mPrefs;

    private static final String GENRES_ORDER = "genres_order";
    private static final String SUGGESTED_ORDER = "suggested_order";
    private static final String ARTISTS_ORDER = "artists_order";
    private static final String ALBUMS_ORDER = "albums_order";
    private static final String SONGS_ORDER = "songs_order";
    private static final String FOLDERS_ORDER = "folders_order";
    private static final String PLAYLISTS_ORDER = "playlists_order";

    private static final String SHOW_GENRES = "show_genres";
    private static final String SHOW_SUGGESTED = "show_suggested";
    private static final String SHOW_ARTISTS = "show_artists";
    private static final String SHOW_ALBUMS = "show_albums";
    private static final String SHOW_SONGS = "show_songs";
    private static final String SHOW_FOLDERS = "show_folders";
    private static final String SHOW_PLAYLISTS = "show_playlists";

    public TabsAdapter(Context context) {

        mContext = context;
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        boolean showGenres = mPrefs.getBoolean(SHOW_GENRES, true);
        boolean showSuggested = mPrefs.getBoolean(SHOW_SUGGESTED, true);
        boolean showArtists = mPrefs.getBoolean(SHOW_ARTISTS, true);
        boolean showAlbums = mPrefs.getBoolean(SHOW_ALBUMS, true);
        boolean showSongs = mPrefs.getBoolean(SHOW_SONGS, true);
        boolean showFolders = mPrefs.getBoolean(SHOW_FOLDERS, false);
        boolean showPlaylists = mPrefs.getBoolean(SHOW_PLAYLISTS, false);

        int genresOrder = mPrefs.getInt(GENRES_ORDER, 0);
        int suggestedOrder = mPrefs.getInt(SUGGESTED_ORDER, 1);
        int artistsOrder = mPrefs.getInt(ARTISTS_ORDER, 2);
        int albumsOrder = mPrefs.getInt(ALBUMS_ORDER, 3);
        int songsOrder = mPrefs.getInt(SONGS_ORDER, 4);
        int foldersOrder = mPrefs.getInt(FOLDERS_ORDER, 5);
        int playlistsOrder = mPrefs.getInt(PLAYLISTS_ORDER, 6);

        List<CategoryItem> categoryItems = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            if (genresOrder == i) {
                categoryItems.add(new CategoryItem(mContext.getString(R.string.genres_title), showGenres));
            } else if (suggestedOrder == i) {
                categoryItems.add(new CategoryItem(mContext.getString(R.string.suggested_title), showSuggested));
            } else if (artistsOrder == i) {
                categoryItems.add(new CategoryItem(mContext.getString(R.string.artists_title), showArtists));
            } else if (albumsOrder == i) {
                categoryItems.add(new CategoryItem(mContext.getString(R.string.albums_title), showAlbums));
            } else if (songsOrder == i) {
                categoryItems.add(new CategoryItem(mContext.getString(R.string.tracks_title), showSongs));
            } else if (foldersOrder == i) {
                categoryItems.add(new CategoryItem(mContext.getString(R.string.folders_title), showFolders));
            } else if (playlistsOrder == i) {
                categoryItems.add(new CategoryItem(mContext.getString(R.string.playlists_title), showPlaylists));
            }
        }

        setItems(Stream.of(categoryItems)
                .map(TabView::new)
                .collect(Collectors.toList()));
    }

    @Override
    protected void attachListeners(RecyclerView.ViewHolder viewHolder) {
        super.attachListeners(viewHolder);

        if (viewHolder instanceof TabView.ViewHolder) {

            viewHolder.itemView.setOnClickListener(v -> {

                if (mListener != null && viewHolder.getAdapterPosition() != -1) {
                    mListener.onItemClick(v, viewHolder.getAdapterPosition(), ((TabView) items.get(viewHolder.getAdapterPosition())).categoryItem);
                }
            });

            if (((TabView.ViewHolder) viewHolder).dragHandle != null) {
                ((TabView.ViewHolder) viewHolder).dragHandle.setOnTouchListener((v, event) -> {
                    if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
                        mListener.onStartDrag(viewHolder);
                    }
                    return false;
                });
            }
        }
    }

    public void updatePreferences() {

        String genresTitle = mContext.getString(R.string.genres_title);
        String suggestedTitle = mContext.getString(R.string.suggested_title);
        String artistsTitle = mContext.getString(R.string.artists_title);
        String albumsTitle = mContext.getString(R.string.albums_title);
        String songsTitle = mContext.getString(R.string.tracks_title);
        String foldersTitle = mContext.getString(R.string.folders_title);
        String playlistsTitle = mContext.getString(R.string.playlists_title);

        SharedPreferences.Editor editor = mPrefs.edit();
        for (int i = 0, length = getItemCount(); i < length; i++) {
            CategoryItem item = ((TabView) items.get(i)).categoryItem;
            if (item.title.equals(genresTitle)) {
                editor.putInt(GENRES_ORDER, i);
                editor.putBoolean(SHOW_GENRES, item.checked);
            } else if (item.title.equals(suggestedTitle)) {
                editor.putInt(SUGGESTED_ORDER, i);
                editor.putBoolean(SHOW_SUGGESTED, item.checked);
            } else if (item.title.equals(artistsTitle)) {
                editor.putInt(ARTISTS_ORDER, i);
                editor.putBoolean(SHOW_ARTISTS, item.checked);
            } else if (item.title.equals(albumsTitle)) {
                editor.putInt(ALBUMS_ORDER, i);
                editor.putBoolean(SHOW_ALBUMS, item.checked);
            } else if (item.title.equals(songsTitle)) {
                editor.putInt(SONGS_ORDER, i);
                editor.putBoolean(SHOW_SONGS, item.checked);
            } else if (item.title.equals(foldersTitle)) {
                editor.putInt(FOLDERS_ORDER, i);
                editor.putBoolean(SHOW_FOLDERS, item.checked);
            } else if (item.title.equals(playlistsTitle)) {
                editor.putInt(PLAYLISTS_ORDER, i);
                editor.putBoolean(SHOW_PLAYLISTS, item.checked);
            }
            editor.apply();
        }
    }
}
