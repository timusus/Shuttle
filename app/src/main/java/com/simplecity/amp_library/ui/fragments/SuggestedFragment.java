package com.simplecity.amp_library.ui.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

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
import com.simplecity.amp_library.utils.LogUtils;
import com.simplecity.amp_library.utils.MenuUtils;
import com.simplecity.amp_library.utils.MusicUtils;
import com.simplecity.amp_library.utils.Operators;
import com.simplecity.amp_library.utils.PermissionUtils;
import com.simplecityapps.recycler_adapter.adapter.ViewModelAdapter;
import com.simplecityapps.recycler_adapter.model.ViewModel;
import com.simplecityapps.recycler_adapter.recyclerview.RecyclerListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;

public class SuggestedFragment extends BaseFragment implements
        SuggestedHeaderView.ClickListener,
        AlbumView.ClickListener {

    public interface SuggestedClickListener {

        void onAlbumArtistClicked(AlbumArtist albumArtist, View transitionView);

        void onAlbumClicked(Album album, View transitionView);
    }

    public class SongClickListener implements SuggestedSongView.ClickListener {

        List<Song> songs;

        public SongClickListener(List<Song> songs) {
            this.songs = songs;
        }

        @Override
        public void onSongClick(Song song, SuggestedSongView.ViewHolder holder) {
            MusicUtils.playAll(songs, songs.indexOf(song), (String message) -> Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show());
        }

        @Override
        public boolean onSongLongClick(Song song) {
            return false;
        }

        @Override
        public void onSongOverflowClicked(View v, Song song) {
            PopupMenu popupMenu = new PopupMenu(getContext(), v);
            MenuUtils.setupSongMenu(getContext(), popupMenu, false);
            popupMenu.setOnMenuItemClickListener(MenuUtils.getSongMenuClickListener(getContext(), song, taggerDialog -> taggerDialog.show(getFragmentManager()), null));
            popupMenu.show();
        }
    }

    private static final String TAG = "SuggestedFragment";

    private static final String ARG_PAGE_TITLE = "page_title";

    private RecyclerView recyclerView;

    private ViewModelAdapter viewModelAdapter;

    private CompositeDisposable disposables = new CompositeDisposable();

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

        viewModelAdapter = new ViewModelAdapter();
        mostPlayedRecyclerView = new HorizontalRecyclerView();
        favoriteRecyclerView = new HorizontalRecyclerView();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        if (recyclerView == null) {

            recyclerView = (RecyclerView) inflater.inflate(R.layout.fragment_suggested, container, false);
            recyclerView.addItemDecoration(new SuggestedDividerDecoration(getResources()));
            recyclerView.setAdapter(viewModelAdapter);
            recyclerView.setRecyclerListener(new RecyclerListener());

            GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 6);
            gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    if (!viewModelAdapter.items.isEmpty() && position >= 0) {
                        ViewModel item = viewModelAdapter.items.get(position);
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

    Observable<List<ViewModel>> getMostPlayedViewModels() {
        return Playlist.mostPlayedPlaylist
                .getSongsObservable()
                .map(songs -> {
                    if (!songs.isEmpty()) {
                        List<ViewModel> viewModels = new ArrayList<>();

                        SuggestedHeader mostPlayedHeader = new SuggestedHeader(getString(R.string.mostplayed), getString(R.string.suggested_most_played_songs_subtitle), Playlist.mostPlayedPlaylist);
                        SuggestedHeaderView mostPlayedHeaderView = new SuggestedHeaderView(mostPlayedHeader);
                        mostPlayedHeaderView.setClickListener(this);
                        viewModels.add(mostPlayedHeaderView);

                        viewModels.add(mostPlayedRecyclerView);

                        Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(b.playCount, a.playCount));
                        SongClickListener songClickListener = new SongClickListener(songs);

                        mostPlayedRecyclerView.viewModelAdapter.setItems(Stream.of(songs)
                                .map(song -> {
                                    SuggestedSongView suggestedSongView = new SuggestedSongView(song, requestManager);
                                    suggestedSongView.setClickListener(songClickListener);
                                    return (ViewModel) suggestedSongView;
                                })
                                .limit(20)
                                .toList());

                        return viewModels;
                    } else {
                        return Collections.emptyList();
                    }
                });
    }

    Observable<List<ViewModel>> getRecentlyPlayedViewModels() {
        return Playlist.recentlyPlayedPlaylist
                .getSongsObservable()
                .flatMap(songs -> Observable.just(Operators.songsToAlbums(songs)))
                .flatMapSingle(albums -> Observable.fromIterable(albums)
                        .sorted((a, b) -> ComparisonUtils.compareLong(b.lastPlayed, a.lastPlayed))
                        .flatMapSingle(album ->
                                // We need to populate the song count
                                album.getSongsSingle()
                                        .map(songs -> {
                                            album.numSongs = songs.size();
                                            return album;
                                        })
                                        .filter(a -> a.numSongs > 0)
                                        .toSingle()
                        )
                        .take(6)
                        .toList()
                )
                .map(albums -> {
                    if (!albums.isEmpty()) {
                        List<ViewModel> viewModels = new ArrayList<>();

                        SuggestedHeader recentlyPlayedHeader = new SuggestedHeader(getString(R.string.suggested_recent_title), getString(R.string.suggested_recent_subtitle), Playlist.recentlyPlayedPlaylist);
                        SuggestedHeaderView recentlyPlayedHeaderView = new SuggestedHeaderView(recentlyPlayedHeader);
                        recentlyPlayedHeaderView.setClickListener(this);
                        viewModels.add(recentlyPlayedHeaderView);

                        viewModels.addAll(Stream.of(albums)
                                .map(album -> {
                                    AlbumView albumView = new AlbumView(album, ViewType.ALBUM_LIST_SMALL, requestManager);
                                    albumView.setClickListener(this);
                                    return albumView;
                                }).toList());

                        return viewModels;
                    } else {
                        return Collections.emptyList();
                    }
                });
    }

    Observable<List<ViewModel>> getFavoriteSongViewModels() {
        Observable<Playlist> favoritesPlaylist = Playlist.favoritesPlaylist().toObservable().cache();
        Observable<List<Song>> favoritesSongs = favoritesPlaylist
                .flatMap(Playlist::getSongsObservable)
                .flatMapSingle(songs -> Observable.fromIterable(songs)
                        .take(20)
                        .toList());

        return Observable.zip(favoritesPlaylist, favoritesSongs, (playlist, songs) -> {
            if (!songs.isEmpty()) {
                List<ViewModel> viewModels = new ArrayList<>();

                SuggestedHeader favoriteHeader = new SuggestedHeader(getString(R.string.fav_title), getString(R.string.suggested_favorite_subtitle), playlist);
                SuggestedHeaderView favoriteHeaderView = new SuggestedHeaderView(favoriteHeader);
                favoriteHeaderView.setClickListener(this);
                viewModels.add(favoriteHeaderView);

                viewModels.add(favoriteRecyclerView);

                SongClickListener songClickListener = new SongClickListener(songs);
                favoriteRecyclerView.viewModelAdapter.setItems(Stream.of(songs).map(song -> {
                    SuggestedSongView suggestedSongView = new SuggestedSongView(song, requestManager);
                    suggestedSongView.setClickListener(songClickListener);
                    return (ViewModel) suggestedSongView;
                }).toList());

                return viewModels;
            } else {
                return Collections.emptyList();
            }
        });
    }

    Observable<List<ViewModel>> getRecentlyAddedViewModels() {
        return Playlist.recentlyAddedPlaylist
                .getSongsObservable()
                .flatMap(songs -> Observable.just(Operators.songsToAlbums(songs)))
                .flatMapSingle(source -> Observable.fromIterable(source)
                        .sorted((a, b) -> ComparisonUtils.compareLong(b.songPlayCount, a.songPlayCount))
                        .take(20)
                        .toList())
                .map(albums -> {
                    if (!albums.isEmpty()) {
                        List<ViewModel> viewModels = new ArrayList<>();

                        SuggestedHeader recentlyAddedHeader = new SuggestedHeader(getString(R.string.recentlyadded), getString(R.string.suggested_recently_added_subtitle), Playlist.recentlyAddedPlaylist);
                        SuggestedHeaderView recentlyAddedHeaderView = new SuggestedHeaderView(recentlyAddedHeader);
                        recentlyAddedHeaderView.setClickListener(this);
                        viewModels.add(recentlyAddedHeaderView);

                        viewModels.addAll(Stream.of(albums).map(album -> {
                            AlbumView albumView = new AlbumView(album, ViewType.ALBUM_CARD, requestManager);
                            albumView.setClickListener(this);
                            return albumView;
                        }).toList());

                        return viewModels;
                    } else {
                        return Collections.emptyList();
                    }
                });
    }

    void refreshAdapterItems() {
        PermissionUtils.RequestStoragePermissions(() -> {
            if (getActivity() != null && isAdded()) {
                disposables.add(
                        Observable.combineLatest(getMostPlayedViewModels(), getRecentlyPlayedViewModels(), getFavoriteSongViewModels(), getRecentlyAddedViewModels(),
                                (mostPlayedSongs1, recentlyPlayedAlbums1, favoriteSongs1, recentlyAddedAlbums1) -> {
                                    List<ViewModel> items = new ArrayList<>();
                                    items.addAll(mostPlayedSongs1);
                                    items.addAll(recentlyPlayedAlbums1);
                                    items.addAll(favoriteSongs1);
                                    items.addAll(recentlyAddedAlbums1);
                                    return items;
                                })
                                .switchIfEmpty(Observable.just(Collections.emptyList()))
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(adaptableItems -> {
                                    if (adaptableItems.isEmpty()) {
                                        viewModelAdapter.setItems(Collections.singletonList((new EmptyView(R.string.empty_suggested))));
                                    } else {
                                        viewModelAdapter.setItems(adaptableItems);
                                    }
                                }, error -> LogUtils.logException(TAG, "Error setting items", error)));
            }
        });
    }

    @Override
    public void onPause() {

        if (disposables != null) {
            disposables.clear();
        }

        super.onPause();
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
        PopupMenu menu = new PopupMenu(getContext(), v);
        menu.inflate(R.menu.menu_album);
        menu.setOnMenuItemClickListener(MenuUtils.getAlbumMenuClickListener(getContext(), album, taggerDialog -> taggerDialog.show(getFragmentManager())));
        menu.show();
    }

    @Override
    public void onSuggestedHeaderClick(SuggestedHeader suggestedHeader) {
        getNavigationController().pushViewController(PlaylistDetailFragment.newInstance(suggestedHeader.playlist), "PlaylistFragment");
    }

    @Override
    protected String screenName() {
        return TAG;
    }
}
