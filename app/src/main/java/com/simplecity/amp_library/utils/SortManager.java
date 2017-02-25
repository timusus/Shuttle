package com.simplecity.amp_library.utils;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.model.Album;
import com.simplecity.amp_library.model.AlbumArtist;
import com.simplecity.amp_library.model.Song;

import java.util.Collections;
import java.util.List;

public class SortManager {

    private static SortManager sInstance;

    public static SortManager getInstance() {
        if (sInstance == null) {
            sInstance = new SortManager();
        }
        return sInstance;
    }

    private SharedPreferences mPrefs;

    private SortManager() {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(ShuttleApplication.getInstance());
    }

    private void setSortOrder(String key, int sortOrder) {
        mPrefs.edit().putInt(key, sortOrder).apply();
    }

    private void setAscending(String key, boolean ascending) {
        mPrefs.edit().putBoolean(key, ascending).apply();
    }

    public void setArtistsSortOrder(@ArtistSort int sortOrder) {
        setSortOrder(Key.ARTISTS, sortOrder);
    }

    public int getArtistsSortOrder() {
        return mPrefs.getInt(Key.ARTISTS, ArtistSort.DEFAULT);
    }

    public void setArtistsAscending(boolean ascending) {
        setAscending(Key.ARTISTS_ASC, ascending);
    }

    public boolean getArtistsAscending() {
        return mPrefs.getBoolean(Key.ARTISTS_ASC, true);
    }

    public void setAlbumsSortOrder(@AlbumSort int sortOrder) {
        setSortOrder(Key.ALBUMS, sortOrder);
    }

    public int getAlbumsSortOrder() {
        return mPrefs.getInt(Key.ALBUMS, AlbumSort.DEFAULT);
    }

    public void setAlbumsAscending(boolean ascending) {
        setAscending(Key.ALBUMS_ASC, ascending);
    }

    public boolean getAlbumsAscending() {
        return mPrefs.getBoolean(Key.ALBUMS_ASC, true);
    }

    public void setSongsSortOrder(@SongSort int sortOrder) {
        setSortOrder(Key.SONGS, sortOrder);
    }

    public int getSongsSortOrder() {
        return mPrefs.getInt(Key.SONGS, SongSort.DEFAULT);
    }

    public void setSongsAscending(boolean ascending) {
        setAscending(Key.SONGS_ASC, ascending);
    }

    public boolean getSongsAscending() {
        return mPrefs.getBoolean(Key.SONGS_ASC, true);
    }


    //Detail Fragment:

    public void setDetailSongsSortOrder(@SongSort int sortOrder) {
        setSortOrder(Key.DETAIL_SONGS, sortOrder);
    }

    public int getDetailSongsSortOrder() {
        return mPrefs.getInt(Key.DETAIL_SONGS, SongSort.DETAIL_DEFAULT);
    }

    public void setDetailAlbumSongsSortOrder(@SongSort int sortOrder) {
        setSortOrder(Key.DETAIL_ALBUM_SONGS, sortOrder);
    }

    public int getDetailAlbumSongsSortOrder() {
        return mPrefs.getInt(Key.DETAIL_ALBUM_SONGS, SongSort.DETAIL_DEFAULT);
    }

    public void setDetailPlaylistSongsSortOrder(@SongSort int sortOrder) {
        setSortOrder(Key.DETAIL_PLAYLIST_SONGS, sortOrder);
    }

    public int getDetailPlaylistSongsSortOrder() {
        return mPrefs.getInt(Key.DETAIL_PLAYLIST_SONGS, SongSort.DETAIL_DEFAULT);
    }

    public void setDetailGenreSongsSortOrder(@SongSort int sortOrder) {
        setSortOrder(Key.DETAIL_GENRE_SONGS, sortOrder);
    }

    public int getDetailGenreSongsSortOrder() {
        return mPrefs.getInt(Key.DETAIL_GENRE_SONGS, SongSort.DEFAULT);
    }


    public void setDetailAlbumsSortOrder(@AlbumSort int sortOrder) {
        setSortOrder(Key.DETAIL_ALBUMS, sortOrder);
    }

    public int getDetailAlbumsSortOrder() {
        return mPrefs.getInt(Key.DETAIL_ALBUMS, AlbumSort.DEFAULT);
    }

    public void setDetailPlaylistAlbumsSortOrder(@AlbumSort int sortOrder) {
        setSortOrder(Key.DETAIL_PLAYLIST_ALBUMS, sortOrder);
    }

    public int getDetailPlaylistAlbumsSortOrder() {
        return mPrefs.getInt(Key.DETAIL_PLAYLIST_ALBUMS, AlbumSort.DEFAULT);
    }

    public void setDetailGenreAlbumsSortOrder(@AlbumSort int sortOrder) {
        setSortOrder(Key.DETAIL_GENRE_ALBUMS, sortOrder);
    }

    public int getDetailGenreAlbumsSortOrder() {
        return mPrefs.getInt(Key.DETAIL_GENRE_ALBUMS, AlbumSort.DEFAULT);
    }


    public void setDetailSongsAscending(boolean ascending) {
        setAscending(Key.DETAIL_SONGS_ASC, ascending);
    }

    public boolean getDetailSongsAscending() {
        return mPrefs.getBoolean(Key.DETAIL_SONGS_ASC, true);
    }

    public void setDetailPlaylistSongsAscending(boolean ascending) {
        setAscending(Key.DETAIL_PLAYLIST_SONGS_ASC, ascending);
    }

    public boolean getDetailPlaylistSongsAscending() {
        return mPrefs.getBoolean(Key.DETAIL_PLAYLIST_SONGS_ASC, true);
    }

    public void setDetailGenreSongsAscending(boolean ascending) {
        setAscending(Key.DETAIL_GENRE_SONGS_ASC, ascending);
    }

    public boolean getDetailGenreSongsAscending() {
        return mPrefs.getBoolean(Key.DETAIL_GENRE_SONGS_ASC, true);
    }


    public void setDetailAlbumsAscending(boolean ascending) {
        setAscending(Key.DETAIL_ALBUMS_ASC, ascending);
    }

    public boolean getDetailAlbumsAscending() {
        return mPrefs.getBoolean(Key.DETAIL_ALBUMS_ASC, true);
    }

    public void setDetailPlaylistAlbumsAscending(boolean ascending) {
        setAscending(Key.DETAIL_PLAYLIST_ALBUMS_ASC, ascending);
    }

    public boolean getDetailPlaylistAlbumsAscending() {
        return mPrefs.getBoolean(Key.DETAIL_PLAYLIST_ALBUMS_ASC, true);
    }

    public void setDetailGenreAlbumsAscending(boolean ascending) {
        setAscending(Key.DETAIL_GENRE_ALBUMS_ASC, ascending);
    }

    public boolean getDetailGenreAlbumsAscending() {
        return mPrefs.getBoolean(Key.DETAIL_GENRE_ALBUMS_ASC, true);
    }

    static int PREF_VERSION = 0;

    public interface Key {
        String ARTISTS = "key_artists_sort_order_" + PREF_VERSION;
        String ALBUMS = "key_albums_sort_order_" + PREF_VERSION;
        String SONGS = "key_songs_sort_order_" + PREF_VERSION;

        String DETAIL_ALBUMS = "key_detail_albums_sort_order_" + PREF_VERSION;
        String DETAIL_PLAYLIST_ALBUMS = "key_detail_playlist_albums_sort_order_" + PREF_VERSION;
        String DETAIL_GENRE_ALBUMS = "key_genre_albums_sort_order_" + PREF_VERSION;

        String DETAIL_SONGS = "key_detail_songs_sort_order_" + PREF_VERSION;
        String DETAIL_ALBUM_SONGS = "key_detail_album_songs_sort_order_" + PREF_VERSION;
        String DETAIL_PLAYLIST_SONGS = "key_detail_playlist_songs_sort_order_" + PREF_VERSION;
        String DETAIL_GENRE_SONGS = "key_genre_songs_sort_order_" + PREF_VERSION;

        String ARTISTS_ASC = "key_artists_sort_order_asc_" + PREF_VERSION;
        String ALBUMS_ASC = "key_albums_sort_order_asc_" + PREF_VERSION;
        String SONGS_ASC = "key_songs_sort_order_asc_" + PREF_VERSION;

        String DETAIL_SONGS_ASC = "key_detail_songs_sort_order_asc_" + PREF_VERSION;
        String DETAIL_PLAYLIST_SONGS_ASC = "key_playlist_songs_sort_order_asc_" + PREF_VERSION;
        String DETAIL_GENRE_SONGS_ASC = "key_genre_songs_sort_order_asc_" + PREF_VERSION;

        String DETAIL_ALBUMS_ASC = "key_detail_albums_sort_order_asc_" + PREF_VERSION;
        String DETAIL_PLAYLIST_ALBUMS_ASC = "key_detail_playlist_albums_sort_order_asc_" + PREF_VERSION;
        String DETAIL_GENRE_ALBUMS_ASC = "key_detail_genre_albums_sort_order_asc_" + PREF_VERSION;
    }

    public @interface ArtistSort {
        int DEFAULT = 0;
        int NAME = 1;
    }

    public @interface AlbumSort {
        int DEFAULT = 0;
        int NAME = 1;
        int YEAR = 2;
        int ARTIST_NAME = 3;
    }

    public @interface SongSort {
        int DEFAULT = 0;
        int NAME = 1;
        int TRACK_NUMBER = 2;
        int DURATION = 3;
        int DATE = 4;
        int YEAR = 5;
        int ALBUM_NAME = 6;
        int ARTIST_NAME = 7;
        int DETAIL_DEFAULT = 8;
    }

    public interface SortFiles {
        String DEFAULT = "default";
        String FILE_NAME = "file_name";
        String SIZE = "size";
        String ARTIST_NAME = "artist_name";
        String ALBUM_NAME = "album_name";
        String TRACK_NAME = "track_name";
    }

    public interface SortFolders {
        String DEFAULT = "default";
        String COUNT = "count";
    }

    public void sortAlbums(List<Album> albums) {
        sortAlbums(albums, getAlbumsSortOrder());
    }

    public void sortAlbums(List<Album> albums, int key) {
        switch (key) {
            case AlbumSort.DEFAULT:
                Collections.sort(albums, Album::compareTo);
                break;
            case AlbumSort.NAME:
                Collections.sort(albums, (a, b) -> ComparisonUtils.compare(a.name, b.name));
                Collections.sort(albums, (a, b) -> ComparisonUtils.compare(a.name, b.name));
                break;
            case AlbumSort.YEAR:
                Collections.sort(albums, (a, b) -> ComparisonUtils.compareInt(b.year, a.year));
                break;
            case AlbumSort.ARTIST_NAME:
                Collections.sort(albums, (a, b) -> ComparisonUtils.compare(a.albumArtistName, b.albumArtistName));
                break;
        }
    }

    public void sortSongs(List<Song> songs) {
        sortSongs(songs, getSongsSortOrder());
    }

    public void sortSongs(List<Song> songs, @SongSort int key) {
        switch (key) {
            case SongSort.DEFAULT:
                Collections.sort(songs, Song::compareTo);
                break;
            case SongSort.NAME:
                Collections.sort(songs, (a, b) -> ComparisonUtils.compare(a.name, b.name));
                break;
            case SongSort.TRACK_NUMBER:
                Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(a.track, b.track));
                Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(a.discNumber, b.discNumber));
                break;
            case SongSort.DURATION:
                Collections.sort(songs, (a, b) -> ComparisonUtils.compareLong(a.duration, b.duration));
                break;
            case SongSort.DATE:
                Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(b.dateAdded, a.dateAdded));
                break;
            case SongSort.YEAR:
                Collections.sort(songs, (a, b) -> ComparisonUtils.compare(a.albumArtistName, b.albumArtistName));
                Collections.sort(songs, (a, b) -> ComparisonUtils.compare(a.albumName, b.albumName));
                Collections.sort(songs, Song::compareTo);
                Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(b.year, a.year));
                break;
            case SongSort.ALBUM_NAME:
                Collections.sort(songs, (a, b) -> ComparisonUtils.compare(a.albumArtistName, b.albumArtistName));
                Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(a.track, b.track));
                Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(a.discNumber, b.discNumber));
                Collections.sort(songs, (a, b) -> ComparisonUtils.compare(a.albumName, b.albumName));
                break;
            case SongSort.ARTIST_NAME:
                Collections.sort(songs, (a, b) -> ComparisonUtils.compare(a.albumName, b.albumName));
                Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(a.track, b.track));
                Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(a.discNumber, b.discNumber));
                Collections.sort(songs, (a, b) -> ComparisonUtils.compare(a.albumArtistName, b.albumArtistName));
                break;
            case SongSort.DETAIL_DEFAULT:
                Collections.sort(songs, (a, b) -> ComparisonUtils.compare(a.albumArtistName, b.albumArtistName));
                Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(b.year, a.year));
                Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(a.track, b.track));
                Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(a.discNumber, b.discNumber));
                Collections.sort(songs, (a, b) -> ComparisonUtils.compare(a.albumName, b.albumName));
                break;
        }
    }

    public void sortAlbumArtists(List<AlbumArtist> albumArtists) {
        int sortOrder = mPrefs.getInt(Key.ARTISTS, ArtistSort.DEFAULT);
        switch (sortOrder) {
            case ArtistSort.DEFAULT:
                Collections.sort(albumArtists, AlbumArtist::compareTo);
                break;
            case ArtistSort.NAME:
                Collections.sort(albumArtists, (a, b) -> ComparisonUtils.compare(a.name, b.name));
                break;
        }
    }
}