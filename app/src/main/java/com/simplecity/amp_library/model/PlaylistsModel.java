package com.simplecity.amp_library.model;

import com.simplecity.amp_library.utils.ComparisonUtils;
import com.simplecity.amp_library.utils.DataManager;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import rx.Observable;
import rx.schedulers.Schedulers;

public class PlaylistsModel {

    @Inject
    public PlaylistsModel() {
    }

    public Observable<List<Playlist>> getPlaylists() {

        Observable<List<Playlist>> defaultPlaylistsObservable =
                Observable.fromCallable(() -> {
                            List<Playlist> playlists = new ArrayList<>();

                            Playlist podcastPlaylist = Playlist.podcastPlaylist();
                            if (podcastPlaylist != null) {
                                playlists.add(podcastPlaylist);
                            }

                            playlists.add(Playlist.recentlyAddedPlaylist());
                            playlists.add(Playlist.mostPlayedPlaylist());

                            return playlists;
                        }
                ).subscribeOn(Schedulers.io());

        Observable<List<Playlist>> playlistsObservable = DataManager.getInstance().getPlaylistsRelay();

        return Observable.combineLatest(
                defaultPlaylistsObservable, playlistsObservable, (defaultPlaylists, playlists) -> {
                    List<Playlist> list = new ArrayList<>();
                    list.addAll(defaultPlaylists);
                    list.addAll(playlists);
                    return list;
                })
                .flatMap(playlists -> Observable.from(playlists)
                        .flatMap(playlist -> playlist.getSongsObservable()
                                .flatMap(songs -> {
                                    if (playlist.type == Playlist.Type.USER_CREATED
                                            || playlist.type == Playlist.Type.RECENTLY_ADDED
                                            || !songs.isEmpty()) {
                                        return Observable.just(playlist);
                                    } else {
                                        return Observable.empty();
                                    }
                                }))
                        .sorted((a, b) -> ComparisonUtils.compare(a.name, b.name))
                        .sorted((a, b) -> ComparisonUtils.compareInt(a.type, b.type))
                        .toList());
    }
}