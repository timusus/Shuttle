package com.simplecity.amp_library.utils;

import android.content.Context;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.SubMenu;
import android.widget.Toast;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.model.Album;
import com.simplecity.amp_library.model.AlbumArtist;
import com.simplecity.amp_library.model.Genre;
import com.simplecity.amp_library.model.Playlist;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.sql.databases.BlacklistHelper;
import com.simplecity.amp_library.tagger.TaggerDialog;

import java.util.Collections;
import java.util.List;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.schedulers.Schedulers;

public class MenuUtils implements MusicUtils.Defs {

    // Songs

    public static void playNext(Context context, Song song) {
        MusicUtils.playNext(Collections.singletonList(song), string ->
                Toast.makeText(context, string, Toast.LENGTH_SHORT).show());
    }

    public static void newPlaylist(Context context, List<Song> songs) {
        PlaylistUtils.createPlaylistDialog(context, songs);
    }

    public static void addToPlaylist(Context context, MenuItem item, List<Song> songs) {
        Playlist playlist = (Playlist) item.getIntent().getSerializableExtra(ShuttleUtils.ARG_PLAYLIST);
        PlaylistUtils.addToPlaylist(context, playlist, songs);
    }

    public static void showSongInfo(Context context, Song song) {
        DialogUtils.showSongInfoDialog(context, song);
    }

    public static void setRingtone(Context context, Song song) {
        ShuttleUtils.setRingtone(context, song);
    }

    public static TaggerDialog editTags(Song song) {
        return TaggerDialog.newInstance(song);
    }

    public static void addToQueue(Context context, List<Song> songs) {
        MusicUtils.addToQueue(songs, message -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
    }

    public static void delete(Context context, List<Song> songs) {
        new DialogUtils.DeleteDialogBuilder()
                .context(context)
                .singleMessageId(R.string.delete_song_desc)
                .multipleMessage(R.string.delete_song_desc_multiple)
                .itemNames(Stream.of(songs)
                        .map(song -> song.name)
                        .collect(Collectors.toList()))
                .songsToDelete(Observable.just(songs))
                .build()
                .show();
    }

    public static void setupSongMenu(Context context, PopupMenu menu, boolean showRemoveButton) {
        menu.inflate(R.menu.menu_song);

        if (!showRemoveButton) {
            menu.getMenu().findItem(R.id.remove).setVisible(false);
        }

        // Add playlist menu
        SubMenu sub = menu.getMenu().findItem(R.id.addToPlaylist).getSubMenu();
        PlaylistUtils.makePlaylistMenu(context, sub);
    }

    public static Toolbar.OnMenuItemClickListener getSongMenuClickListener(Context context, Func0<List<Song>> func) {
        return item -> {
            List<Song> songs = func.call();
            switch (item.getItemId()) {
                case NEW_PLAYLIST:
                    newPlaylist(context, songs);
                    return true;
                case PLAYLIST_SELECTED:
                    addToPlaylist(context, item, songs);
                    return true;
                case R.id.addToQueue:
                    addToQueue(context, songs);
                    return true;
                case R.id.blacklist:
                    BlacklistHelper.addToBlacklist(songs);
                    return true;
                case R.id.delete:
                    delete(context, songs);
                    return true;
            }
            return false;
        };
    }

    public static PopupMenu.OnMenuItemClickListener getSongMenuClickListener(Context context, Song song, Action1<TaggerDialog> tagEditorCallback) {
        return item -> {
            switch (item.getItemId()) {
                case R.id.playNext:
                    playNext(context, song);
                    return true;
                case NEW_PLAYLIST:
                    newPlaylist(context, Collections.singletonList(song));
                    return true;
                case PLAYLIST_SELECTED:
                    addToPlaylist(context, item, Collections.singletonList(song));
                    return true;
                case R.id.addToQueue:
                    addToQueue(context, Collections.singletonList(song));
                    return true;
                case R.id.editTags:
                    tagEditorCallback.call(editTags(song));
                    return true;
                case R.id.share:
                    song.share(context);
                    return true;
                case R.id.ringtone:
                    setRingtone(context, song);
                    return true;
                case R.id.songInfo:
                    showSongInfo(context, song);
                    return true;
                case R.id.blacklist:
                    BlacklistHelper.addToBlacklist(song);
                    return true;
                case R.id.delete:
                    delete(context, Collections.singletonList(song));
                    return true;
            }
            return false;
        };
    }

    // Albums

    public static void play(Context context, Observable<List<Song>> observable) {
        MusicUtils.playAll(observable, message -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
    }

    public static void newPlaylist(Context context, Observable<List<Song>> observable) {
        observable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(songs -> PlaylistUtils.createPlaylistDialog(context, songs));
    }

    public static void addToPlaylist(Context context, MenuItem item, Observable<List<Song>> observable) {
        observable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(songs -> {
                    Playlist playlist = (Playlist) item.getIntent().getSerializableExtra(ShuttleUtils.ARG_PLAYLIST);
                    PlaylistUtils.addToPlaylist(context, playlist, songs);
                });
    }

    public static void addToQueue(Context context, Observable<List<Song>> observable) {
        observable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(songs -> MusicUtils.addToQueue(songs, message -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show()));
    }

    public static TaggerDialog editTags(Album album) {
        return TaggerDialog.newInstance(album);
    }

    public static void albumInfo(Context context, Album album) {
        DialogUtils.showAlbumBiographyDialog(context, album.albumArtistName, album.name);
    }

    public static void showArtworkChooserDialog(Context context, Album album) {
        ArtworkDialog.build(context, album).show();
    }

    public static void blackList(Observable<List<Song>> observable) {
        observable.observeOn(AndroidSchedulers.mainThread())
                .subscribe(BlacklistHelper::addToBlacklist);
    }

    public static void deleteAlbums(Context context, List<Album> albums, Observable<List<Song>> songsObservable) {
        new DialogUtils.DeleteDialogBuilder()
                .context(context)
                .singleMessageId(R.string.delete_album_desc)
                .multipleMessage(R.string.delete_album_desc_multiple)
                .itemNames(Stream.of(albums)
                        .map(album -> album.name)
                        .collect(Collectors.toList()))
                .songsToDelete(songsObservable)
                .build()
                .show();
    }

    public static Toolbar.OnMenuItemClickListener getAlbumMenuClickListener(Context context, Func0<List<Album>> func) {
        return item -> {

            List<Album> albums = func.call();

            Observable<List<Song>> songsObservable = Observable.defer(() ->
                    Observable.from(albums)
                            .flatMap(Album::getSongsObservable)
                            .reduce((songs, songs2) -> Stream.concat(Stream.of(songs), Stream.of(songs2))
                                    .collect(Collectors.toList()))
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread()));

            switch (item.getItemId()) {
                case NEW_PLAYLIST:
                    newPlaylist(context, songsObservable);
                    return true;
                case PLAYLIST_SELECTED:
                    addToPlaylist(context, item, songsObservable);
                    return true;
                case R.id.addToQueue:
                    addToQueue(context, songsObservable);
                    return true;
                case R.id.delete:
                    deleteAlbums(context, albums, songsObservable);
                    return true;
            }
            return false;
        };
    }

    public static PopupMenu.OnMenuItemClickListener getAlbumMenuClickListener(Context context, Album album, Action1<TaggerDialog> tagEditorCallback) {
        return new PopupMenu.OnMenuItemClickListener() {
            Observable<List<Song>> songsObservable = album.getSongsObservable()
                    .doOnNext(songs -> {
                        Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(b.year, a.year));
                        Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(a.track, b.track));
                        Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(a.discNumber, b.discNumber));
                    });

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.play:
                        play(context, songsObservable);
                        return true;
                    case NEW_PLAYLIST:
                        newPlaylist(context, songsObservable);
                        return true;
                    case PLAYLIST_SELECTED:
                        addToPlaylist(context, item, songsObservable);
                        return true;
                    case R.id.addToQueue:
                        addToQueue(context, songsObservable);
                        return true;
                    case R.id.editTags:
                        tagEditorCallback.call(editTags(album));
                        return true;
                    case R.id.info:
                        albumInfo(context, album);
                        return true;
                    case R.id.artwork:
                        showArtworkChooserDialog(context, album);
                        return true;
                    case R.id.blacklist:
                        blackList(songsObservable);
                        return true;
                    case R.id.delete:
                        deleteAlbums(context, Collections.singletonList(album), album.getSongsObservable());
                        return true;
                }
                return false;
            }
        };
    }

    // AlbumArtists

    public static TaggerDialog editTags(AlbumArtist albumArtist) {
        return TaggerDialog.newInstance(albumArtist);
    }

    public static void albumArtistInfo(Context context, AlbumArtist albumArtist) {
        DialogUtils.showArtistBiographyDialog(context, albumArtist.name);
    }

    public static void showArtworkChooserDialog(Context context, AlbumArtist albumArtist) {
        ArtworkDialog.build(context, albumArtist).show();
    }

    public static void deleteAlbumArtists(Context context, List<AlbumArtist> albumArtists, Observable<List<Song>> songsObservable) {
        new DialogUtils.DeleteDialogBuilder()
                .context(context)
                .singleMessageId(R.string.delete_album_artist_desc)
                .multipleMessage(R.string.delete_album_artist_desc_multiple)
                .itemNames(Stream.of(albumArtists)
                        .map(albumArtist -> albumArtist.name)
                        .collect(Collectors.toList()))
                .songsToDelete(songsObservable)
                .build()
                .show();
    }

    public static Toolbar.OnMenuItemClickListener getAlbumArtistMenuClickListener(Context context, Func0<List<AlbumArtist>> func) {
        return item -> {

            List<AlbumArtist> albumArtists = func.call();

            Observable<List<Song>> songsObservable = Observable.defer(() ->
                    Observable.from(albumArtists)
                            .flatMap(AlbumArtist::getSongsObservable)
                            .reduce((songs, songs2) -> Stream.concat(Stream.of(songs), Stream.of(songs2))
                                    .collect(Collectors.toList()))
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread()));

            switch (item.getItemId()) {
                case NEW_PLAYLIST:
                    newPlaylist(context, songsObservable);
                    return true;
                case PLAYLIST_SELECTED:
                    addToPlaylist(context, item, songsObservable);
                    return true;
                case R.id.addToQueue:
                    addToQueue(context, songsObservable);
                    return true;
                case R.id.delete:
                    deleteAlbumArtists(context, albumArtists, songsObservable);
                    return true;
            }
            return false;
        };
    }

    public static PopupMenu.OnMenuItemClickListener getAlbumArtistClickListener(Context context, AlbumArtist albumArtist, Action1<TaggerDialog> tagEditorCallback) {

        Observable<List<Song>> songsObservable = albumArtist.getSongsObservable()
                .map(songs -> {
                    Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(b.year, a.year));
                    Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(a.track, b.track));
                    Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(a.discNumber, b.discNumber));
                    Collections.sort(songs, (a, b) -> ComparisonUtils.compare(a.albumName, b.albumName));
                    return songs;
                });

        return item -> {
            switch (item.getItemId()) {
                case R.id.play:
                    play(context, songsObservable);
                    return true;
                case NEW_PLAYLIST:
                    newPlaylist(context, songsObservable);
                    return true;
                case PLAYLIST_SELECTED:
                    addToPlaylist(context, item, songsObservable);
                    return true;
                case R.id.addToQueue:
                    addToQueue(context, songsObservable);
                    return true;
                case R.id.editTags:
                    tagEditorCallback.call(editTags(albumArtist));
                    return true;
                case R.id.info:
                    albumArtistInfo(context, albumArtist);
                    return true;
                case R.id.editArtwork:
                    showArtworkChooserDialog(context, albumArtist);
                    return true;
                case R.id.blacklist:
                    blackList(songsObservable);
                    return true;
                case R.id.delete:
                    deleteAlbumArtists(context, Collections.singletonList(albumArtist), songsObservable);
                    return true;
            }
            return false;
        };
    }

    // Playlists

    public static void delete(Context context, Playlist playlist) {
        playlist.delete(context);
        Toast.makeText(context, R.string.playlist_deleted_message, Toast.LENGTH_SHORT).show();
    }

    public static void edit(Context context, Playlist playlist) {
        if (playlist.id == MusicUtils.PlaylistIds.RECENTLY_ADDED_PLAYLIST) {
            DialogUtils.showWeekSelectorDialog(context);
        }
    }

    public static void rename(Context context, Playlist playlist) {
        PlaylistUtils.renamePlaylistDialog(context, playlist);
    }

    public static void export(Context context, Playlist playlist) {
        PlaylistUtils.createM3uPlaylist(context, playlist);
    }

    public static void clear(Playlist playlist) {
        playlist.clear();
    }

    public static void setupPlaylistMenu(PopupMenu menu, Playlist playlist) {
        menu.inflate(R.menu.menu_playlist);

        if (!playlist.canDelete) {
            menu.getMenu().findItem(R.id.deletePlaylist).setVisible(false);
        }

        if (!playlist.canClear) {
            menu.getMenu().findItem(R.id.clearPlaylist).setVisible(false);
        }

        if (playlist.id != MusicUtils.PlaylistIds.RECENTLY_ADDED_PLAYLIST) {
            menu.getMenu().findItem(R.id.editPlaylist).setVisible(false);
        }

        if (!playlist.canRename) {
            menu.getMenu().findItem(R.id.renamePlaylist).setVisible(false);
        }

        if (playlist.id == MusicUtils.PlaylistIds.MOST_PLAYED_PLAYLIST) {
            menu.getMenu().findItem(R.id.exportPlaylist).setVisible(false);
        }
    }

    public static PopupMenu.OnMenuItemClickListener getPlaylistClickListener(final Context context, final Playlist playlist) {
        return item -> {
            switch (item.getItemId()) {
                case R.id.playPlaylist:
                    play(context, playlist.getSongsObservable());
                    return true;
                case R.id.deletePlaylist:
                    delete(context, playlist);
                    return true;
                case R.id.editPlaylist:
                    edit(context, playlist);
                    return true;
                case R.id.renamePlaylist:
                    rename(context, playlist);
                    return true;
                case R.id.exportPlaylist:
                    export(context, playlist);
                    return true;
                case R.id.clearPlaylist:
                    clear(playlist);
                    return true;
            }
            return false;
        };
    }

    // Genres

    public static PopupMenu.OnMenuItemClickListener getGenreClickListener(final Context context, final Genre genre) {
        return item -> {

            Observable<List<Song>> songsObservable = genre.getSongsObservable()
                    .map(songs -> {
                        Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(b.year, a.year));
                        Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(a.track, b.track));
                        Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(a.discNumber, b.discNumber));
                        Collections.sort(songs, (a, b) -> ComparisonUtils.compare(a.albumName, b.albumName));
                        Collections.sort(songs, (a, b) -> ComparisonUtils.compare(a.albumArtistName, b.albumArtistName));
                        return songs;
                    });

            switch (item.getItemId()) {
                case R.id.play:
                    play(context, genre.getSongsObservable());
                    return true;
                case NEW_PLAYLIST:
                    newPlaylist(context, songsObservable);
                    return true;
                case PLAYLIST_SELECTED:
                    addToPlaylist(context, item, songsObservable);
                    return true;
                case R.id.addToQueue:
                    addToQueue(context, songsObservable);
                    return true;
            }
            return false;
        };
    }
}
