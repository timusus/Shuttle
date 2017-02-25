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
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.bignerdranch.android.multiselector.ModalMultiSelectorCallback;
import com.bignerdranch.android.multiselector.MultiSelector;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.model.Playlist;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.playback.MusicService;
import com.simplecity.amp_library.sql.databases.BlacklistHelper;
import com.simplecity.amp_library.ui.activities.MainActivity;
import com.simplecity.amp_library.ui.adapters.QueueAdapter;
import com.simplecity.amp_library.ui.adapters.SongAdapter;
import com.simplecity.amp_library.ui.modelviews.SongView;
import com.simplecity.amp_library.ui.recyclerview.ItemTouchHelperCallback;
import com.simplecity.amp_library.ui.recyclerview.ScrollBlockingRecyclerView;
import com.simplecity.amp_library.utils.ColorUtils;
import com.simplecity.amp_library.utils.DialogUtils;
import com.simplecity.amp_library.utils.MenuUtils;
import com.simplecity.amp_library.utils.MusicUtils;
import com.simplecity.amp_library.utils.PermissionUtils;
import com.simplecity.amp_library.utils.PlaylistUtils;
import com.simplecity.amp_library.utils.ShuttleUtils;
import com.simplecity.amp_library.utils.ThemeUtils;
import com.simplecityapps.recyclerview_fastscroll.interfaces.OnFastScrollStateChangeListener;

import java.util.List;

import rx.Observable;

public class QueueFragment extends BaseFragment implements
        MusicUtils.Defs,
        SongAdapter.SongListener,
        RecyclerView.RecyclerListener {

    private static final String TAG = "QueueFragment";

    public static final String UPDATE_QUEUE_FRAGMENT = "update_queue_fragment";

    private SharedPreferences prefs;

    private View rootView;

    private ScrollBlockingRecyclerView recyclerView;

    private ItemTouchHelper itemTouchHelper;

    QueueAdapter songAdapter;

    private View header;

    private TextView lineTwo;

    MultiSelector multiSelector = new MultiSelector();

    ActionMode actionMode;

    boolean inActionMode = false;

    private BroadcastReceiver receiver;

    private SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener;

    private RequestManager requestManager;

    public static QueueFragment newInstance() {

        Bundle args = new Bundle();

        QueueFragment fragment = new QueueFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public QueueFragment() {

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).toggleQueue(true);
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        songAdapter = new QueueAdapter();
        songAdapter.setListener(this);

        prefs = PreferenceManager.getDefaultSharedPreferences(this.getActivity());

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();
                if (action != null) {
                    switch (action) {
                        case MusicService.InternalIntents.META_CHANGED:
                            updateTrackInfo();
                            songAdapter.notifyDataSetChanged();
                            break;
                        case MusicService.InternalIntents.QUEUE_CHANGED:
                            if (intent.getBooleanExtra(MusicService.FROM_USER, false)) {
                            } else {
                                refreshAdapterItems();
                            }
                            break;
                        case MusicService.InternalIntents.SHUFFLE_CHANGED:
                            refreshAdapterItems();
                            break;
                        case MusicService.InternalIntents.PLAY_STATE_CHANGED:
                        case UPDATE_QUEUE_FRAGMENT:
                            updateTrackInfo();
                            if (songAdapter != null) {
                                songAdapter.notifyDataSetChanged();
                            }
                            break;
                    }
                }
            }
        };

        sharedPreferenceChangeListener = (sharedPreferences, key) -> {
            if (key.equals("pref_theme_highlight_color") || key.equals("pref_theme_accent_color") || key.equals("pref_theme_white_accent")) {
                themeUIComponents();
            }
        };

        prefs.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);

        if (requestManager == null) {
            requestManager = Glide.with(this);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_queue, container, false);

        header = rootView.findViewById(R.id.header);
        if (ShuttleUtils.isLandscape() || ShuttleUtils.isTablet()) {
            header.setVisibility(View.GONE);
        } else {
            header.setVisibility(View.VISIBLE);
        }

        lineTwo = (TextView) rootView.findViewById(R.id.line2);

        recyclerView = (ScrollBlockingRecyclerView) rootView.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setRecyclerListener(this);
        recyclerView.setAdapter(songAdapter);
        recyclerView.setStateChangeListener(new OnFastScrollStateChangeListener() {
            @Override
            public void onFastScrollStart() {
                recyclerView.setBlockScroll(false);
            }

            @Override
            public void onFastScrollStop() {
                recyclerView.setBlockScroll(true);
            }
        });

        itemTouchHelper = new ItemTouchHelper(new ItemTouchHelperCallback((fromPosition, toPosition) -> {
            songAdapter.moveItem(fromPosition, toPosition);
        }, (fromPosition, toPosition) -> {
            MusicUtils.moveQueueItem(fromPosition, toPosition);
            recyclerView.setBlockScroll(true);
        }, () -> {
            //We've finished our drag event. Allow the sliding up panel to intercept touch events
            recyclerView.setBlockScroll(true);
        }));

        itemTouchHelper.attachToRecyclerView(recyclerView);

        themeUIComponents();

        updateTrackInfo();

        return rootView;
    }

    @Override
    public void onPause() {
        if (receiver != null) {
            getActivity().unregisterReceiver(receiver);
        }

        if (getParentFragment() != null && getParentFragment() instanceof PlayerFragment) {
            ((PlayerFragment) getParentFragment()).setDragView(null);
        }

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setScrollableView(null);
        }

        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        songAdapter.notifyItemRangeChanged(0, songAdapter.getItemCount());

        IntentFilter filter = new IntentFilter();
        filter.addAction(MusicService.InternalIntents.META_CHANGED);
        filter.addAction(MusicService.InternalIntents.QUEUE_CHANGED);
        filter.addAction(MusicService.InternalIntents.SHUFFLE_CHANGED);
        filter.addAction(MusicService.InternalIntents.PLAY_STATE_CHANGED);
        filter.addAction(UPDATE_QUEUE_FRAGMENT);
        getActivity().registerReceiver(receiver, filter);

        updateTrackInfo();

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setScrollableView(recyclerView);
        }

        if (getParentFragment() != null && getParentFragment() instanceof PlayerFragment) {
            ((PlayerFragment) getParentFragment()).setDragView(rootView);
        }

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

                int count = songAdapter.items.size();
                songAdapter.items.clear();
                songAdapter.notifyItemRangeRemoved(0, count);
                songAdapter.items.addAll(Stream.of(MusicUtils.getQueue())
                        .map(song -> {
                            SongView songView = new SongView(song, multiSelector, requestManager);
                            songView.setShowAlbumArt(true);
                            songView.setEditable(true);
                            return songView;
                        }).collect(Collectors.toList()));
                songAdapter.notifyItemRangeInserted(0, songAdapter.items.size());

                scrollToCurrentItem();
            }
        });
    }

    void updateTrackInfo() {
        if (lineTwo != null) {
            String trackName = MusicUtils.getSongName();
            String artistName = MusicUtils.getAlbumArtistName();
            if (trackName != null) {
                lineTwo.setText(MusicUtils.getSongName());
                if (artistName != null) {
                    lineTwo.setText(String.format("%s | %s", trackName, artistName));
                }
            }
        }
    }

    private void themeUIComponents() {
        ThemeUtils.themeRecyclerView(recyclerView);
        recyclerView.setThumbColor(ColorUtils.getAccentColor());
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                ThemeUtils.themeRecyclerView(recyclerView);
                super.onScrollStateChanged(recyclerView, newState);
            }
        });

        if (header != null) {
            header.setBackgroundColor(ColorUtils.getPrimaryColor());
        }
    }

    @Override
    public void onItemClick(View v, int position, Song song) {
        if (inActionMode) {
            multiSelector.setSelected(position, songAdapter.getItemId(position), !multiSelector.isSelected(position, songAdapter.getItemId(position)));
            if (multiSelector.getSelectedPositions().size() == 0) {
                if (actionMode != null) {
                    actionMode.finish();
                }
            }
        } else {
            MusicUtils.setQueuePosition(position);
            songAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onOverflowClick(View v, int position, Song song) {
        PopupMenu menu = new PopupMenu(QueueFragment.this.getActivity(), v);
        MenuUtils.addQueueMenuOptions(getActivity(), menu);
        MenuUtils.addClickHandler((AppCompatActivity) getActivity(), menu, song, item -> {
            switch (item.getItemId()) {
                case REMOVE: {
                    MusicUtils.removeFromQueue(song, true);
                    songAdapter.removeItem(position);
                    return true;
                }
                case BLACKLIST: {
                    BlacklistHelper.addToBlacklist(song);
                    MusicUtils.removeFromQueue(song, true);
                    songAdapter.removeItem(position);
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

        multiSelector.setSelected(position, songAdapter.getItemId(position), !multiSelector.isSelected(position, songAdapter.getItemId(position)));
    }

    @Override
    public void onShuffleClick() {
        //Nothing to do
    }

    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        if (itemTouchHelper != null) {
            //We've started a drag event, so don't allow the SlidingUpPanel to intercept touch events
            recyclerView.setBlockScroll(false);
            itemTouchHelper.startDrag(viewHolder);
        }
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        if (holder.getAdapterPosition() != -1) {
            songAdapter.items.get(holder.getAdapterPosition()).recycle(holder);
        }
    }

    public void scrollToCurrentItem() {
        recyclerView.scrollToPosition(MusicUtils.getQueuePosition());
    }

    private ActionMode.Callback mActionModeCallback = new ModalMultiSelectorCallback(multiSelector) {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            ThemeUtils.themeContextualActionBar(getActivity());
            inActionMode = true;
            MenuInflater inflater = getActivity().getMenuInflater();
            inflater.inflate(R.menu.context_menu_queue, menu);
            SubMenu sub = menu.getItem(0).getSubMenu();
            PlaylistUtils.makePlaylistMenu(QueueFragment.this.getActivity(), sub, SONG_FRAGMENT_GROUP_ID);
            return true;
        }

        private List<Song> getCheckedSongs() {
            return Stream.of(multiSelector.getSelectedPositions())
                    .map(i -> songAdapter.getSong(i))
                    .collect(Collectors.toList());
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
            }
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            super.onDestroyActionMode(actionMode);
            inActionMode = false;
            QueueFragment.this.actionMode = null;
            multiSelector.clearSelections();
        }
    };

    @Override
    protected String screenName() {
        return TAG;
    }
}
