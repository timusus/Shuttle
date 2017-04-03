package com.simplecity.amp_library.search;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.bignerdranch.android.multiselector.MultiSelector;
import com.bumptech.glide.RequestManager;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.format.PrefixHighlighter;
import com.simplecity.amp_library.model.AdaptableItem;
import com.simplecity.amp_library.model.Album;
import com.simplecity.amp_library.model.AlbumArtist;
import com.simplecity.amp_library.model.Header;
import com.simplecity.amp_library.model.Playlist;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.tagger.TaggerDialog;
import com.simplecity.amp_library.ui.activities.MainActivity;
import com.simplecity.amp_library.ui.adapters.SearchAdapter;
import com.simplecity.amp_library.ui.modelviews.AlbumArtistView;
import com.simplecity.amp_library.ui.modelviews.AlbumView;
import com.simplecity.amp_library.ui.modelviews.SearchHeaderView;
import com.simplecity.amp_library.ui.modelviews.SongView;
import com.simplecity.amp_library.ui.modelviews.ViewType;
import com.simplecity.amp_library.ui.presenters.Presenter;
import com.simplecity.amp_library.utils.DataManager;
import com.simplecity.amp_library.utils.DialogUtils;
import com.simplecity.amp_library.utils.MusicUtils;
import com.simplecity.amp_library.utils.Operators;
import com.simplecity.amp_library.utils.PlaylistUtils;
import com.simplecity.amp_library.utils.SettingsManager;
import com.simplecity.amp_library.utils.ShuttleUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

class SearchPresenter extends Presenter<SearchView> implements
        SearchAdapter.SearchListener,
        Toolbar.OnMenuItemClickListener {

    private static final double SCORE_THRESHOLD = 0.80;

    private PrefixHighlighter prefixHighlighter;

    private RequestManager requestManager;

    private Subscription performSearchSubscription;
    private Subscription setItemsSubscription;

    private String query;

    SearchPresenter(PrefixHighlighter prefixHighlighter, RequestManager requestManager) {
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

    void queryChanged(@Nullable String query) {
        if (TextUtils.isEmpty(query)) {
            query = "";
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
                performSearchSubscription.unsubscribe();
            }

            boolean searchArtists = SettingsManager.getInstance().getSearchArtists();

            Observable<List<AdaptableItem>> albumArtistsObservable = searchArtists ? DataManager.getInstance().getAlbumArtistsRelay()
                    .first()
                    .lift(new AlbumArtistFilterOperator(query, requestManager, prefixHighlighter)) : Observable.just(Collections.emptyList());

            boolean searchAlbums = SettingsManager.getInstance().getSearchAlbums();

            Observable<List<AdaptableItem>> albumsObservable = searchAlbums ? DataManager.getInstance().getAlbumsRelay()
                    .first()
                    .lift(new AlbumFilterOperator(query, requestManager, prefixHighlighter)) : Observable.just(Collections.emptyList());

            Observable<List<AdaptableItem>> songsObservable = DataManager.getInstance().getSongsRelay()
                    .first()
                    .lift(new SongFilterOperator(query, requestManager, prefixHighlighter));

            performSearchSubscription = Observable.combineLatest(
                    albumArtistsObservable, albumsObservable, songsObservable,
                    (adaptableItems, adaptableItems2, adaptableItems3) -> {
                        List<AdaptableItem> list = new ArrayList<>();
                        list.addAll(adaptableItems);
                        list.addAll(adaptableItems2);
                        list.addAll(adaptableItems3);
                        return list;
                    })
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(adaptableItems -> {

                        //We've got a new set of items to adapt.. Cancel the in-flight subscription.
                        if (setItemsSubscription != null) {
                            setItemsSubscription.unsubscribe();
                        }

                        if (adaptableItems.isEmpty()) {
                            searchView.setEmpty(true);
                        } else {
                            setItemsSubscription = searchView.setItems(adaptableItems);
                        }
                    });

            addSubcscription(performSearchSubscription);
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
    public boolean onMenuItemClick(MenuItem item) {
        return false;
    }

    @Override
    public void onItemClick(AlbumArtist albumArtist) {

        SearchView view = getView();

        Intent intent = new Intent();
        intent.putExtra(MainActivity.ARG_MODEL, albumArtist);
        if (view != null) {
            view.finish(Activity.RESULT_OK, intent);
        }
    }

    @Override
    public void onItemClick(Album album) {

        SearchView view = getView();

        Intent intent = new Intent();
        intent.putExtra(MainActivity.ARG_MODEL, album);
        if (view != null) {
            view.finish(Activity.RESULT_OK, intent);
        }
    }

    @Override
    public void onItemClick(Song song, List<Song> allSongs) {

        SearchView view = getView();

        int index = allSongs.indexOf(song);

        MusicUtils.playAll(allSongs, index, false, () -> {
            if (view != null) {
                view.showEmptyPlaylistToast();
            }
        });

        if (view != null) {
            view.finish();
        }
    }

    @Override
    public void onOverflowClick(View v, AlbumArtist albumArtist) {
        PopupMenu menu = new PopupMenu(v.getContext(), v);

        menu.getMenu().add(0, MusicUtils.Defs.PLAY_SELECTION, 0, R.string.play_selection);

        SubMenu sub = menu.getMenu().addSubMenu(0, MusicUtils.Defs.ADD_TO_PLAYLIST, 1, R.string.add_to_playlist);
        PlaylistUtils.makePlaylistMenu(v.getContext(), sub, 0);

        menu.getMenu().add(0, MusicUtils.Defs.QUEUE, 2, R.string.add_to_queue);

        if (ShuttleUtils.isUpgraded()) {
            menu.getMenu().add(0, MusicUtils.Defs.TAGGER, 3, R.string.edit_tags);
        }

        menu.getMenu().add(0, MusicUtils.Defs.DELETE_ITEM, 6, R.string.delete_item);

        menu.setOnMenuItemClickListener(item -> {

                    SearchView view = getView();

                    Observable<List<Song>> songsObservable = albumArtist.getSongsObservable();

                    switch (item.getItemId()) {
                        case MusicUtils.Defs.PLAY_SELECTION:
                            songsObservable
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(songs -> MusicUtils.playAll(songs, () -> {
                                        if (view != null) {
                                            view.showEmptyPlaylistToast();
                                        }
                                    }));
                            return true;
                        case MusicUtils.Defs.PLAY_NEXT:
                            songsObservable
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(songs -> MusicUtils.playNext(v.getContext(), songs));
                            return true;
                        case MusicUtils.Defs.NEW_PLAYLIST:
                            songsObservable
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(songs -> PlaylistUtils.createPlaylistDialog(v.getContext(), songs));
                            return true;
                        case MusicUtils.Defs.PLAYLIST_SELECTED:
                            songsObservable
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(songs -> {
                                        Playlist playlist = (Playlist) item.getIntent().getSerializableExtra(ShuttleUtils.ARG_PLAYLIST);
                                        PlaylistUtils.addToPlaylist(v.getContext(), playlist, songs);
                                    });
                            return true;
                        case MusicUtils.Defs.QUEUE:
                            songsObservable
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(songs -> MusicUtils.addToQueue(v.getContext(), songs));
                            return true;
                        case MusicUtils.Defs.TAGGER:
                            if (view != null) {
                                view.showTaggerDialog(TaggerDialog.newInstance(albumArtist));
                            }
                            return true;
                        case MusicUtils.Defs.DELETE_ITEM:
                            if (view != null) {
                                view.showDeleteDialog(new DialogUtils.DeleteDialogBuilder()
                                        .context(v.getContext())
                                        .songsToDelete(songsObservable)
                                        .singleMessageId(R.string.delete_album_artist_desc)
                                        .multipleMessage(R.string.delete_album_artist_desc_multiple)
                                        .itemNames(Collections.singletonList((albumArtist.name)))
                                        .build());
                            }

                            return true;
                    }
                    return false;
                }
        );
        menu.show();
    }

    @Override
    public void onOverflowClick(View v, Album album) {

        PopupMenu menu = new PopupMenu(v.getContext(), v);

        menu.getMenu().add(0, MusicUtils.Defs.PLAY_SELECTION, 0, R.string.play_selection);

        SubMenu sub = menu.getMenu().addSubMenu(0, MusicUtils.Defs.ADD_TO_PLAYLIST, 1, R.string.add_to_playlist);
        PlaylistUtils.makePlaylistMenu(v.getContext(), sub, 0);

        menu.getMenu().add(0, MusicUtils.Defs.QUEUE, 2, R.string.add_to_queue);

        if (ShuttleUtils.isUpgraded()) {
            menu.getMenu().add(0, MusicUtils.Defs.TAGGER, 3, R.string.edit_tags);
        }

        menu.getMenu().add(0, MusicUtils.Defs.DELETE_ITEM, 6, R.string.delete_item);

        menu.setOnMenuItemClickListener(item -> {

                    SearchView view = getView();

                    Observable<List<Song>> songsObservable = (album.getSongsObservable());

                    switch (item.getItemId()) {
                        case MusicUtils.Defs.PLAY_SELECTION:
                            songsObservable
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(songs -> MusicUtils.playAll(songs, () -> {
                                        if (view != null) {
                                            view.showEmptyPlaylistToast();
                                        }
                                    }));
                            return true;
                        case MusicUtils.Defs.PLAY_NEXT:
                            songsObservable
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(songs -> MusicUtils.playNext(v.getContext(), songs));
                            return true;
                        case MusicUtils.Defs.NEW_PLAYLIST:
                            songsObservable
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(songs -> PlaylistUtils.createPlaylistDialog(v.getContext(), songs));
                            return true;
                        case MusicUtils.Defs.PLAYLIST_SELECTED:
                            songsObservable
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(songs -> {
                                        Playlist playlist = (Playlist) item.getIntent().getSerializableExtra(ShuttleUtils.ARG_PLAYLIST);
                                        PlaylistUtils.addToPlaylist(v.getContext(), playlist, songs);
                                    });
                            return true;
                        case MusicUtils.Defs.QUEUE:
                            songsObservable
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(songs -> MusicUtils.addToQueue(v.getContext(), songs));
                            return true;
                        case MusicUtils.Defs.TAGGER:
                            if (view != null) {
                                view.showTaggerDialog(TaggerDialog.newInstance(album));
                            }
                            return true;
                        case MusicUtils.Defs.DELETE_ITEM:
                            if (view != null) {
                                view.showDeleteDialog(new DialogUtils.DeleteDialogBuilder()
                                        .context(v.getContext())
                                        .songsToDelete(songsObservable)
                                        .singleMessageId(R.string.delete_album_desc)
                                        .multipleMessage(R.string.delete_album_desc_multiple)
                                        .itemNames(Collections.singletonList((album.name)))
                                        .build());
                            }
                            return true;
                    }
                    return false;
                }
        );
        menu.show();
    }

    @Override
    public void onOverflowClick(View v, Song song) {

        PopupMenu menu = new PopupMenu(v.getContext(), v);
        menu.getMenu().add(0, MusicUtils.Defs.PLAY_NEXT, 0, R.string.play_next);
        menu.getMenu().add(0, MusicUtils.Defs.USE_AS_RINGTONE, 4, R.string.ringtone_menu);
        SubMenu sub = menu.getMenu().addSubMenu(0, MusicUtils.Defs.ADD_TO_PLAYLIST, 1, R.string.add_to_playlist);

        PlaylistUtils.makePlaylistMenu(v.getContext(), sub, 0);

        menu.getMenu().add(0, MusicUtils.Defs.QUEUE, 2, R.string.add_to_queue);

        if (ShuttleUtils.isUpgraded()) {
            menu.getMenu().add(0, MusicUtils.Defs.TAGGER, 3, R.string.edit_tags);
        }

        menu.getMenu().add(0, MusicUtils.Defs.DELETE_ITEM, 6, R.string.delete_item);

        menu.setOnMenuItemClickListener(item -> {

                    SearchView view = getView();

                    Observable<List<Song>> songsObservable = Observable.just(Collections.singletonList(song));

                    switch (item.getItemId()) {
                        case MusicUtils.Defs.PLAY_SELECTION:
                            songsObservable
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(songs -> MusicUtils.playAll(songs, () -> {
                                        if (view != null) {
                                            view.showEmptyPlaylistToast();
                                        }
                                    }));
                            return true;
                        case MusicUtils.Defs.PLAY_NEXT:
                            songsObservable
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(songs -> MusicUtils.playNext(v.getContext(), songs));
                            return true;
                        case MusicUtils.Defs.NEW_PLAYLIST:
                            songsObservable
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(songs -> PlaylistUtils.createPlaylistDialog(v.getContext(), songs));
                            return true;
                        case MusicUtils.Defs.PLAYLIST_SELECTED:
                            songsObservable
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(songs -> {
                                        Playlist playlist = (Playlist) item.getIntent().getSerializableExtra(ShuttleUtils.ARG_PLAYLIST);
                                        PlaylistUtils.addToPlaylist(v.getContext(), playlist, songs);
                                    });
                            return true;
                        case MusicUtils.Defs.USE_AS_RINGTONE:
                            // Set the system setting to make this the current
                            // ringtone
                            ShuttleUtils.setRingtone(v.getContext(), song);
                            return true;
                        case MusicUtils.Defs.QUEUE:
                            songsObservable
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(songs -> MusicUtils.addToQueue(v.getContext(), songs));
                            return true;
                        case MusicUtils.Defs.TAGGER:
                            if (view != null) {
                                view.showTaggerDialog(TaggerDialog.newInstance(song));
                            }
                            return true;
                        case MusicUtils.Defs.DELETE_ITEM:
                            if (view != null) {
                                view.showDeleteDialog(new DialogUtils.DeleteDialogBuilder()
                                        .context(v.getContext())
                                        .songsToDelete(songsObservable)
                                        .singleMessageId(R.string.delete_song_desc)
                                        .multipleMessage(R.string.delete_song_desc_multiple)
                                        .itemNames(Collections.singletonList((song.name)))
                                        .build());
                            }
                            return true;
                    }
                    return false;
                }
        );
        menu.show();
    }

    private static class SongFilterOperator implements Observable.Operator<List<AdaptableItem>, List<Song>> {

        private String filterString;

        private RequestManager requestManager;

        private PrefixHighlighter prefixHighlighter;

        private MultiSelector dummySelector = new MultiSelector();

        private SearchHeaderView songsHeader = new SearchHeaderView(new Header(ShuttleApplication.getInstance().getString(R.string.tracks_title)));

        SongFilterOperator(@NonNull String filterString, @NonNull RequestManager requestManager, @NonNull PrefixHighlighter prefixHighlighter) {
            this.filterString = filterString;
            this.requestManager = requestManager;
            this.prefixHighlighter = prefixHighlighter;
        }

        @Override
        public Subscriber<List<Song>> call(Subscriber<? super List<AdaptableItem>> subscriber) {
            return new Subscriber<List<Song>>() {
                @Override
                public void onNext(List<Song> songs) {

                    char[] prefix = filterString.toUpperCase().toCharArray();

                    List<Album> albums = Operators.songsToAlbums(songs);
                    Collections.sort(albums, Album::compareTo);

                    List<AlbumArtist> albumArtists = Operators.albumsToAlbumArtists(albums);
                    Collections.sort(albumArtists, AlbumArtist::compareTo);

                    if (isUnsubscribed()) return;

                    boolean fuzzy = SettingsManager.getInstance().getSearchFuzzy();

                    Stream<Song> songStream = Stream.of(songs)
                            .filter(song -> song.name != null);

                    Stream<Song> filteredStream = fuzzy ? applyJaroWinklerFilter(songStream) : applySongFilter(songStream);

                    List<AdaptableItem> adaptableItems = filteredStream.map(song -> {
                        SongView songView = new SongView(song, dummySelector, requestManager);
                        songView.setPrefix(prefixHighlighter, prefix);
                        return songView;
                    })
                            .collect(Collectors.toList());

                    if (!adaptableItems.isEmpty()) {
                        adaptableItems.add(0, songsHeader);
                    }

                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onNext(adaptableItems);
                    }
                }

                @Override
                public void onCompleted() {
                    subscriber.onCompleted();
                }

                @Override
                public void onError(Throwable e) {
                    subscriber.onError(e);
                }

            };
        }

        private Stream<Song> applyJaroWinklerFilter(Stream<Song> songStream) {
            return songStream.map(song -> new SearchUtils.JaroWinklerObject<>(song, filterString, song.name ))
                    .filter(jaroWinklerObject -> jaroWinklerObject.score > SCORE_THRESHOLD || TextUtils.isEmpty(filterString))
                    .sorted((a, b) -> a.object.compareTo(b.object))
                    .sorted((a, b) -> Double.compare(b.score, a.score))
                    .map(jaroWinklerObject -> jaroWinklerObject.object);
        }

        private Stream<Song> applySongFilter(Stream<Song> songStream) {
            return songStream.filter(song -> song.name.contains(filterString));
        }
    }

    private static class AlbumFilterOperator implements Observable.Operator<List<AdaptableItem>, List<Album>> {

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
        public Subscriber<? super List<Album>> call(Subscriber<? super List<AdaptableItem>> subscriber) {
            return new Subscriber<List<Album>>() {
                @Override
                public void onNext(List<Album> albums) {

                    char[] prefix = filterString.toUpperCase().toCharArray();

                    Collections.sort(albums, Album::compareTo);

                    if (isUnsubscribed()) return;

                    boolean fuzzy = SettingsManager.getInstance().getSearchFuzzy();

                    Stream<Album> albumStream = Stream.of(albums)
                            .filter(album -> album.name != null);

                    Stream<Album> filteredStream = fuzzy ? applyJaroWinklerAlbumFilter(albumStream) : applyAlbumFilter(albumStream);

                    List<AdaptableItem> adaptableItems = filteredStream.map(album -> {
                        AlbumView albumView = new AlbumView(album, ViewType.ALBUM_LIST, requestManager);
                        albumView.setPrefix(prefixHighlighter, prefix);
                        return albumView;
                    }).collect(Collectors.toList());

                    if (!adaptableItems.isEmpty()) {
                        adaptableItems.add(0, albumsHeader);
                    }

                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onNext(adaptableItems);
                    }
                }

                @Override
                public void onCompleted() {
                    subscriber.onCompleted();
                }

                @Override
                public void onError(Throwable e) {
                    subscriber.onError(e);
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
            return stream.filter(album -> album.name.contains(filterString));
        }
    }

    private static class AlbumArtistFilterOperator implements Observable.Operator<List<AdaptableItem>, List<AlbumArtist>> {

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
        public Subscriber<? super List<AlbumArtist>> call(Subscriber<? super List<AdaptableItem>> subscriber) {
            return new Subscriber<List<AlbumArtist>>() {
                @Override
                public void onNext(List<AlbumArtist> albumArtists) {

                    char[] prefix = filterString.toUpperCase().toCharArray();

                    Collections.sort(albumArtists, AlbumArtist::compareTo);

                    if (isUnsubscribed()) return;

                    boolean fuzzy = SettingsManager.getInstance().getSearchFuzzy();

                    Stream<AlbumArtist> albumArtistStream = Stream.of(albumArtists)
                            .filter(albumArtist -> albumArtist.name != null);

                    Stream<AlbumArtist> filteredStream = fuzzy ? applyJaroWinklerAlbumArtistFilter(albumArtistStream) : applyAlbumArtistFilter(albumArtistStream);

                    List<AdaptableItem> adaptableItems = filteredStream
                            .map(albumArtist -> {
                                AlbumArtistView albumArtistView = new AlbumArtistView(albumArtist, ViewType.ARTIST_LIST, requestManager);
                                albumArtistView.setPrefix(prefixHighlighter, prefix);
                                return (AdaptableItem) albumArtistView;
                            })
                            .collect(Collectors.toList());

                    if (!adaptableItems.isEmpty()) {
                        adaptableItems.add(0, artistsHeader);
                    }

                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onNext(adaptableItems);
                    }
                }

                @Override
                public void onCompleted() {
                    subscriber.onCompleted();
                }

                @Override
                public void onError(Throwable e) {
                    subscriber.onError(e);
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
            return stream.filter(albumArtist -> albumArtist.name.contains(filterString));
        }
    }
}