package com.simplecity.amp_library.ui.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
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
import com.simplecity.amp_library.dagger.module.FragmentModule;
import com.simplecity.amp_library.model.Album;
import com.simplecity.amp_library.ui.adapters.SectionedAdapter;
import com.simplecity.amp_library.ui.adapters.ViewType;
import com.simplecity.amp_library.ui.modelviews.AlbumView;
import com.simplecity.amp_library.ui.modelviews.EmptyView;
import com.simplecity.amp_library.ui.modelviews.SelectableViewModel;
import com.simplecity.amp_library.ui.recyclerview.GridDividerDecoration;
import com.simplecity.amp_library.ui.views.ContextualToolbar;
import com.simplecity.amp_library.utils.ContextualToolbarHelper;
import com.simplecity.amp_library.utils.DataManager;
import com.simplecity.amp_library.utils.LogUtils;
import com.simplecity.amp_library.utils.MenuUtils;
import com.simplecity.amp_library.utils.MusicUtils;
import com.simplecity.amp_library.utils.PermissionUtils;
import com.simplecity.amp_library.utils.PlaylistUtils;
import com.simplecity.amp_library.utils.SettingsManager;
import com.simplecity.amp_library.utils.SortManager;
import com.simplecityapps.recycler_adapter.model.ViewModel;
import com.simplecityapps.recycler_adapter.recyclerview.RecyclerListener;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import java.util.Collections;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

public class AlbumFragment extends BaseFragment implements
        MusicUtils.Defs,
        AlbumView.ClickListener {

    interface AlbumClickListener {

        void onAlbumClicked(Album album, View transitionView);
    }

    private final static String TAG = "AlbumFragment";

    private static final String ARG_PAGE_TITLE = "page_title";

    private static final int MENU_GRID_SIZE = 100;
    private static final int MENU_GROUP_GRID = 1;

    @Nullable
    private AlbumClickListener albumClickListener;

    private FastScrollRecyclerView recyclerView;

    private GridLayoutManager layoutManager;

    private SectionedAdapter adapter;

    private boolean sortOrderChanged = false;

    private Disposable subscription;

    @Inject
    RequestManager requestManager;

    private ContextualToolbarHelper<Album> contextualToolbarHelper;

    public AlbumFragment() {

    }

    public static AlbumFragment newInstance(String pageTitle) {
        AlbumFragment albumFragment = new AlbumFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PAGE_TITLE, pageTitle);
        albumFragment.setArguments(args);
        return albumFragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        Fragment parentFragment = getParentFragment();
        if (parentFragment instanceof AlbumClickListener) {
            albumClickListener = (AlbumClickListener) parentFragment;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ShuttleApplication.getInstance().getAppComponent()
                .plus(new FragmentModule(this))
                .inject(this);

        setHasOptionsMenu(true);

        adapter = new SectionedAdapter();
    }

    @SuppressLint("NewApi")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        int spanCount = SettingsManager.getInstance().getAlbumColumnCount(getResources());
        layoutManager = new GridLayoutManager(getContext(), spanCount);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (adapter.items.get(position) instanceof EmptyView) {
                    return spanCount;
                }
                return 1;
            }
        });

        recyclerView = (FastScrollRecyclerView) inflater.inflate(R.layout.fragment_recycler, container, false);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new GridDividerDecoration(getResources(), 4, true));
        recyclerView.setRecyclerListener(new RecyclerListener());
        recyclerView.setAdapter(adapter);

        return recyclerView;
    }

    @Override
    public void onPause() {

        if (subscription != null) {
            subscription.dispose();
        }

        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        refreshAdapterItems();

        if (getUserVisibleHint()) {
            setupContextualToolbar();
        }
    }

    void refreshAdapterItems() {

        PermissionUtils.RequestStoragePermissions(() -> {
            if (getActivity() != null && isAdded()) {

                int albumDisplayType = SettingsManager.getInstance().getAlbumDisplayType();

                boolean ascending = SortManager.getInstance().getAlbumsAscending();

                subscription = DataManager.getInstance().getAlbumsRelay()
                        .flatMapSingle(albums -> {
                            //Sort
                            SortManager.getInstance().sortAlbums(albums);
                            //Reverse if required
                            if (!ascending) {
                                Collections.reverse(albums);
                            }
                            return Observable.fromIterable(albums)
                                    .map(album -> {

                                        // Look for an existing AlbumView wrapping the album, we'll reuse it if it exists.
                                        AlbumView albumView = (AlbumView) Stream.of(adapter.items)
                                                .filter(viewModel -> viewModel instanceof AlbumView && (((AlbumView) viewModel).album.equals(album)))
                                                .findFirst()
                                                .orElse(null);

                                        if (albumView == null) {
                                            albumView = new AlbumView(album, albumDisplayType, requestManager);
                                            albumView.setClickListener(this);
                                        }

                                        return (ViewModel) albumView;
                                    })
                                    .toList();
                        })
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(items -> {

                            if (items.isEmpty()) {
                                adapter.setItems(Collections.singletonList(new EmptyView(R.string.empty_albums)));
                            } else {
                                adapter.setItems(items);
                            }

                            //Move the RV back to the top if we've had a sort order change.
                            if (sortOrderChanged) {
                                recyclerView.scrollToPosition(0);
                            }

                            sortOrderChanged = false;
                        }, error -> LogUtils.logException(TAG, "Error refreshing adapter items", error));
            }
        });
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.menu_sort_albums, menu);
        inflater.inflate(R.menu.menu_view_as, menu);

        menu.addSubMenu(0, MENU_GRID_SIZE, 0, R.string.menu_grid_size);
        SubMenu subMenu = menu.findItem(MENU_GRID_SIZE).getSubMenu();

        int[] columnRange = getResources().getIntArray(R.array.column_range);
        for (int i = 0; i < columnRange.length; i++) {
            subMenu.add(MENU_GROUP_GRID, columnRange[i] + 1000, i, String.valueOf(columnRange[i]));
        }
        subMenu.setGroupCheckable(MENU_GROUP_GRID, true, true);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        int sortOrder = SortManager.getInstance().getAlbumsSortOrder();

        switch (sortOrder) {
            case SortManager.AlbumSort.DEFAULT:
                menu.findItem(R.id.sort_default).setChecked(true);
                break;
            case SortManager.AlbumSort.NAME:
                menu.findItem(R.id.sort_album_name).setChecked(true);
                break;
            case SortManager.AlbumSort.YEAR:
                menu.findItem(R.id.sort_album_year).setChecked(true);
                break;
            case SortManager.AlbumSort.ARTIST_NAME:
                menu.findItem(R.id.sort_album_artist_name).setChecked(true);
                break;
        }

        menu.findItem(R.id.sort_ascending).setChecked(SortManager.getInstance().getAlbumsAscending());

        int displayType = SettingsManager.getInstance().getAlbumDisplayType();
        switch (displayType) {
            case ViewType.ALBUM_LIST:
                menu.findItem(R.id.view_as_list).setChecked(true);
                break;
            case ViewType.ALBUM_GRID:
                menu.findItem(R.id.view_as_grid).setChecked(true);
                break;
            case ViewType.ALBUM_CARD:
                menu.findItem(R.id.view_as_grid_card).setChecked(true);
                break;
            case ViewType.ALBUM_PALETTE:
                menu.findItem(R.id.view_as_grid_palette).setChecked(true);
                break;
        }

        MenuItem gridMenuItem = menu.findItem(MENU_GRID_SIZE);
        if (displayType == ViewType.ALBUM_LIST) {
            gridMenuItem.setVisible(false);
        } else {
            gridMenuItem.setVisible(true);
            int columnCount = SettingsManager.getInstance().getAlbumColumnCount(getResources());
            gridMenuItem.getSubMenu().findItem(columnCount + 1000).setChecked(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.sort_default:
                SortManager.getInstance().setAlbumsSortOrder(SortManager.AlbumSort.DEFAULT);
                sortOrderChanged = true;
                refreshAdapterItems();
                break;
            case R.id.sort_album_name:
                SortManager.getInstance().setAlbumsSortOrder(SortManager.AlbumSort.NAME);
                sortOrderChanged = true;
                refreshAdapterItems();
                break;
            case R.id.sort_album_year:
                SortManager.getInstance().setAlbumsSortOrder(SortManager.AlbumSort.YEAR);
                sortOrderChanged = true;
                refreshAdapterItems();
                break;
            case R.id.sort_album_artist_name:
                SortManager.getInstance().setAlbumsSortOrder(SortManager.AlbumSort.ARTIST_NAME);
                sortOrderChanged = true;
                refreshAdapterItems();
                break;
            case R.id.sort_ascending:
                SortManager.getInstance().setAlbumsAscending(!item.isChecked());
                sortOrderChanged = true;
                refreshAdapterItems();
                break;
            case R.id.view_as_list:
                int viewType = ViewType.ALBUM_LIST;
                SettingsManager.getInstance().setAlbumDisplayType(viewType);
                layoutManager.setSpanCount(getResources().getInteger(R.integer.list_num_columns));
                updateViewType(viewType);
                break;
            case R.id.view_as_grid:
                viewType = ViewType.ALBUM_GRID;
                SettingsManager.getInstance().setAlbumDisplayType(viewType);
                layoutManager.setSpanCount(SettingsManager.getInstance().getAlbumColumnCount(getResources()));
                updateViewType(viewType);
                break;
            case R.id.view_as_grid_card:
                viewType = ViewType.ALBUM_CARD;
                SettingsManager.getInstance().setAlbumDisplayType(viewType);
                layoutManager.setSpanCount(SettingsManager.getInstance().getAlbumColumnCount(getResources()));
                updateViewType(viewType);
                break;
            case R.id.view_as_grid_palette:
                viewType = ViewType.ALBUM_PALETTE;
                SettingsManager.getInstance().setAlbumDisplayType(viewType);
                layoutManager.setSpanCount(SettingsManager.getInstance().getAlbumColumnCount(getResources()));
                updateViewType(viewType);
                break;
        }

        if (item.getGroupId() == MENU_GROUP_GRID) {

            SettingsManager.getInstance().setAlbumColumnCount(item.getItemId() - 1000);

            if (SettingsManager.getInstance().getAlbumDisplayType() != ViewType.ALBUM_LIST) {
                ((GridLayoutManager) recyclerView.getLayoutManager()).setSpanCount(SettingsManager.getInstance().getAlbumColumnCount(getResources()));
                adapter.notifyItemRangeChanged(0, adapter.getItemCount());
            }
        }

        getActivity().supportInvalidateOptionsMenu();

        return super.onOptionsItemSelected(item);
    }

    void updateViewType(@ViewType int viewType) {
        Stream.of(adapter.items)
                .filter(viewModel -> viewModel instanceof AlbumView)
                .forEach(viewModel -> ((AlbumView) viewModel).setViewType(viewType));
        adapter.notifyItemRangeChanged(0, adapter.getItemCount());
    }

    @Override
    public void onAlbumClick(int position, AlbumView albumView, AlbumView.ViewHolder viewHolder) {
        if (!contextualToolbarHelper.handleClick(position, albumView)) {
            if (albumClickListener != null) {
                albumClickListener.onAlbumClicked(albumView.album, viewHolder.imageOne);
            }
        }
    }

    @Override
    public boolean onAlbumLongClick(int position, AlbumView albumView) {
        return contextualToolbarHelper.handleLongClick(position, albumView);
    }

    @Override
    public void onAlbumOverflowClicked(View v, Album album) {
        PopupMenu menu = new PopupMenu(getContext(), v);
        menu.inflate(R.menu.menu_album);
        SubMenu sub = menu.getMenu().findItem(R.id.addToPlaylist).getSubMenu();
        PlaylistUtils.makePlaylistMenu(getActivity(), sub);
        menu.setOnMenuItemClickListener(MenuUtils.getAlbumMenuClickListener(getContext(), album, taggerDialog -> taggerDialog.show(getFragmentManager())));
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
            contextualToolbar.inflateMenu(R.menu.context_menu_songs);
            SubMenu sub = contextualToolbar.getMenu().findItem(R.id.addToPlaylist).getSubMenu();
            PlaylistUtils.makePlaylistMenu(getActivity(), sub);

            contextualToolbar.setOnMenuItemClickListener(MenuUtils.getAlbumMenuClickListener(
                    getContext(),
                    () -> Stream.of(contextualToolbarHelper.getItems())
                            .map(SelectableViewModel::getItem)
                            .toList()
                    )
            );

            contextualToolbarHelper = new ContextualToolbarHelper<>(contextualToolbar, new ContextualToolbarHelper.Callback() {
                @Override
                public void notifyItemChanged(int position) {
                    adapter.notifyItemChanged(position, 0);
                }

                @Override
                public void notifyDatasetChanged() {
                    adapter.notifyItemRangeChanged(0, adapter.items.size(), 0);
                }
            });
        }
        Log.i(TAG, "setupContextualToolbar.. Visible to user: " + getUserVisibleHint());
    }

    @Override
    protected String screenName() {
        return TAG;
    }
}
