package com.simplecity.amp_library.ui.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
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
import com.bumptech.glide.RequestManager;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.model.Playlist;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.ui.modelviews.SongView;
import com.simplecity.amp_library.ui.presenters.PlayerPresenter;
import com.simplecity.amp_library.ui.presenters.QueuePresenter;
import com.simplecity.amp_library.ui.recyclerview.ItemTouchHelperCallback;
import com.simplecity.amp_library.ui.views.PlayerViewAdapter;
import com.simplecity.amp_library.ui.views.QueueView;
import com.simplecity.amp_library.utils.ColorUtils;
import com.simplecity.amp_library.utils.DialogUtils;
import com.simplecity.amp_library.utils.MusicUtils;
import com.simplecity.amp_library.utils.PermissionUtils;
import com.simplecity.amp_library.utils.PlaylistUtils;
import com.simplecity.amp_library.utils.ShuttleUtils;
import com.simplecity.amp_library.utils.ThemeUtils;
import com.simplecityapps.recycler_adapter.adapter.ViewModelAdapter;
import com.simplecityapps.recycler_adapter.model.ViewModel;
import com.simplecityapps.recycler_adapter.recyclerview.RecyclerListener;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;
import com.sothree.slidinguppanel.ScrollableViewHelper;

import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.Observable;
import test.com.multisheetview.ui.view.MultiSheetView;

public class QueueFragment extends BaseFragment implements
        MusicUtils.Defs, QueueView, Toolbar.OnMenuItemClickListener {

    private static final String TAG = "QueueFragment";

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.line1)
    TextView lineOne;

    @BindView(R.id.line2)
    TextView lineTwo;

    @BindView(R.id.recyclerView)
    FastScrollRecyclerView recyclerView;

    private SharedPreferences prefs;

    private ItemTouchHelper itemTouchHelper;

    private ViewModelAdapter adapter;

    private MultiSelector multiSelector = new MultiSelector();

    private ActionMode actionMode;

    boolean inActionMode = false;

    private SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener;

    @Inject
    RequestManager requestManager;

    @Inject QueuePresenter queuePresenter;

    @Inject PlayerPresenter playerPresenter;

    private boolean canScroll = true;

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
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ShuttleApplication.getInstance().getAppComponent().inject(this);

        setHasOptionsMenu(true);

        adapter = new ViewModelAdapter();

        prefs = PreferenceManager.getDefaultSharedPreferences(this.getActivity());

        sharedPreferenceChangeListener = (sharedPreferences, key) -> {
            if (key.equals("pref_theme_highlight_color") || key.equals("pref_theme_accent_color") || key.equals("pref_theme_white_accent")) {
                themeUIComponents();
            }
        };

        prefs.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_queue, container, false);

        ButterKnife.bind(this, rootView);

        toolbar.setNavigationOnClickListener(v -> getActivity().onBackPressed());
        toolbar.inflateMenu(R.menu.menu_queue);

        SubMenu sub = toolbar.getMenu().addSubMenu(0, ADD_TO_PLAYLIST, 1, R.string.save_as_playlist);
        PlaylistUtils.makePlaylistMenu(getContext(), sub, 0);

        toolbar.setOnMenuItemClickListener(this);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setRecyclerListener(new RecyclerListener());
        recyclerView.setAdapter(adapter);

        itemTouchHelper = new ItemTouchHelper(new ItemTouchHelperCallback(
                (fromPosition, toPosition) ->
                        adapter.moveItem(fromPosition, toPosition), MusicUtils::moveQueueItem,
                () -> {
                    //We've finished our drag event. Allow the sliding up panel to intercept touch events
                    canScroll = true;
                }));

        itemTouchHelper.attachToRecyclerView(recyclerView);

        themeUIComponents();

        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        MultiSheetView.setScrollableView(recyclerView, recyclerView);
        MultiSheetView.setScrollableViewHelper(recyclerView, new ScrollableViewHelper() {
            @Override
            public int getScrollableViewScrollPosition(View scrollableView, boolean isSlidingUp) {
                if (!canScroll) {
                    return 1;
                }
                return super.getScrollableViewScrollPosition(scrollableView, isSlidingUp);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        adapter.notifyItemRangeChanged(0, adapter.getItemCount());

        playerPresenter.bindView(playerViewAdapter);
        queuePresenter.bindView(this);
    }

    @Override
    public void onPause() {
        super.onPause();

        playerPresenter.unbindView(playerViewAdapter);
        queuePresenter.unbindView(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        MultiSheetView.setScrollableView(recyclerView, null);
        MultiSheetView.setScrollableViewHelper(recyclerView, null);
    }

    @Override
    public void onDestroy() {
        prefs.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
        super.onDestroy();
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

//        if (header != null) {
//            header.setBackgroundColor(ColorUtils.getPrimaryColor());
//        }
    }

//    @Override
//    public void onItemClick(View v, int position, Song song) {
//        if (inActionMode) {
//            multiSelector.setSelected(position, songAdapter.getItemId(position), !multiSelector.isSelected(position, songAdapter.getItemId(position)));
//            if (multiSelector.getSelectedPositions().size() == 0) {
//                if (actionMode != null) {
//                    actionMode.finish();
//                }
//            }
//        } else {
//            MusicUtils.setQueuePosition(position);
//            songAdapter.notifyDataSetChanged();
//        }
//    }

//    @Override
//    public void onOverflowClick(View v, int position, Song song) {
//        PopupMenu menu = new PopupMenu(QueueFragment.this.getActivity(), v);
//        MenuUtils.addQueueMenuOptions(getActivity(), menu);
//        MenuUtils.addClickHandler((AppCompatActivity) getActivity(), menu, song, item -> {
//            switch (item.getItemId()) {
//                case REMOVE: {
//                    MusicUtils.removeFromQueue(song, true);
//                    songAdapter.removeItem(position);
//                    return true;
//                }
//                case BLACKLIST: {
//                    BlacklistHelper.addToBlacklist(song);
//                    MusicUtils.removeFromQueue(song, true);
//                    songAdapter.removeItem(position);
//                    return true;
//                }
//            }
//            return false;
//        });
//
//        menu.show();
//    }

//    @Override
//    public void onAlbumLongClick(View v, int position, Song song) {
//        if (inActionMode) {
//            return;
//        }
//
//        if (multiSelector.getSelectedPositions().size() == 0) {
//            actionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(mActionModeCallback);
//            inActionMode = true;
//        }
//
//        multiSelector.setSelected(position, songAdapter.getItemId(position), !multiSelector.isSelected(position, songAdapter.getItemId(position)));
//    }

//    @Override
//    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
//        if (itemTouchHelper != null) {
//            //We've started a drag event, so don't allow the SlidingUpPanel to intercept touch events
////            recyclerView.setBlockScroll(false);
//            itemTouchHelper.onStartDrag(viewHolder);
//        }
//    }

    @Override
    protected String screenName() {
        return TAG;
    }

    @Override
    public void loadData(List<ViewModel> items, int position) {
        PermissionUtils.RequestStoragePermissions(() -> {
            if (getActivity() != null && isAdded()) {

                adapter.setItems(items);

                //Todo: Call after setItems() is complete.
                recyclerView.scrollToPosition(position);
            }
        });
    }

    @Override
    public void updateQueuePosition(int position) {
        recyclerView.scrollToPosition(position);
    }

    @Override
    public void showToast(String message, int duration) {

    }

    @Override
    public void startDrag(SongView.ViewHolder holder) {
        canScroll = false;
        itemTouchHelper.startDrag(holder);
    }

    @Override
    public void setCurrentQueueItem(int position) {

        int prevPosition = -1;
        int len = adapter.items.size();
        for (int i = 0; i < len; i++) {
            ViewModel viewModel = adapter.items.get(i);
            if (viewModel instanceof SongView) {
                if (((SongView) viewModel).isCurrentTrack()) {
                    prevPosition = i;
                }
                ((SongView) viewModel).setCurrentTrack(i == position);
            }
        }

        ((SongView) adapter.items.get(position)).setCurrentTrack(true);

        adapter.notifyItemChanged(prevPosition, 1);
        adapter.notifyItemChanged(position, 1);
    }

    private PlayerViewAdapter playerViewAdapter = new PlayerViewAdapter() {
        @Override
        public void trackInfoChanged(@Nullable Song song) {
            if (song != null) {
                lineOne.setText(song.name);
                if (song.albumArtistName != null && song.albumName != null) {
                    lineTwo.setText(String.format("%s | %s", song.albumArtistName, song.albumName));
                }
            }
        }
    };


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
            return null;

//            Stream.of(multiSelector.getSelectedPositions())
//                    .map(i -> adapter.getSong(i))
//                    .collect(Collectors.toList());
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
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_clear:
                queuePresenter.clearQueue();
                return true;
            case NEW_PLAYLIST: {
                queuePresenter.saveQueue(getContext());
                return true;
            }
            case PLAYLIST_SELECTED: {
                queuePresenter.saveQueue(getContext(), (Playlist) item.getIntent().getSerializableExtra(ShuttleUtils.ARG_PLAYLIST));
                return true;
            }
        }
        return false;
    }
}