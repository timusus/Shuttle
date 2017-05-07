package com.simplecity.amp_library.detail;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.MenuItem;

import com.afollestad.materialdialogs.MaterialDialog;
import com.simplecity.amp_library.model.Playlist;
import com.simplecity.amp_library.tagger.TaggerDialog;
import com.simplecity.amp_library.ui.presenters.Presenter;
import com.simplecity.amp_library.utils.MusicUtils;
import com.simplecity.amp_library.utils.PermissionUtils;
import com.simplecity.amp_library.utils.PlaylistUtils;
import com.simplecity.amp_library.utils.ShuttleUtils;
import com.simplecityapps.recycler_adapter.model.ViewModel;

import java.util.ArrayList;
import java.util.List;

import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

class DetailPresenter extends Presenter<DetailView> {

    @NonNull
    private SongsProvider songsProvider;

    @Nullable
    private AlbumsProvider albumsProvider;

    DetailPresenter(@NonNull SongsProvider songsProvider, @Nullable AlbumsProvider albumsProvider) {
        this.songsProvider = songsProvider;
        this.albumsProvider = albumsProvider;
    }

    void loadData() {
        PermissionUtils.RequestStoragePermissions(() ->
                addSubcscription(songsProvider.getSongs()
                        .map(songs -> {
                            List<ViewModel> viewModels = new ArrayList<>();
                            if (albumsProvider != null) {
                                viewModels.addAll(albumsProvider.getAdaptableItems(songs));
                            }
                            viewModels.addAll(songsProvider.getAdaptableItems(songs));
                            return viewModels;
                        })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(adaptableItems -> {
                            DetailView detailView = getView();
                            if (detailView != null) {
                                if (adaptableItems.isEmpty()) {
                                    detailView.setEmpty(true);
                                } else {
                                    detailView.itemsLoaded(adaptableItems);
                                }
                            }
                        })));
    }

    void fabClicked() {
        MusicUtils.shuffleAll(songsProvider.getSongs(), message -> {
            DetailView detailView = getView();
            if (detailView != null) {
                detailView.showToast(message);
            }
        });
    }

    void playAll() {
        MusicUtils.playAll(songsProvider.getSongs(), message -> {
            DetailView detailView = getView();
            if (detailView != null) {
                detailView.showToast(message);
            }
        });
    }

    void addToQueue() {
        songsProvider.getSongs().observeOn(AndroidSchedulers.mainThread())
                .subscribe(songs -> MusicUtils.addToQueue(songs, message -> {
                    DetailView detailView = getView();
                    if (detailView != null) {
                        detailView.showToast(message);
                    }
                }));
    }

    void editTags(TaggerDialog taggerDialog) {
        DetailView detailView = getView();
        if (detailView != null) {
            detailView.showTaggerDialog(taggerDialog);
        }
    }

    void editArtwork(MaterialDialog artworkDialog) {
        DetailView detailView = getView();
        if (detailView != null) {
            detailView.showArtworkDialog(artworkDialog);
        }
    }

    void newPlaylist(Context context) {
        songsProvider.getSongs().observeOn(AndroidSchedulers.mainThread())
                .subscribe(songs -> PlaylistUtils.createPlaylistDialog(context, songs));
    }

    void playlistSelected(Context context, MenuItem item) {
        songsProvider.getSongs().observeOn(AndroidSchedulers.mainThread())
                .subscribe(songs -> {
                    Playlist playlist = (Playlist) item.getIntent().getSerializableExtra(ShuttleUtils.ARG_PLAYLIST);
                    PlaylistUtils.addToPlaylist(context, playlist, songs);
                });
    }
}