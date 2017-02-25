package com.simplecity.amp_library.ui.activities;

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

import com.annimon.stream.Stream;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.model.Album;
import com.simplecity.amp_library.model.AlbumArtist;
import com.simplecity.amp_library.utils.ComparisonUtils;
import com.simplecity.amp_library.utils.DataManager;
import com.simplecity.amp_library.utils.MusicUtils;

import java.util.Collections;
import java.util.Locale;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;

import static com.simplecity.amp_library.utils.StringUtils.containsIgnoreCase;

public class VoiceSearchActivity extends BaseActivity {

    private static final String TAG = "VoiceSearchActivity";

    private String filterString;

    private Intent mIntent;

    private int position = -1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mIntent = getIntent();

        filterString = mIntent.getStringExtra(SearchManager.QUERY);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        super.onServiceConnected(name, service);
        if (mIntent != null && mIntent.getAction() != null && mIntent.getAction().equals("android.media.action.MEDIA_PLAY_FROM_SEARCH")) {
            searchAndPlaySongs();
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
    }

    private void searchAndPlaySongs() {
        //Search for album-artists, albums & songs matching our filter. Then, create an Observable emitting List<Song> for each type of result.
        //Then we concat the results, and return the first one which is non-empty. Order is important here, we want album-artist first, if it's
        //available, then albums, then songs.
        Observable.concat(
                //If we have an album artist matching our query, then play the songs by that album artist
                DataManager.getInstance().getAlbumArtistsRelay()
                        .first()
                        .flatMap(Observable::from)
                        .filter(albumArtist -> albumArtist.name.toLowerCase(Locale.getDefault()).contains(filterString.toLowerCase()))
                        .flatMap(AlbumArtist::getSongsObservable)
                        .map(songs -> {
                            Collections.sort(songs, (a, b) -> a.getAlbumArtist().compareTo(b.getAlbumArtist()));
                            Collections.sort(songs, (a, b) -> a.getAlbum().compareTo(b.getAlbum()));
                            Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(a.track, b.track));
                            Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(a.discNumber, b.discNumber));
                            return songs;
                        }),
                //If we have an album matching our query, then play the songs from that album
                DataManager.getInstance().getAlbumsRelay()
                        .first()
                        .flatMap(Observable::from)
                        .filter(album -> containsIgnoreCase(album.name, filterString)
                                || containsIgnoreCase(album.name, filterString)
                                || (Stream.of(album.artists).anyMatch(artist -> containsIgnoreCase(artist.name, filterString)))
                                || containsIgnoreCase(album.albumArtistName, filterString))
                        .flatMap(Album::getSongsObservable)
                        .map(songs -> {
                            Collections.sort(songs, (a, b) -> a.getAlbum().compareTo(b.getAlbum()));
                            Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(a.track, b.track));
                            Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(a.discNumber, b.discNumber));
                            return songs;
                        }),
                //If have a song, play that song, as well as others from the same album.
                DataManager.getInstance().getSongsRelay()
                        .first()
                        .flatMap(Observable::from)
                        .filter(song -> containsIgnoreCase(song.name, filterString)
                                || containsIgnoreCase(song.albumName, filterString)
                                || containsIgnoreCase(song.artistName, filterString)
                                || containsIgnoreCase(song.albumArtistName, filterString))
                        .flatMap(song -> song.getAlbum().getSongsObservable()
                                .map(songs -> {
                                    Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(a.track, b.track));
                                    Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(a.discNumber, b.discNumber));
                                    position = songs.indexOf(song);
                                    return songs;
                                }))
        )
                .first(songs -> !songs.isEmpty())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(songs -> {
                    if (songs != null) {
                        MusicUtils.playAll(songs, position, () -> {
                            final String message = getString(R.string.emptyplaylist);
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                        });
                    }
                    finish();
                });
    }

    @Override
    protected String screenName() {
        return TAG;
    }
}