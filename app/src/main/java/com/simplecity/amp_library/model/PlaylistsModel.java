package com.simplecity.amp_library.model;

import com.annimon.stream.Optional;
import com.simplecity.amp_library.utils.DataManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

public class PlaylistsModel {

    private static final String TAG = "PlaylistsModel";

    @Inject
    public PlaylistsModel() {
    }

    public Observable<List<Playlist>> getPlaylistsObservable() {
        Observable<List<Playlist>> defaultPlaylistsObservable =
                Observable.fromCallable(() -> {
                    List<Playlist> playlists = new ArrayList<>();

                    Playlist podcastPlaylist = Playlist.podcastPlaylist();
                    if (podcastPlaylist != null) {
                        playlists.add(podcastPlaylist);
                    }

                    playlists.add(Playlist.recentlyAddedPlaylist);
                    playlists.add(Playlist.mostPlayedPlaylist);

                    return playlists;
                }).subscribeOn(Schedulers.io());

        Observable<List<Playlist>> playlistsObservable = DataManager.getInstance().getPlaylistsRelay();


        return Observable.combineLatest(
                defaultPlaylistsObservable, playlistsObservable, (defaultPlaylists, playlists1) -> {
                    List<Playlist> list = new ArrayList<>();
                    list.addAll(defaultPlaylists);
                    list.addAll(playlists1);
                    return list;
                })
                .concatMap(playlists -> Observable.fromIterable(playlists)
                        .concatMap(playlist -> playlist.getSongsObservable()
                                .first(Collections.emptyList())
                                .flatMapObservable(songs -> {
                                            if (playlist.type == Playlist.Type.USER_CREATED
                                                    || playlist.type == Playlist.Type.RECENTLY_ADDED
                                                    || !songs.isEmpty()) {
                                                return Observable.just(Optional.of(playlist));
                                            }
                                            return Observable.just(Optional.empty());
                                        }
                                )
                                .filter(Optional::isPresent)
                                .map(playlistOptional -> (Playlist) playlistOptional.get())
                        )
                        .toList()
                        .toObservable());
    }
}