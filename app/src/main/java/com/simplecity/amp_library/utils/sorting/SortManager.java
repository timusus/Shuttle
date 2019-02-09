package com.simplecity.amp_library.utils.sorting;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.model.Album;
import com.simplecity.amp_library.model.AlbumArtist;
import com.simplecity.amp_library.model.Playlist;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.utils.ComparisonUtils;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SortManager {

    private SharedPreferences mPrefs;

    @Inject
    public SortManager(ShuttleApplication application) {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(application);
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

    //  Songs sort order

    public void setArtistDetailSongsSortOrder(@SongSort int sortOrder) {
        setSortOrder(Key.ARTIST_DETAIL_SONGS, sortOrder);
    }

    public int getArtistDetailSongsSortOrder() {
        return mPrefs.getInt(Key.ARTIST_DETAIL_SONGS, SongSort.DETAIL_DEFAULT);
    }

    public void setAlbumDetailSongsSortOrder(@SongSort int sortOrder) {
        setSortOrder(Key.ALBUM_DETAIL_SONGS, sortOrder);
    }

    public int getAlbumDetailSongsSortOrder() {
        return mPrefs.getInt(Key.ALBUM_DETAIL_SONGS, SongSort.DETAIL_DEFAULT);
    }

    public void setPlaylistDetailSongsSortOrder(Playlist playlist, @SongSort int sortOrder) {
        setSortOrder(String.format("%s_%s", Key.PLAYLIST_DETAIL_SONGS, playlist.id), sortOrder);
    }

    public int getPlaylistDetailSongsSortOrder(Playlist playlist) {
        return mPrefs.getInt(String.format("%s_%s", Key.PLAYLIST_DETAIL_SONGS, playlist.id), SongSort.DETAIL_DEFAULT);
    }

    public void setGenreDetailSongsSortOrder(@SongSort int sortOrder) {
        setSortOrder(Key.GENRE_DETAIL_SONGS, sortOrder);
    }

    public int getGenreDetailSongsSortOrder() {
        return mPrefs.getInt(Key.GENRE_DETAIL_SONGS, SongSort.DETAIL_DEFAULT);
    }

    // Albums sort order

    public void setArtistDetailAlbumsSortOrder(@AlbumSort int sortOrder) {
        setSortOrder(Key.ARTIST_DETAIL_ALBUMS, sortOrder);
    }

    public int getArtistDetailAlbumsSortOrder() {
        return mPrefs.getInt(Key.ARTIST_DETAIL_ALBUMS, AlbumSort.DEFAULT);
    }

    public void setPlaylistDetailAlbumsSortOrder(Playlist playlist, @AlbumSort int sortOrder) {
        setSortOrder(String.format("%s_%s", Key.PLAYLIST_DETAIL_ALBUMS, playlist.id), sortOrder);
    }

    public int getPlaylistDetailAlbumsSortOrder(Playlist playlist) {
        return mPrefs.getInt(String.format("%s_%s", Key.PLAYLIST_DETAIL_ALBUMS, playlist.id), AlbumSort.DEFAULT);
    }

    public void setGenreDetailAlbumsSortOrder(@AlbumSort int sortOrder) {
        setSortOrder(Key.GENRE_DETAIL_ALBUMS, sortOrder);
    }

    public int getGenreDetailAlbumsSortOrder() {
        return mPrefs.getInt(Key.GENRE_DETAIL_ALBUMS, AlbumSort.DEFAULT);
    }

    // Asc/Desc songs sort order

    public void setArtistDetailSongsAscending(boolean ascending) {
        setAscending(Key.ARTIST_DETAIL_SONGS_ASC, ascending);
    }

    public boolean getArtistDetailSongsAscending() {
        return mPrefs.getBoolean(Key.ARTIST_DETAIL_SONGS_ASC, true);
    }

    public void setAlbumDetailSongsAscending(boolean ascending) {
        setAscending(Key.ALBUM_DETAIL_SONGS_ASC, ascending);
    }

    public boolean getAlbumDetailSongsAscending() {
        return mPrefs.getBoolean(Key.ALBUM_DETAIL_SONGS_ASC, true);
    }

    public void setPlaylistDetailSongsAscending(Playlist playlist, boolean ascending) {
        setAscending(String.format("%s_%s", Key.PLAYLIST_DETAIL_SONGS_ASC, playlist.id), ascending);
    }

    public boolean getPlaylistDetailSongsAscending(Playlist playlist) {
        return mPrefs.getBoolean(String.format("%s_%s", Key.PLAYLIST_DETAIL_SONGS_ASC, playlist.id), true);
    }

    public void setGenreDetailSongsAscending(boolean ascending) {
        setAscending(Key.GENRE_DETAIL_SONGS_ASC, ascending);
    }

    public boolean getGenreDetailSongsAscending() {
        return mPrefs.getBoolean(Key.GENRE_DETAIL_SONGS_ASC, true);
    }

    // Asc/Desc albums sort order

    public void setArtistDetailAlbumsAscending(boolean ascending) {
        setAscending(Key.ARTIST_DETAIL_ALBUMS_ASC, ascending);
    }

    public boolean getArtistDetailAlbumsAscending() {
        return mPrefs.getBoolean(Key.ARTIST_DETAIL_ALBUMS_ASC, true);
    }

    public void setPlaylistDetailAlbumsAscending(Playlist playlist, boolean ascending) {
        setAscending(String.format("%s_%s", Key.PLAYLIST_DETAIL_ALBUMS_ASC, playlist.id), ascending);
    }

    public boolean getPlaylistDetailAlbumsAscending(Playlist playlist) {
        return mPrefs.getBoolean(String.format("%s_%s", Key.PLAYLIST_DETAIL_ALBUMS_ASC, playlist.id), true);
    }

    public void setGenreDetailAlbumsAscending(boolean ascending) {
        setAscending(Key.GENRE_DETAIL_ALBUMS_ASC, ascending);
    }

    public boolean getGenreDetailAlbumsAscending() {
        return mPrefs.getBoolean(Key.GENRE_DETAIL_ALBUMS_ASC, true);
    }

    static int PREF_VERSION = 0;

    public interface Key {
        String ARTISTS = "key_artists_sort_order_" + PREF_VERSION;
        String ALBUMS = "key_albums_sort_order_" + PREF_VERSION;
        String SONGS = "key_songs_sort_order_" + PREF_VERSION;

        String ARTIST_DETAIL_ALBUMS = "key_detail_albums_sort_order_" + PREF_VERSION;
        String PLAYLIST_DETAIL_ALBUMS = "key_detail_playlist_albums_sort_order_" + PREF_VERSION;
        String GENRE_DETAIL_ALBUMS = "key_genre_albums_sort_order_" + PREF_VERSION;

        String ARTIST_DETAIL_SONGS = "key_detail_songs_sort_order_" + PREF_VERSION;
        String ALBUM_DETAIL_SONGS = "key_detail_album_songs_sort_order_" + PREF_VERSION;
        String PLAYLIST_DETAIL_SONGS = "key_detail_playlist_songs_sort_order_" + PREF_VERSION;
        String GENRE_DETAIL_SONGS = "key_genre_songs_sort_order_" + PREF_VERSION;

        String ARTISTS_ASC = "key_artists_sort_order_asc_" + PREF_VERSION;
        String ALBUMS_ASC = "key_albums_sort_order_asc_" + PREF_VERSION;
        String SONGS_ASC = "key_songs_sort_order_asc_" + PREF_VERSION;

        String ARTIST_DETAIL_SONGS_ASC = "key_artist_detail_songs_sort_order_asc_" + PREF_VERSION;
        String ALBUM_DETAIL_SONGS_ASC = "key_album_detail_songs_sort_order_asc_" + PREF_VERSION;
        String PLAYLIST_DETAIL_SONGS_ASC = "key_playlist_songs_sort_order_asc_" + PREF_VERSION;
        String GENRE_DETAIL_SONGS_ASC = "key_genre_songs_sort_order_asc_" + PREF_VERSION;

        String ARTIST_DETAIL_ALBUMS_ASC = "key_detail_albums_sort_order_asc_" + PREF_VERSION;
        String PLAYLIST_DETAIL_ALBUMS_ASC = "key_detail_playlist_albums_sort_order_asc_" + PREF_VERSION;
        String GENRE_DETAIL_ALBUMS_ASC = "key_detail_genre_albums_sort_order_asc_" + PREF_VERSION;
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