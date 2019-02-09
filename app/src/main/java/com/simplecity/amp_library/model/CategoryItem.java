package com.simplecity.amp_library.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ui.screens.album.list.AlbumListFragment;
import com.simplecity.amp_library.ui.screens.artist.list.AlbumArtistListFragment;
import com.simplecity.amp_library.ui.screens.folders.FolderFragment;
import com.simplecity.amp_library.ui.screens.genre.list.GenreListFragment;
import com.simplecity.amp_library.ui.screens.playlist.list.PlaylistListFragment;
import com.simplecity.amp_library.ui.screens.songs.list.SongListFragment;
import com.simplecity.amp_library.ui.screens.suggested.SuggestedFragment;
import com.simplecity.amp_library.utils.ComparisonUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CategoryItem {

    public @interface Type {
        int GENRES = 0;
        int SUGGESTED = 1;
        int ARTISTS = 2;
        int ALBUMS = 3;
        int SONGS = 4;
        int PLAYLISTS = 5;
        int FOLDERS = 6;
    }

    @Type
    public int type;

    public int sortOrder;

    public boolean isChecked;

    private CategoryItem(@Type int type, SharedPreferences sharedPreferences) {
        this.type = type;
        isChecked = sharedPreferences.getBoolean(getEnabledKey(), isEnabledByDefault());
        sortOrder = sharedPreferences.getInt(getSortKey(), 0);
    }

    public static List<CategoryItem> getCategoryItems(SharedPreferences sharedPreferences) {
        List<CategoryItem> items = new ArrayList<>();
        items.add(new CategoryItem(Type.GENRES, sharedPreferences));
        items.add(new CategoryItem(Type.SUGGESTED, sharedPreferences));
        items.add(new CategoryItem(Type.ARTISTS, sharedPreferences));
        items.add(new CategoryItem(Type.ALBUMS, sharedPreferences));
        items.add(new CategoryItem(Type.SONGS, sharedPreferences));
        items.add(new CategoryItem(Type.FOLDERS, sharedPreferences));
        items.add(new CategoryItem(Type.PLAYLISTS, sharedPreferences));
        Collections.sort(items, (a, b) -> ComparisonUtils.compareInt(a.sortOrder, b.sortOrder));
        return items;
    }

    public void savePrefs(SharedPreferences.Editor editor) {
        editor.putBoolean(getEnabledKey(), isChecked);
        editor.putInt(getSortKey(), sortOrder);
        editor.apply();
    }

    @StringRes
    public int getTitleResId() {
        switch (type) {
            case Type.GENRES:
                return R.string.genres_title;
            case Type.SUGGESTED:
                return R.string.suggested_title;
            case Type.ARTISTS:
                return R.string.artists_title;
            case Type.ALBUMS:
                return R.string.albums_title;
            case Type.SONGS:
                return R.string.tracks_title;
            case Type.FOLDERS:
                return R.string.folders_title;
            case Type.PLAYLISTS:
                return R.string.playlists_title;
        }
        return -1;
    }

    public String getKey() {
        switch (type) {
            case Type.GENRES:
                return "genres";
            case Type.SUGGESTED:
                return "suggested";
            case Type.ARTISTS:
                return "artists";
            case Type.ALBUMS:
                return "albums";
            case Type.SONGS:
                return "songs";
            case Type.FOLDERS:
                return "folders";
            case Type.PLAYLISTS:
                return "playlists";
        }
        return null;
    }

    public boolean isEnabledByDefault() {
        switch (type) {
            case Type.GENRES:
                return true;
            case Type.SUGGESTED:
                return true;
            case Type.ARTISTS:
                return true;
            case Type.ALBUMS:
                return true;
            case Type.SONGS:
                return true;
            case Type.FOLDERS:
                return false;
            case Type.PLAYLISTS:
                return false;
        }
        return true;
    }

    public String getSortKey() {
        return getKey() + "_sort";
    }

    public String getEnabledKey() {
        return getKey() + "_enabled";
    }

    public Fragment getFragment(Context context) {
        switch (type) {
            case Type.GENRES:
                return GenreListFragment.Companion.newInstance(context.getString(getTitleResId()));
            case Type.SUGGESTED:
                return SuggestedFragment.Companion.newInstance(context.getString(getTitleResId()));
            case Type.ARTISTS:
                return AlbumArtistListFragment.Companion.newInstance(context.getString(getTitleResId()));
            case Type.ALBUMS:
                return AlbumListFragment.Companion.newInstance(context.getString(getTitleResId()));
            case Type.SONGS:
                return SongListFragment.Companion.newInstance(context.getString(getTitleResId()));
            case Type.FOLDERS:
                return FolderFragment.newInstance(context.getString(getTitleResId()), true);
            case Type.PLAYLISTS:
                return PlaylistListFragment.Companion.newInstance(context.getString(getTitleResId()));
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CategoryItem that = (CategoryItem) o;

        return type == that.type;
    }

    @Override
    public int hashCode() {
        return type;
    }
}