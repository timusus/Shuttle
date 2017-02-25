package com.simplecity.amp_library.ui.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.model.AdaptableItem;
import com.simplecity.amp_library.model.Album;
import com.simplecity.amp_library.model.AlbumArtist;
import com.simplecity.amp_library.model.Playlist;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.model.SuggestedHeader;
import com.simplecity.amp_library.sql.databases.BlacklistHelper;
import com.simplecity.amp_library.ui.adapters.ItemAdapter;
import com.simplecity.amp_library.ui.adapters.SuggestedAdapter;
import com.simplecity.amp_library.ui.modelviews.AlbumView;
import com.simplecity.amp_library.ui.modelviews.EmptyView;
import com.simplecity.amp_library.ui.modelviews.HorizontalRecyclerView;
import com.simplecity.amp_library.ui.modelviews.SuggestedHeaderView;
import com.simplecity.amp_library.ui.modelviews.SuggestedSongView;
import com.simplecity.amp_library.ui.modelviews.ViewType;
import com.simplecity.amp_library.ui.views.SuggestedDividerDecoration;
import com.simplecity.amp_library.utils.ComparisonUtils;
import com.simplecity.amp_library.utils.MenuUtils;
import com.simplecity.amp_library.utils.MusicUtils;
import com.simplecity.amp_library.utils.Operators;
import com.simplecity.amp_library.utils.PermissionUtils;
import com.simplecity.amp_library.utils.ThemeUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class SuggestedFragment extends BaseFragment implements
        MusicUtils.Defs,
        SuggestedAdapter.SuggestedListener,
        RecyclerView.RecyclerListener,
        HorizontalRecyclerView.HorizontalAdapter.ItemListener {

    public interface SuggestedClickListener {

        void onItemClicked(Serializable item, View transitionView);
    }

    private static final String TAG = "SuggestedFragment";

    private static final String ARG_PAGE_TITLE = "page_title";

    private RecyclerView recyclerView;

    SuggestedAdapter suggestedAdapter;

    private BroadcastReceiver mReceiver;

    private SharedPreferences mPrefs;

    private SharedPreferences.OnSharedPreferenceChangeListener mSharedPreferenceChangeListener;

    private CompositeSubscription subscription;

    private RequestManager requestManager;

    private HorizontalRecyclerView favoriteRecyclerView;
    private HorizontalRecyclerView mostPlayedRecyclerView;

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

        suggestedClickListener = (SuggestedClickListener) getActivity();
    }

    @Override
    public void onDetach() {
        super.onDetach();

        suggestedClickListener = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this.getActivity());

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction() != null && intent.getAction().equals("restartLoader")) {
                    refreshAdapterItems();
                }
            }
        };

        mSharedPreferenceChangeListener = (sharedPreferences, key) -> {
            if (key.equals("pref_theme_highlight_color") || key.equals("pref_theme_accent_color") || key.equals("pref_theme_white_accent")) {
                themeUIComponents();
            } else if (key.equals("albumWhitelist")) {
                refreshAdapterItems();
            }
        };

        mPrefs.registerOnSharedPreferenceChangeListener(mSharedPreferenceChangeListener);

        suggestedAdapter = new SuggestedAdapter();
        suggestedAdapter.setListener(this);

        if (requestManager == null) {
            requestManager = Glide.with(this);
        }

        mostPlayedRecyclerView = new HorizontalRecyclerView();
        mostPlayedRecyclerView.setListener(this);

        favoriteRecyclerView = new HorizontalRecyclerView();
        favoriteRecyclerView.setListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        if (recyclerView == null) {

            recyclerView = (RecyclerView) inflater.inflate(R.layout.fragment_suggested, container, false);
            recyclerView.addItemDecoration(new SuggestedDividerDecoration(getResources()));
            recyclerView.setAdapter(suggestedAdapter);
            recyclerView.setRecyclerListener(this);

            GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 6);
            gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    if (!suggestedAdapter.items.isEmpty() && position >= 0) {
                        AdaptableItem item = suggestedAdapter.items.get(position);
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

            themeUIComponents();
        }

        return recyclerView;
    }

    private void themeUIComponents() {
        if (recyclerView != null) {
            ThemeUtils.themeRecyclerView(recyclerView);
            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    ThemeUtils.themeRecyclerView(recyclerView);
                    super.onScrollStateChanged(recyclerView, newState);
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter();
        filter.addAction("restartLoader");
        getActivity().registerReceiver(mReceiver, filter);

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
                        .flatMap(playlist -> playlist.getSongsObservable(getContext()))
                        .cache();

                Observable<List<AdaptableItem>> mostPlayedItemsObservable = mostPlayedPlaylistObservable
                        .flatMap(playlist -> {

                            SuggestedHeader mostPlayedHeader = new SuggestedHeader(getString(R.string.mostplayed), getString(R.string.suggested_most_played_songs_subtitle), playlist);
                            SuggestedHeaderView mostPlayedHeaderView = new SuggestedHeaderView(mostPlayedHeader);

                            return mostPlayedSongsObservable
                                    .map(songs -> {
                                        List<AdaptableItem> items = new ArrayList<>();
                                        if (!songs.isEmpty()) {
                                            items.add(mostPlayedHeaderView);
                                            items.add(mostPlayedRecyclerView);
                                        }
                                        return items;
                                    });
                        })
                        .switchIfEmpty(Observable.just(Collections.emptyList()));

                Observable<List<AdaptableItem>> recentlyPlayedAlbums = Observable.fromCallable(Playlist::recentlyPlayedPlaylist)
                        .subscribeOn(Schedulers.io())
                        .filter(playlist -> playlist != null)
                        .flatMap(playlist -> {

                            SuggestedHeader recentlyPlayedHeader = new SuggestedHeader(getString(R.string.suggested_recent_title), getString(R.string.suggested_recent_subtitle), playlist);
                            SuggestedHeaderView recentlyPlayedHeaderView = new SuggestedHeaderView(recentlyPlayedHeader);

                            return playlist.getSongsObservable(getContext())
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
                                    .map(album -> (AdaptableItem) new AlbumView(album, ViewType.ALBUM_LIST_SMALL, requestManager))
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
                        .flatMap(playlist -> playlist.getSongsObservable(getContext()))
                        .cache();

                Observable<List<AdaptableItem>> favoriteSongsItemsObservable = favouritesPlaylistObservable
                        .flatMap(playlist -> {

                            SuggestedHeader favoriteHeader = new SuggestedHeader(getString(R.string.fav_title), getString(R.string.suggested_favorite_subtitle), playlist);
                            SuggestedHeaderView favoriteHeaderView = new SuggestedHeaderView(favoriteHeader);

                            return favouritesSongsObservable
                                    .map(songs -> {
                                        List<AdaptableItem> items = new ArrayList<>();
                                        if (!songs.isEmpty()) {
                                            items.add(favoriteHeaderView);
                                            items.add(favoriteRecyclerView);
                                        }
                                        return items;
                                    });
                        })
                        .switchIfEmpty(Observable.just(Collections.emptyList()));

                Observable<List<AdaptableItem>> recentlyAddedAlbums = Observable.fromCallable(Playlist::recentlyAddedPlaylist)
                        .subscribeOn(Schedulers.io())
                        .filter(playlist -> playlist != null)
                        .flatMap(playlist -> {

                            SuggestedHeader recentlyAddedHeader = new SuggestedHeader(getString(R.string.recentlyadded), getString(R.string.suggested_recently_added_subtitle), playlist);
                            SuggestedHeaderView recentlyAddedHeaderView = new SuggestedHeaderView(recentlyAddedHeader);

                            return playlist.getSongsObservable(getContext())
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
                                    .map(album -> (AdaptableItem) new AlbumView(album, ViewType.ALBUM_LIST_SMALL, requestManager))
                                    .toList()
                                    .map(adaptableItems -> {
                                        if (!adaptableItems.isEmpty()) {
                                            adaptableItems.add(0, recentlyAddedHeaderView);
                                        }
                                        return adaptableItems;
                                    });
                        })
                        .switchIfEmpty(Observable.just(Collections.emptyList()));

                Observable.merge(mostPlayedItemsObservable, recentlyPlayedAlbums, favoriteSongsItemsObservable, recentlyAddedAlbums);

                subscription.add(
                        Observable.combineLatest(mostPlayedItemsObservable, recentlyPlayedAlbums, favoriteSongsItemsObservable, recentlyAddedAlbums,
                                (mostPlayedSongs1, recentlyPlayedAlbums1, favoriteSongs1, recentlyAddedAlbums1) -> {
                                    List<AdaptableItem> items = new ArrayList<>();
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
                                        suggestedAdapter.setEmpty(new EmptyView(R.string.empty_suggested));
                                    } else {
                                        suggestedAdapter.setItems(adaptableItems);
                                    }
                                }));

                subscription.add(mostPlayedSongsObservable
                        .map(songs -> {
                            Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(b.playCount, a.playCount));
                            return Stream.of(songs)
                                    .map(song -> (AdaptableItem) new SuggestedSongView(song, requestManager))
                                    .limit(20)
                                    .collect(Collectors.toList());
                        })
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(adaptableItems -> {
                            mostPlayedRecyclerView.itemAdapter.setItems(adaptableItems);
                        }));

                subscription.add(favouritesSongsObservable
                        .map(songs -> Stream.of(songs)
                                .map(song -> (AdaptableItem) new SuggestedSongView(song, requestManager))
                                .limit(20)
                                .collect(Collectors.toList()))
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(adaptableItems -> {
                            favoriteRecyclerView.itemAdapter.setItems(adaptableItems);
                        }));
            }
        });
    }

    @Override
    public void onPause() {
        if (mReceiver != null) {
            getActivity().unregisterReceiver(mReceiver);
        }

        if (subscription != null) {
            subscription.unsubscribe();
        }

        super.onPause();
    }

    @Override
    public void onDestroy() {
        mPrefs.unregisterOnSharedPreferenceChangeListener(mSharedPreferenceChangeListener);
        super.onDestroy();
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        if (holder.getAdapterPosition() != -1) {
            suggestedAdapter.items.get(holder.getAdapterPosition()).recycle(holder);
        }
    }

    @Override
    public void onItemClick(ItemAdapter adapter, View v, int position, final Object item) {
        if (item instanceof Song) {

            Observable<List<Song>> songsObservable;
            if (adapter instanceof HorizontalRecyclerView.HorizontalAdapter) {
                //The user tapped a song belonging to a HorizontalRecyclerView adapter. Play it amongst the
                //other songs within that adapter.
                songsObservable = Observable.fromCallable(() ->
                        Stream.of(((HorizontalRecyclerView.HorizontalAdapter) adapter).items)
                                .map(adaptableItem -> (Song) adaptableItem.getItem())
                                .collect(Collectors.toList()));
            } else {
                //Otherwise, play the song amongst other songs from the same album
                songsObservable = ((Song) item).getAlbum()
                        .getSongsObservable()
                        .map(songs -> {
                            Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(a.track, b.track));
                            Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(a.discNumber, b.discNumber));
                            return songs;
                        });
            }

            songsObservable.observeOn(AndroidSchedulers.mainThread())
                    .subscribe(songs -> MusicUtils.playAll(songs, songs.indexOf((Song) item), () -> {
                        final String message = getContext().getString(R.string.emptyplaylist);
                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                    }));
        } else {
            Object model = item;
            if (suggestedClickListener != null) {
                if (item instanceof SuggestedHeader) {
                    model = ((SuggestedHeader) item).playlist;
                }
                suggestedClickListener.onItemClicked((Serializable) model, v.findViewById(R.id.image));
            }
        }
    }

    @Override
    public void onOverflowClick(View v, int position, Object item) {
        if (item instanceof AlbumArtist) {
            PopupMenu menu = new PopupMenu(SuggestedFragment.this.getActivity(), v);
            MenuUtils.addAlbumArtistMenuOptions(getActivity(), menu);
            MenuUtils.addClickHandler((AppCompatActivity) getActivity(), menu, (AlbumArtist) item);
            menu.show();
        } else if (item instanceof Album) {
            PopupMenu menu = new PopupMenu(SuggestedFragment.this.getActivity(), v);
            MenuUtils.addAlbumMenuOptions(getActivity(), menu);
            MenuUtils.addClickHandler((AppCompatActivity) getActivity(), menu, (Album) item);
            menu.show();
        } else if (item instanceof Song) {
            PopupMenu menu = new PopupMenu(SuggestedFragment.this.getActivity(), v);
            MenuUtils.addSongMenuOptions(getActivity(), menu);
            MenuUtils.addClickHandler((AppCompatActivity) getActivity(), menu, (Song) item, menuItem -> {
                switch (menuItem.getItemId()) {
                    case BLACKLIST: {
                        BlacklistHelper.addToBlacklist(((Song) item));
                        suggestedAdapter.removeItem(position);
                        return true;
                    }
                }
                return false;
            });
            menu.show();
        }
    }

    @Override
    protected String screenName() {
        return TAG;
    }
}
