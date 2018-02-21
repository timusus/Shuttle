package com.simplecity.amp_library.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.interfaces.FileType;
import com.simplecity.amp_library.model.Album;
import com.simplecity.amp_library.model.AlbumArtist;
import com.simplecity.amp_library.model.BaseFileObject;
import com.simplecity.amp_library.model.FileObject;
import com.simplecity.amp_library.model.FolderObject;
import com.simplecity.amp_library.model.Genre;
import com.simplecity.amp_library.model.InclExclItem;
import com.simplecity.amp_library.model.Playlist;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.rx.UnsafeAction;
import com.simplecity.amp_library.rx.UnsafeCallable;
import com.simplecity.amp_library.rx.UnsafeConsumer;
import com.simplecity.amp_library.sql.databases.InclExclHelper;
import com.simplecity.amp_library.tagger.TaggerDialog;
import com.simplecity.amp_library.ui.dialog.BiographyDialog;
import com.simplecity.amp_library.ui.dialog.DeleteDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class MenuUtils implements MusicUtils.Defs {

    private static final String TAG = "MenuUtils";

    // Songs

    public static void playNext(Context context, Song song) {
        MusicUtils.playNext(Collections.singletonList(song), s ->
                Toast.makeText(context, s, Toast.LENGTH_SHORT).show());
    }

    public static void playNext(Context context, List<Song> songs) {
        MusicUtils.playNext(songs, s -> Toast.makeText(context, s, Toast.LENGTH_SHORT).show());
    }

    public static void newPlaylist(Context context, List<Song> songs, Runnable insertCallback) {
        PlaylistUtils.createPlaylistDialog(context, songs, insertCallback);
    }

    public static void addToPlaylist(Context context, MenuItem item, List<Song> songs, Runnable insertCallback) {
        Playlist playlist = (Playlist) item.getIntent().getSerializableExtra(PlaylistUtils.ARG_PLAYLIST);
        PlaylistUtils.addToPlaylist(context, playlist, songs, insertCallback);
    }

    public static void showSongInfo(Context context, Song song) {
        BiographyDialog.getSongInfoDialog(context, song).show();
    }

    public static void setRingtone(Context context, Song song) {
        ShuttleUtils.setRingtone(context, song);
    }

    public static TaggerDialog editTags(Song song) {
        return TaggerDialog.newInstance(song);
    }

    public static void addToQueue(Context context, List<Song> songs, Runnable onAdded) {
        MusicUtils.addToQueue(songs,
                message -> {
                    if (onAdded != null) {
                        onAdded.run();
                    }
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                });
    }

    public static void whitelist(Song song) {
        InclExclHelper.addToInclExcl(song, InclExclItem.Type.INCLUDE);
    }

    public static void whitelist(List<Song> songs) {
        InclExclHelper.addToInclExcl(songs, InclExclItem.Type.INCLUDE);
    }

    public static void blacklist(Song song) {
        InclExclHelper.addToInclExcl(song, InclExclItem.Type.EXCLUDE);
    }

    public static void blacklist(List<Song> songs) {
        InclExclHelper.addToInclExcl(songs, InclExclItem.Type.EXCLUDE);
    }

    public static void setupSongMenu(PopupMenu menu, boolean showRemoveButton) {
        menu.inflate(R.menu.menu_song);

        if (!showRemoveButton) {
            menu.getMenu().findItem(R.id.remove).setVisible(false);
        }

        // Add playlist menu
        SubMenu sub = menu.getMenu().findItem(R.id.addToPlaylist).getSubMenu();
        PlaylistUtils.createPlaylistMenu(sub);
    }

    public static Toolbar.OnMenuItemClickListener getQueueMenuClickListener(
            Context context, Single<List<Song>> songsSingle,
            UnsafeConsumer<DeleteDialog> deleteDialogCallback,
            Runnable removeFromQueue, Runnable insertCallback) {
        return item -> {
            switch (item.getItemId()) {
                case NEW_PLAYLIST:
                    newPlaylist(context, songsSingle, insertCallback);
                    return true;
                case PLAYLIST_SELECTED:
                    addToPlaylist(context, item, songsSingle, insertCallback);
                    return true;
                case R.id.delete:
                    songsSingle
                            .subscribeOn(AndroidSchedulers.mainThread())
                            .subscribe(songs -> {
                                final DeleteDialog dd = DeleteDialog.newInstance(() -> songs);
                                deleteDialogCallback.accept(dd);
                            });
                    return true;
                case R.id.queue_remove:
                    removeFromQueue.run();
                    return true;
            }
            return false;
        };
    }

    public static Toolbar.OnMenuItemClickListener getSongMenuClickListener(
            Context context, Single<List<Song>> songsSingle,
            UnsafeConsumer<DeleteDialog> deleteDialogCallback,
            Runnable insertCallback) {
        return item -> {
            switch (item.getItemId()) {
                case NEW_PLAYLIST:
                    newPlaylist(context, songsSingle, insertCallback);
                    return true;
                case PLAYLIST_SELECTED:
                    addToPlaylist(context, item, songsSingle, insertCallback);
                    return true;
                case R.id.playNext:
                    songsSingle
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(songs -> playNext(context, songs));
                    if (insertCallback != null) {
                        insertCallback.run();
                    }
                    return true;
                case R.id.addToQueue:
                    addToQueue(context, songsSingle, insertCallback);
                    return true;
                case R.id.blacklist:
                    blacklist(songsSingle);
                    return true;
                case R.id.delete:
                    songsSingle
                            .subscribeOn(AndroidSchedulers.mainThread())
                            .subscribe(songs -> deleteDialogCallback.accept(DeleteDialog.newInstance(() -> songs)));
                    return true;
            }
            return false;
        };
    }

    public static PopupMenu.OnMenuItemClickListener getSongMenuClickListener(
            Context context,
            Song song,
            UnsafeConsumer<TaggerDialog> tagEditorCallback,
            UnsafeConsumer<DeleteDialog> deleteDialogCallback,
            @Nullable UnsafeAction onSongRemoved,
            @Nullable UnsafeAction onPlayNext, Runnable insertCallback) {
        return item -> {
            switch (item.getItemId()) {
                case R.id.playNext:
                    if (onPlayNext != null) {
                        onPlayNext.run();
                    } else {
                        playNext(context, song);
                    }
                    return true;
                case NEW_PLAYLIST:
                    newPlaylist(context, Collections.singletonList(song), insertCallback);
                    return true;
                case PLAYLIST_SELECTED:
                    addToPlaylist(context, item, Collections.singletonList(song), insertCallback);
                    return true;
                case R.id.addToQueue:
                    addToQueue(context, Collections.singletonList(song), insertCallback);
                    return true;
                case R.id.editTags:
                    tagEditorCallback.accept(editTags(song));
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
                    blacklist(song);
                    return true;
                case R.id.delete:
                    deleteDialogCallback.accept(DeleteDialog.newInstance(() -> Collections.singletonList(song)));
                    return true;
                case R.id.remove:
                    if (onSongRemoved != null) {
                        onSongRemoved.run();
                    }
                    return true;
            }
            return false;
        };
    }

    // Albums

    public static void setupAlbumMenu(PopupMenu menu) {
        menu.inflate(R.menu.menu_album);

        // Add playlist menu
        SubMenu sub = menu.getMenu().findItem(R.id.addToPlaylist).getSubMenu();
        PlaylistUtils.createPlaylistMenu(sub);
    }

    private static Single<List<Song>> getSongsForAlbum(Album album) {
        return album.getSongsSingle()
                .map(songs -> {
                    Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(b.year, a.year));
                    Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(a.track, b.track));
                    Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(a.discNumber, b.discNumber));
                    return songs;
                });
    }

    private static Single<List<Song>> getSongsForAlbums(List<Album> albums) {
        return Observable.fromIterable(albums)
                .concatMap((Album album) -> getSongsForAlbum(album).toObservable())
                .reduce(Collections.emptyList(),
                        (BiFunction<List<Song>, List<Song>, List<Song>>) (songs, songs2) -> {
                            List<Song> allSongs = new ArrayList<>(songs);
                            allSongs.addAll(songs2);
                            return allSongs;
                        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public static void play(Context context, Single<List<Song>> observable) {
        MusicUtils.playAll(observable, message -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
    }

    public static void newPlaylist(Context context, Single<List<Song>> single, Runnable insertCallback) {
        single.observeOn(AndroidSchedulers.mainThread())
                .subscribe(songs -> PlaylistUtils.createPlaylistDialog(context, songs, insertCallback),
                        throwable -> LogUtils.logException(TAG, "Error adding to new playlist", throwable));
    }

    public static void addToPlaylist(Context context, MenuItem item, Single<List<Song>> single, Runnable insertCallback) {
        single.observeOn(AndroidSchedulers.mainThread())
                .subscribe(songs -> {
                    Playlist playlist = (Playlist) item.getIntent().getSerializableExtra(PlaylistUtils.ARG_PLAYLIST);
                    PlaylistUtils.addToPlaylist(context, playlist, songs, insertCallback);
                }, throwable -> LogUtils.logException(TAG, "Error adding to playlist", throwable));
    }

    public static void addToQueue(Context context, Single<List<Song>> single, Runnable onAdded) {
        single.observeOn(AndroidSchedulers.mainThread())
                .subscribe(songs -> MusicUtils.addToQueue(songs, message -> {
                            if (onAdded != null) {
                                onAdded.run();
                            }
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                        }),
                        throwable -> LogUtils.logException(TAG, "Error adding to queue", throwable));
    }

    public static TaggerDialog editTags(Album album) {
        return TaggerDialog.newInstance(album);
    }

    public static void showAlbumInfo(Context context, Album album) {
        BiographyDialog.getAlbumBiographyDialog(context, album.albumArtistName, album.name).show();
    }

    public static void showArtworkChooserDialog(Context context, Album album) {
        ArtworkDialog.build(context, album).show();
    }

    public static void whitelist(Single<List<Song>> single) {
        single.observeOn(AndroidSchedulers.mainThread())
                .subscribe(songs -> whitelist(songs),
                        throwable -> LogUtils.logException(TAG, "whitelist failed", throwable));
    }

    public static void blacklist(Single<List<Song>> single) {
        single.observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        songs -> blacklist(songs),
                        throwable -> LogUtils.logException(TAG, "blacklist failed", throwable));
    }

    public static Toolbar.OnMenuItemClickListener getAlbumMenuClickListener(
            Context context, UnsafeCallable<List<Album>> callable,
            UnsafeConsumer<DeleteDialog> deleteDialogCallback, Runnable insertCallback) {
        return item -> {
            switch (item.getItemId()) {
                case NEW_PLAYLIST:
                    newPlaylist(context, getSongsForAlbums(callable.call()), insertCallback);
                    return true;
                case PLAYLIST_SELECTED:
                    addToPlaylist(context, item, getSongsForAlbums(callable.call()), insertCallback);
                    return true;
                case R.id.playNext:
                    getSongsForAlbums(callable.call())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((List<Song> songs) -> playNext(context, songs));
                    if (insertCallback != null) {
                        insertCallback.run();
                    }
                    return true;
                case R.id.addToQueue:
                    addToQueue(context, getSongsForAlbums(callable.call()), insertCallback);
                    return true;
                case R.id.delete:
                    deleteDialogCallback.accept(DeleteDialog.newInstance(callable::call));
                    return true;
            }
            return false;
        };
    }

    public static PopupMenu.OnMenuItemClickListener getAlbumMenuClickListener(
            Context context, Album album, UnsafeConsumer<TaggerDialog> tagEditorCallback,
            UnsafeConsumer<DeleteDialog> deleteDialogCallback, UnsafeAction showUpgradeDialog,
            Runnable insertCallback) {
        return item -> {
            switch (item.getItemId()) {
                case R.id.play:
                    play(context, getSongsForAlbum(album));
                    return true;
                case R.id.playNext:
                    getSongsForAlbum(album)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(songs -> playNext(context, songs));
                    return true;
                case NEW_PLAYLIST:
                    newPlaylist(context, getSongsForAlbum(album), insertCallback);
                    return true;
                case PLAYLIST_SELECTED:
                    addToPlaylist(context, item, getSongsForAlbum(album), insertCallback);
                    return true;
                case R.id.addToQueue:
                    addToQueue(context, getSongsForAlbum(album), insertCallback);
                    return true;
                case R.id.editTags:
                    if (ShuttleUtils.isUpgraded()) {
                        tagEditorCallback.accept(editTags(album));
                    } else {
                        showUpgradeDialog.run();
                    }
                    return true;
                case R.id.info:
                    showAlbumInfo(context, album);
                    return true;
                case R.id.artwork:
                    showArtworkChooserDialog(context, album);
                    return true;
                case R.id.blacklist:
                    blacklist(getSongsForAlbum(album));
                    return true;
                case R.id.delete:
                    deleteDialogCallback.accept(DeleteDialog.newInstance(() -> Collections.singletonList(album)));
                    return true;
            }
            return false;
        };
    }

    // AlbumArtists

    private static Single<List<Song>> getSongsForAlbumArtist(AlbumArtist albumArtist) {
        return albumArtist.getSongsSingle()
                .map(songs -> {
                    Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(b.year, a.year));
                    Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(a.track, b.track));
                    Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(a.discNumber, b.discNumber));
                    Collections.sort(songs, (a, b) -> ComparisonUtils.compare(a.albumName, b.albumName));
                    return songs;
                });
    }

    private static Single<List<Song>> getSongsForAlbumArtists(List<AlbumArtist> albumArtists) {
        return Observable.fromIterable(albumArtists)
                .concatMap((AlbumArtist artist) -> getSongsForAlbumArtist(artist).toObservable())
                .reduce(Collections.emptyList(),
                        (BiFunction<List<Song>, List<Song>, List<Song>>) (songs, songs2) -> {
                            List<Song> allSongs = new ArrayList<>(songs);
                            allSongs.addAll(songs2);
                            return allSongs;
                        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public static TaggerDialog editTags(AlbumArtist albumArtist) {
        return TaggerDialog.newInstance(albumArtist);
    }

    public static void showArtistInfo(Context context, AlbumArtist albumArtist) {
        BiographyDialog.getArtistBiographyDialog(context, albumArtist.name).show();
    }

    public static void showArtworkChooserDialog(Context context, AlbumArtist albumArtist) {
        ArtworkDialog.build(context, albumArtist).show();
    }

    public static Toolbar.OnMenuItemClickListener getAlbumArtistMenuClickListener(
            Context context, UnsafeCallable<List<AlbumArtist>> callable,
            UnsafeConsumer<DeleteDialog> deleteDialogCallback, Runnable insertCallback) {
        return item -> {
            switch (item.getItemId()) {
                case NEW_PLAYLIST:
                    newPlaylist(context, getSongsForAlbumArtists(callable.call()), insertCallback);
                    return true;
                case PLAYLIST_SELECTED:
                    addToPlaylist(context, item, getSongsForAlbumArtists(callable.call()), insertCallback);
                    return true;
                case R.id.playNext:
                    getSongsForAlbumArtists(callable.call())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(songs -> playNext(context, songs));
                    if (insertCallback != null) {
                        insertCallback.run();
                    }
                    return true;
                case R.id.addToQueue:
                    addToQueue(context, getSongsForAlbumArtists(callable.call()), insertCallback);
                    return true;
                case R.id.delete:
                    deleteDialogCallback.accept(DeleteDialog.newInstance(callable::call));
                    return true;
            }
            return false;
        };
    }

    public static PopupMenu.OnMenuItemClickListener getAlbumArtistClickListener(
            Context context, AlbumArtist albumArtist,
            UnsafeConsumer<TaggerDialog> tagEditorCallback,
            UnsafeConsumer<DeleteDialog> deleteDialogCallback,
            UnsafeAction showUpgradeDialog, Runnable insertCallback) {
        return item -> {
            switch (item.getItemId()) {
                case R.id.play:
                    play(context, getSongsForAlbumArtist(albumArtist));
                    return true;
                case R.id.playNext:
                    getSongsForAlbumArtist(albumArtist)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(songs -> playNext(context, songs));
                    return true;
                case R.id.albumShuffle:
                    play(context, getSongsForAlbumArtist(albumArtist).map(Operators::albumShuffleSongs));
                    return true;
                case NEW_PLAYLIST:
                    newPlaylist(context, getSongsForAlbumArtist(albumArtist), insertCallback);
                    return true;
                case PLAYLIST_SELECTED:
                    addToPlaylist(context, item, getSongsForAlbumArtist(albumArtist), insertCallback);
                    return true;
                case R.id.addToQueue:
                    addToQueue(context, getSongsForAlbumArtist(albumArtist), insertCallback);
                    return true;
                case R.id.editTags:
                    if (!ShuttleUtils.isUpgraded()) {
                        showUpgradeDialog.run();
                    } else {
                        tagEditorCallback.accept(editTags(albumArtist));
                    }
                    return true;
                case R.id.info:
                    showArtistInfo(context, albumArtist);
                    return true;
                case R.id.artwork:
                    showArtworkChooserDialog(context, albumArtist);
                    return true;
                case R.id.blacklist:
                    blacklist(getSongsForAlbumArtist(albumArtist));
                    return true;
                case R.id.delete:
                    deleteDialogCallback.accept(DeleteDialog.newInstance(() -> Collections.singletonList(albumArtist)));
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
        if (playlist.id == PlaylistUtils.PlaylistIds.RECENTLY_ADDED_PLAYLIST) {
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

        if (playlist.id != PlaylistUtils.PlaylistIds.RECENTLY_ADDED_PLAYLIST) {
            menu.getMenu().findItem(R.id.editPlaylist).setVisible(false);
        }

        if (!playlist.canRename) {
            menu.getMenu().findItem(R.id.renamePlaylist).setVisible(false);
        }

        if (playlist.id == PlaylistUtils.PlaylistIds.MOST_PLAYED_PLAYLIST) {
            menu.getMenu().findItem(R.id.exportPlaylist).setVisible(false);
        }
    }

    public static void setupPlaylistMenu(Toolbar toolbar, Playlist playlist) {
        toolbar.inflateMenu(R.menu.menu_playlist);

        if (!playlist.canDelete) {
            toolbar.getMenu().findItem(R.id.deletePlaylist).setVisible(false);
        }

        if (!playlist.canClear) {
            toolbar.getMenu().findItem(R.id.clearPlaylist).setVisible(false);
        }

        if (playlist.id != PlaylistUtils.PlaylistIds.RECENTLY_ADDED_PLAYLIST) {
            toolbar.getMenu().findItem(R.id.editPlaylist).setVisible(false);
        }

        if (!playlist.canRename) {
            toolbar.getMenu().findItem(R.id.renamePlaylist).setVisible(false);
        }

        if (playlist.id == PlaylistUtils.PlaylistIds.MOST_PLAYED_PLAYLIST) {
            toolbar.getMenu().findItem(R.id.exportPlaylist).setVisible(false);
        }
    }

    public static PopupMenu.OnMenuItemClickListener getPlaylistPopupMenuClickListener(final Context context, final Playlist playlist, @Nullable UnsafeAction playlistDeleted) {
        return item -> handleMenuItemClicks(context, item, playlist, playlistDeleted);
    }

    public static boolean handleMenuItemClicks(Context context, MenuItem menuItem, Playlist playlist, @Nullable UnsafeAction playlistDeleted) {
        switch (menuItem.getItemId()) {
            case R.id.playPlaylist:
                play(context, playlist.getSongsObservable().first(Collections.emptyList()));
                return true;
            case R.id.playNext:
                playlist.getSongsObservable().first(Collections.emptyList())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(songs -> playNext(context, songs));
                return true;
            case R.id.deletePlaylist:
                delete(context, playlist);
                if (playlistDeleted != null) {
                    playlistDeleted.run();
                }
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
    }

    // Genres

    private static Single<List<Song>> getSongsForGenre(Genre genre) {
        return genre.getSongsObservable()
                .map(songs -> {
                    Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(b.year, a.year));
                    Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(a.track, b.track));
                    Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(a.discNumber, b.discNumber));
                    Collections.sort(songs, (a, b) -> ComparisonUtils.compare(a.albumName, b.albumName));
                    Collections.sort(songs, (a, b) -> ComparisonUtils.compare(a.albumArtistName, b.albumArtistName));
                    return songs;
                });
    }

    public static PopupMenu.OnMenuItemClickListener getGenreClickListener(
            final Context context, final Genre genre, Runnable insertCallback) {
        return item -> {
            switch (item.getItemId()) {
                case R.id.play:
                    play(context, getSongsForGenre(genre));
                    return true;
                case R.id.playNext:
                    getSongsForGenre(genre)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(songs -> playNext(context, songs));
                    return true;
                case NEW_PLAYLIST:
                    newPlaylist(context, getSongsForGenre(genre), insertCallback);
                    return true;
                case PLAYLIST_SELECTED:
                    addToPlaylist(context, item, getSongsForGenre(genre), insertCallback);
                    return true;
                case R.id.addToQueue:
                    addToQueue(context, getSongsForGenre(genre), insertCallback);
                    return true;
            }
            return false;
        };
    }

    // Folders

    static Single<Song> getSongForFile(FileObject fileObject) {
        return FileHelper.getSong(new File(fileObject.path))
                .observeOn(AndroidSchedulers.mainThread());
    }

    static Single<List<Song>> getSongsForFolderObject(FolderObject folderObject) {
        return FileHelper.getSongList(new File(folderObject.path), true, false);
    }

    public static void setupFolderMenu(PopupMenu menu, BaseFileObject fileObject) {

        menu.inflate(R.menu.menu_file);

        // Add playlist menu
        SubMenu sub = menu.getMenu().findItem(R.id.addToPlaylist).getSubMenu();
        PlaylistUtils.createPlaylistMenu(sub);

        if (!fileObject.canReadWrite()) {
            menu.getMenu().findItem(R.id.rename).setVisible(false);
        }

        switch (fileObject.fileType) {
            case FileType.FILE:
                menu.getMenu().findItem(R.id.play).setVisible(false);
                break;
            case FileType.FOLDER:
                menu.getMenu().findItem(R.id.songInfo).setVisible(false);
                menu.getMenu().findItem(R.id.ringtone).setVisible(false);
                menu.getMenu().findItem(R.id.share).setVisible(false);
                menu.getMenu().findItem(R.id.editTags).setVisible(false);
                break;
            case FileType.PARENT:
                break;
        }
    }

    public static void scanFile(Context context, FileObject fileObject) {
        CustomMediaScanner.scanFile(fileObject.path, message -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
    }

    public static void scanFolder(Context context, FolderObject folderObject) {
        CustomMediaScanner.scanFile(context, folderObject);
    }

    public static void renameFile(Context context, BaseFileObject fileObject, UnsafeAction filenameChanged) {

        @SuppressLint("InflateParams")
        View customView = LayoutInflater.from(context).inflate(R.layout.dialog_rename, null);

        final EditText editText = customView.findViewById(R.id.editText);
        editText.setText(fileObject.name);

        MaterialDialog.Builder builder = DialogUtils.getBuilder(context);
        if (fileObject.fileType == FileType.FILE) {
            builder.title(R.string.rename_file);
        } else {
            builder.title(R.string.rename_folder);
        }

        builder.customView(customView, false);
        builder.positiveText(R.string.save)
                .onPositive((materialDialog, dialogAction) -> {
                    if (editText.getText() != null) {
                        if (FileHelper.renameFile(context, fileObject, editText.getText().toString())) {
                            filenameChanged.run();
                        } else {
                            Toast.makeText(context,
                                    fileObject.fileType == FileType.FOLDER ? R.string.rename_folder_failed : R.string.rename_file_failed,
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
        builder.negativeText(R.string.cancel)
                .show();
    }

    public static void deleteFile(Context context, BaseFileObject fileObject, UnsafeAction fileDeleted) {
        MaterialDialog.Builder builder = DialogUtils.getBuilder(context)
                .title(R.string.delete_item)
                .iconRes(R.drawable.ic_warning_24dp);
        if (fileObject.fileType == FileType.FILE) {
            builder.content(String.format(context.getResources().getString(
                    R.string.delete_file_confirmation_dialog), fileObject.name));
        } else {
            builder.content(String.format(context.getResources().getString(
                    R.string.delete_folder_confirmation_dialog), fileObject.path));
        }
        builder.positiveText(R.string.button_ok)
                .onPositive((materialDialog, dialogAction) -> {
                    if (FileHelper.deleteFile(new File(fileObject.path))) {
                        fileDeleted.run();
                        CustomMediaScanner.scanFiles(Collections.singletonList(fileObject.path), null);
                    } else {
                        Toast.makeText(context,
                                fileObject.fileType == FileType.FOLDER ? R.string.delete_folder_failed : R.string.delete_file_failed,
                                Toast.LENGTH_LONG).show();
                    }
                });
        builder.negativeText(R.string.cancel)
                .show();
    }

    @Nullable
    public static PopupMenu.OnMenuItemClickListener getFolderMenuClickListener(
            Context context, BaseFileObject fileObject,
            UnsafeConsumer<TaggerDialog> tagEditorCallback,
            UnsafeAction filenameChanged, UnsafeAction fileDeleted, Runnable insertCallback) {
        switch (fileObject.fileType) {
            case FileType.FILE:
                return getFileMenuClickListener(context, (FileObject) fileObject, tagEditorCallback, filenameChanged, fileDeleted, insertCallback);
            case FileType.FOLDER:
                return getFolderMenuClickListener(context, (FolderObject) fileObject, filenameChanged, fileDeleted, insertCallback);
        }
        return null;
    }

    private static PopupMenu.OnMenuItemClickListener getFileMenuClickListener(
            Context context, FileObject fileObject, UnsafeConsumer<TaggerDialog> tagEditorCallback,
            UnsafeAction filenameChanged, UnsafeAction fileDeleted, Runnable insertCallback) {
        return menuItem -> {

            Consumer<Throwable> errorHandler = e -> LogUtils.logException(TAG, "getFileMenuClickListener threw error", e);

            switch (menuItem.getItemId()) {
                case R.id.playNext:
                    getSongForFile(fileObject).subscribe(song -> playNext(context, song), errorHandler);
                    return true;
                case NEW_PLAYLIST:
                    getSongForFile(fileObject).subscribe(song -> newPlaylist(context, Collections.singletonList(song), insertCallback), errorHandler);
                    return true;
                case PLAYLIST_SELECTED:
                    getSongForFile(fileObject).subscribe(song -> addToPlaylist(context, menuItem, Collections.singletonList(song), insertCallback), errorHandler);
                    return true;
                case R.id.addToQueue:
                    getSongForFile(fileObject).subscribe(song -> addToQueue(context, Collections.singletonList(song), insertCallback), errorHandler);
                    return true;
                case R.id.scan:
                    scanFile(context, fileObject);
                    return true;
                case R.id.editTags:
                    getSongForFile(fileObject).subscribe(song -> tagEditorCallback.accept(editTags(song)), errorHandler);
                    return true;
                case R.id.share:
                    getSongForFile(fileObject).subscribe(song -> song.share(context), errorHandler);
                    return true;
                case R.id.ringtone:
                    getSongForFile(fileObject).subscribe(song -> setRingtone(context, song), errorHandler);
                    return true;
                case R.id.songInfo:
                    getSongForFile(fileObject).subscribe(song -> showSongInfo(context, song), errorHandler);
                    return true;
                case R.id.blacklist:
                    getSongForFile(fileObject).subscribe(song -> blacklist(song), errorHandler);
                case R.id.whitelist:
                    getSongForFile(fileObject).subscribe(song -> whitelist(song), errorHandler);
                    return true;
                case R.id.rename:
                    renameFile(context, fileObject, filenameChanged);
                    return true;
                case R.id.delete:
                    deleteFile(context, fileObject, fileDeleted);
                    return true;
            }
            return false;
        };
    }

    private static PopupMenu.OnMenuItemClickListener getFolderMenuClickListener(
            Context context, FolderObject folderObject, UnsafeAction filenameChanged,
            UnsafeAction fileDeleted, Runnable insertCallback) {
        return menuItem -> {
            switch (menuItem.getItemId()) {
                case R.id.play:
                    play(context, getSongsForFolderObject(folderObject));
                    return true;
                case R.id.playNext:
                    getSongsForFolderObject(folderObject)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(songs -> playNext(context, songs));
                    return true;
                case NEW_PLAYLIST:
                    newPlaylist(context, getSongsForFolderObject(folderObject), insertCallback);
                    return true;
                case PLAYLIST_SELECTED:
                    addToPlaylist(context, menuItem, getSongsForFolderObject(folderObject), insertCallback);
                    return true;
                case R.id.addToQueue:
                    addToQueue(context, getSongsForFolderObject(folderObject), insertCallback);
                    return true;
                case R.id.scan:
                    scanFolder(context, folderObject);
                    return true;
                case R.id.whitelist:
                    whitelist(getSongsForFolderObject(folderObject));
                case R.id.blacklist:
                    blacklist(getSongsForFolderObject(folderObject));
                    return true;
                case R.id.rename:
                    renameFile(context, folderObject, filenameChanged);
                    return true;
                case R.id.delete:
                    deleteFile(context, folderObject, fileDeleted);
                    return true;
            }
            return false;
        };
    }
}
