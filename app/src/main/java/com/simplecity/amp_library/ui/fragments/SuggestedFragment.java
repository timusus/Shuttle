package com.simplecity.amp_library.ui.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.bumptech.glide.RequestManager;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.dagger.module.FragmentModule;
import com.simplecity.amp_library.model.Album;
import com.simplecity.amp_library.model.AlbumArtist;
import com.simplecity.amp_library.model.Playlist;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.model.SuggestedHeader;
import com.simplecity.amp_library.ui.adapters.ViewType;
import com.simplecity.amp_library.ui.detail.PlaylistDetailFragment;
import com.simplecity.amp_library.ui.modelviews.AlbumView;
import com.simplecity.amp_library.ui.modelviews.EmptyView;
import com.simplecity.amp_library.ui.modelviews.HorizontalRecyclerView;
import com.simplecity.amp_library.ui.modelviews.SuggestedHeaderView;
import com.simplecity.amp_library.ui.modelviews.SuggestedSongView;
import com.simplecity.amp_library.ui.views.SuggestedDividerDecoration;
import com.simplecity.amp_library.utils.ComparisonUtils;
import com.simplecity.amp_library.utils.MusicUtils;
import com.simplecity.amp_library.utils.Operators;
import com.simplecity.amp_library.utils.PermissionUtils;
import com.simplecityapps.recycler_adapter.adapter.ViewModelAdapter;
import com.simplecityapps.recycler_adapter.model.ViewModel;
import com.simplecityapps.recycler_adapter.recyclerview.RecyclerListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class SuggestedFragment extends BaseFragment implements
        MusicUtils.Defs, SuggestedSongView.ClickListener, SuggestedHeaderView.ClickListener, AlbumView.ClickListener {

    public interface SuggestedClickListener {

        void onAlbumArtistClicked(AlbumArtist albumArtist, View transitionView);

        void onAlbumClicked(Album album, View transitionView);
    }

    private static final String TAG = "SuggestedFragment";

    private static final String ARG_PAGE_TITLE = "page_title";

    private RecyclerView recyclerView;

    private ViewModelAdapter ViewModelAdapter;

    private CompositeSubscription subscription;

    @Inject
    RequestManager requestManager;

    private HorizontalRecyclerView favoriteRecyclerView;
    private HorizontalRecyclerView mostPlayedRecyclerView;

    @Nullable
    private SuggestedClickListener suggestedClickListener;

    public SuggestedFragment() {
    }

    public static SuggestedFragment newInstance(String pageTitle) {
        SuggestedFragment fragment = new SuggestedFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PAGE_TITLE, pageTitle);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (getParentFragment() instanceof SuggestedClickListener) {
            suggestedClickListener = (SuggestedClickListener) getParentFragment();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        suggestedClickListener = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ShuttleApplication.getInstance().getAppComponent()
                .plus(new FragmentModule(this))
                .inject(this);

        ViewModelAdapter = new ViewModelAdapter();

        mostPlayedRecyclerView = new HorizontalRecyclerView();

        favoriteRecyclerView = new HorizontalRecyclerView();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        if (recyclerView == null) {

            recyclerView = (RecyclerView) inflater.inflate(R.layout.fragment_suggested, container, false);
            recyclerView.addItemDecoration(new SuggestedDividerDecoration(getResources()));
            recyclerView.setAdapter(ViewModelAdapter);
            recyclerView.setRecyclerListener(new RecyclerListener());

            GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 6);
            gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    if (!ViewModelAdapter.items.isEmpty() && position >= 0) {
                        ViewModel item = ViewModelAdapter.items.get(position);
                        if (item instanceof HorizontalRecyclerView
                                || item instanceof SuggestedHeaderView
                                || (item instanceof AlbumView && item.getViewType() == ViewType.ALBUM_LIST)
                                || (item instanceof AlbumView && item.getViewType() == ViewType.ALBUM_LIST_SMALL)
                                || item instanceof EmptyView) {
                            return 6;
                        }
                        if (item instanceof AlbumView && item.getViewType() == ViewType.ALBUM_CARD_LARGE) {
                            return 3;
                        }
                    }

                    return 2;
                }
            });

            recyclerView.setLayoutManager(gridLayoutManager);
        }

        return recyclerView;
    }

    @Override
    public void onResume() {
        super.onResume();

        refreshAdapterItems();
    }

    void refreshAdapterItems() {

        PermissionUtils.RequestStoragePermissions(() -> {
            if (getActivity() != null && isAdded()) {

                subscription = new CompositeSubscription();

                Observable<Playlist> mostPlayedPlaylistObservable = Observable.fromCallable(Playlist::mostPlayedPlaylist)
                        .subscribeOn(Schedulers.io())
                        .cache();

                Observable<List<Song>> mostPlayedSongsObservable = mostPlayedPlaylistObservable
                        .filter(playlist -> playlist != null)
                        .flatMap(Playlist::getSongsObservable)
                        .cache();

                Observable<List<ViewModel>> mostPlayedItemsObservable = mostPlayedPlaylistObservable
                        .flatMap(playlist -> {

                            SuggestedHeader mostPlayedHeader = new SuggestedHeader(getString(R.string.mostplayed), getString(R.string.suggested_most_played_songs_subtitle), playlist);
                            SuggestedHeaderView mostPlayedHeaderView = new SuggestedHeaderView(mostPlayedHeader);
                            mostPlayedHeaderView.setClickListener(this);

                            return mostPlayedSongsObservable
                                    .map(songs -> {
                                        List<ViewModel> items = new ArrayList<>();
                                        if (!songs.isEmpty()) {
                                            items.add(mostPlayedHeaderView);
                                            items.add(mostPlayedRecyclerView);
                                        }
                                        return items;
                                    });
                        })
                        .switchIfEmpty(Observable.just(Collections.emptyList()));

                Observable<List<ViewModel>> recentlyPlayedAlbums = Observable.fromCallable(Playlist::recentlyPlayedPlaylist)
                        .subscribeOn(Schedulers.io())
                        .filter(playlist -> playlist != null)
                        .flatMap(playlist -> {

                            SuggestedHeader recentlyPlayedHeader = new SuggestedHeader(getString(R.string.suggested_recent_title), getString(R.string.suggested_recent_subtitle), playlist);
                            SuggestedHeaderView recentlyPlayedHeaderView = new SuggestedHeaderView(recentlyPlayedHeader);
                            recentlyPlayedHeaderView.setClickListener(this);

                            return playlist.getSongsObservable()
                                    .flatMap(songs -> Observable.just(Operators.songsToAlbums(songs)))
                                    .flatMap(Observable::from)
                                    .sorted((a, b) -> ComparisonUtils.compareLong(b.lastPlayed, a.lastPlayed))
                                    .limit(6)
                                    .flatMap(album ->
                                            //We need to populate the song count
                                            album.getSongsObservable()
                                                    .map(songs -> {
                                                        album.numSongs = songs.size();
                                                        return album;
                                                    }))
                                    .sorted((a, b) -> ComparisonUtils.compareLong(b.lastPlayed, a.lastPlayed))
                                    .filter(album -> album.numSongs > 0)
                                    .map(album -> {
                                        AlbumView albumView = new AlbumView(album, ViewType.ALBUM_LIST_SMALL, requestManager);
                                        albumView.setClickListener(this);
                                        return (ViewModel) albumView;
                                    })
                                    .toList()
                                    .map(adaptableItems -> {
                                        if (!adaptableItems.isEmpty()) {
                                            adaptableItems.add(0, recentlyPlayedHeaderView);
                                        }
                                        return adaptableItems;
                                    });
                        })
                        .switchIfEmpty(Observable.just(Collections.emptyList()));

                Observable<Playlist> favouritesPlaylistObservable = Observable.fromCallable(Playlist::favoritesPlaylist)
                        .subscribeOn(Schedulers.io())
                        .cache();

                Observable<List<Song>> favouritesSongsObservable = favouritesPlaylistObservable
                        .filter(playlist -> playlist != null)
                        .flatMap(Playlist::getSongsObservable)
                        .cache();

                Observable<List<ViewModel>> favoriteSongsItemsObservable = favouritesPlaylistObservable
                        .flatMap(playlist -> {

                            SuggestedHeader favoriteHeader = new SuggestedHeader(getString(R.string.fav_title), getString(R.string.suggested_favorite_subtitle), playlist);
                            SuggestedHeaderView favoriteHeaderView = new SuggestedHeaderView(favoriteHeader);
                            favoriteHeaderView.setClickListener(this);

                            return favouritesSongsObservable
                                    .map(songs -> {
                                        List<ViewModel> items = new ArrayList<>();
                                        if (!songs.isEmpty()) {
                                            items.add(favoriteHeaderView);
                                            items.add(favoriteRecyclerView);
                                        }
                                        return items;
                                    });
                        })
                        .switchIfEmpty(Observable.just(Collections.emptyList()));

                Observable<List<ViewModel>> recentlyAddedAlbums = Observable.fromCallable(Playlist::recentlyAddedPlaylist)
                        .subscribeOn(Schedulers.io())
                        .filter(playlist -> playlist != null)
                        .flatMap(playlist -> {

                            SuggestedHeader recentlyAddedHeader = new SuggestedHeader(getString(R.string.recentlyadded), getString(R.string.suggested_recently_added_subtitle), playlist);
                            SuggestedHeaderView recentlyAddedHeaderView = new SuggestedHeaderView(recentlyAddedHeader);
                            recentlyAddedHeaderView.setClickListener(this);

                            return playlist.getSongsObservable()
                                    .flatMap(songs -> Observable.just(Operators.songsToAlbums(songs)))
                                    .flatMap(Observable::from)
                                    .sorted((a, b) -> ComparisonUtils.compareLong(b.dateAdded, a.dateAdded))
                                    .limit(4)
                                    .flatMap(album ->
                                            //We need to populate the song count
                                            album.getSongsObservable()
                                                    .map(songs -> {
                                                        album.numSongs = songs.size();
                                                        return album;
                                                    }))
                                    .sorted((a, b) -> ComparisonUtils.compareLong(b.dateAdded, a.dateAdded))
                                    .filter(album -> album.numSongs > 0)
                                    .map(album -> {
                                        AlbumView albumView = new AlbumView(album, ViewType.ALBUM_LIST_SMALL, requestManager);
                                        albumView.setClickListener(this);
                                        return (ViewModel) albumView;
                                    })
                                    .toList()
                                    .map(adaptableItems -> {
                                        if (!adaptableItems.isEmpty()) {
                                            adaptableItems.add(0, recentlyAddedHeaderView);
                                        }
                                        return adaptableItems;
                                    });
                        })
                        .switchIfEmpty(Observable.just(Collections.emptyList()));

                subscription.add(
                        Observable.combineLatest(mostPlayedItemsObservable, recentlyPlayedAlbums, favoriteSongsItemsObservable, recentlyAddedAlbums,
                                (mostPlayedSongs1, recentlyPlayedAlbums1, favoriteSongs1, recentlyAddedAlbums1) -> {
                                    List<ViewModel> items = new ArrayList<>();
                                    items.addAll(mostPlayedSongs1);
                                    items.addAll(recentlyPlayedAlbums1);
                                    items.addAll(favoriteSongs1);
                                    items.addAll(recentlyAddedAlbums1);
                                    return items;
                                })
                                .debounce(250, TimeUnit.MILLISECONDS)
                                .switchIfEmpty(Observable.just(new ArrayList<>()))
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(adaptableItems -> {
                                    if (adaptableItems.isEmpty()) {
                                        ViewModelAdapter.setItems(Collections.singletonList((new EmptyView(R.string.empty_suggested))));
                                    } else {
                                        ViewModelAdapter.setItems(adaptableItems);
                                    }
                                }));

                subscription.add(mostPlayedSongsObservable
                        .map(songs -> {
                            Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(b.playCount, a.playCount));
                            return Stream.of(songs)
                                    .map(song -> {
                                        SuggestedSongView suggestedSongView = new SuggestedSongView(song, requestManager);
                                        suggestedSongView.setClickListener(this);
                                        return (ViewModel) suggestedSongView;
                                    })
                                    .limit(20)
                                    .collect(Collectors.toList());
                        })
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(adaptableItems -> mostPlayedRecyclerView.ViewModelAdapter.setItems(adaptableItems)));

                subscription.add(favouritesSongsObservable
                        .map(songs -> Stream.of(songs)
                                .map(song -> {
                                    SuggestedSongView suggestedSongView = new SuggestedSongView(song, requestManager);
                                    suggestedSongView.setClickListener(this);
                                    return (ViewModel) suggestedSongView;
                                })
                                .limit(20)
                                .collect(Collectors.toList()))
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(adaptableItems -> favoriteRecyclerView.ViewModelAdapter.setItems(adaptableItems)));
            }
        });
    }

    @Override
    public void onPause() {

        if (subscription != null) {
            subscription.unsubscribe();
        }

        super.onPause();
    }

    @Override
    public void onSongClick(Song song, SuggestedSongView.ViewHolder holder) {
        Toast.makeText(getContext(), song.name + " clicked", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onSongLongClick(Song song) {
        return false;
    }

    @Override
    public void onSongOverflowClicked(View v, Song song) {

    }

    @Override
    public void onAlbumClick(int position, AlbumView albumView, AlbumView.ViewHolder viewHolder) {
        if (suggestedClickListener != null) {
            suggestedClickListener.onAlbumClicked(albumView.album, viewHolder.imageOne);
        }
    }

    @Override
    public boolean onAlbumLongClick(int position, AlbumView albumView) {
        return false;
    }

    @Override
    public void onAlbumOverflowClicked(View v, Album album) {

    }

    @Override
    public void onSuggestedHeaderClick(SuggestedHeader suggestedHeader) {
        getNavigationController().pushViewController(PlaylistDetailFragment.newInstance(suggestedHeader.playlist), "PlaylistFrasgment");
    }

//    @Override
//    public void onItemClick(ViewModelAdapter adapter, View v, int position, final Object item) {
//        if (item instanceof Song) {
//
//            Observable<List<Song>> songsObservable;
//            if (adapter instanceof HorizontalRecyclerView.HorizontalAdapter) {
//                //The user tapped a song belonging to a HorizontalRecyclerView adapter. Play it amongst the
//                //other songs within that adapter.
//                songsObservable = Observable.fromCallable(() ->
//                        Stream.of(((HorizontalRecyclerView.HorizontalAdapter) adapter).items)
//                                .map(adaptableItem -> (Song) adaptableItem.getItem())
//                                .collect(Collectors.toList()));
//            } else {
//                //Otherwise, play the song amongst other songs from the same album
//                songsObservable = ((Song) item).getAlbum()
//                        .getSongsObservable()
//                        .map(songs -> {
//                            Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(a.track, b.track));
//                            Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(a.discNumber, b.discNumber));
//                            return songs;
//                        });
//            }
//
//            songsObservable.observeOn(AndroidSchedulers.mainThread())
//                    .subscribe(songs -> MusicUtils.playAll(songs, songs.indexOf((Song) item), (String message) -> {
//                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
//                    }));
//        } else {
//            Object model = item;
//            if (suggestedClickListener != null) {
//                if (item instanceof SuggestedHeader) {
//                    model = ((SuggestedHeader) item).playlist;
//                }
//                suggestedClickListener.onItemClicked((Serializable) model, v.findViewById(R.id.image));
//            }
//        }
//    }
//
//    @Override
//    public void onOverflowClick(View v, int position, Object item) {
//        if (item instanceof AlbumArtist) {
//            PopupMenu menu = new PopupMenu(SuggestedFragment.this.getActivity(), v);
//            MenuUtils.addAlbumArtistMenuOptions(getActivity(), menu);
//            MenuUtils.addClickHandler((AppCompatActivity) getActivity(), menu, (AlbumArtist) item);
//            menu.show();
//        } else if (item instanceof Album) {
//            PopupMenu menu = new PopupMenu(SuggestedFragment.this.getActivity(), v);
//            MenuUtils.addAlbumMenuOptions(getActivity(), menu);
//            MenuUtils.addClickHandler((AppCompatActivity) getActivity(), menu, (Album) item);
//            menu.show();
//        } else if (item instanceof Song) {
//            PopupMenu menu = new PopupMenu(SuggestedFragment.this.getActivity(), v);
//            MenuUtils.addSongMenuOptions(getActivity(), menu);
//            MenuUtils.addClickHandler((AppCompatActivity) getActivity(), menu, (Song) item, menuItem -> {
//                switch (menuItem.getItemId()) {
//                    case BLACKLIST: {
//                        BlacklistHelper.addToBlacklist(((Song) item));
//                        ViewModelAdapter.removeItem(position);
//                        return true;
//                    }
//                }
//                return false;
//            });
//            menu.show();
//        }
//    }

    @Override
    protected String screenName() {
        return TAG;
    }
}
