package com.simplecity.amp_library.ui.fragments;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import com.simplecity.amp_library.model.Album;
import com.simplecity.amp_library.model.Playlist;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.ui.adapters.AlbumAdapter;
import com.simplecity.amp_library.ui.modelviews.AlbumView;
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

public class AlbumFragment extends BaseFragment implements
        MusicUtils.Defs,
        RecyclerView.RecyclerListener,
        AlbumAdapter.AlbumListener {

    public interface AlbumClickListener {

        void onItemClicked(Album album, View transitionView);
    }

    private final static String TAG = "AlbumFragment";

    private static final String ARG_PAGE_TITLE = "page_title";

    private static final int MENU_GRID_SIZE = 100;
    private static final int MENU_GROUP_GRID = 1;

    private SharedPreferences prefs;

    private AlbumClickListener albumClickListener;

    private FastScrollRecyclerView recyclerView;

    private GridLayoutManager layoutManager;

    AlbumAdapter albumAdapter;

    MultiSelector multiSelector = new MultiSelector();

    ActionMode actionMode;

    boolean inActionMode = false;

    private BroadcastReceiver receiver;

    private SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener;

    private boolean sortOrderChanged = false;

    private Subscription subscription;

    private RequestManager requestManager;

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

        albumClickListener = (AlbumClickListener) getActivity();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        albumAdapter = new AlbumAdapter();
        albumAdapter.setListener(this);

        prefs = PreferenceManager.getDefaultSharedPreferences(this.getActivity());

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction() != null && intent.getAction().equals("restartLoader")) {
                    refreshAdapterItems();
                }
            }
        };

        sharedPreferenceChangeListener = (sharedPreferences, key) -> {
            if (key.equals("pref_theme_highlight_color")
                    || key.equals("pref_theme_accent_color")
                    || key.equals("pref_theme_white_accent")) {
                themeUIComponents();
            } else if (key.equals("albumWhitelist")) {
                refreshAdapterItems();
            }
        };

        prefs.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);

        if (requestManager == null) {
            requestManager = Glide.with(this);
        }
    }

    public void themeUIComponents() {
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

    @SuppressLint("NewApi")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        if (recyclerView == null) {

            int spanCount = SettingsManager.getInstance().getAlbumColumnCount(getResources());
            layoutManager = new GridLayoutManager(getContext(), spanCount);
            layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    if (albumAdapter.items.get(position) instanceof EmptyView) {
                        return spanCount;
                    }
                    return 1;
                }
            });

            recyclerView = (FastScrollRecyclerView) inflater.inflate(R.layout.fragment_recycler, container, false);
            recyclerView.setLayoutManager(layoutManager);
            recyclerView.addItemDecoration(new GridDividerDecoration(getResources(), 4, true));
            recyclerView.setRecyclerListener(this);
            recyclerView.setAdapter(albumAdapter);

            actionMode = null;

            themeUIComponents();
        }

        return recyclerView;
    }

    @Override
    public void onPause() {
        if (receiver != null) {
            getActivity().unregisterReceiver(receiver);
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
        getActivity().registerReceiver(receiver, filter);

        refreshAdapterItems();
    }

    @Override
    public void onDestroy() {
        prefs.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
        super.onDestroy();
    }

    void refreshAdapterItems() {

        PermissionUtils.RequestStoragePermissions(() -> {
            if (getActivity() != null && isAdded()) {

                int albumDisplayType = SettingsManager.getInstance().getAlbumDisplayType();

                boolean ascending = SortManager.getInstance().getAlbumsAscending();

                subscription = DataManager.getInstance().getAlbumsRelay()
                        .flatMap(albums -> {
                            //Sort
                            SortManager.getInstance().sortAlbums(albums);
                            //Reverse if required
                            if (!ascending) {
                                Collections.reverse(albums);
                            }
                            return Observable.from(albums)
                                    .map(album -> (AdaptableItem) new AlbumView(album, albumDisplayType, requestManager, multiSelector))
                                    .toList();
                        })
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(items -> {

                            if (items.isEmpty()) {
                                albumAdapter.setEmpty(new EmptyView(R.string.empty_albums));
                            } else {
                                albumAdapter.setItems(items);
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
                SettingsManager.getInstance().setAlbumDisplayType(ViewType.ALBUM_LIST);
                layoutManager.setSpanCount(getResources().getInteger(R.integer.list_num_columns));
                albumAdapter.updateItemViewType();
                albumAdapter.notifyItemRangeChanged(0, albumAdapter.getItemCount());
                break;
            case R.id.view_as_grid:
                SettingsManager.getInstance().setAlbumDisplayType(ViewType.ALBUM_GRID);
                layoutManager.setSpanCount(SettingsManager.getInstance().getAlbumColumnCount(getResources()));
                albumAdapter.updateItemViewType();
                albumAdapter.notifyItemRangeChanged(0, albumAdapter.getItemCount());
                break;
            case R.id.view_as_grid_card:
                SettingsManager.getInstance().setAlbumDisplayType(ViewType.ALBUM_CARD);
                layoutManager.setSpanCount(SettingsManager.getInstance().getAlbumColumnCount(getResources()));
                albumAdapter.updateItemViewType();
                albumAdapter.notifyItemRangeChanged(0, albumAdapter.getItemCount());
                break;
            case R.id.view_as_grid_palette:
                SettingsManager.getInstance().setAlbumDisplayType(ViewType.ALBUM_PALETTE);
                layoutManager.setSpanCount(SettingsManager.getInstance().getAlbumColumnCount(getResources()));
                albumAdapter.updateItemViewType();
                albumAdapter.notifyItemRangeChanged(0, albumAdapter.getItemCount());
                break;
        }

        if (item.getGroupId() == MENU_GROUP_GRID) {

            SettingsManager.getInstance().setAlbumColumnCount(item.getItemId() - 1000);

            if (SettingsManager.getInstance().getAlbumDisplayType() != ViewType.ALBUM_LIST) {
                ((GridLayoutManager) recyclerView.getLayoutManager()).setSpanCount(SettingsManager.getInstance().getAlbumColumnCount(getResources()));
                albumAdapter.notifyItemRangeChanged(0, albumAdapter.getItemCount());
            }
        }

        getActivity().supportInvalidateOptionsMenu();

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(View v, int position, Album album) {
        if (inActionMode) {
            multiSelector.setSelected(position, albumAdapter.getItemId(position), !multiSelector.isSelected(position, albumAdapter.getItemId(position)));

            if (multiSelector.getSelectedPositions().size() == 0) {
                if (actionMode != null) {
                    actionMode.finish();
                }
            }

            updateActionModeSelectionCount();
        } else {
            albumClickListener.onItemClicked(album, v.findViewById(R.id.image));
        }
    }

    @Override
    public void onOverflowClick(View v, int position, Album album) {
        PopupMenu menu = new PopupMenu(AlbumFragment.this.getActivity(), v);
        MenuUtils.addAlbumMenuOptions(getActivity(), menu);
        MenuUtils.addClickHandler((AppCompatActivity) getActivity(), menu, album);
        menu.show();
    }

    @Override
    public void onLongClick(View v, int position, Album album) {
        if (inActionMode) {
            return;
        }

        if (multiSelector.getSelectedPositions().size() == 0) {
            actionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(mActionModeCallback);
            inActionMode = true;
        }

        multiSelector.setSelected(position, albumAdapter.getItemId(position), !multiSelector.isSelected(position, albumAdapter.getItemId(position)));

        updateActionModeSelectionCount();
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        if (holder.getAdapterPosition() != -1) {
            albumAdapter.items.get(holder.getAdapterPosition()).recycle(holder);
        }
    }

    private void updateActionModeSelectionCount() {
        if (actionMode != null && multiSelector != null) {
            actionMode.setTitle(getString(R.string.action_mode_selection_count, multiSelector.getSelectedPositions().size()));
        }
    }

    private ActionMode.Callback mActionModeCallback = new ModalMultiSelectorCallback(multiSelector) {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            ThemeUtils.themeContextualActionBar(getActivity());
            inActionMode = true;
            MenuInflater inflater = getActivity().getMenuInflater();
            inflater.inflate(R.menu.context_menu_songs, menu);
            SubMenu sub = menu.getItem(0).getSubMenu();
            PlaylistUtils.makePlaylistMenu(AlbumFragment.this.getActivity(), sub, ALBUM_FRAGMENT_GROUP_ID);
            return true;
        }

        @Override
        public boolean onActionItemClicked(final ActionMode mode, MenuItem menuItem) {

            List<Album> checkedAlbums = getCheckedAlbums();

            if (checkedAlbums == null || checkedAlbums.size() == 0) {
                return true;
            }

            Observable<List<Song>> songsObservable = Observable.defer(() ->
                    Observable.merge(Stream.of(checkedAlbums)
                            .map(Album::getSongsObservable)
                            .collect(Collectors.toList()))
                            .reduce((songs, songs2) -> Stream.concat(Stream.of(songs), Stream.of(songs2))
                                    .collect(Collectors.toList())));

            switch (menuItem.getItemId()) {
                case NEW_PLAYLIST:
                    songsObservable
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(songs -> PlaylistUtils.createPlaylistDialog(getActivity(), songs));
                    return true;
                case PLAYLIST_SELECTED:
                    songsObservable
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(songs -> {
                                Playlist playlist = (Playlist) menuItem.getIntent().getSerializableExtra(ShuttleUtils.ARG_PLAYLIST);
                                PlaylistUtils.addToPlaylist(getContext(), playlist, songs);
                            });
                    return true;
                case R.id.delete: {
                    new DialogUtils.DeleteDialogBuilder()
                            .context(getContext())
                            .singleMessageId(R.string.delete_album_artist_desc)
                            .multipleMessage(R.string.delete_album_artist_desc_multiple)
                            .itemNames(Stream.of(checkedAlbums)
                                    .map(album -> album.name)
                                    .collect(Collectors.toList()))
                            .songsToDelete(songsObservable)
                            .build()
                            .show();
                    mode.finish();
                    return true;
                }
                case R.id.menu_add_to_queue: {
                    songsObservable
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(songs -> MusicUtils.addToQueue(getActivity(), songs));
                    break;
                }
            }
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            super.onDestroyActionMode(actionMode);
            inActionMode = false;
            AlbumFragment.this.actionMode = null;
            multiSelector.clearSelections();
        }
    };

    List<Album> getCheckedAlbums() {
        return Stream.of(multiSelector.getSelectedPositions())
                .map(i -> ((AlbumView) albumAdapter.items.get(i)).album)
                .collect(Collectors.toList());

    }

    @Override
    protected String screenName() {
        return TAG;
    }
}
