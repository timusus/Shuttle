package com.simplecity.amp_library.ui.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import com.annimon.stream.Stream;
import com.bumptech.glide.RequestManager;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.dagger.module.ActivityModule;
import com.simplecity.amp_library.dagger.module.FragmentModule;
import com.simplecity.amp_library.model.AlbumArtist;
import com.simplecity.amp_library.ui.adapters.SectionedAdapter;
import com.simplecity.amp_library.ui.adapters.ViewType;
import com.simplecity.amp_library.ui.modelviews.AlbumArtistView;
import com.simplecity.amp_library.ui.modelviews.EmptyView;
import com.simplecity.amp_library.ui.modelviews.SelectableViewModel;
import com.simplecity.amp_library.ui.recyclerview.GridDividerDecoration;
import com.simplecity.amp_library.ui.views.ContextualToolbar;
import com.simplecity.amp_library.utils.ContextualToolbarHelper;
import com.simplecity.amp_library.utils.DataManager;
import com.simplecity.amp_library.utils.PermissionUtils;
import com.simplecity.amp_library.utils.PlaylistUtils;
import com.simplecity.amp_library.utils.SettingsManager;
import com.simplecity.amp_library.utils.menu.albumartist.AlbumArtistMenuCallbacksAdapter;
import com.simplecity.amp_library.utils.menu.albumartist.AlbumArtistMenuUtils;
import com.simplecity.amp_library.utils.sorting.SortManager;
import com.simplecityapps.recycler_adapter.model.ViewModel;
import com.simplecityapps.recycler_adapter.recyclerview.RecyclerListener;
import com.simplecityapps.recycler_adapter.recyclerview.SpanSizeLookup;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;

public class AlbumArtistFragment extends BaseFragment implements
        AlbumArtistView.ClickListener {

    interface AlbumArtistClickListener {

        void onAlbumArtistClicked(AlbumArtist albumArtist, View transitionView);
    }

    private static final String TAG = "AlbumArtistFragment";

    private static final String ARG_PAGE_TITLE = "page_title";

    private static final int MENU_GRID_SIZE = 100;
    private static final int MENU_GROUP_GRID = 1;

    @Nullable
    private AlbumArtistClickListener albumArtistClickListener;

    private FastScrollRecyclerView recyclerView;

    private GridLayoutManager layoutManager;

    SectionedAdapter adapter;

    private SpanSizeLookup spanSizeLookup;

    private boolean sortOrderChanged = false;

    private Disposable refreshDisposable;

    private ContextualToolbarHelper<AlbumArtist> contextualToolbarHelper;

    @Inject
    RequestManager requestManager;

    @Nullable
    private Disposable playlistMenuDisposable;

    private CompositeDisposable menuDisposable = new CompositeDisposable();

    private AlbumArtistMenuCallbacksAdapter albumArtistMenuCallbacksAdapter = new AlbumArtistMenuCallbacksAdapter(this, menuDisposable);

    public static AlbumArtistFragment newInstance(String pageTitle) {
        Bundle args = new Bundle();
        AlbumArtistFragment fragment = new AlbumArtistFragment();
        args.putString(ARG_PAGE_TITLE, pageTitle);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        Fragment parentFragment = getParentFragment();
        if (parentFragment instanceof AlbumArtistClickListener) {
            albumArtistClickListener = (AlbumArtistClickListener) parentFragment;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ShuttleApplication.getInstance().getAppComponent()
                .plus(new ActivityModule(getActivity()))
                .plus(new FragmentModule(this))
                .inject(this);

        setHasOptionsMenu(true);

        adapter = new SectionedAdapter();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        if (recyclerView == null) {
            int spanCount = SettingsManager.getInstance().getArtistColumnCount(getResources());
            layoutManager = new GridLayoutManager(getContext(), spanCount);
            spanSizeLookup = new SpanSizeLookup(adapter, spanCount);
            spanSizeLookup.setSpanIndexCacheEnabled(true);
            layoutManager.setSpanSizeLookup(spanSizeLookup);

            recyclerView = (FastScrollRecyclerView) inflater.inflate(R.layout.fragment_recycler, container, false);
            recyclerView.setLayoutManager(layoutManager);
            recyclerView.addItemDecoration(new GridDividerDecoration(getResources(), 4, true));
            recyclerView.setRecyclerListener(new RecyclerListener());
        }
        if (recyclerView.getAdapter() != adapter) {
            recyclerView.setAdapter(adapter);
        }

        return recyclerView;
    }

    @Override
    public void onPause() {

        if (refreshDisposable != null) {
            refreshDisposable.dispose();
        }

        if (playlistMenuDisposable != null) {
            playlistMenuDisposable.dispose();
        }

        menuDisposable.clear();

        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        refreshAdapterItems(false);

        if (getUserVisibleHint()) {
            setupContextualToolbar();
        }
    }

    void refreshAdapterItems(boolean force) {
        PermissionUtils.RequestStoragePermissions(() -> {
            if (getActivity() != null && isAdded()) {

                int artistDisplayType = SettingsManager.getInstance().getArtistDisplayType();

                boolean ascending = SortManager.getInstance().getArtistsAscending();

                refreshDisposable = DataManager.getInstance().getAlbumArtistsRelay()
                        .skipWhile(albumArtists -> !force && adapter.items.size() == albumArtists.size())
                        .debounce(150, TimeUnit.MILLISECONDS)
                        .flatMapSingle(albumArtists -> {
                            //Sort
                            SortManager.getInstance().sortAlbumArtists(albumArtists);
                            //Reverse if required
                            if (!ascending) {
                                Collections.reverse(albumArtists);
                            }
                            return Observable.fromIterable(albumArtists)
                                    .map(albumArtist -> {

                                        // Look for an existing AlbumArtistView wrapping the song, we'll reuse it if it exists.
                                        AlbumArtistView albumArtistView = (AlbumArtistView) Stream.of(adapter.items)
                                                .filter(viewModel -> viewModel instanceof AlbumArtistView && (((AlbumArtistView) viewModel).albumArtist.equals(albumArtist)))
                                                .findFirst()
                                                .orElse(null);

                                        if (albumArtistView == null) {
                                            albumArtistView = new AlbumArtistView(albumArtist, artistDisplayType, requestManager);
                                            albumArtistView.setClickListener(this);
                                        }

                                        return (ViewModel) albumArtistView;
                                    })
                                    .toList();
                        })
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(items -> {

                            if (items.isEmpty()) {
                                adapter.setItems(Collections.singletonList(new EmptyView(R.string.empty_artists)));
                            } else {
                                adapter.setItems(items);
                            }

                            //Move the RV back to the top if we've had a sort order change.
                            if (sortOrderChanged) {
                                recyclerView.scrollToPosition(0);
                            }

                            sortOrderChanged = false;
                        });
            }
        });
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.menu_sort_artists, menu);
        inflater.inflate(R.menu.menu_view_as, menu);

        menu.addSubMenu(0, MENU_GRID_SIZE, 0, R.string.menu_grid_size);
        SubMenu subMenu = menu.findItem(MENU_GRID_SIZE).getSubMenu();

        int[] spanCountArray = getResources().getIntArray(R.array.span_count);
        for (int i = 0; i < spanCountArray.length; i++) {
            subMenu.add(MENU_GROUP_GRID, spanCountArray[i], i, String.valueOf(spanCountArray[i]));
        }
        subMenu.setGroupCheckable(MENU_GROUP_GRID, true, true);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        //Strip the 'asc' or 'desc' flag, we just want to know the sort type
        int sortOrder = SortManager.getInstance().getArtistsSortOrder();

        switch (sortOrder) {
            case SortManager.ArtistSort.DEFAULT:
                MenuItem sortDefault = menu.findItem(R.id.sort_artist_default);
                if (sortDefault != null) {
                    sortDefault.setChecked(true);
                }
                break;
            case SortManager.ArtistSort.NAME:
                MenuItem sortArtistName = menu.findItem(R.id.sort_artist_name);
                if (sortArtistName != null) {
                    sortArtistName.setChecked(true);
                }
                break;
        }

        MenuItem sortAscending = menu.findItem(R.id.sort_artist_ascending);
        if (sortAscending != null) {
            sortAscending.setChecked(SortManager.getInstance().getArtistsAscending());
        }

        int displayType = SettingsManager.getInstance().getArtistDisplayType();
        switch (displayType) {
            case ViewType.ARTIST_LIST:
                MenuItem viewAsList = menu.findItem(R.id.view_as_list);
                if (viewAsList != null) {
                    viewAsList.setChecked(true);
                }
                break;
            case ViewType.ARTIST_GRID:
                MenuItem viewAsGrid = menu.findItem(R.id.view_as_grid);
                if (viewAsGrid != null) {
                    viewAsGrid.setChecked(true);
                }
                break;
            case ViewType.ARTIST_CARD:
                MenuItem viewAsGridCard = menu.findItem(R.id.view_as_grid_card);
                if (viewAsGridCard != null) {
                    viewAsGridCard.setChecked(true);
                }
                break;
            case ViewType.ARTIST_PALETTE:
                MenuItem viewAsGridPalette = menu.findItem(R.id.view_as_grid_palette);
                if (viewAsGridPalette != null) {
                    viewAsGridPalette.setChecked(true);
                }
                break;
        }

        MenuItem gridMenuItem = menu.findItem(MENU_GRID_SIZE);
        if (displayType == ViewType.ARTIST_LIST) {
            gridMenuItem.setVisible(false);
        } else {
            gridMenuItem.setVisible(true);
            SubMenu subMenu = gridMenuItem.getSubMenu();
            if (subMenu != null) {
                subMenu.findItem(SettingsManager.getInstance().getArtistColumnCount(getResources()))
                        .setChecked(true);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.sort_artist_default:
                SortManager.getInstance().setArtistsSortOrder(SortManager.ArtistSort.DEFAULT);
                sortOrderChanged = true;
                refreshAdapterItems(true);
                break;
            case R.id.sort_artist_name:
                SortManager.getInstance().setArtistsSortOrder(SortManager.ArtistSort.NAME);
                sortOrderChanged = true;
                refreshAdapterItems(true);
                break;
            case R.id.sort_artist_ascending:
                SortManager.getInstance().setArtistsAscending(!item.isChecked());
                sortOrderChanged = true;
                refreshAdapterItems(true);
                break;
            case R.id.view_as_list:
                int viewType = ViewType.ARTIST_LIST;
                SettingsManager.getInstance().setArtistDisplayType(viewType);
                setupListSpan();
                updateViewType(viewType);
                break;
            case R.id.view_as_grid:
                viewType = ViewType.ARTIST_GRID;
                SettingsManager.getInstance().setArtistDisplayType(viewType);
                setupGridSpan();
                updateViewType(viewType);
                break;
            case R.id.view_as_grid_card:
                viewType = ViewType.ARTIST_CARD;
                SettingsManager.getInstance().setArtistDisplayType(viewType);
                setupGridSpan();
                updateViewType(viewType);
                break;
            case R.id.view_as_grid_palette:
                viewType = ViewType.ARTIST_PALETTE;
                SettingsManager.getInstance().setArtistDisplayType(viewType);
                setupGridSpan();
                updateViewType(viewType);
                break;
        }

        if (item.getGroupId() == MENU_GROUP_GRID) {
            SettingsManager.getInstance().setArtistColumnCount(item.getItemId());
            spanSizeLookup.setSpanCount(item.getItemId());
            ((GridLayoutManager) recyclerView.getLayoutManager()).setSpanCount(SettingsManager.getInstance().getArtistColumnCount(getResources()));
            adapter.notifyItemRangeChanged(0, adapter.getItemCount());
        }

        getActivity().invalidateOptionsMenu();

        return super.onOptionsItemSelected(item);
    }

    private void setupGridSpan() {
        int spanCount = SettingsManager.getInstance().getArtistColumnCount(getResources());
        spanSizeLookup.setSpanCount(spanCount);
        layoutManager.setSpanCount(spanCount);
    }

    private void setupListSpan() {
        int spanCount = getResources().getInteger(R.integer.list_num_columns);
        spanSizeLookup.setSpanCount(spanCount);
        layoutManager.setSpanCount(spanCount);
    }

    void updateViewType(@ViewType int viewType) {
        Stream.of(adapter.items)
                .filter(viewModel -> viewModel instanceof AlbumArtistView)
                .forEach(viewModel -> ((AlbumArtistView) viewModel).setViewType(viewType));
        adapter.notifyItemRangeChanged(0, adapter.getItemCount());
    }

    @Override
    public void onAlbumArtistClick(int position, AlbumArtistView albumArtistView, AlbumArtistView.ViewHolder viewholder) {
        if (!contextualToolbarHelper.handleClick(albumArtistView, albumArtistView.albumArtist)) {
            if (albumArtistClickListener != null) {
                albumArtistClickListener.onAlbumArtistClicked(albumArtistView.albumArtist, viewholder.imageOne);
            }
        }
    }

    @Override
    public boolean onAlbumArtistLongClick(int position, AlbumArtistView albumArtistView) {
        return contextualToolbarHelper.handleLongClick(albumArtistView, albumArtistView.albumArtist);
    }

    @Override
    public void onAlbumArtistOverflowClicked(View v, AlbumArtist albumArtist) {
        PopupMenu menu = new PopupMenu(getContext(), v);
        menu.inflate(R.menu.menu_artist);
        SubMenu sub = menu.getMenu().findItem(R.id.addToPlaylist).getSubMenu();
        PlaylistUtils.createPlaylistMenu(sub);
        menu.setOnMenuItemClickListener(AlbumArtistMenuUtils.INSTANCE.getAlbumArtistClickListener(getContext(), mediaManager, albumArtist, albumArtistMenuCallbacksAdapter));
        menu.show();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            setupContextualToolbar();
        } else {
            if (contextualToolbarHelper != null) {
                contextualToolbarHelper.finish();
            }
        }
    }

    private void setupContextualToolbar() {
        ContextualToolbar contextualToolbar = ContextualToolbar.findContextualToolbar(this);
        if (contextualToolbar != null) {
            contextualToolbar.getMenu().clear();
            contextualToolbar.inflateMenu(R.menu.context_menu_general);
            SubMenu sub = contextualToolbar.getMenu().findItem(R.id.addToPlaylist).getSubMenu();
            if (playlistMenuDisposable != null) {
                playlistMenuDisposable.dispose();
            }
            playlistMenuDisposable = PlaylistUtils.createUpdatingPlaylistMenu(sub).subscribe();

            contextualToolbar.setOnMenuItemClickListener(
                    AlbumArtistMenuUtils.INSTANCE.getAlbumArtistMenuClickListener(
                            getContext(),
                            mediaManager,
                            Single.defer(() -> Single.just(contextualToolbarHelper.getItems())),
                            albumArtistMenuCallbacksAdapter
                    ));

            contextualToolbarHelper = new ContextualToolbarHelper<>(contextualToolbar, new ContextualToolbarHelper.Callback() {
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
            });
        }
    }

    @Override
    protected String screenName() {
        return TAG;
    }
}
