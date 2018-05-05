package com.simplecity.amp_library.search;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import com.annimon.stream.Stream;
import com.simplecity.amp_library.model.Album;
import com.simplecity.amp_library.model.AlbumArtist;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.playback.MediaManager;
import com.simplecity.amp_library.ui.modelviews.AlbumArtistView;
import com.simplecity.amp_library.ui.modelviews.AlbumView;
import com.simplecity.amp_library.ui.presenters.Presenter;
import com.simplecity.amp_library.utils.DataManager;
import com.simplecity.amp_library.utils.LogUtils;
import com.simplecity.amp_library.utils.SettingsManager;
import com.simplecity.amp_library.utils.StringUtils;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.SingleOperator;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import kotlin.Unit;

public class SearchPresenter extends Presenter<SearchView> {

    private static final String TAG = "SearchPresenter";

    private static final double SCORE_THRESHOLD = 0.80;

    private Disposable performSearchSubscription;

    private String query;
    private MediaManager mediaManager;

    @Inject
    public SearchPresenter(@NonNull MediaManager mediaManager) {
        this.mediaManager = mediaManager;
    }

    @Override
    public void bindView(@NonNull SearchView view) {
        super.bindView(view);

        view.setFilterFuzzyChecked(SettingsManager.getInstance().getSearchFuzzy());
        view.setFilterArtistsChecked(SettingsManager.getInstance().getSearchArtists());
        view.setFilterAlbumsChecked(SettingsManager.getInstance().getSearchAlbums());
    }

    @Override
    public void unbindView(@NonNull SearchView view) {
        super.unbindView(view);
    }

    void queryChanged(@Nullable String query) {

        if (TextUtils.isEmpty(query)) {
            query = "";
        }

        if (query.equals(this.query)) {
            return;
        }

        loadData(query);

        this.query = query;
    }

    private void loadData(@NonNull String query) {

        SearchView searchView = getView();

        if (searchView != null) {

            searchView.setLoading(true);

            //We've received a new refresh call. Unsubscribe the in-flight subscription if it exists.
            if (performSearchSubscription != null) {
                performSearchSubscription.dispose();
            }

            Single<List<AlbumArtist>> albumArtistsObservable = SettingsManager.getInstance().getSearchArtists() ? DataManager.getInstance().getAlbumArtistsRelay()
                    .first(Collections.emptyList())
                    .lift(new AlbumArtistFilterOperator(query)) : Single.just(Collections.emptyList());

            Single<List<Album>> albumsObservable = SettingsManager.getInstance().getSearchAlbums() ? DataManager.getInstance().getAlbumsRelay()
                    .first(Collections.emptyList())
                    .lift(new AlbumFilterOperator(query)) : Single.just(Collections.emptyList());

            Single<List<Song>> songsObservable = DataManager.getInstance().getSongsRelay()
                    .first(Collections.emptyList())
                    .lift(new SongFilterOperator(query));

            performSearchSubscription = Single.zip(albumArtistsObservable, albumsObservable, songsObservable, SearchResult::new)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            searchView::setData,
                            error -> LogUtils.logException(TAG, "Error refreshing adapter", error)
                    );

            addDisposable(performSearchSubscription);
        }
    }

    void setSearchFuzzy(boolean searchFuzzy) {
        SettingsManager.getInstance().setSearchFuzzy(searchFuzzy);
        loadData(query);
    }

    void setSearchArtists(boolean searchArtists) {
        SettingsManager.getInstance().setSearchArtists(searchArtists);
        loadData(query);
    }

    void setSearchAlbums(boolean searchAlbums) {
        SettingsManager.getInstance().setSearchAlbums(searchAlbums);
        loadData(query);
    }

    void onSongClick(List<Song> songs, Song song) {
        SearchView view = getView();

        mediaManager.playAll(songs, songs.indexOf(song), true, message -> {
            if (view != null) {
                view.showToast(message);
            }
            return Unit.INSTANCE;
        });
    }

    void onArtistClicked(AlbumArtistView albumArtistView, AlbumArtistView.ViewHolder viewholder) {
        com.simplecity.amp_library.search.SearchView view = getView();
        if (view != null) {
            view.goToArtist(albumArtistView.albumArtist, viewholder.imageOne);
        }
    }

    void onAlbumClick(AlbumView albumView, AlbumView.ViewHolder viewHolder) {
        com.simplecity.amp_library.search.SearchView view = getView();
        if (view != null) {
            view.goToAlbum(albumView.album, viewHolder.imageOne);
        }
    }

    private class SongFilterOperator implements SingleOperator<List<Song>, List<Song>> {

        String filterString;

        SongFilterOperator(@NonNull String filterString) {
            this.filterString = filterString;
        }

        @Override
        public SingleObserver<? super List<Song>> apply(SingleObserver<? super List<Song>> observer) {
            return new SingleObserver<List<Song>>() {
                @Override
                public void onSubscribe(Disposable d) {
                    observer.onSubscribe(d);
                }

                @Override
                public void onSuccess(List<Song> songs) {
                    Stream<Song> songStream = Stream.of(songs).filter(song -> song.name != null);
                    songs = (SettingsManager.getInstance().getSearchFuzzy() ? applyJaroWinklerFilter(songStream) : applySongFilter(songStream)).toList();
                    observer.onSuccess(songs);
                }

                @Override
                public void onError(Throwable e) {
                    observer.onError(e);
                }
            };
        }

        Stream<Song> applyJaroWinklerFilter(Stream<Song> songStream) {
            return songStream.map(song -> new SearchUtils.JaroWinklerObject<>(song, filterString, song.name))
                    .filter(jaroWinklerObject -> jaroWinklerObject.score > SCORE_THRESHOLD || TextUtils.isEmpty(filterString))
                    .sorted((a, b) -> a.object.compareTo(b.object))
                    .sorted((a, b) -> Double.compare(b.score, a.score))
                    .map(jaroWinklerObject -> jaroWinklerObject.object);
        }

        Stream<Song> applySongFilter(Stream<Song> songStream) {
            return songStream.filter(song -> StringUtils.containsIgnoreCase(song.name, filterString));
        }
    }

    private class AlbumFilterOperator implements SingleOperator<List<Album>, List<Album>> {

        String filterString;

        AlbumFilterOperator(@NonNull String filterString) {
            this.filterString = filterString;
        }

        @Override
        public SingleObserver<? super List<Album>> apply(SingleObserver<? super List<Album>> observer) {
            return new SingleObserver<List<Album>>() {
                @Override
                public void onSubscribe(Disposable d) {
                    observer.onSubscribe(d);
                }

                @Override
                public void onSuccess(List<Album> albums) {
                    Collections.sort(albums, Album::compareTo);
                    Stream<Album> albumStream = Stream.of(albums).filter(album -> album.name != null);
                    Stream<Album> filteredStream = SettingsManager.getInstance().getSearchFuzzy() ? applyJaroWinklerAlbumFilter(albumStream) : applyAlbumFilter(albumStream);
                    observer.onSuccess(filteredStream.toList());
                }

                @Override
                public void onError(Throwable e) {
                    observer.onError(e);
                }
            };
        }

        Stream<Album> applyJaroWinklerAlbumFilter(Stream<Album> stream) {
            return stream.map(album -> new SearchUtils.JaroWinklerObject<>(album, filterString, album.name))
                    .filter(jaroWinklerObject -> jaroWinklerObject.score > SCORE_THRESHOLD || TextUtils.isEmpty(filterString))
                    .sorted((a, b) -> a.object.compareTo(b.object))
                    .sorted((a, b) -> Double.compare(b.score, a.score))
                    .map(jaroWinklerObject -> jaroWinklerObject.object);
        }

        Stream<Album> applyAlbumFilter(Stream<Album> stream) {
            return stream.filter(album -> StringUtils.containsIgnoreCase(album.name, filterString));
        }
    }

    private class AlbumArtistFilterOperator implements SingleOperator<List<AlbumArtist>, List<AlbumArtist>> {

        String filterString;

        AlbumArtistFilterOperator(@NonNull String filterString) {
            this.filterString = filterString;
        }

        @Override
        public SingleObserver<? super List<AlbumArtist>> apply(SingleObserver<? super List<AlbumArtist>> observer) {
            return new SingleObserver<List<AlbumArtist>>() {
                @Override
                public void onSubscribe(Disposable d) {
                    observer.onSubscribe(d);
                }

                @Override
                public void onSuccess(List<AlbumArtist> albumArtists) {
                    Collections.sort(albumArtists, AlbumArtist::compareTo);
                    Stream<AlbumArtist> albumArtistStream = Stream.of(albumArtists).filter(albumArtist -> albumArtist.name != null);
                    Stream<AlbumArtist> filteredStream =
                            SettingsManager.getInstance().getSearchFuzzy() ? applyJaroWinklerAlbumArtistFilter(albumArtistStream) : applyAlbumArtistFilter(albumArtistStream);
                    observer.onSuccess(filteredStream.toList());
                }

                @Override
                public void onError(Throwable e) {
                    observer.onError(e);
                }
            };
        }

        Stream<AlbumArtist> applyJaroWinklerAlbumArtistFilter(Stream<AlbumArtist> stream) {
            return stream.map(albumArtist -> new SearchUtils.JaroWinklerObject<>(albumArtist, filterString, albumArtist.name))
                    .filter(jaroWinklerObject -> jaroWinklerObject.score > SCORE_THRESHOLD || TextUtils.isEmpty(filterString))
                    .sorted((a, b) -> a.object.compareTo(b.object))
                    .sorted((a, b) -> Double.compare(b.score, a.score))
                    .map(jaroWinklerObject -> jaroWinklerObject.object);
        }

        Stream<AlbumArtist> applyAlbumArtistFilter(Stream<AlbumArtist> stream) {
            return stream.filter(albumArtist -> StringUtils.containsIgnoreCase(albumArtist.name, filterString));
        }
    }
}
