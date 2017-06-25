package com.simplecity.amp_library.utils;

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
import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.interfaces.FileType;
import com.simplecity.amp_library.model.Album;
import com.simplecity.amp_library.model.AlbumArtist;
import com.simplecity.amp_library.model.BaseFileObject;
import com.simplecity.amp_library.model.FileObject;
import com.simplecity.amp_library.model.FolderObject;
import com.simplecity.amp_library.model.Genre;
import com.simplecity.amp_library.model.Playlist;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.sql.databases.BlacklistHelper;
import com.simplecity.amp_library.tagger.TaggerDialog;
import com.simplecity.amp_library.ui.dialog.BiographyDialog;
import com.simplecity.amp_library.ui.dialog.DeleteDialog;

import java.io.File;
import java.util.Collections;
import java.util.List;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
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
        BiographyDialog.getSongInfoDialog(context, song).show();
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

    public static void blacklist(Song song) {
        BlacklistHelper.addToBlacklist(song);
    }

    public static void blacklist(List<Song> songs) {
        BlacklistHelper.addToBlacklist(songs);
    }

    public static void delete(Context context, List<Song> songs) {
        new DeleteDialog.DeleteDialogBuilder()
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
                    blacklist(songs);
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
                    blacklist(song);
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
        BiographyDialog.getAlbumBiographyDialog(context, album.albumArtistName, album.name);
    }

    public static void showArtworkChooserDialog(Context context, Album album) {
        ArtworkDialog.build(context, album).show();
    }

    public static void blacklist(Observable<List<Song>> observable) {
        observable.observeOn(AndroidSchedulers.mainThread())
                .subscribe(MenuUtils::blacklist);
    }

    public static void deleteAlbums(Context context, List<Album> albums, Observable<List<Song>> songsObservable) {
        new DeleteDialog.DeleteDialogBuilder()
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
                        blacklist(songsObservable);
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
        BiographyDialog.getArtistBiographyDialog(context, albumArtist.name);
    }

    public static void showArtworkChooserDialog(Context context, AlbumArtist albumArtist) {
        ArtworkDialog.build(context, albumArtist).show();
    }

    public static void deleteAlbumArtists(Context context, List<AlbumArtist> albumArtists, Observable<List<Song>> songsObservable) {
        new DeleteDialog.DeleteDialogBuilder()
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
                case R.id.artwork:
                    showArtworkChooserDialog(context, albumArtist);
                    return true;
                case R.id.blacklist:
                    blacklist(songsObservable);
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

    // Folders

    public static void setupFolderMenu(Context context, PopupMenu menu, BaseFileObject fileObject) {

        menu.inflate(R.menu.menu_file);

        // Add playlist menu
        SubMenu sub = menu.getMenu().findItem(R.id.addToPlaylist).getSubMenu();
        PlaylistUtils.makePlaylistMenu(context, sub);

        if (!fileObject.canReadWrite()) {
            menu.getMenu().findItem(R.id.rename).setVisible(false);
            menu.getMenu().findItem(R.id.remove).setVisible(false);
        }

        switch (fileObject.fileType) {
            case FileType.FILE:
                menu.getMenu().findItem(R.id.play).setVisible(false);
                menu.getMenu().findItem(R.id.setInitialDir).setVisible(false);
                break;
            case FileType.FOLDER:
                menu.getMenu().findItem(R.id.playNext).setVisible(false);
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

    public static void renameFile(Context context, BaseFileObject fileObject, Action0 filenameChanged) {
        View customView = LayoutInflater.from(context).inflate(R.layout.dialog_rename, null);
        final EditText editText = (EditText) customView.findViewById(R.id.editText);
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
                            filenameChanged.call();
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

    public static void deleteFile(Context context, BaseFileObject fileObject, Action0 fileDeleted) {
        MaterialDialog.Builder builder = DialogUtils.getBuilder(context)
                .title(R.string.delete_item)
                .iconRes(R.drawable.ic_dialog_alert);
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
                        fileDeleted.call();
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

    public static void setInitialDir(Context context, FolderObject folderObject) {
        SettingsManager.getInstance().setFolderBrowserInitialDir(folderObject.path);
        Toast.makeText(context, folderObject.path + context.getResources().getString(R.string.initial_dir_set_message), Toast.LENGTH_SHORT).show();
    }

    @Nullable
    public static PopupMenu.OnMenuItemClickListener getFolderMenuClickListener(Context context, BaseFileObject fileObject, Action1<TaggerDialog> tagEditorCallback, Action0 filenameChanged, Action0 fileDeleted) {
        switch (fileObject.fileType) {
            case FileType.FILE:
                return getFileMenuClickListener(context, (FileObject) fileObject, tagEditorCallback, filenameChanged, fileDeleted);
            case FileType.FOLDER:
                return getFolderMenuClickListener(context, (FolderObject) fileObject, filenameChanged, fileDeleted);
        }
        return null;
    }

    private static PopupMenu.OnMenuItemClickListener getFileMenuClickListener(Context context, FileObject fileObject, Action1<TaggerDialog> tagEditorCallback, Action0 filenameChanged, Action0 fileDeleted) {
        return menuItem -> {
            Observable<Song> songObservable = FileHelper.getSong(new File(fileObject.path))
                    .observeOn(AndroidSchedulers.mainThread());

            switch (menuItem.getItemId()) {
                case R.id.playNext:
                    songObservable.subscribe(song -> playNext(context, song));
                    return true;
                case NEW_PLAYLIST:
                    songObservable.subscribe(song -> newPlaylist(context, Collections.singletonList(song)));
                    return true;
                case PLAYLIST_SELECTED:
                    songObservable.subscribe(song -> addToPlaylist(context, menuItem, Collections.singletonList(song)));
                    return true;
                case R.id.addToQueue:
                    songObservable.subscribe(song -> addToQueue(context, Collections.singletonList(song)));
                    return true;
                case R.id.scan:
                    scanFile(context, fileObject);
                    return true;
                case R.id.editTags:
                    songObservable.subscribe(song -> tagEditorCallback.call(editTags(song)));
                    return true;
                case R.id.share:
                    songObservable.subscribe(song -> song.share(context));
                    return true;
                case R.id.ringtone:
                    songObservable.subscribe(song -> setRingtone(context, song));
                    return true;
                case R.id.songInfo:
                    songObservable.subscribe(song -> showSongInfo(context, song));
                    return true;
                case R.id.blacklist:
                    songObservable.subscribe(MenuUtils::blacklist);
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

    private static PopupMenu.OnMenuItemClickListener getFolderMenuClickListener(Context context, FolderObject folderObject, Action0 filenameChanged, Action0 fileDeleted) {
        return menuItem -> {

            Observable<List<Song>> songsObservable = FileHelper.getSongList(new File(folderObject.path), true, false);

            switch (menuItem.getItemId()) {
                case R.id.play:
                    play(context, songsObservable);
                    return true;
                case NEW_PLAYLIST:
                    newPlaylist(context, songsObservable);
                    return true;
                case PLAYLIST_SELECTED:
                    addToPlaylist(context, menuItem, songsObservable);
                    return true;
                case R.id.addToQueue:
                    addToQueue(context, songsObservable);
                    return true;
                case R.id.setInitialDir:
                    setInitialDir(context, folderObject);
                    return true;
                case R.id.scan:
                    scanFolder(context, folderObject);
                    return true;
                case R.id.blacklist:
                    blacklist(songsObservable);
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