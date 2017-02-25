package com.simplecity.amp_library.ui.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.bignerdranch.android.multiselector.ModalMultiSelectorCallback;
import com.bignerdranch.android.multiselector.MultiSelector;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.model.AdaptableItem;
import com.simplecity.amp_library.model.Playlist;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.sql.databases.BlacklistHelper;
import com.simplecity.amp_library.ui.adapters.SongAdapter;
import com.simplecity.amp_library.ui.modelviews.EmptyView;
import com.simplecity.amp_library.ui.modelviews.ShuffleView;
import com.simplecity.amp_library.ui.modelviews.SongView;
import com.simplecity.amp_library.utils.ColorUtils;
import com.simplecity.amp_library.utils.DataManager;
import com.simplecity.amp_library.utils.DialogUtils;
import com.simplecity.amp_library.utils.MenuUtils;
import com.simplecity.amp_library.utils.MusicUtils;
import com.simplecity.amp_library.utils.PermissionUtils;
import com.simplecity.amp_library.utils.PlaylistUtils;
import com.simplecity.amp_library.utils.ShuttleUtils;
import com.simplecity.amp_library.utils.SortManager;
import com.simplecity.amp_library.utils.ThemeUtils;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import java.util.Collections;
import java.util.List;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

public class SongFragment extends BaseFragment implements
        MusicUtils.Defs,
        RecyclerView.RecyclerListener,
        SongAdapter.SongListener {

    private static final String TAG = "SongFragment";

    private static final String ARG_PAGE_TITLE = "page_title";

    private SharedPreferences mPrefs;

    private FastScrollRecyclerView mRecyclerView;

    private SongAdapter songsAdapter;

    MultiSelector multiSelector = new MultiSelector();

    ActionMode actionMode;

    boolean inActionMode = false;

    private BroadcastReceiver mReceiver;

    private SharedPreferences.OnSharedPreferenceChangeListener mSharedPreferenceChangeListener;

    private boolean sortOrderChanged = false;

    private Subscription subscription;

    private ShuffleView shuffleView;

    public SongFragment() {

    }

    public static SongFragment newInstance(String pageTitle) {

        SongFragment fragment = new SongFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PAGE_TITLE, pageTitle);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(final Bundle icicle) {
        super.onCreate(icicle);

        setHasOptionsMenu(true);

        songsAdapter = new SongAdapter();
        songsAdapter.setListener(this);

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
                songsAdapter.notifyItemRangeChanged(0, songsAdapter.getItemCount());
                themeUIComponents();
            } else if (key.equals("songWhitelist")) {
                refreshAdapterItems();
            }
        };

        mPrefs.registerOnSharedPreferenceChangeListener(mSharedPreferenceChangeListener);

        shuffleView = new ShuffleView();
    }

    private void themeUIComponents() {
        ThemeUtils.themeRecyclerView(mRecyclerView);
        mRecyclerView.setThumbColor(ColorUtils.getAccentColor());
        mRecyclerView.setPopupBgColor(ColorUtils.getAccentColor());
        mRecyclerView.setPopupTextColor(ColorUtils.getAccentColorSensitiveTextColor(getContext()));

        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                ThemeUtils.themeRecyclerView(recyclerView);
                super.onScrollStateChanged(recyclerView, newState);
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        if (mRecyclerView == null) {

            mRecyclerView = (FastScrollRecyclerView) inflater.inflate(R.layout.fragment_recycler, container, false);
            mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            mRecyclerView.setRecyclerListener(this);
            mRecyclerView.setAdapter(songsAdapter);

            themeUIComponents();
        }

        return mRecyclerView;
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

                        boolean ascending = SortManager.getInstance().getSongsAscending();

                        subscription = DataManager.getInstance().getSongsRelay()
                                .flatMap(songs -> {
                                    //Sort
                                    SortManager.getInstance().sortSongs(songs);
                                    //Reverse if required
                                    if (!ascending) {
                                        Collections.reverse(songs);
                                    }
                                    return Observable.from(songs)
                                            .map(song -> (AdaptableItem) new SongView(song, multiSelector, null))
                                            .toList();
                                })
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(items -> {

                                    if (items.isEmpty()) {
                                        songsAdapter.setEmpty(new EmptyView(R.string.empty_songlist));
                                    } else {
                                        items.add(0, shuffleView);
                                        songsAdapter.setItems(items);
                                    }

                                    //Move the RV back to the top if we've had a sort order change.
                                    if (sortOrderChanged) {
                                        mRecyclerView.scrollToPosition(0);
                                    }

                                    sortOrderChanged = false;
                                });
                    }
                }
        );
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.menu_sort_songs, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        int sortOrder = SortManager.getInstance().getSongsSortOrder();

        switch (sortOrder) {
            case SortManager.SongSort.DEFAULT:
                menu.findItem(R.id.sort_default).setChecked(true);
                break;
            case SortManager.SongSort.NAME:
                menu.findItem(R.id.sort_song_name).setChecked(true);
                break;
            case SortManager.SongSort.TRACK_NUMBER:
                menu.findItem(R.id.sort_song_track_number).setChecked(true);
                break;
            case SortManager.SongSort.DURATION:
                menu.findItem(R.id.sort_song_duration).setChecked(true);
                break;
            case SortManager.SongSort.DATE:
                menu.findItem(R.id.sort_song_date).setChecked(true);
                break;
            case SortManager.SongSort.YEAR:
                menu.findItem(R.id.sort_song_year).setChecked(true);
                break;
            case SortManager.SongSort.ALBUM_NAME:
                menu.findItem(R.id.sort_song_album_name).setChecked(true);
                break;
            case SortManager.SongSort.ARTIST_NAME:
                menu.findItem(R.id.sort_song_artist_name).setChecked(true);
                break;
        }

        menu.findItem(R.id.sort_ascending).setChecked(SortManager.getInstance().getSongsAscending());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.sort_default:
                SortManager.getInstance().setSongsSortOrder(SortManager.SongSort.DEFAULT);
                sortOrderChanged = true;
                break;
            case R.id.sort_song_name:
                SortManager.getInstance().setSongsSortOrder(SortManager.SongSort.NAME);
                sortOrderChanged = true;
                break;
            case R.id.sort_song_track_number:
                SortManager.getInstance().setSongsSortOrder(SortManager.SongSort.TRACK_NUMBER);
                sortOrderChanged = true;
                break;
            case R.id.sort_song_duration:
                SortManager.getInstance().setSongsSortOrder(SortManager.SongSort.DURATION);
                sortOrderChanged = true;
                break;
            case R.id.sort_song_year:
                SortManager.getInstance().setSongsSortOrder(SortManager.SongSort.YEAR);
                sortOrderChanged = true;
                break;
            case R.id.sort_song_date:
                SortManager.getInstance().setSongsSortOrder(SortManager.SongSort.DATE);
                sortOrderChanged = true;
                break;
            case R.id.sort_song_album_name:
                SortManager.getInstance().setSongsSortOrder(SortManager.SongSort.ALBUM_NAME);
                sortOrderChanged = true;
                break;
            case R.id.sort_song_artist_name:
                SortManager.getInstance().setSongsSortOrder(SortManager.SongSort.ARTIST_NAME);
                sortOrderChanged = true;
                break;
            case R.id.sort_ascending:
                SortManager.getInstance().setSongsAscending(!item.isChecked());
                sortOrderChanged = true;
                break;
        }

        if (sortOrderChanged) {
            refreshAdapterItems();
            getActivity().supportInvalidateOptionsMenu();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(View v, int position, Song song) {
        if (inActionMode) {
            multiSelector.setSelected(position, songsAdapter.getItemId(position), !multiSelector.isSelected(position, songsAdapter.getItemId(position)));

            if (multiSelector.getSelectedPositions().size() == 0) {
                if (actionMode != null) {
                    actionMode.finish();
                }
            }

            updateActionModeSelectionCount();
        } else {
            List<Song> songs = Stream.of(songsAdapter.items)
                    .filter(adaptableItem -> adaptableItem instanceof SongView)
                    .map(adaptableItem -> ((SongView) adaptableItem).song)
                    .collect(Collectors.toList());

            int pos = songs.indexOf(song);

            MusicUtils.playAll(songs, pos, () -> {
                final String message = getContext().getString(R.string.emptyplaylist);
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            });
        }
    }

    @Override
    public void onOverflowClick(View v, int position, final Song song) {
        PopupMenu menu = new PopupMenu(SongFragment.this.getActivity(), v);
        MenuUtils.addSongMenuOptions(getActivity(), menu);
        MenuUtils.addClickHandler((AppCompatActivity) getActivity(), menu, song, item -> {
            switch (item.getItemId()) {
                case BLACKLIST: {
                    BlacklistHelper.addToBlacklist(song);
                    return true;
                }
            }
            return false;
        });
        menu.show();
    }

    @Override
    public void onLongClick(View v, int position, Song song) {
        if (inActionMode) {
            return;
        }

        if (multiSelector.getSelectedPositions().size() == 0) {
            actionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(mActionModeCallback);
            inActionMode = true;
        }

        multiSelector.setSelected(position, songsAdapter.getItemId(position), !multiSelector.isSelected(position, songsAdapter.getItemId(position)));

        updateActionModeSelectionCount();
    }

    @Override
    public void onShuffleClick() {
        MusicUtils.shuffleAll(getContext());
    }

    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        //Nothing to do.
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
            getActivity().getMenuInflater().inflate(R.menu.context_menu_songs, menu);
            SubMenu sub = menu.getItem(0).getSubMenu();
            PlaylistUtils.makePlaylistMenu(getActivity(), sub, SONG_FRAGMENT_GROUP_ID);
            return true;
        }

        @Override
        public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {

            final List<Song> checkedSongs = getCheckedSongs();

            if (checkedSongs == null || checkedSongs.size() == 0) {
                return true;
            }

            switch (item.getItemId()) {
                case NEW_PLAYLIST:
                    PlaylistUtils.createPlaylistDialog(getActivity(), checkedSongs);
                    break;
                case PLAYLIST_SELECTED:
                    Playlist playlist = (Playlist) item.getIntent().getSerializableExtra(ShuttleUtils.ARG_PLAYLIST);
                    PlaylistUtils.addToPlaylist(getContext(), playlist, checkedSongs);
                    break;
                case R.id.delete:
                    new DialogUtils.DeleteDialogBuilder()
                            .context(getContext())
                            .singleMessageId(R.string.delete_song_desc)
                            .multipleMessage(R.string.delete_song_desc_multiple)
                            .itemNames(Stream.of(checkedSongs)
                                    .map(song -> song.name)
                                    .collect(Collectors.toList()))
                            .songsToDelete(Observable.just(checkedSongs))
                            .build()
                            .show();
                    mode.finish();
                    break;
                case R.id.menu_add_to_queue:
                    MusicUtils.addToQueue(SongFragment.this.getActivity(), checkedSongs);
                    break;
            }
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            super.onDestroyActionMode(actionMode);
            inActionMode = false;
            SongFragment.this.actionMode = null;
            multiSelector.clearSelections();
        }
    };

    List<Song> getCheckedSongs() {
        return Stream.of(multiSelector.getSelectedPositions())
                .map(i -> songsAdapter.getSong(i))
                .collect(Collectors.toList());
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        if (holder.getAdapterPosition() != -1) {
            songsAdapter.items.get(holder.getAdapterPosition()).recycle(holder);
        }
    }

    @Override
    protected String screenName() {
        return TAG;
    }
}