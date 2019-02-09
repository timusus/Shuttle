package com.simplecity.amp_library.utils.menu;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import com.simplecity.amp_library.data.Repository;
import com.simplecity.amp_library.model.Album;
import com.simplecity.amp_library.model.AlbumArtist;
import com.simplecity.amp_library.model.Genre;
import com.simplecity.amp_library.model.Playlist;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.playback.MediaManager;
import com.simplecity.amp_library.rx.UnsafeConsumer;
import com.simplecity.amp_library.ui.screens.drawer.NavigationEventRelay;
import com.simplecity.amp_library.ui.screens.playlist.dialog.CreatePlaylistDialog;
import com.simplecity.amp_library.utils.LogUtils;
import com.simplecity.amp_library.utils.playlists.PlaylistManager;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import java.util.Collections;
import java.util.List;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;

public class MenuUtils {

    private static final String TAG = "MenuUtils";

    private MenuUtils() {
        //no instance
    }

    // Todo: Remove context requirement
    public static void addToPlaylist(PlaylistManager playlistManager, Playlist playlist, List<Song> songs, Function1<Integer, Unit> insertCallback) {
        playlistManager.addToPlaylist(playlist, songs, insertCallback);
    }

    public static void addToQueue(MediaManager mediaManager, List<Song> songs, @NonNull UnsafeConsumer<Integer> onSongsAddedToQueue) {
        mediaManager.addToQueue(songs, numSongs -> {
            onSongsAddedToQueue.accept(numSongs);
            return Unit.INSTANCE;
        });
    }

    public static void whitelist(Repository.WhitelistRepository whitelistRepository, Song song) {
        whitelistRepository.addSong(song);
    }

    public static void whitelist(Repository.WhitelistRepository whitelistRepository, List<Song> songs) {
        whitelistRepository.addAllSongs(songs);
    }

    public static void blacklist(Repository.BlacklistRepository blacklistRepository, Song song) {
        blacklistRepository.addSong(song);
    }

    public static void blacklist(Repository.BlacklistRepository blacklistRepository, List<Song> songs) {
        blacklistRepository.addAllSongs(songs);
    }

    public static void play(MediaManager mediaManager, Single<List<Song>> observable, Function0<Unit> onPlaybackError) {
        mediaManager.playAll(observable, () -> {
            onPlaybackError.invoke();
            return Unit.INSTANCE;
        });
    }

    @SuppressLint("CheckResult")
    public static void whitelist(Repository.WhitelistRepository whitelistRepository, Single<List<Song>> single) {
        single.observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        songs -> MenuUtils.whitelist(whitelistRepository, songs),
                        throwable -> LogUtils.logException(TAG, "whitelist failed", throwable)
                );
    }

    @SuppressLint("CheckResult")
    public static void blacklist(Repository.BlacklistRepository blacklistRepository, Single<List<Song>> single) {
        single.observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        songs -> MenuUtils.blacklist(blacklistRepository, songs),
                        throwable -> LogUtils.logException(TAG, "blacklist failed", throwable)
                );
    }

    @SuppressLint("CheckResult")
    public static void goToArtist(Repository.AlbumArtistsRepository albumArtistsRepository, AlbumArtist currentAlbumArtist, NavigationEventRelay navigationEventRelay) {
        // MediaManager.getAlbumArtist() is only populate with the album the current Song belongs to.
        // Let's find the matching AlbumArtist in the DataManager.albumArtistRelay
        albumArtistsRepository.getAlbumArtists()
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
     */
    public static Disposable newPlaylist(Fragment fragment, Single<List<Song>> single) {
        return single.observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        songs -> CreatePlaylistDialog.Companion.newInstance(songs).show(fragment.getChildFragmentManager(), "CreatePlaylistFragment"),
                        throwable -> LogUtils.logException(TAG, "Error adding to new playlist", throwable)
                );
    }

    /**
     * Adds the passed in songs to the queue, then calls onComplete with a message to be displayed
     * in a toast.
     *
     * @param single the songs to be added to the queue.
     */
    public static Disposable addToQueue(
            MediaManager mediaManager,
            Single<List<Song>> single, Function1<Integer, Unit> onSongsAddedToQueue) {
        return single.observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        songs -> mediaManager.addToQueue(songs, numSongs -> {
                            onSongsAddedToQueue.invoke(numSongs);
                            return Unit.INSTANCE;
                        }),
                        throwable -> LogUtils.logException(TAG, "Error adding to queue", throwable)
                );
    }
}