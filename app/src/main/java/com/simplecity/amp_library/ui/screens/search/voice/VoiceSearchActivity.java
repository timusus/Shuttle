package com.simplecity.amp_library.ui.screens.search.voice;

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import com.annimon.stream.Stream;
import com.simplecity.amp_library.data.Repository;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.playback.MediaManager;
import com.simplecity.amp_library.ui.common.BaseActivity;
import com.simplecity.amp_library.ui.screens.main.MainActivity;
import com.simplecity.amp_library.utils.ComparisonUtils;
import com.simplecity.amp_library.utils.LogUtils;
import com.simplecity.amp_library.utils.extensions.AlbumExtKt;
import dagger.android.AndroidInjection;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import java.util.Collections;
import java.util.Locale;
import javax.inject.Inject;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

import static com.simplecity.amp_library.utils.StringUtils.containsIgnoreCase;

public class VoiceSearchActivity extends BaseActivity {

    private static final String TAG = "VoiceSearchActivity";

    private String filterString;

    private Intent intent;

    private int position = -1;

    @Inject
    MediaManager mediaManager;

    @Inject
    Repository.SongsRepository songsRepository;

    @Inject
    Repository.AlbumsRepository albumsRepository;

    @Inject
    Repository.AlbumArtistsRepository albumArtistsRepository;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);

        intent = getIntent();

        filterString = intent.getStringExtra(SearchManager.QUERY);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        super.onServiceConnected(name, service);
        if (intent != null && intent.getAction() != null && intent.getAction().equals("android.media.action.MEDIA_PLAY_FROM_SEARCH")) {
            searchAndPlaySongs();
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
    }

    private void searchAndPlaySongs() {

        albumArtistsRepository.getAlbumArtists()
                .first(Collections.emptyList())
                .flatMapObservable(Observable::fromIterable)
                .filter(albumArtist -> albumArtist.name.toLowerCase(Locale.getDefault()).contains(filterString.toLowerCase()))
                .flatMapSingle(albumArtist -> albumArtist.getSongsSingle(songsRepository))
                .map(songs -> {
                    Collections.sort(songs, (a, b) -> a.getAlbumArtist().compareTo(b.getAlbumArtist()));
                    Collections.sort(songs, (a, b) -> a.getAlbum().compareTo(b.getAlbum()));
                    Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(a.track, b.track));
                    Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(a.discNumber, b.discNumber));
                    return songs;
                });

        //Search for album-artists, albums & songs matching our filter. Then, create an Observable emitting List<Song> for each type of result.
        //Then we concat the results, and return the first one which is non-empty. Order is important here, we want album-artist first, if it's
        //available, then albums, then songs.
        Observable.concat(
                //If we have an album artist matching our query, then play the songs by that album artist
                albumArtistsRepository.getAlbumArtists()
                        .first(Collections.emptyList())
                        .flatMapObservable(Observable::fromIterable)
                        .filter(albumArtist -> albumArtist.name.toLowerCase(Locale.getDefault()).contains(filterString.toLowerCase()))
                        .flatMapSingle(albumArtist -> albumArtist.getSongsSingle(songsRepository))
                        .map(songs -> {
                            Collections.sort(songs, (a, b) -> a.getAlbumArtist().compareTo(b.getAlbumArtist()));
                            Collections.sort(songs, (a, b) -> a.getAlbum().compareTo(b.getAlbum()));
                            Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(a.track, b.track));
                            Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(a.discNumber, b.discNumber));
                            return songs;
                        }),
                //If we have an album matching our query, then play the songs from that album
                albumsRepository.getAlbums()
                        .first(Collections.emptyList())
                        .flatMapObservable(Observable::fromIterable)
                        .filter(album -> containsIgnoreCase(album.name, filterString)
                                || containsIgnoreCase(album.name, filterString)
                                || (Stream.of(album.artists).anyMatch(artist -> containsIgnoreCase(artist.name, filterString)))
                                || containsIgnoreCase(album.albumArtistName, filterString))
                        .flatMapSingle(album -> AlbumExtKt.getSongsSingle(album, songsRepository))
                        .map(songs -> {
                            Collections.sort(songs, (a, b) -> a.getAlbum().compareTo(b.getAlbum()));
                            Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(a.track, b.track));
                            Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(a.discNumber, b.discNumber));
                            return songs;
                        }),
                //If have a song, play that song, as well as others from the same album.
                songsRepository.getSongs((Function1<? super Song, Boolean>) null)
                        .first(Collections.emptyList())
                        .flatMapObservable(Observable::fromIterable)
                        .filter(song -> containsIgnoreCase(song.name, filterString)
                                || containsIgnoreCase(song.albumName, filterString)
                                || containsIgnoreCase(song.artistName, filterString)
                                || containsIgnoreCase(song.albumArtistName, filterString))
                        .flatMapSingle(song -> AlbumExtKt.getSongsSingle(song.getAlbum(), songsRepository)
                                .map(songs -> {
                                    Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(a.track, b.track));
                                    Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(a.discNumber, b.discNumber));
                                    position = songs.indexOf(song);
                                    return songs;
                                }))
        )
                .filter(songs -> !songs.isEmpty())
                .firstOrError()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(songs -> {
                    if (songs != null) {
                        mediaManager.playAll(songs, position, true, () -> {
                            // Todo: Show playback error toast
                            return Unit.INSTANCE;
                        });
                        startActivity(new Intent(this, MainActivity.class));
                    }
                    finish();
                }, error -> {
                    LogUtils.logException(TAG, "Error attempting to playAll()", error);
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                });
    }

    @Override
    protected String screenName() {
        return TAG;
    }
}