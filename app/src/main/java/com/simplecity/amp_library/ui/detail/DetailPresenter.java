//package com.simplecity.amp_library.ui.detail;
//
//import android.content.Context;
//import android.support.annotation.NonNull;
//import android.support.annotation.Nullable;
//import android.support.v4.util.Pair;
//import android.view.MenuItem;
//
//import com.afollestad.materialdialogs.MaterialDialog;
//import com.annimon.stream.Optional;
//import com.simplecity.amp_library.model.Album;
//import com.simplecity.amp_library.model.Playlist;
//import com.simplecity.amp_library.model.Song;
//import com.simplecity.amp_library.rx.UnsafeAction;
//import com.simplecity.amp_library.tagger.TaggerDialog;
//import com.simplecity.amp_library.ui.detail.data.AlbumsProvider;
//import com.simplecity.amp_library.ui.detail.data.SongsProvider;
//import com.simplecity.amp_library.ui.presenters.Presenter;
//import com.simplecity.amp_library.utils.LogUtils;
//import com.simplecity.amp_library.utils.MusicUtils;
//import com.simplecity.amp_library.utils.PermissionUtils;
//import com.simplecity.amp_library.utils.PlaylistUtils;
//import com.simplecity.amp_library.utils.ShuttleUtils;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Random;
//import java.util.concurrent.TimeUnit;
//
//import javax.inject.Inject;
//
//import io.reactivex.Observable;
//import io.reactivex.android.schedulers.AndroidSchedulers;
//import io.reactivex.schedulers.Schedulers;
//
//public abstract class DetailPresenter extends Presenter<DetailView> {
//
//    private static final String TAG = "DetailPresenter";
//
//    @Inject
//    @NonNull
//    SongsProvider songsProvider;
//
//    @Inject
//    @NonNull
//    AlbumsProvider albumsProvider;
//
//    @Nullable
//    private
//    Album currentSlideShowAlbum;
//
//    private List<Album> albums = new ArrayList<>();
//    private List<Song> songs = new ArrayList<>();
//
//    DetailPresenter(@NonNull SongsProvider songsProvider, @NonNull AlbumsProvider albumsProvider) {
//        this.songsProvider = songsProvider;
//        this.albumsProvider = albumsProvider;
//    }
//
//    boolean canPlaySlideshow() {
//        return false;
//    }
//
//    @Override
//    public void bindView(@NonNull DetailView view) {
//        super.bindView(view);
//
//        if (canPlaySlideshow()) {
//            startSlideShow();
//        }
//    }
//
//    @Override
//    public void unbindView(@NonNull DetailView view) {
//        super.unbindView(view);
//    }
//
//    void loadData() {
//        PermissionUtils.RequestStoragePermissions(() ->
//                addDisposable(songsProvider.getSongs().zipWith(albumsProvider.provideAlbums(), (songs, albums) ->
//                        new Pair<>(albums, songs)).subscribeOn(Schedulers.io())
//                        .observeOn(AndroidSchedulers.mainThread())
//                        .subscribe(pair -> {
//
//                            this.albums = pair.first;
//                            this.songs = pair.second;
//
//                            DetailView detailView = getView();
//                            if (detailView != null) {
//                                detailView.setData(pair);
//                            }
//                        })));
//    }
//
//
//    void startSlideShow() {
//        addDisposable(
//                Observable.combineLatest(
//                        albumsProvider.provideAlbums().toObservable(),
//                        Observable.interval(8, TimeUnit.SECONDS)
//                                // Load an image straight away
//                                .startWith(0L)
//                                // If we have a 'current slideshowAlbum' then we're coming back from onResume. Don't load a new one immediately.
//                                .delay(currentSlideShowAlbum == null ? 0 : 8, TimeUnit.SECONDS),
//                        (albums, aLong) -> {
//                            if (albums.isEmpty()) {
//                                return Optional.ofNullable(currentSlideShowAlbum);
//                            } else {
//                                return Optional.of(albums.get(new Random().nextInt(albums.size())));
//                            }
//                        }
//                ).subscribeOn(Schedulers.io())
//                        .observeOn(AndroidSchedulers.mainThread())
//                        .subscribe(nextSlideShowAlbumOptional -> {
//                            if (nextSlideShowAlbumOptional.isPresent()) {
//                                Album nextSlideshowAlbum = nextSlideShowAlbumOptional.get();
//                                if (nextSlideshowAlbum != currentSlideShowAlbum) {
//                                    getView().fadeInSlideshowAlbum(nextSlideshowAlbum);
//                                    currentSlideShowAlbum = nextSlideshowAlbum;
//                                }
//                            }
//                        }, error -> LogUtils.logException(TAG, "startSlideShow threw error", error)));
//    }
//
//    void fabClicked() {
//        MusicUtils.shuffleAll(songsProvider.getSongs(), message -> {
//            DetailView detailView = getView();
//            if (detailView != null) {
//                detailView.showToast(message);
//            }
//        });
//    }
//
//    void playAll() {
//        MusicUtils.playAll(songsProvider.getSongs(), message -> {
//            DetailView detailView = getView();
//            if (detailView != null) {
//                detailView.showToast(message);
//            }
//        });
//    }
//
//    void playNext() {
//        MusicUtils.playNext(songsProvider.getSongs(), message -> {
//            DetailView detailView = getView();
//            if (detailView != null) {
//                detailView.showToast(message);
//            }
//        });
//    }
//
//    void addToQueue() {
//        MusicUtils.addToQueue(songs, message -> {
//            DetailView detailView = getView();
//            if (detailView != null) {
//                detailView.showToast(message);
//            }
//        });
//    }
//
//    void editTags(TaggerDialog taggerDialog, MaterialDialog upgradeDialog) {
//        DetailView detailView = getView();
//        if (detailView != null) {
//            if (!ShuttleUtils.isUpgraded()) {
//                detailView.showUpgradeDialog(upgradeDialog);
//            } else {
//                detailView.showTaggerDialog(taggerDialog);
//            }
//        }
//    }
//
//    void editArtwork(MaterialDialog artworkDialog) {
//        DetailView detailView = getView();
//        if (detailView != null) {
//            detailView.showArtworkDialog(artworkDialog);
//        }
//    }
//
//    void newPlaylist(Context context, UnsafeAction insertCallback) {
//        PlaylistUtils.createPlaylistDialog(context, songs, insertCallback);
//    }
//
//    void playlistSelected(Context context, MenuItem item, UnsafeAction insertCallback) {
//        Playlist playlist = (Playlist) item.getIntent().getSerializableExtra(PlaylistUtils.ARG_PLAYLIST);
//        PlaylistUtils.addToPlaylist(context, playlist, songs, insertCallback);
//    }
//
//    void infoClicked(MaterialDialog dialog) {
//        DetailView detailView = getView();
//        if (detailView != null) {
//            detailView.showInfoDialog(dialog);
//        }
//    }
//
//    void onSongClicked(Song song) {
//        addDisposable(songsProvider.getSongs()
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(songs -> MusicUtils.playAll(songs, songs.indexOf(song), true, message -> {
//                    DetailView detailView = getView();
//                    if (detailView != null) {
//                        detailView.showToast(message);
//                    }
//                })));
//    }
//}
