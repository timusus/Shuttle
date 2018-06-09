package com.simplecity.amp_library.search;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.annimon.stream.Stream;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.jakewharton.rxbinding2.support.v7.widget.RxSearchView;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.format.PrefixHighlighter;
import com.simplecity.amp_library.model.Album;
import com.simplecity.amp_library.model.AlbumArtist;
import com.simplecity.amp_library.model.Header;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.tagger.TaggerDialog;
import com.simplecity.amp_library.ui.adapters.ViewType;
import com.simplecity.amp_library.ui.detail.album.AlbumDetailFragment;
import com.simplecity.amp_library.ui.detail.artist.ArtistDetailFragment;
import com.simplecity.amp_library.ui.dialog.DeleteDialog;
import com.simplecity.amp_library.ui.dialog.UpgradeDialog;
import com.simplecity.amp_library.ui.fragments.BaseFragment;
import com.simplecity.amp_library.ui.modelviews.AlbumArtistView;
import com.simplecity.amp_library.ui.modelviews.AlbumView;
import com.simplecity.amp_library.ui.modelviews.EmptyView;
import com.simplecity.amp_library.ui.modelviews.LoadingView;
import com.simplecity.amp_library.ui.modelviews.SearchHeaderView;
import com.simplecity.amp_library.ui.modelviews.SelectableViewModel;
import com.simplecity.amp_library.ui.modelviews.SongView;
import com.simplecity.amp_library.ui.views.ContextualToolbar;
import com.simplecity.amp_library.ui.views.ContextualToolbarHost;
import com.simplecity.amp_library.utils.ContextualToolbarHelper;
import com.simplecity.amp_library.utils.Operators;
import com.simplecity.amp_library.utils.PlaylistUtils;
import com.simplecity.amp_library.utils.ResourceUtils;
import com.simplecity.amp_library.utils.menu.album.AlbumMenuCallbacksAdapter;
import com.simplecity.amp_library.utils.menu.album.AlbumMenuUtils;
import com.simplecity.amp_library.utils.menu.albumartist.AlbumArtistMenuCallbacksAdapter;
import com.simplecity.amp_library.utils.menu.albumartist.AlbumArtistMenuUtils;
import com.simplecity.amp_library.utils.menu.song.SongMenuCallbacksAdapter;
import com.simplecity.amp_library.utils.menu.song.SongMenuUtils;
import com.simplecityapps.recycler_adapter.adapter.CompletionListUpdateCallbackAdapter;
import com.simplecityapps.recycler_adapter.adapter.ViewModelAdapter;
import com.simplecityapps.recycler_adapter.model.ViewModel;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SearchFragment extends BaseFragment implements
        com.simplecity.amp_library.search.SearchView,
        ContextualToolbarHost {

    private static final String TAG = "SearchFragment";

    public static final String ARG_QUERY = "query";

    private String query = "";

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.contextualToolbar)
    ContextualToolbar contextualToolbar;

    @BindView(R.id.recyclerView)
    FastScrollRecyclerView recyclerView;

    private ViewModelAdapter adapter;

    private LoadingView loadingView;

    private CompositeDisposable disposables = new CompositeDisposable();

    private SearchPresenter searchPresenter;

    private View rootView;

    private SearchView searchView;

    private ContextualToolbarHelper<Single<List<Song>>> contextualToolbarHelper;

    private EmptyView emptyView = new EmptyView(R.string.empty_search);
    private SearchHeaderView artistsHeader = new SearchHeaderView(new Header(ShuttleApplication.getInstance().getString(R.string.artists_title)));
    private SearchHeaderView albumsHeader = new SearchHeaderView(new Header(ShuttleApplication.getInstance().getString(R.string.albums_title)));
    private SearchHeaderView songsHeader = new SearchHeaderView(new Header(ShuttleApplication.getInstance().getString(R.string.tracks_title)));

    private PrefixHighlighter prefixHighlighter;

    private RequestManager requestManager;

    private AlbumArtistMenuCallbacksAdapter albumArtistMenuCallbacksAdapter = new AlbumArtistMenuCallbacksAdapter(this, disposables);
    private AlbumMenuCallbacksAdapter albumMenuCallbacksAdapter = new AlbumMenuCallbacksAdapter(this, disposables);
    private SongMenuCallbacksAdapter songMenuCallbacksAdapter = new SongMenuCallbacksAdapter(this, disposables);

    @Nullable
    private Disposable setDataDisposable;

    public static SearchFragment newInstance(String query) {
        Bundle args = new Bundle();
        args.putString(ARG_QUERY, query);
        SearchFragment fragment = new SearchFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @SuppressLint("InlinedApi")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefixHighlighter = new PrefixHighlighter();

        requestManager = Glide.with(this);

        searchPresenter = new SearchPresenter(mediaManager);

        query = getArguments().getString(ARG_QUERY, "");

        loadingView = new LoadingView();

        emptyView.setHeight(ResourceUtils.toPixels(96));

        adapter = new ViewModelAdapter();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_search, container, false);

        ButterKnife.bind(this, rootView);

        toolbar.inflateMenu(R.menu.menu_search);
        toolbar.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.search_fuzzy:
                    item.setChecked(!item.isChecked());
                    searchPresenter.setSearchFuzzy(item.isChecked());
                    break;
                case R.id.search_artist:
                    item.setChecked(!item.isChecked());
                    searchPresenter.setSearchArtists(item.isChecked());
                    break;
                case R.id.search_album:
                    item.setChecked(!item.isChecked());
                    searchPresenter.setSearchAlbums(item.isChecked());
                    break;
            }
            return false;
        });

        setupContextualToolbar();

        MenuItem searchItem = toolbar.getMenu().findItem(R.id.search);
        searchItem.expandActionView();
        searchView = (SearchView) searchItem.getActionView();

        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return false;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                InputMethodManager inputMethodManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(rootView.getWindowToken(), 0);
                searchView.getHandler().postDelayed(() -> getNavigationController().popViewController(), 150);
                return false;
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        searchPresenter.bindView(this);

        disposables.add(RxSearchView.queryTextChangeEvents(searchView)
                .skip(1)
                .debounce(200, TimeUnit.MILLISECONDS)
                .toFlowable(BackpressureStrategy.LATEST)
                .subscribe(searchViewQueryTextEvent -> {
                    query = searchViewQueryTextEvent.queryText().toString();
                    searchPresenter.queryChanged(query);
                }));

        searchPresenter.queryChanged(query);
    }

    @Override
    public void onPause() {
        disposables.clear();
        searchPresenter.unbindView(this);

        super.onPause();
    }

    @Override
    protected String screenName() {
        return TAG;
    }

    @Override
    public void setLoading(boolean loading) {
        adapter.setItems(Collections.singletonList(loadingView));
    }

    @Override
    public void setData(@NonNull SearchResult searchResult) {

        if (setDataDisposable != null) {
            setDataDisposable.dispose();
        }

        char[] prefix = query.toUpperCase().toCharArray();

        List<ViewModel> viewModels = new ArrayList<>();

        if (!searchResult.albumArtists.isEmpty()) {
            viewModels.add(artistsHeader);
            viewModels.addAll(Stream.of(searchResult.albumArtists)
                    .map(albumArtist -> {
                        AlbumArtistView albumArtistView = new AlbumArtistView(albumArtist, ViewType.ARTIST_LIST, requestManager);
                        albumArtistView.setClickListener(albumArtistClickListener);
                        albumArtistView.setPrefix(prefixHighlighter, prefix);
                        return (ViewModel) albumArtistView;
                    })
                    .toList());
        }

        if (!searchResult.albums.isEmpty()) {
            viewModels.add(albumsHeader);
            viewModels.addAll(Stream.of(searchResult.albums).map(album -> {
                AlbumView albumView = new AlbumView(album, ViewType.ALBUM_LIST, requestManager);
                albumView.setClickListener(albumViewClickListener);
                albumView.setPrefix(prefixHighlighter, prefix);
                return albumView;
            }).toList());
        }

        if (!searchResult.songs.isEmpty()) {
            viewModels.add(songsHeader);
            viewModels.addAll(Stream.of(searchResult.songs).map(song -> {
                SongView songView = new SongView(song, requestManager);
                songView.setClickListener(songViewClickListener);
                songView.setPrefix(prefixHighlighter, prefix);
                return songView;
            }).toList());
        }

        if (viewModels.isEmpty()) {
            viewModels.add(emptyView);
        }

        setDataDisposable = adapter.setItems(viewModels, new CompletionListUpdateCallbackAdapter() {
            @Override
            public void onComplete() {
                super.onComplete();

                recyclerView.scrollToPosition(0);
            }
        });
    }

    @Override
    public void setFilterFuzzyChecked(boolean checked) {
        toolbar.getMenu().findItem(R.id.search_fuzzy).setChecked(checked);
    }

    @Override
    public void setFilterArtistsChecked(boolean checked) {
        toolbar.getMenu().findItem(R.id.search_artist).setChecked(checked);
    }

    @Override
    public void setFilterAlbumsChecked(boolean checked) {
        toolbar.getMenu().findItem(R.id.search_album).setChecked(checked);
    }

    @Override
    public void showToast(String message) {
        Toast.makeText(getContext(), (ShuttleApplication.getInstance().getString(R.string.emptyplaylist)), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showTaggerDialog(@NonNull TaggerDialog taggerDialog) {
        taggerDialog.show(getChildFragmentManager());
    }

    @Override
    public void showDeleteDialog(@NonNull DeleteDialog deleteDialog) {
        deleteDialog.show(getChildFragmentManager());
    }

    @Override
    public void goToArtist(AlbumArtist albumArtist, View transitionView) {
        InputMethodManager inputMethodManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(rootView.getWindowToken(), 0);
        String transitionName = ViewCompat.getTransitionName(transitionView);
        searchView.getHandler().postDelayed(() -> pushDetailFragment(ArtistDetailFragment.newInstance(albumArtist, transitionName), transitionView), 50);
    }

    @Override
    public void goToAlbum(Album album, View transitionView) {
        InputMethodManager inputMethodManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(rootView.getWindowToken(), 0);
        String transitionName = ViewCompat.getTransitionName(transitionView);
        searchView.getHandler().postDelayed(() -> pushDetailFragment(AlbumDetailFragment.newInstance(album, transitionName), transitionView), 50);
    }

    @Override
    public void showUpgradeDialog() {
        UpgradeDialog.getUpgradeDialog(getActivity()).show();
    }

    void pushDetailFragment(Fragment fragment, @Nullable View transitionView) {

        List<Pair<View, String>> transitions = new ArrayList<>();

        if (transitionView != null) {
            String transitionName = ViewCompat.getTransitionName(transitionView);
            transitions.add(new Pair<>(transitionView, transitionName));

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                Transition moveTransition = TransitionInflater.from(getContext()).inflateTransition(R.transition.image_transition);
                fragment.setSharedElementEnterTransition(moveTransition);
                fragment.setSharedElementReturnTransition(moveTransition);
            }
        }

        getNavigationController().pushViewController(fragment, "DetailFragment", transitions);
    }

    @Override
    public ContextualToolbar getContextualToolbar() {
        return contextualToolbar;
    }

    private void setupContextualToolbar() {

        ContextualToolbar contextualToolbar = ContextualToolbar.findContextualToolbar(this);
        if (contextualToolbar != null) {

            contextualToolbar.getMenu().clear();
            contextualToolbar.inflateMenu(R.menu.context_menu_general);
            SubMenu sub = contextualToolbar.getMenu().findItem(R.id.addToPlaylist).getSubMenu();
            disposables.add(PlaylistUtils.createUpdatingPlaylistMenu(sub).subscribe());

            contextualToolbar.setOnMenuItemClickListener(SongMenuUtils.INSTANCE.getSongMenuClickListener(
                    Single.defer(() -> Operators.reduceSongSingles(contextualToolbarHelper.getItems())), songMenuCallbacksAdapter
            ));

            contextualToolbarHelper = new ContextualToolbarHelper<Single<List<Song>>>(contextualToolbar, new ContextualToolbarHelper.Callback() {

                @Override
                public void notifyItemChanged(SelectableViewModel viewModel) {
                    int index = adapter.items.indexOf(viewModel);
                    if (index >= 0) {
                        adapter.notifyItemChanged(index, 0);
                    }
                }

                @Override
                public void notifyDatasetChanged() {
                    adapter.notifyItemRangeChanged(0, adapter.items.size(), 0);
                }
            }) {
                @Override
                public void start() {
                    super.start();

                    toolbar.setVisibility(View.GONE);
                }

                @Override
                public void finish() {
                    if (toolbar != null) {
                        toolbar.setVisibility(View.VISIBLE);
                    }
                    super.finish();
                }
            };
        }
    }

    private SongView.ClickListener songViewClickListener = new SongView.ClickListener() {

        @Override
        public void onSongClick(int position, SongView songView) {
            if (!contextualToolbarHelper.handleClick(songView, Single.just(Collections.singletonList(songView.song)))) {
                searchPresenter.onSongClick(
                        Stream.of(adapter.items)
                                .filter(item -> item instanceof SongView)
                                .map(item -> ((SongView) item).song).toList(),
                        songView.song
                );
            }
        }

        @Override
        public boolean onSongLongClick(int position, SongView songView) {
            return contextualToolbarHelper.handleLongClick(songView, Single.just(Collections.singletonList(songView.song)));
        }

        @Override
        public void onSongOverflowClick(int position, View v, Song song) {
            PopupMenu menu = new PopupMenu(v.getContext(), v);
            SongMenuUtils.INSTANCE.setupSongMenu(menu, false);
            menu.setOnMenuItemClickListener(SongMenuUtils.INSTANCE.getSongMenuClickListener(song, songMenuCallbacksAdapter));
            menu.show();
        }

        @Override
        public void onStartDrag(SongView.ViewHolder holder) {

        }
    };

    private AlbumView.ClickListener albumViewClickListener = new AlbumView.ClickListener() {
        @Override
        public void onAlbumClick(int position, AlbumView albumView, AlbumView.ViewHolder viewHolder) {
            if (!contextualToolbarHelper.handleClick(albumView, albumView.album.getSongsSingle())) {
                searchPresenter.onAlbumClick(albumView, viewHolder);
            }
        }

        @Override
        public boolean onAlbumLongClick(int position, AlbumView albumView) {
            return contextualToolbarHelper.handleLongClick(albumView, albumView.album.getSongsSingle());
        }

        @Override
        public void onAlbumOverflowClicked(View v, Album album) {
            PopupMenu menu = new PopupMenu(v.getContext(), v);
            AlbumMenuUtils.INSTANCE.setupAlbumMenu(menu);
            menu.setOnMenuItemClickListener(AlbumMenuUtils.INSTANCE.getAlbumMenuClickListener(v.getContext(), mediaManager, album, albumMenuCallbacksAdapter));
            menu.show();
        }
    };

    private AlbumArtistView.ClickListener albumArtistClickListener = new AlbumArtistView.ClickListener() {
        @Override
        public void onAlbumArtistClick(int position, AlbumArtistView albumArtistView, AlbumArtistView.ViewHolder viewholder) {
            if (!contextualToolbarHelper.handleClick(albumArtistView, albumArtistView.albumArtist.getSongsSingle())) {
                searchPresenter.onArtistClicked(albumArtistView, viewholder);
            }
        }

        @Override
        public boolean onAlbumArtistLongClick(int position, AlbumArtistView albumArtistView) {
            return contextualToolbarHelper.handleLongClick(albumArtistView, albumArtistView.albumArtist.getSongsSingle());
        }

        @Override
        public void onAlbumArtistOverflowClicked(View v, AlbumArtist albumArtist) {
            PopupMenu menu = new PopupMenu(v.getContext(), v);
            menu.inflate(R.menu.menu_artist);
            menu.setOnMenuItemClickListener(AlbumArtistMenuUtils.INSTANCE.getAlbumArtistClickListener(v.getContext(), mediaManager, albumArtist, albumArtistMenuCallbacksAdapter));
            menu.show();
        }
    };
}
