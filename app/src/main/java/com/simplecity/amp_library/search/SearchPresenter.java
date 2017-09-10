package com.simplecity.amp_library.search;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.PopupMenu;
import android.text.TextUtils;
import android.view.View;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.bumptech.glide.RequestManager;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.format.PrefixHighlighter;
import com.simplecity.amp_library.model.Album;
import com.simplecity.amp_library.model.AlbumArtist;
import com.simplecity.amp_library.model.Header;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.ui.adapters.ViewType;
import com.simplecity.amp_library.ui.modelviews.AlbumArtistView;
import com.simplecity.amp_library.ui.modelviews.AlbumView;
import com.simplecity.amp_library.ui.modelviews.SearchHeaderView;
import com.simplecity.amp_library.ui.modelviews.SongView;
import com.simplecity.amp_library.ui.presenters.Presenter;
import com.simplecity.amp_library.utils.ContextualToolbarHelper;
import com.simplecity.amp_library.utils.DataManager;
import com.simplecity.amp_library.utils.LogUtils;
import com.simplecity.amp_library.utils.MenuUtils;
import com.simplecity.amp_library.utils.MusicUtils;
import com.simplecity.amp_library.utils.Operators;
import com.simplecity.amp_library.utils.SettingsManager;
import com.simplecity.amp_library.utils.ShuttleUtils;
import com.simplecity.amp_library.utils.StringUtils;
import com.simplecityapps.recycler_adapter.model.ViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.SingleOperator;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

public class SearchPresenter extends Presenter<SearchView> implements
        AlbumView.ClickListener,
        AlbumArtistView.ClickListener {

    private static final String TAG = "SearchPresenter";

    private static final double SCORE_THRESHOLD = 0.80;

    private PrefixHighlighter prefixHighlighter;

    private RequestManager requestManager;

    private ContextualToolbarHelper<Single<List<Song>>> contextualToolbarHelper;

    private Disposable performSearchSubscription;
    private Disposable setItemsSubscription;

    private String query;

    @Inject
    public SearchPresenter(PrefixHighlighter prefixHighlighter, RequestManager requestManager) {
        this.prefixHighlighter = prefixHighlighter;
        this.requestManager = requestManager;
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

    public void setContextualToolbarHelper(ContextualToolbarHelper<Single<List<Song>>> contextualToolbarHelper) {
        this.contextualToolbarHelper = contextualToolbarHelper;
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

            boolean searchArtists = SettingsManager.getInstance().getSearchArtists();

            Single<List<ViewModel>> albumArtistsObservable = searchArtists ? DataManager.getInstance().getAlbumArtistsRelay()
                    .first(Collections.emptyList())
                    .lift(new AlbumArtistFilterOperator(query, requestManager, prefixHighlighter)) : Single.just(Collections.emptyList());

            boolean searchAlbums = SettingsManager.getInstance().getSearchAlbums();

            Single<List<ViewModel>> albumsObservable = searchAlbums ? DataManager.getInstance().getAlbumsRelay()
                    .first(Collections.emptyList())
                    .lift(new AlbumFilterOperator(query, requestManager, prefixHighlighter)) : Single.just(Collections.emptyList());

            Single<List<ViewModel>> songsObservable = DataManager.getInstance().getSongsRelay()
                    .first(Collections.emptyList())
                    .lift(new SongFilterOperator(query, requestManager, prefixHighlighter));

            performSearchSubscription = Single.zip(
                    albumArtistsObservable, albumsObservable, songsObservable,
                    (adaptableItems, adaptableItems2, adaptableItems3) -> {
                        List<ViewModel> list = new ArrayList<>();
                        list.addAll(adaptableItems);
                        list.addAll(adaptableItems2);
                        list.addAll(adaptableItems3);
                        return list;
                    })
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(adaptableItems -> {

                        //We've got a new set of items to adapt.. Cancel the in-flight subscription.
                        if (setItemsSubscription != null) {
                            setItemsSubscription.dispose();
                        }

                        if (adaptableItems.isEmpty()) {
                            searchView.setEmpty(true);
                        } else {
                            setItemsSubscription = searchView.setItems(adaptableItems);
                        }
                    }, error -> LogUtils.logException(TAG, "Error refreshing adapter", error));

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

    @Override
    public void onAlbumArtistClick(int position, AlbumArtistView albumArtistView, AlbumArtistView.ViewHolder viewholder) {
        if (!contextualToolbarHelper.handleClick(position, albumArtistView, albumArtistView.albumArtist.getSongsSingle())) {
            SearchView view = getView();
            if (view != null) {
                view.goToArtist(albumArtistView.albumArtist, viewholder.imageOne);
            }
        }
    }

    @Override
    public boolean onAlbumArtistLongClick(int position, AlbumArtistView albumArtistView) {
        return contextualToolbarHelper.handleLongClick(position, albumArtistView, albumArtistView.albumArtist.getSongsSingle());
    }

    @Override
    public void onAlbumArtistOverflowClicked(View v, AlbumArtist albumArtist) {
        PopupMenu menu = new PopupMenu(v.getContext(), v);
        menu.inflate(R.menu.menu_artist);
        menu.setOnMenuItemClickListener(MenuUtils.getAlbumArtistClickListener(
                v.getContext(),
                albumArtist,
                taggerDialog -> {
                    SearchView searchView = getView();
                    if (searchView != null) {
                        searchView.showTaggerDialog(taggerDialog);
                    }
                },
                () -> {
                    SearchView searchView = getView();
                    if (searchView != null) {
                        searchView.showUpgradeDialog();
                    }
                }));
        menu.show();
    }

    @Override
    public void onAlbumClick(int position, AlbumView albumView, AlbumView.ViewHolder viewHolder) {
        if (!contextualToolbarHelper.handleClick(position, albumView, albumView.album.getSongsSingle())) {
            SearchView view = getView();
            if (view != null) {
                view.goToAlbum(albumView.album, viewHolder.imageOne);
            }
        }
    }

    @Override
    public boolean onAlbumLongClick(int position, AlbumView albumView) {
        return contextualToolbarHelper.handleLongClick(position, albumView, albumView.album.getSongsSingle());
    }

    @Override
    public void onAlbumOverflowClicked(View v, Album album) {
        PopupMenu menu = new PopupMenu(v.getContext(), v);
        MenuUtils.setupAlbumMenu(menu);
        menu.setOnMenuItemClickListener(MenuUtils.getAlbumMenuClickListener(
                v.getContext(),
                album,
                taggerDialog -> {
                    SearchView searchView = getView();
                    if (searchView != null) {
                        searchView.showTaggerDialog(taggerDialog);
                    }
                },
                () -> {
                    SearchView searchView = getView();
                    if (searchView != null) {
                        searchView.showUpgradeDialog();
                    }
                }
        ));
        menu.show();
    }

    private class SongFilterOperator implements SingleOperator<List<ViewModel>, List<Song>> {

        private String filterString;

        private RequestManager requestManager;

        private PrefixHighlighter prefixHighlighter;

        private SearchHeaderView songsHeader = new SearchHeaderView(new Header(ShuttleApplication.getInstance().getString(R.string.tracks_title)));

        SongFilterOperator(@NonNull String filterString, @NonNull RequestManager requestManager, @NonNull PrefixHighlighter prefixHighlighter) {
            this.filterString = filterString;
            this.requestManager = requestManager;
            this.prefixHighlighter = prefixHighlighter;
        }

        @Override
        public SingleObserver<? super List<Song>> apply(SingleObserver<? super List<ViewModel>> observer) throws Exception {
            return new SingleObserver<List<Song>>() {
                @Override
                public void onSubscribe(Disposable d) {
                    observer.onSubscribe(d);
                }

                @Override
                public void onSuccess(List<Song> songs) {
                    char[] prefix = filterString.toUpperCase().toCharArray();

                    List<Album> albums = Operators.songsToAlbums(songs);
                    Collections.sort(albums, Album::compareTo);

                    List<AlbumArtist> albumArtists = Operators.albumsToAlbumArtists(albums);
                    Collections.sort(albumArtists, AlbumArtist::compareTo);

                    boolean fuzzy = SettingsManager.getInstance().getSearchFuzzy();

                    Stream<Song> songStream = Stream.of(songs)
                            .filter(song -> song.name != null);

                    songs = (fuzzy ? applyJaroWinklerFilter(songStream) : applySongFilter(songStream))
                            .toList();

                    SongViewClickListener songViewClickListener = new SongViewClickListener(songs);

                    List<ViewModel> viewModels = Stream.of(songs).map(song -> {
                        SongView songView = new SongView(song, requestManager);
                        songView.setClickListener(songViewClickListener);
                        songView.setPrefix(prefixHighlighter, prefix);
                        return songView;
                    }).collect(Collectors.toList());

                    if (!viewModels.isEmpty()) {
                        viewModels.add(0, songsHeader);
                    }

                    observer.onSuccess(viewModels);
                }

                @Override
                public void onError(Throwable e) {
                    observer.onError(e);
                }
            };
        }

        private Stream<Song> applyJaroWinklerFilter(Stream<Song> songStream) {
            return songStream.map(song -> new SearchUtils.JaroWinklerObject<>(song, filterString, song.name))
                    .filter(jaroWinklerObject -> jaroWinklerObject.score > SCORE_THRESHOLD || TextUtils.isEmpty(filterString))
                    .sorted((a, b) -> a.object.compareTo(b.object))
                    .sorted((a, b) -> Double.compare(b.score, a.score))
                    .map(jaroWinklerObject -> jaroWinklerObject.object);
        }

        private Stream<Song> applySongFilter(Stream<Song> songStream) {
            return songStream.filter(song -> StringUtils.containsIgnoreCase(song.name, filterString));
        }
    }

    private class AlbumFilterOperator implements SingleOperator<List<ViewModel>, List<Album>> {

        private String filterString;

        private RequestManager requestManager;

        private PrefixHighlighter prefixHighlighter;

        private SearchHeaderView albumsHeader = new SearchHeaderView(new Header(ShuttleApplication.getInstance().getString(R.string.albums_title)));

        AlbumFilterOperator(@NonNull String filterString, @NonNull RequestManager requestManager, @NonNull PrefixHighlighter prefixHighlighter) {
            this.filterString = filterString;
            this.requestManager = requestManager;
            this.prefixHighlighter = prefixHighlighter;
        }

        @Override
        public SingleObserver<? super List<Album>> apply(SingleObserver<? super List<ViewModel>> observer) throws Exception {
            return new SingleObserver<List<Album>>() {
                @Override
                public void onSubscribe(Disposable d) {
                    observer.onSubscribe(d);
                }

                @Override
                public void onSuccess(List<Album> albums) {
                    char[] prefix = filterString.toUpperCase().toCharArray();

                    Collections.sort(albums, Album::compareTo);

                    boolean fuzzy = SettingsManager.getInstance().getSearchFuzzy();

                    Stream<Album> albumStream = Stream.of(albums)
                            .filter(album -> album.name != null);

                    Stream<Album> filteredStream = fuzzy ? applyJaroWinklerAlbumFilter(albumStream) : applyAlbumFilter(albumStream);

                    List<ViewModel> viewModels = filteredStream.map(album -> {
                        AlbumView albumView = new AlbumView(album, ViewType.ALBUM_LIST, requestManager);
                        albumView.setClickListener(SearchPresenter.this);
                        albumView.setPrefix(prefixHighlighter, prefix);
                        return albumView;
                    }).collect(Collectors.toList());

                    if (!viewModels.isEmpty()) {
                        viewModels.add(0, albumsHeader);
                    }

                    observer.onSuccess(viewModels);
                }

                @Override
                public void onError(Throwable e) {
                    observer.onError(e);
                }
            };
        }

        private Stream<Album> applyJaroWinklerAlbumFilter(Stream<Album> stream) {
            return stream.map(album -> new SearchUtils.JaroWinklerObject<>(album, filterString, album.name))
                    .filter(jaroWinklerObject -> jaroWinklerObject.score > SCORE_THRESHOLD || TextUtils.isEmpty(filterString))
                    .sorted((a, b) -> a.object.compareTo(b.object))
                    .sorted((a, b) -> Double.compare(b.score, a.score))
                    .map(jaroWinklerObject -> jaroWinklerObject.object);
        }

        private Stream<Album> applyAlbumFilter(Stream<Album> stream) {
            return stream.filter(album -> StringUtils.containsIgnoreCase(album.name, filterString));
        }
    }

    private class AlbumArtistFilterOperator implements SingleOperator<List<ViewModel>, List<AlbumArtist>> {

        private String filterString;

        private RequestManager requestManager;

        private PrefixHighlighter prefixHighlighter;

        private SearchHeaderView artistsHeader = new SearchHeaderView(new Header(ShuttleApplication.getInstance().getString(R.string.artists_title)));

        AlbumArtistFilterOperator(@NonNull String filterString, @NonNull RequestManager requestManager, @NonNull PrefixHighlighter prefixHighlighter) {
            this.filterString = filterString;
            this.requestManager = requestManager;
            this.prefixHighlighter = prefixHighlighter;
        }

        @Override
        public SingleObserver<? super List<AlbumArtist>> apply(SingleObserver<? super List<ViewModel>> observer) throws Exception {
            return new SingleObserver<List<AlbumArtist>>() {
                @Override
                public void onSubscribe(Disposable d) {
                    observer.onSubscribe(d);
                }

                @Override
                public void onSuccess(List<AlbumArtist> albumArtists) {
                    char[] prefix = filterString.toUpperCase().toCharArray();

                    Collections.sort(albumArtists, AlbumArtist::compareTo);

                    boolean fuzzy = SettingsManager.getInstance().getSearchFuzzy();

                    Stream<AlbumArtist> albumArtistStream = Stream.of(albumArtists)
                            .filter(albumArtist -> albumArtist.name != null);

                    Stream<AlbumArtist> filteredStream = fuzzy ? applyJaroWinklerAlbumArtistFilter(albumArtistStream) : applyAlbumArtistFilter(albumArtistStream);

                    List<ViewModel> viewModels = filteredStream
                            .map(albumArtist -> {
                                AlbumArtistView albumArtistView = new AlbumArtistView(albumArtist, ViewType.ARTIST_LIST, requestManager);
                                albumArtistView.setClickListener(SearchPresenter.this);
                                albumArtistView.setPrefix(prefixHighlighter, prefix);
                                return (ViewModel) albumArtistView;
                            })
                            .toList();

                    if (!viewModels.isEmpty()) {
                        viewModels.add(0, artistsHeader);
                    }

                    observer.onSuccess(viewModels);
                }

                @Override
                public void onError(Throwable e) {
                    observer.onError(e);
                }
            };
        }

        private Stream<AlbumArtist> applyJaroWinklerAlbumArtistFilter(Stream<AlbumArtist> stream) {
            return stream.map(albumArtist -> new SearchUtils.JaroWinklerObject<>(albumArtist, filterString, albumArtist.name))
                    .filter(jaroWinklerObject -> jaroWinklerObject.score > SCORE_THRESHOLD || TextUtils.isEmpty(filterString))
                    .sorted((a, b) -> a.object.compareTo(b.object))
                    .sorted((a, b) -> Double.compare(b.score, a.score))
                    .map(jaroWinklerObject -> jaroWinklerObject.object);
        }

        private Stream<AlbumArtist> applyAlbumArtistFilter(Stream<AlbumArtist> stream) {
            return stream.filter(albumArtist -> StringUtils.containsIgnoreCase(albumArtist.name, filterString));
        }
    }

    private class SongViewClickListener implements SongView.ClickListener {

        List<Song> songs;

        public SongViewClickListener(List<Song> songs) {
            this.songs = songs;
        }

        @Override
        public void onSongClick(int position, SongView songView) {
            if (!contextualToolbarHelper.handleClick(position, songView, Single.just(Collections.singletonList(songView.song)))) {

                SearchView view = getView();

                int index = songs.indexOf(songView.song);

                MusicUtils.playAll(songs, index, (String message) -> {
                    if (view != null) {
                        view.showToast(message);
                    }
                });
            }
        }

        @Override
        public boolean onSongLongClick(int position, SongView songView) {
            return contextualToolbarHelper.handleLongClick(position, songView, Single.just(Collections.singletonList(songView.song)));
        }

        @Override
        public void onSongOverflowClick(int position, View v, Song song) {
            PopupMenu menu = new PopupMenu(v.getContext(), v);
            MenuUtils.setupSongMenu(menu, false);
            menu.setOnMenuItemClickListener(MenuUtils.getSongMenuClickListener(
                    v.getContext(),
                    song,
                    taggerDialog -> {
                        SearchView searchView = getView();
                        if (searchView != null) {
                            if (!ShuttleUtils.isUpgraded()) {
                                searchView.showUpgradeDialog();
                            } else {
                                searchView.showTaggerDialog(taggerDialog);
                            }
                        }
                    },
                    null, null));
            menu.show();
        }

        @Override
        public void onStartDrag(SongView.ViewHolder holder) {

        }
    }
}