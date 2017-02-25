package com.simplecity.amp_library.utils;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.view.SubMenu;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.model.Album;
import com.simplecity.amp_library.model.AlbumArtist;
import com.simplecity.amp_library.model.Playlist;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.sql.databases.BlacklistHelper;
import com.simplecity.amp_library.tagger.TaggerDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;

public class MenuUtils implements MusicUtils.Defs {

    public static void addSongMenuOptions(final Context context, final PopupMenu menu) {
        menu.getMenu().add(SONG_FRAGMENT_GROUP_ID, PLAY_NEXT, 0, R.string.play_next);
        SubMenu sub = menu.getMenu().addSubMenu(SONG_FRAGMENT_GROUP_ID, ADD_TO_PLAYLIST, 1, R.string.add_to_playlist);
        PlaylistUtils.makePlaylistMenu(context, sub, SONG_FRAGMENT_GROUP_ID);
        menu.getMenu().add(SONG_FRAGMENT_GROUP_ID, QUEUE, 2, R.string.add_to_queue);
        if (ShuttleUtils.isUpgraded()) {
            menu.getMenu().add(SONG_FRAGMENT_GROUP_ID, TAGGER, 3, R.string.edit_tags);
        }
        menu.getMenu().add(SONG_FRAGMENT_GROUP_ID, USE_AS_RINGTONE, 5, R.string.ringtone_menu);

        menu.getMenu().add(SONG_FRAGMENT_GROUP_ID, VIEW_INFO, 6, R.string.song_info);

        menu.getMenu().add(SONG_FRAGMENT_GROUP_ID, SHARE, 7, R.string.share);

        menu.getMenu().add(SONG_FRAGMENT_GROUP_ID, BLACKLIST, 8, R.string.blacklist_title);

        menu.getMenu().add(SONG_FRAGMENT_GROUP_ID, DELETE_ITEM, 9, R.string.delete_item);
    }

    public static void addClickHandler(final AppCompatActivity activity, final PopupMenu menu, final Song song, final PopupMenu.OnMenuItemClickListener onMenuItemClickListener) {
        PopupMenu.OnMenuItemClickListener listener = item -> {
            switch (item.getItemId()) {
                case QUEUE:
                    List<Song> songs = new ArrayList<>();
                    songs.add(song);
                    MusicUtils.addToQueue(activity, songs);
                    break;
                case PLAY_NEXT:
                    songs = new ArrayList<>();
                    songs.add(song);
                    MusicUtils.playNext(activity, songs);
                    break;
                case NEW_PLAYLIST:
                    songs = new ArrayList<>();
                    songs.add(song);
                    PlaylistUtils.createPlaylistDialog(activity, songs);
                    break;
                case PLAYLIST_SELECTED:
                    songs = new ArrayList<>();
                    songs.add(song);
                    Playlist playlist = (Playlist) item.getIntent().getSerializableExtra(ShuttleUtils.ARG_PLAYLIST);
                    PlaylistUtils.addToPlaylist(activity, playlist, songs);
                    break;
                case USE_AS_RINGTONE:
                    // Set the system setting to make this the current ringtone
                    ShuttleUtils.setRingtone(activity, song);
                    break;
                case DELETE_ITEM:
                    new DialogUtils.DeleteDialogBuilder()
                            .context(activity)
                            .singleMessageId(R.string.delete_song_desc)
                            .multipleMessage(R.string.delete_song_desc_multiple)
                            .itemNames(Collections.singletonList(song.name))
                            .songsToDelete(Observable.just(Collections.singletonList(song)))
                            .build()
                            .show();
                    break;
                case TAGGER:
                    TaggerDialog.newInstance(song)
                            .show(activity.getSupportFragmentManager());
                    break;
                case VIEW_INFO:
                    DialogUtils.showSongInfoDialog(activity, song);
                    break;
                case SHARE:
                    song.share(activity);
                    break;
            }
            return onMenuItemClickListener.onMenuItemClick(item);
        };
        menu.setOnMenuItemClickListener(listener);
    }

    public static void addQueueMenuOptions(final Context context, final PopupMenu menu) {
        menu.getMenu().add(QUEUE_FRAGMENT_GROUP_ID, PLAY_NEXT, 0, R.string.play_next);
        SubMenu sub = menu.getMenu().addSubMenu(SONG_FRAGMENT_GROUP_ID, ADD_TO_PLAYLIST, 1, R.string.add_to_playlist);
        PlaylistUtils.makePlaylistMenu(context, sub, SONG_FRAGMENT_GROUP_ID);
        if (ShuttleUtils.isUpgraded()) {
            menu.getMenu().add(QUEUE_FRAGMENT_GROUP_ID, TAGGER, 3, R.string.edit_tags);
        }
        menu.getMenu().add(QUEUE_FRAGMENT_GROUP_ID, USE_AS_RINGTONE, 5, R.string.ringtone_menu);

        menu.getMenu().add(QUEUE_FRAGMENT_GROUP_ID, REMOVE, 6, R.string.remove_from_queue);

        menu.getMenu().add(QUEUE_FRAGMENT_GROUP_ID, VIEW_INFO, 7, R.string.song_info);

        menu.getMenu().add(SONG_FRAGMENT_GROUP_ID, SHARE, 8, R.string.share);

        menu.getMenu().add(QUEUE_FRAGMENT_GROUP_ID, BLACKLIST, 9, R.string.blacklist_title);

        menu.getMenu().add(QUEUE_FRAGMENT_GROUP_ID, DELETE_ITEM, 10, R.string.delete_item);

    }

    public static void addAlbumMenuOptions(Context context, PopupMenu menu) {
        menu.getMenu().add(ALBUM_FRAGMENT_GROUP_ID, PLAY_SELECTION, 0, R.string.play_selection);
        SubMenu sub = menu.getMenu().addSubMenu(ALBUM_FRAGMENT_GROUP_ID, 1, 0, R.string.add_to_playlist);
        PlaylistUtils.makePlaylistMenu(context, sub, ALBUM_FRAGMENT_GROUP_ID);
        menu.getMenu().add(ALBUM_FRAGMENT_GROUP_ID, QUEUE, 2, R.string.add_to_queue);
        if (ShuttleUtils.isUpgraded()) {
            menu.getMenu().add(ALBUM_FRAGMENT_GROUP_ID, TAGGER, 3, R.string.edit_tags);
        }

        menu.getMenu().add(ALBUM_FRAGMENT_GROUP_ID, EDIT_ARTWORK, 4, R.string.artwork_edit);

        menu.getMenu().add(QUEUE_FRAGMENT_GROUP_ID, BLACKLIST, 5, R.string.blacklist_title);

        menu.getMenu().add(ALBUM_FRAGMENT_GROUP_ID, DELETE_ITEM, 6, R.string.delete_item);
    }

    public static void addClickHandler(final AppCompatActivity activity, final PopupMenu menu, final Album album) {
        PopupMenu.OnMenuItemClickListener listener = item -> {

            Observable<List<Song>> songsObservable = album.getSongsObservable()
                    .doOnNext(songs -> {
                        Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(b.year, a.year));
                        Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(a.track, b.track));
                        Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(a.discNumber, b.discNumber));
                    });

            switch (item.getItemId()) {
                case PLAY_SELECTION:
                    MusicUtils.playAll(activity, songsObservable);
                    return true;
                case NEW_PLAYLIST:
                    songsObservable
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(songs -> PlaylistUtils.createPlaylistDialog(activity, songs));
                    return true;
                case PLAYLIST_SELECTED:
                    songsObservable
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(songs -> {
                                Playlist playlist = (Playlist) item.getIntent().getSerializableExtra(ShuttleUtils.ARG_PLAYLIST);
                                PlaylistUtils.addToPlaylist(activity, playlist, songs);
                            });
                    return true;
                case QUEUE:
                    songsObservable
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(songs -> MusicUtils.addToQueue(activity, songs));
                    return true;
                case TAGGER:
                    TaggerDialog.newInstance(album)
                            .show(activity.getSupportFragmentManager());
                    return true;
                case BLACKLIST:
                        songsObservable
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(BlacklistHelper::addToBlacklist);
                    return true;
                case DELETE_ITEM:
                    new DialogUtils.DeleteDialogBuilder()
                            .context(activity)
                            .singleMessageId(R.string.delete_album_desc)
                            .multipleMessage(R.string.delete_album_desc_multiple)
                            .itemNames(Collections.singletonList(album.name))
                            .songsToDelete(album.getSongsObservable())
                            .build()
                            .show();
                    return true;
                case VIEW_INFO:
                    DialogUtils.showAlbumBiographyDialog(activity, album.albumArtistName, album.name);
                    return true;
                case EDIT_ARTWORK:
                    ArtworkDialog.showDialog(activity, album);
                    return true;
            }
            return false;
        };
        menu.setOnMenuItemClickListener(listener);
    }

    public static void addAlbumArtistMenuOptions(Context context, PopupMenu menu) {
        menu.getMenu().add(ARTIST_FRAGMENT_GROUP_ID, PLAY_SELECTION, 0, R.string.play_selection);
        SubMenu sub = menu.getMenu().addSubMenu(ARTIST_FRAGMENT_GROUP_ID, ADD_TO_PLAYLIST, 1, R.string.add_to_playlist);
        PlaylistUtils.makePlaylistMenu(context, sub, ARTIST_FRAGMENT_GROUP_ID);
        menu.getMenu().add(ARTIST_FRAGMENT_GROUP_ID, QUEUE, 2, R.string.add_to_queue);
        if (ShuttleUtils.isUpgraded()) {
            menu.getMenu().add(ARTIST_FRAGMENT_GROUP_ID, TAGGER, 3, R.string.edit_tags);
            menu.getMenu().add(ARTIST_FRAGMENT_GROUP_ID, EDIT_ARTWORK, 4, R.string.artwork_edit);
        }

        menu.getMenu().add(QUEUE_FRAGMENT_GROUP_ID, BLACKLIST, 5, R.string.blacklist_title);

        menu.getMenu().add(ARTIST_FRAGMENT_GROUP_ID, DELETE_ITEM, 6, R.string.delete_item);
    }

    public static void addClickHandler(final AppCompatActivity activity, final PopupMenu menu, final AlbumArtist albumArtist) {
        PopupMenu.OnMenuItemClickListener listener = item -> {

            Observable<List<Song>> songsObservable = albumArtist.getSongsObservable()
                    .doOnNext(songs -> {
                        Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(b.year, a.year));
                        Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(a.track, b.track));
                        Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(a.discNumber, b.discNumber));
                        Collections.sort(songs, (a, b) -> ComparisonUtils.compare(a.albumName, b.albumName));
                    });

            switch (item.getItemId()) {
                case PLAY_SELECTION:
                    MusicUtils.playAll(activity, songsObservable);
                    return true;
                case NEW_PLAYLIST:
                    songsObservable
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(songs -> PlaylistUtils.createPlaylistDialog(activity, songs));
                    return true;
                case PLAYLIST_SELECTED:
                    songsObservable
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(songs -> {
                                Playlist playlist = (Playlist) item.getIntent().getSerializableExtra(ShuttleUtils.ARG_PLAYLIST);
                                PlaylistUtils.addToPlaylist(activity, playlist, songs);
                            });
                    return true;
                case QUEUE:
                    songsObservable
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(songs -> MusicUtils.addToQueue(activity, songs));
                    return true;
                case TAGGER:
                    TaggerDialog.newInstance(albumArtist)
                            .show(activity.getSupportFragmentManager());
                    return true;
                case BLACKLIST:
                    songsObservable
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(BlacklistHelper::addToBlacklist);
                    return true;
                case DELETE_ITEM:
                    new DialogUtils.DeleteDialogBuilder()
                            .context(activity)
                            .singleMessageId(R.string.delete_album_artist_desc)
                            .multipleMessage(R.string.delete_album_artist_desc_multiple)
                            .itemNames(Collections.singletonList(albumArtist.name))
                            .songsToDelete(albumArtist.getSongsObservable())
                            .build()
                            .show();
                    return true;
                case VIEW_INFO:
                    DialogUtils.showArtistBiographyDialog(activity, albumArtist.name);
                    return true;
                case EDIT_ARTWORK:
                    ArtworkDialog.showDialog(activity, albumArtist);
                    return true;
            }
            return false;
        };
        menu.setOnMenuItemClickListener(listener);
    }

    public static void addPlaylistMenuOptions(PopupMenu menu, Playlist playlist) {

        menu.getMenu().add(PLAYLIST_FRAGMENT_GROUP_ID, PLAY_SELECTION, 0, R.string.play_selection);

        if (playlist.canDelete) {
            menu.getMenu().add(PLAYLIST_FRAGMENT_GROUP_ID, MusicUtils.PlaylistMenuOrder.DELETE_PLAYLIST, 0, R.string.delete_playlist_menu);
        }

        if (playlist.canClear) {
            menu.getMenu().add(PLAYLIST_FRAGMENT_GROUP_ID, MusicUtils.PlaylistMenuOrder.CLEAR_PLAYLIST, 1, R.string.clear_playlist_menu);
        }

        if (playlist.id == MusicUtils.PlaylistIds.RECENTLY_ADDED_PLAYLIST) {
            menu.getMenu().add(PLAYLIST_FRAGMENT_GROUP_ID, MusicUtils.PlaylistMenuOrder.EDIT_PLAYLIST, 0, R.string.edit_playlist_menu);
        }

        if (playlist.canRename) {
            menu.getMenu().add(PLAYLIST_FRAGMENT_GROUP_ID, MusicUtils.PlaylistMenuOrder.RENAME_PLAYLIST, 0, R.string.rename_playlist_menu);
        }

        if (playlist.id != MusicUtils.PlaylistIds.MOST_PLAYED_PLAYLIST) {
            menu.getMenu().add(PLAYLIST_FRAGMENT_GROUP_ID, MusicUtils.PlaylistMenuOrder.EXPORT_PLAYLIST, 0, R.string.export_playlist);
        }

    }

    public static void addClickHandler(final Context context, final PopupMenu menu, final Playlist playlist, final MaterialDialog.SingleButtonCallback renameListener, final MaterialDialog.SingleButtonCallback editListener) {
        final PopupMenu.OnMenuItemClickListener listener = item -> {
            switch (item.getItemId()) {
                case PLAY_SELECTION:
                    MusicUtils.playAll(context, playlist.getSongsObservable(context));
                    break;
                case MusicUtils.PlaylistMenuOrder.DELETE_PLAYLIST:
                    playlist.delete(context);
                    Toast.makeText(context, R.string.playlist_deleted_message, Toast.LENGTH_SHORT).show();
                    break;
                case MusicUtils.PlaylistMenuOrder.EDIT_PLAYLIST:
                    if (playlist.id == MusicUtils.PlaylistIds.RECENTLY_ADDED_PLAYLIST) {
                        DialogUtils.showWeekSelectorDialog(context, editListener);
                    }
                    break;
                case MusicUtils.PlaylistMenuOrder.RENAME_PLAYLIST:
                    PlaylistUtils.renamePlaylistDialog(context, playlist, renameListener);
                    break;
                case MusicUtils.PlaylistMenuOrder.EXPORT_PLAYLIST:
                    PlaylistUtils.createM3uPlaylist(context, playlist);
                    break;
                case MusicUtils.PlaylistMenuOrder.CLEAR_PLAYLIST:
                    if (playlist.id == Playlist.favoritesPlaylist().id) {
                        PlaylistUtils.clearFavorites(context);
                    } else if (playlist.id == MusicUtils.PlaylistIds.MOST_PLAYED_PLAYLIST) {
                        PlaylistUtils.clearMostPlayed(context);
                    }
            }

            return true;
        };
        menu.setOnMenuItemClickListener(listener);
    }

}
