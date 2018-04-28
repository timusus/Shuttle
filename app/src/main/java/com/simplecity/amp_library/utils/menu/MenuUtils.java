package com.simplecity.amp_library.utils.menu;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.MenuItem;

import com.simplecity.amp_library.model.InclExclItem;
import com.simplecity.amp_library.model.Playlist;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.rx.UnsafeAction;
import com.simplecity.amp_library.rx.UnsafeConsumer;
import com.simplecity.amp_library.sql.databases.InclExclHelper;
import com.simplecity.amp_library.utils.LogUtils;
import com.simplecity.amp_library.utils.MusicUtils;
import com.simplecity.amp_library.utils.PlaylistUtils;

import java.util.Collections;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

public class MenuUtils {

    private static final String TAG = "MenuUtils";

    private MenuUtils() {
        //no instance
    }

    public static void playNext(Song song, UnsafeConsumer<String> showToast) {
        MusicUtils.playNext(Collections.singletonList(song), showToast);
    }

    public static void playNext(List<Song> songs, UnsafeConsumer<String> showToast) {
        MusicUtils.playNext(songs, showToast);
    }

    // Todo: Remove context requirement
    public static void newPlaylist(Context context, List<Song> songs, UnsafeAction insertCallback) {
        PlaylistUtils.createPlaylistDialog(context, songs, insertCallback);
    }

    // Todo: Remove context requirement
    public static void addToPlaylist(Context context, MenuItem item, List<Song> songs, UnsafeAction insertCallback) {
        Playlist playlist = (Playlist) item.getIntent().getSerializableExtra(PlaylistUtils.ARG_PLAYLIST);
        PlaylistUtils.addToPlaylist(context, playlist, songs, insertCallback);
    }

    public static void addToQueue(List<Song> songs, @NonNull UnsafeConsumer<String> onComplete) {
        MusicUtils.addToQueue(songs, onComplete);
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

    public static void play(Single<List<Song>> observable, UnsafeConsumer<String> showToast) {
        MusicUtils.playAll(observable, showToast);
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

    /**
     * Todo: Remove context requirement
     * Add the passed in songs to a new playlist. The 'create playlist dialog' will be presented to the user.
     *
     * @param single         the songs to be added to the playlist
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
     * @param item           the menu item containing the intent which holds the Playlist
     * @param single         the songs to be added to the playlist
     * @param insertCallback called once the items have been successfully inserted into the playlist
     */
    public static Disposable addToPlaylist(Context context, MenuItem item, Single<List<Song>> single, UnsafeAction insertCallback) {
        return single.observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        songs -> {
                            Playlist playlist = (Playlist) item.getIntent().getSerializableExtra(PlaylistUtils.ARG_PLAYLIST);
                            PlaylistUtils.addToPlaylist(context, playlist, songs, insertCallback);
                        },
                        throwable -> LogUtils.logException(TAG, "Error adding to playlist", throwable)
                );
    }

    /**
     * Adds the passed in songs to the queue, then calls onComplete with a message to be displayed
     * in a toast.
     *
     * @param single     the songs to be added to the queue.
     * @param onComplete the consumer to consume the toast message
     */
    public static Disposable addToQueue(Single<List<Song>> single, @NonNull UnsafeConsumer<String> onComplete) {
        return single.observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        songs -> MusicUtils.addToQueue(songs, onComplete),
                        throwable -> LogUtils.logException(TAG, "Error adding to queue", throwable)
                );
    }
}