package com.simplecity.amp_library.ui.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.bignerdranch.android.multiselector.ModalMultiSelectorCallback;
import com.bignerdranch.android.multiselector.MultiSelector;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.model.AdaptableItem;
import com.simplecity.amp_library.model.AlbumArtist;
import com.simplecity.amp_library.model.Playlist;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.ui.adapters.AlbumArtistAdapter;
import com.simplecity.amp_library.ui.modelviews.AlbumArtistView;
import com.simplecity.amp_library.ui.modelviews.EmptyView;
import com.simplecity.amp_library.ui.modelviews.ViewType;
import com.simplecity.amp_library.ui.recyclerview.GridDividerDecoration;
import com.simplecity.amp_library.utils.ColorUtils;
import com.simplecity.amp_library.utils.DataManager;
import com.simplecity.amp_library.utils.DialogUtils;
import com.simplecity.amp_library.utils.MenuUtils;
import com.simplecity.amp_library.utils.MusicUtils;
import com.simplecity.amp_library.utils.PermissionUtils;
import com.simplecity.amp_library.utils.PlaylistUtils;
import com.simplecity.amp_library.utils.SettingsManager;
import com.simplecity.amp_library.utils.ShuttleUtils;
import com.simplecity.amp_library.utils.SortManager;
import com.simplecity.amp_library.utils.ThemeUtils;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import java.util.Collections;
import java.util.List;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class AlbumArtistFragment extends BaseFragment implements
        MusicUtils.Defs,
        AlbumArtistAdapter.AlbumArtistListener,
        RecyclerView.RecyclerListener {

    public interface AlbumArtistClickListener {

        void onItemClicked(AlbumArtist albumArtist, View transitionView);
    }

    private static final String TAG = "AlbumArtistFragment";

    private static final String ARG_PAGE_TITLE = "page_title";

    private static final int MENU_GRID_SIZE = 100;
    private static final int MENU_GROUP_GRID = 1;

    private SharedPreferences sharedPreferences;

    private AlbumArtistClickListener albumArtistClickListener;

    private FastScrollRecyclerView recyclerView;

    private GridLayoutManager layoutManager;

    AlbumArtistAdapter albumArtistAdapter;

    MultiSelector multiSelector = new MultiSelector();

    ActionMode actionMode;

    boolean inActionMode = false;

    private BroadcastReceiver broadcastReceiver;

    private SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener;

    private boolean sortOrderChanged = false;

    private Subscription subscription;

    private RequestManager requestManager;

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

        albumArtistClickListener = (AlbumArtistClickListener) getActivity();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        albumArtistAdapter = new AlbumArtistAdapter();
        albumArtistAdapter.setListener(this);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getActivity());

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction() != null && intent.getAction().equals("restartLoader")) {
                    refreshAdapterItems();
                }
            }
        };

        onSharedPreferenceChangeListener = (sharedPreferences, key) -> {
            if (key.equals("pref_theme_highlight_color")
                    || key.equals("pref_theme_accent_color")
                    || key.equals("pref_theme_white_accent")) {
                themeUIComponents();
            } else if (key.equals("artistWhitelist")) {
                refreshAdapterItems();
            }
        };

        sharedPreferences.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);

        if (requestManager == null) {
            requestManager = Glide.with(this);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        if (recyclerView == null) {
            int spanCount = SettingsManager.getInstance().getArtistColumnCount(getResources());
            layoutManager = new GridLayoutManager(getContext(), spanCount);
            layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    if (albumArtistAdapter.items.get(position) instanceof EmptyView) {
                        return spanCount;
                    }
                    return 1;
                }
            });

            recyclerView = (FastScrollRecyclerView) inflater.inflate(R.layout.fragment_recycler, container, false);
            recyclerView.setLayoutManager(layoutManager);
            recyclerView.addItemDecoration(new GridDividerDecoration(getResources(), 4, true));
            recyclerView.setRecyclerListener(this);
            recyclerView.setAdapter(albumArtistAdapter);

            actionMode = null;

            themeUIComponents();
        }

        return recyclerView;
    }

    @Override
    public void onPause() {
        if (broadcastReceiver != null) {
            getActivity().unregisterReceiver(broadcastReceiver);
        }

        if (subscription != null) {
            subscription.unsubscribe();
        }

        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction("restartLoader");
        getActivity().registerReceiver(broadcastReceiver, filter);

        refreshAdapterItems();
    }

    @Override
    public void onDestroy() {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
        super.onDestroy();
    }

    private void themeUIComponents() {
        ThemeUtils.themeRecyclerView(recyclerView);
        recyclerView.setThumbColor(ColorUtils.getAccentColor());
        recyclerView.setPopupBgColor(ColorUtils.getAccentColor());
        recyclerView.setPopupTextColor(ColorUtils.getAccentColorSensitiveTextColor(getContext()));

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                ThemeUtils.themeRecyclerView(recyclerView);
                super.onScrollStateChanged(recyclerView, newState);
            }
        });
    }

    void refreshAdapterItems() {

        PermissionUtils.RequestStoragePermissions(() -> {
            if (getActivity() != null && isAdded()) {

                int artistDisplayType = SettingsManager.getInstance().getArtistDisplayType();

                boolean ascending = SortManager.getInstance().getArtistsAscending();

                subscription = DataManager.getInstance().getAlbumArtistsRelay()
                        .flatMap(albumArtists -> {
                            //Sort
                            SortManager.getInstance().sortAlbumArtists(albumArtists);
                            //Reverse if required
                            if (!ascending) {
                                Collections.reverse(albumArtists);
                            }
                            return Observable.from(albumArtists)
                                    .map(albumArtist -> (AdaptableItem) new AlbumArtistView(albumArtist, artistDisplayType, multiSelector, requestManager))
                                    .toList();
                        })
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(items -> {

                            if (items.isEmpty()) {
                                albumArtistAdapter.setEmpty(new EmptyView(R.string.empty_artists));
                            } else {
                                albumArtistAdapter.setItems(items);
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
        int[] columnRange = getResources().getIntArray(R.array.column_range);
        for (int i = 0; i < columnRange.length; i++) {
            subMenu.add(MENU_GROUP_GRID, columnRange[i] + 1000, i, String.valueOf(columnRange[i]));
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
                MenuItem sortDefault = menu.findItem(R.id.sort_default);
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

        MenuItem sortAscending = menu.findItem(R.id.sort_ascending);
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
            int columnCount = SettingsManager.getInstance().getArtistColumnCount(getResources());
            gridMenuItem.getSubMenu().findItem(columnCount + 1000).setChecked(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.sort_default:
                SortManager.getInstance().setArtistsSortOrder(SortManager.ArtistSort.DEFAULT);
                sortOrderChanged = true;
                refreshAdapterItems();
                break;
            case R.id.sort_artist_name:
                SortManager.getInstance().setArtistsSortOrder(SortManager.ArtistSort.NAME);
                sortOrderChanged = true;
                refreshAdapterItems();
                break;
            case R.id.sort_ascending:
                SortManager.getInstance().setArtistsAscending(!item.isChecked());
                sortOrderChanged = true;
                refreshAdapterItems();
                break;
            case R.id.view_as_list:
                SettingsManager.getInstance().setArtistDisplayType(ViewType.ARTIST_LIST);
                layoutManager.setSpanCount(getResources().getInteger(R.integer.list_num_columns));
                albumArtistAdapter.updateItemViewType();
                albumArtistAdapter.notifyItemRangeChanged(0, albumArtistAdapter.getItemCount());
                break;
            case R.id.view_as_grid:
                SettingsManager.getInstance().setArtistDisplayType(ViewType.ARTIST_GRID);
                layoutManager.setSpanCount(SettingsManager.getInstance().getArtistColumnCount(getResources()));
                albumArtistAdapter.updateItemViewType();
                albumArtistAdapter.notifyItemRangeChanged(0, albumArtistAdapter.getItemCount());
                break;
            case R.id.view_as_grid_card:
                SettingsManager.getInstance().setArtistDisplayType(ViewType.ARTIST_CARD);
                layoutManager.setSpanCount(SettingsManager.getInstance().getArtistColumnCount(getResources()));
                albumArtistAdapter.updateItemViewType();
                albumArtistAdapter.notifyItemRangeChanged(0, albumArtistAdapter.getItemCount());
                break;
            case R.id.view_as_grid_palette:
                SettingsManager.getInstance().setArtistDisplayType(ViewType.ARTIST_PALETTE);
                layoutManager.setSpanCount(SettingsManager.getInstance().getArtistColumnCount(getResources()));
                albumArtistAdapter.updateItemViewType();
                albumArtistAdapter.notifyItemRangeChanged(0, albumArtistAdapter.getItemCount());
                break;
        }

        if (item.getGroupId() == MENU_GROUP_GRID) {
            SettingsManager.getInstance().setArtistColumnCount(item.getItemId() - 1000);

            if (SettingsManager.getInstance().getArtistDisplayType() != ViewType.ARTIST_LIST) {
                ((GridLayoutManager) recyclerView.getLayoutManager()).setSpanCount(SettingsManager.getInstance().getArtistColumnCount(getResources()));
                albumArtistAdapter.notifyItemRangeChanged(0, albumArtistAdapter.getItemCount());
            }
        }

        getActivity().supportInvalidateOptionsMenu();

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(View v, int position, AlbumArtist albumArtist) {
        if (inActionMode) {
            multiSelector.setSelected(position, albumArtistAdapter.getItemId(position), !multiSelector.isSelected(position, albumArtistAdapter.getItemId(position)));

            if (multiSelector.getSelectedPositions().size() == 0) {
                if (actionMode != null) {
                    actionMode.finish();
                }
            }

            updateActionModeSelectionCount();

        } else {
            albumArtistClickListener.onItemClicked(albumArtist, v.findViewById(R.id.image));
        }
    }

    @Override
    public void onOverflowClick(View v, int position, AlbumArtist albumArtist) {
        PopupMenu menu = new PopupMenu(AlbumArtistFragment.this.getActivity(), v);
        MenuUtils.addAlbumArtistMenuOptions(getActivity(), menu);
        MenuUtils.addClickHandler((AppCompatActivity) getActivity(), menu, albumArtist);
        menu.show();
    }

    @Override
    public void onLongClick(View v, int position, AlbumArtist albumArtist) {
        if (inActionMode) {
            return;
        }

        if (multiSelector.getSelectedPositions().size() == 0) {
            actionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(mActionModeCallback);
            inActionMode = true;
        }

        multiSelector.setSelected(position, albumArtistAdapter.getItemId(position), !multiSelector.isSelected(position, albumArtistAdapter.getItemId(position)));

        updateActionModeSelectionCount();
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        if (holder.getAdapterPosition() != -1) {
            albumArtistAdapter.items.get(holder.getAdapterPosition()).recycle(holder);
        }
    }

    private void updateActionModeSelectionCount() {
        if (actionMode != null && multiSelector != null) {
            actionMode.setTitle(getString(R.string.action_mode_selection_count, multiSelector.getSelectedPositions().size()));
        }
    }

    private ActionMode.Callback mActionModeCallback = new ModalMultiSelectorCallback(multiSelector) {
        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            ThemeUtils.themeContextualActionBar(getActivity());
            inActionMode = true;
            MenuInflater inflater = getActivity().getMenuInflater();
            inflater.inflate(R.menu.context_menu_songs, menu);
            SubMenu sub = menu.getItem(0).getSubMenu();
            PlaylistUtils.makePlaylistMenu(AlbumArtistFragment.this.getActivity(), sub, ARTIST_FRAGMENT_GROUP_ID);
            return true;
        }

        @Override
        public boolean onActionItemClicked(final ActionMode mode, MenuItem menuItem) {

            List<AlbumArtist> checkedAlbumArtists = getCheckedAlbumArtists();

            if (checkedAlbumArtists == null || checkedAlbumArtists.size() == 0) {
                return true;
            }

            Observable<List<Song>> songsObservable = Observable.defer(() ->
                    Observable.from(checkedAlbumArtists)
                            .flatMap(AlbumArtist::getSongsObservable)
                            .reduce((songs, songs2) -> Stream.concat(Stream.of(songs), Stream.of(songs2))
                                    .collect(Collectors.toList()))
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
            );

            switch (menuItem.getItemId()) {
                case NEW_PLAYLIST:
                    songsObservable.subscribe(songs -> PlaylistUtils.createPlaylistDialog(getActivity(), songs));
                    return true;
                case PLAYLIST_SELECTED:
                    songsObservable
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(songs -> {
                                Playlist playlist = (Playlist) menuItem.getIntent().getSerializableExtra(ShuttleUtils.ARG_PLAYLIST);
                                PlaylistUtils.addToPlaylist(getContext(), playlist, songs);
                            });
                    return true;
                case R.id.delete:
                    new DialogUtils.DeleteDialogBuilder()
                            .context(getContext())
                            .singleMessageId(R.string.delete_album_artist_desc)
                            .multipleMessage(R.string.delete_album_artist_desc_multiple)
                            .itemNames(Stream.of(checkedAlbumArtists)
                                    .map(albumArtist -> albumArtist.name)
                                    .collect(Collectors.toList()))
                            .songsToDelete(songsObservable)
                            .build()
                            .show();
                    mode.finish();
                    return true;
                case R.id.menu_add_to_queue: {
                    songsObservable.subscribe(songs -> MusicUtils.addToQueue(getActivity(), songs));
                    return true;
                }
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            super.onDestroyActionMode(actionMode);
            inActionMode = false;
            AlbumArtistFragment.this.actionMode = null;
            multiSelector.clearSelections();
        }
    };

    List<AlbumArtist> getCheckedAlbumArtists() {
        return Stream.of(multiSelector.getSelectedPositions())
                .map(i -> ((AlbumArtistView) albumArtistAdapter.items.get(i)).albumArtist)
                .collect(Collectors.toList());
    }

    @Override
    protected String screenName() {
        return TAG;
    }
}
