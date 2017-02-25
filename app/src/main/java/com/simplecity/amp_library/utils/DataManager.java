package com.simplecity.amp_library.utils;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.crashlytics.android.Crashlytics;
import com.jakewharton.rxrelay.BehaviorRelay;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.model.Album;
import com.simplecity.amp_library.model.AlbumArtist;
import com.simplecity.amp_library.model.BlacklistedSong;
import com.simplecity.amp_library.model.Genre;
import com.simplecity.amp_library.model.Playlist;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.model.WhitelistFolder;
import com.simplecity.amp_library.sql.databases.BlacklistDbOpenHelper;
import com.simplecity.amp_library.sql.databases.WhitelistDbOpenHelper;
import com.simplecity.amp_library.sql.sqlbrite.SqlBriteUtils;
import com.squareup.sqlbrite.BriteDatabase;
import com.squareup.sqlbrite.SqlBrite;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Subscription;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class DataManager {

    private static final String TAG = "DataManager";

    private static DataManager instance;

    private Subscription songsSubscription;
    private BehaviorRelay<List<Song>> songsRelay = BehaviorRelay.create();

    private Subscription albumsSubscription;
    private BehaviorRelay<List<Album>> albumsRelay = BehaviorRelay.create();

    private Subscription albumArtistsSubscription;
    private BehaviorRelay<List<AlbumArtist>> albumArtistsRelay = BehaviorRelay.create();

    private Subscription genresSubscription;
    private BehaviorRelay<List<Genre>> genresRelay = BehaviorRelay.create();

    private Subscription playlistsSubscription;
    private BehaviorRelay<List<Playlist>> playlistsRelay = BehaviorRelay.create();

    private BriteDatabase blacklistDatabase;
    private Subscription blacklistSubscription;
    private BehaviorRelay<List<BlacklistedSong>> blacklistRelay = BehaviorRelay.create();

    private BriteDatabase whitelistDatabase;
    private Subscription whitelistSubscription;
    private BehaviorRelay<List<WhitelistFolder>> whitelistRelay = BehaviorRelay.create();

    public static DataManager getInstance() {
        if (instance == null) {
            instance = new DataManager();
        }
        return instance;
    }

    private DataManager() {

    }

    /**
     * Returns an {@link Observable}, which emits a List of {@link Song}s retrieved from the MediaStore.
     * <p>
     * This Observable is continuous. It will emit its most recent value upon subscription, and continue to emit
     * whenever the underlying {@code uri}'s data changes.
     * <p>
     * This Observable is backed by an {@link BehaviorRelay} subscribed to a {@link SqlBrite} {@link Observable}.
     * <p>
     * <b>Caution:</b>
     * <p>
     * Although the underlying {@link SqlBrite} {@link Observable} is subscribed on the {@link Schedulers#io()} thread,
     * it seems the {@code Scheduler} will not persist for subsequent {@code subscribe} calls once this {@link Observable} is unsubscribed.
     * Presumably, since subsequent {@code subscribe} calls to this {@link Observable} only re-emit the most recent emission from the
     * source {@link Observable}, the source {@link Observable} is no longer part of the current Observable chain (its job is now
     * just to keep the {@link BehaviorRelay} up to date). So a {@code Scheduler} must be supplied if you wish to ensure the work of this
     * {@link Observable} is not done on the calling thread. For now, the {@link Observable} is automatically subscribed on the
     * {@link Schedulers#io()} {@code scheduler}.
     */
    public Observable<List<Song>> getSongsRelay() {

        if (songsSubscription == null || songsSubscription.isUnsubscribed()) {

            Observable<List<Song>> songsObservable = SqlBriteUtils.createContinuousQuery(ShuttleApplication.getInstance(), Song::new, Song.getQuery());

            songsSubscription = Observable.combineLatest(songsObservable, getBlacklistRelay(), getWhitelistRelay(), (songs, blacklistedSongs, whitelistFolders) ->
            {
                List<Song> result = songs;

                //Filter out blacklisted songs
                if (!blacklistedSongs.isEmpty()) {
                    result = Stream.of(songs)
                            .filter(song -> !Stream.of(blacklistedSongs)
                                    .anyMatch(blacklistedSong -> blacklistedSong.songId == song.id))
                            .collect(Collectors.toList());
                }

                //Filter out non-whitelisted folders
                if (!whitelistFolders.isEmpty()) {
                    result = Stream.of(result)
                            .filter(song -> Stream.of(whitelistFolders)
                                    .anyMatch(whitelistFolder -> StringUtils.containsIgnoreCase(song.path, whitelistFolder.folder)))
                            .collect(Collectors.toList());
                }

                return result;
            }).subscribe(songsRelay, error -> Crashlytics.log("getSongsRelay error: " + error.getMessage()));
        }
        return songsRelay.subscribeOn(Schedulers.io()).map(ArrayList::new);
    }

    /**
     * Returns an {@link Observable}, which emits a List of {@link Album}s built from the {@link Song}s returned by
     * {@link #getSongsRelay()}.
     * <p>
     * This Observable is continuous. It will emit its most recent value upon subscription, and continue to emit
     * whenever the underlying {@code uri}'s data changes.
     * <p>
     * This Observable is backed by an {@link BehaviorRelay} subscribed to a {@link SqlBrite} {@link Observable}.
     * <p>
     * <b>Caution:</b>
     * <p>
     * Although the underlying {@link SqlBrite} {@link Observable} is subscribed on the {@link Schedulers#io()} thread,
     * it seems the {@code Scheduler} will not persist for subsequent {@code subscribe} calls once this {@link Observable} is unsubscribed.
     * Presumably, since subsequent {@code subscribe} calls to this {@link Observable} only re-emit the most recent emission from the
     * source {@link Observable}, the source {@link Observable} is no longer part of the current Observable chain (its job is now
     * just to keep the {@link BehaviorRelay} up to date). So a {@code Scheduler} must be supplied if you wish to ensure the work of this
     * {@link Observable} is not done on the calling thread. For now, the {@link Observable} is automatically subscribed on the
     * {@link Schedulers#io()} {@code scheduler}.
     */
    public Observable<List<Album>> getAlbumsRelay() {
        if (albumsSubscription == null || albumsSubscription.isUnsubscribed()) {
            albumsSubscription = getSongsRelay()
                    .flatMap(songs -> Observable.just(Operators.songsToAlbums(songs)))
                    .subscribe(albumsRelay, error -> Crashlytics.log("getAlbumsRelay error: " + error.getMessage()));
        }
        return albumsRelay.subscribeOn(Schedulers.io()).map(ArrayList::new);
    }

    /**
     * Returns an {@link Observable}, which emits a List of {@link AlbumArtist}s built from the {@link Album}s returned by
     * {@link #getAlbumsRelay()}.
     * <p>
     * This Observable is continuous. It will emit its most recent value upon subscription, and continue to emit
     * whenever the underlying {@code uri}'s data changes.
     * <p>
     * This Observable is backed by an {@link BehaviorRelay} subscribed to a {@link SqlBrite} {@link Observable}.
     * <p>
     * <b>Caution:</b>
     * <p>
     * Although the underlying {@link SqlBrite} {@link Observable} is subscribed on the {@link Schedulers#io()} thread,
     * it seems the {@code Scheduler} will not persist for subsequent {@code subscribe} calls once this {@link Observable} is unsubscribed.
     * Presumably, since subsequent {@code subscribe} calls to this {@link Observable} only re-emit the most recent emission from the
     * source {@link Observable}, the source {@link Observable} is no longer part of the current Observable chain (its job is now
     * just to keep the {@link BehaviorRelay} up to date). So a {@code Scheduler} must be supplied if you wish to ensure the work of this
     * {@link Observable} is not done on the calling thread. For now, the {@link Observable} is automatically subscribed on the
     * {@link Schedulers#io()} {@code scheduler}.
     */
    public Observable<List<AlbumArtist>> getAlbumArtistsRelay() {
        if (albumArtistsSubscription == null || albumArtistsSubscription.isUnsubscribed()) {
            albumArtistsSubscription = getAlbumsRelay()
                    .flatMap(albums -> Observable.just(Operators.albumsToAlbumArtists(albums)))
                    .subscribe(albumArtistsRelay,error -> Crashlytics.log("getAlbumArtistsRelay error: " + error.getMessage()));
        }
        return albumArtistsRelay.subscribeOn(Schedulers.io()).map(ArrayList::new);
    }

    /**
     * Returns an {@link Observable}, which emits a List of {@link Genre}s retrieved from the MediaStore.
     * <p>
     * This Observable is continuous. It will emit its most recent value upon subscription, and continue to emit
     * whenever the underlying {@code uri}'s data changes.
     * <p>
     * This Observable is backed by an {@link BehaviorRelay} subscribed to a {@link SqlBrite} {@link Observable}.
     * <p>
     * <b>Caution:</b>
     * <p>
     * Although the underlying {@link SqlBrite} {@link Observable} is subscribed on the {@link Schedulers#io()} thread,
     * it seems the {@code Scheduler} will not persist for subsequent {@code subscribe} calls once this {@link Observable} is unsubscribed.
     * Presumably, since subsequent {@code subscribe} calls to this {@link Observable} only re-emit the most recent emission from the
     * source {@link Observable}, the source {@link Observable} is no longer part of the current Observable chain (its job is now
     * just to keep the {@link BehaviorRelay} up to date). So a {@code Scheduler} must be supplied if you wish to ensure the work of this
     * {@link Observable} is not done on the calling thread. For now, the {@link Observable} is automatically subscribed on the
     * {@link Schedulers#io()} {@code scheduler}.
     */
    public Observable<List<Genre>> getGenresRelay() {
        if (genresSubscription == null || genresSubscription.isUnsubscribed()) {
            genresSubscription = SqlBriteUtils.createContinuousQuery(ShuttleApplication.getInstance(), Genre::new, Genre.getQuery())
                    .flatMap(genres -> Observable.from(genres)
                            .flatMap(genre -> genre.getSongsObservable(ShuttleApplication.getInstance())
                                    .filter(songs -> !songs.isEmpty())
                                    .map(songs -> {
                                        genre.numSongs = songs.size();
                                        return genre;
                                    }))
                            .toList())
                    .subscribe(genresRelay, error -> Crashlytics.log("getGenresRelay error: " + error.getMessage()));
        }
        return genresRelay.subscribeOn(Schedulers.io()).map(ArrayList::new);
    }

    /**
     * Returns an {@link Observable}, which emits a List of {@link Playlist}s retrieved from the MediaStore.
     * <p>
     * This Observable is continuous. It will emit its most recent value upon subscription, and continue to emit
     * whenever the underlying {@code uri}'s data changes.
     * <p>
     * This Observable is backed by an {@link BehaviorRelay} subscribed to a {@link SqlBrite} {@link Observable}.
     * <p>
     * <b>Caution:</b>
     * <p>
     * Although the underlying {@link SqlBrite} {@link Observable} is subscribed on the {@link Schedulers#io()} thread,
     * it seems the {@code Scheduler} will not persist for subsequent {@code subscribe} calls once this {@link Observable} is unsubscribed.
     * Presumably, since subsequent {@code subscribe} calls to this {@link Observable} only re-emit the most recent emission from the
     * source {@link Observable}, the source {@link Observable} is no longer part of the current Observable chain (its job is now
     * just to keep the {@link BehaviorRelay} up to date). So a {@code Scheduler} must be supplied if you wish to ensure the work of this
     * {@link Observable} is not done on the calling thread. For now, the {@link Observable} is automatically subscribed on the
     * {@link Schedulers#io()} {@code scheduler}.
     */
    public Observable<List<Playlist>> getPlaylistsRelay() {
        if (playlistsSubscription == null || playlistsSubscription.isUnsubscribed()) {
            playlistsSubscription = SqlBriteUtils.createContinuousQuery(ShuttleApplication.getInstance(), Playlist::new, Playlist.getQuery())
                    .subscribe(playlistsRelay, error -> Crashlytics.log("getPlaylistRelay error: " + error.getMessage()));
        }
        return playlistsRelay.subscribeOn(Schedulers.io()).map(ArrayList::new);
    }

    /**
     * Returns an Observable<List<Song>> from the songs relay, filtered by the passed in predicate.
     * <p>
     * This Observable is finite (it only emits once),
     */
    public Observable<List<Song>> getSongsObservable(Func1<Song, Boolean> predicate) {
        return getSongsRelay()
                .first()
                .flatMap(Observable::from)
                .filter(predicate).toList();
    }

    /**
     * @return a {@link BriteDatabase} wrapping the blacklist SqliteOpenHelper.
     */
    public BriteDatabase getBlacklistDatabase() {
        if (blacklistDatabase == null) {
            blacklistDatabase = new SqlBrite.Builder().build()
                    .wrapDatabaseHelper(new BlacklistDbOpenHelper(ShuttleApplication.getInstance()), Schedulers.io());
        }
        return blacklistDatabase;
    }

    /**
     * @return a <b>continuous</b> stream of {@link List<BlacklistedSong>>}, backed by a behavior relay for caching query results.
     */
    private Observable<List<BlacklistedSong>> getBlacklistRelay() {
        if (blacklistSubscription == null || blacklistSubscription.isUnsubscribed()) {
            blacklistSubscription = getBlacklistDatabase()
                    .createQuery(BlacklistDbOpenHelper.TABLE_SONGS, "SELECT * FROM " + BlacklistDbOpenHelper.TABLE_SONGS)
                    .mapToList(BlacklistedSong::new)
                    .subscribe(blacklistRelay, error -> Crashlytics.log("getBlacklistRelay error: " + error.getMessage()));
        }
        return blacklistRelay.subscribeOn(Schedulers.io()).map(ArrayList::new);
    }

    /**
     * @return a {@link BriteDatabase} wrapping the whitelist SqliteOpenHelper.
     */
    public BriteDatabase getWhitelistDatabase() {
        if (whitelistDatabase == null) {
            whitelistDatabase = new SqlBrite.Builder().build()
                    .wrapDatabaseHelper(new WhitelistDbOpenHelper(ShuttleApplication.getInstance()), Schedulers.io());
        }
        return whitelistDatabase;
    }

    /**
     * @return a <b>continuous</b> stream of {@link List<WhitelistFolder>>}, backed by a behavior relay for caching query results.
     */
    private Observable<List<WhitelistFolder>> getWhitelistRelay() {
        if (whitelistSubscription == null || whitelistSubscription.isUnsubscribed()) {
            whitelistSubscription = getWhitelistDatabase()
                    .createQuery(WhitelistDbOpenHelper.TABLE_FOLDERS, "SELECT * FROM " + WhitelistDbOpenHelper.TABLE_FOLDERS)
                    .mapToList(WhitelistFolder::new)
                    .subscribe(whitelistRelay, error -> Crashlytics.log("getWhitelistRelay error: " + error.getMessage()));
        }
        return whitelistRelay.subscribeOn(Schedulers.io()).map(ArrayList::new);
    }
}