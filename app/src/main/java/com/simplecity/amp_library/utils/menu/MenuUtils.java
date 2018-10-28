package com.simplecity.amp_library.utils.menu;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.MenuItem;

import com.simplecity.amp_library.model.Album;
import com.simplecity.amp_library.model.AlbumArtist;
import com.simplecity.amp_library.model.Genre;
import com.simplecity.amp_library.model.InclExclItem;
import com.simplecity.amp_library.model.Playlist;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.playback.MediaManager;
import com.simplecity.amp_library.rx.UnsafeAction;
import com.simplecity.amp_library.rx.UnsafeConsumer;
import com.simplecity.amp_library.sql.databases.InclExclHelper;
import com.simplecity.amp_library.ui.drawer.NavigationEventRelay;
import com.simplecity.amp_library.utils.DataManager;
import com.simplecity.amp_library.utils.LogUtils;
import com.simplecity.amp_library.utils.PlaylistUtils;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import java.util.Collections;
import java.util.List;

import io.reactivex.schedulers.Schedulers;
import kotlin.Unit;

public class MenuUtils {

    private static final String TAG = "MenuUtils";

    private MenuUtils() {
        //no instance
    }

    public static void playNext(MediaManager mediaManager, Song song, UnsafeConsumer<String> showToast) {
        mediaManager.playNext(Collections.singletonList(song), s -> {
            showToast.accept(s);
            return Unit.INSTANCE;
        });
    }

    public static void playNext(MediaManager mediaManager, List<Song> songs, UnsafeConsumer<String> showToast) {
        mediaManager.playNext(songs, s -> {
            showToast.accept(s);
            return Unit.INSTANCE;
        });
    }

    // Todo: Remove context requirement
    public static void newPlaylist(Context context, List<Song> songs, UnsafeAction insertCallback) {
        PlaylistUtils.createPlaylistDialog(context, songs, insertCallback);
    }

    // Todo: Remove context requirement
    public static void addToPlaylist(Context context, Playlist playlist, List<Song> songs, UnsafeAction insertCallback) {
        PlaylistUtils.addToPlaylist(context, playlist, songs, insertCallback);
    }

    public static void addToQueue(MediaManager mediaManager, List<Song> songs, @NonNull UnsafeConsumer<String> onComplete) {
        mediaManager.addToQueue(songs, s -> {
            onComplete.accept(s);
            return Unit.INSTANCE;
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

    public static void play(MediaManager mediaManager, Single<List<Song>> observable, UnsafeConsumer<String> showToast) {
        mediaManager.playAll(observable, s -> {
            showToast.accept(s);
            return Unit.INSTANCE;
        });
    }

    @SuppressLint("CheckResult")
    public static void whitelist(Single<List<Song>> single) {
        single.observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        MenuUtils::whitelist,
                        throwable -> LogUtils.logException(TAG, "whitelist failed", throwable)
                );
    }

    @SuppressLint("CheckResult")
    public static void blacklist(Single<List<Song>> single) {
        single.observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        MenuUtils::blacklist,
                        throwable -> LogUtils.logException(TAG, "blacklist failed", throwable)
                );
    }

    @SuppressLint("CheckResult")
    public static void goToArtist(AlbumArtist currentAlbumArtist, NavigationEventRelay navigationEventRelay) {
        // MediaManager.getAlbumArtist() is only populate with the album the current Song belongs to.
        // Let's find the matching AlbumArtist in the DataManager.albumArtistRelay
        DataManager.getInstance().getAlbumArtistsRelay()
                .first(Collections.emptyList())
                .flatMapObservable(Observable::fromIterable)
                .filter(albumArtist -> currentAlbumArtist != null && albumArtist.name.equals(currentAlbumArtist.name) && albumArtist.albums.containsAll(currentAlbumArtist.albums))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        albumArtist -> navigationEventRelay.sendEvent(new NavigationEventRelay.NavigationEvent(NavigationEventRelay.NavigationEvent.Type.GO_TO_ARTIST, albumArtist, true)),
                        error -> LogUtils.logException(TAG, "goToArtist error", error)
                );
    }

    public static void goToAlbum(Album album, NavigationEventRelay navigationEventRelay) {
        navigationEventRelay.sendEvent(new NavigationEventRelay.NavigationEvent(NavigationEventRelay.NavigationEvent.Type.GO_TO_ALBUM, album, true));
    }

    @SuppressLint("CheckResult")
    public static void goToGenre(Single<Genre> genreSingle, NavigationEventRelay navigationEventRelay) {
        genreSingle
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        (UnsafeConsumer<Genre>) genre -> navigationEventRelay.sendEvent(new NavigationEventRelay.NavigationEvent(NavigationEventRelay.NavigationEvent.Type.GO_TO_GENRE, genre, true)),
                        error -> LogUtils.logException(TAG, "Error retrieving genre", error)
                );
    }

    /**
     * Todo: Remove context requirement
     * Add the passed in songs to a new playlist. The 'create playlist dialog' will be presented to the user.
     *
     * @param single the songs to be added to the playlist
     * @param insertCallback called when the songs are successfully added to the playlist
     */
    public static Disposable newPlaylist(Context context, Single<List<Song>> single, UnsafeAction insertCallback) {
        return single.observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        songs -> PlaylistUtils.createPlaylistDialog(context, songs, insertCallback),
                        throwable -> LogUtils.logException(TAG, "Error adding to new playlist", throwable)
                );
    }

    /**
     * Todo: Remove context requirement
     * <p>
     * Adds the passed in songs to the playlist. The playlist is included in the data of {@link MenuItem#getIntent()}
     *
     * @param playlist the playlist to add the songs to
     * @param single the songs to be added to the playlist
     * @param insertCallback called once the items have been successfully inserted into the playlist
     */
    public static Disposable addToPlaylist(Context context, Playlist playlist, Single<List<Song>> single, UnsafeAction insertCallback) {
        return single.observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        songs -> {
                            PlaylistUtils.addToPlaylist(context, playlist, songs, insertCallback);
                        },
                        throwable -> LogUtils.logException(TAG, "Error adding to playlist", throwable)
                );
    }

    /**
     * Adds the passed in songs to the queue, then calls onComplete with a message to be displayed
     * in a toast.
     *
     * @param single the songs to be added to the queue.
     * @param onComplete the consumer to consume the toast message
     */
    public static Disposable addToQueue(MediaManager mediaManager, Single<List<Song>> single, @NonNull UnsafeConsumer<String> onComplete) {
        return single.observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        songs -> mediaManager.addToQueue(songs, s -> {
                            onComplete.accept(s);
                            return Unit.INSTANCE;
                        }),
                        throwable -> LogUtils.logException(TAG, "Error adding to queue", throwable)
                );
    }
}