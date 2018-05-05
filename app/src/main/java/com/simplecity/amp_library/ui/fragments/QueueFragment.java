package com.simplecity.amp_library.ui.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.afollestad.aesthetic.Aesthetic;
import com.afollestad.aesthetic.Util;
import com.afollestad.materialdialogs.MaterialDialog;
import com.annimon.stream.Stream;
import com.bumptech.glide.RequestManager;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.dagger.module.ActivityModule;
import com.simplecity.amp_library.dagger.module.FragmentModule;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.playback.MediaManager;
import com.simplecity.amp_library.tagger.TaggerDialog;
import com.simplecity.amp_library.ui.dialog.DeleteDialog;
import com.simplecity.amp_library.ui.dialog.UpgradeDialog;
import com.simplecity.amp_library.ui.modelviews.SelectableViewModel;
import com.simplecity.amp_library.ui.modelviews.SongView;
import com.simplecity.amp_library.ui.presenters.PlayerPresenter;
import com.simplecity.amp_library.ui.presenters.QueuePresenter;
import com.simplecity.amp_library.ui.recyclerview.ItemTouchHelperCallback;
import com.simplecity.amp_library.ui.views.ContextualToolbar;
import com.simplecity.amp_library.ui.views.PlayerViewAdapter;
import com.simplecity.amp_library.ui.views.QueueView;
import com.simplecity.amp_library.ui.views.ThemedStatusBarView;
import com.simplecity.amp_library.ui.views.multisheet.MultiSheetSlideEventRelay;
import com.simplecity.amp_library.utils.ContextualToolbarHelper;
import com.simplecity.amp_library.utils.ContextualToolbarHelper.Callback;
import com.simplecity.amp_library.utils.MusicUtils;
import com.simplecity.amp_library.utils.PermissionUtils;
import com.simplecity.amp_library.utils.PlaylistUtils;
import com.simplecity.amp_library.utils.ResourceUtils;
import com.simplecity.amp_library.utils.ShuttleUtils;
import com.simplecity.amp_library.utils.menu.song.SongMenuFragmentHelper;
import com.simplecity.amp_library.utils.menu.song.SongMenuUtils;
import com.simplecity.multisheetview.ui.view.MultiSheetView;
import com.simplecityapps.recycler_adapter.adapter.CompletionListUpdateCallbackAdapter;
import com.simplecityapps.recycler_adapter.adapter.ViewModelAdapter;
import com.simplecityapps.recycler_adapter.model.ViewModel;
import com.simplecityapps.recycler_adapter.recyclerview.RecyclerListener;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import java.util.List;
import javax.inject.Inject;

public class QueueFragment extends BaseFragment implements QueueView {

    private static final String TAG = "QueueFragment";

    private final CompositeDisposable disposables = new CompositeDisposable();

    private SongMenuFragmentHelper songMenuFragmentHelper;

    @BindView(R.id.statusBarView)
    ThemedStatusBarView statusBarView;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.line1)
    TextView lineOne;

    @BindView(R.id.line2)
    TextView lineTwo;

    @BindView(R.id.recyclerView)
    FastScrollRecyclerView recyclerView;

    @BindView(R.id.contextualToolbar)
    ContextualToolbar cabToolbar;

    @Inject
    RequestManager requestManager;

    @Inject
    MultiSheetSlideEventRelay multiSheetSlideEventRelay;

    @Inject
    PlayerPresenter playerPresenter;

    @Inject
    QueuePresenter queuePresenter;

    ItemTouchHelper itemTouchHelper;

    ViewModelAdapter adapter;

    ContextualToolbarHelper<Song> cabHelper;

    Disposable loadDataDisposable;

    Unbinder unbinder;

    public static QueueFragment newInstance() {
        Bundle args = new Bundle();
        QueueFragment fragment = new QueueFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ShuttleApplication.getInstance().getAppComponent()
                .plus(new ActivityModule(getActivity()))
                .plus(new FragmentModule(this))
                .inject(this);

        setHasOptionsMenu(true);

        songMenuFragmentHelper = new SongMenuFragmentHelper(this, disposables, songMenuCallbacks);

        adapter = new ViewModelAdapter();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_queue, container, false);

        unbinder = ButterKnife.bind(this, rootView);

        toolbar.setNavigationOnClickListener(v -> getActivity().onBackPressed());
        toolbar.inflateMenu(R.menu.menu_queue);

        SubMenu sub = toolbar.getMenu().addSubMenu(0, MediaManager.ADD_TO_PLAYLIST, 1, R.string.save_as_playlist);
        disposables.add(PlaylistUtils.createUpdatingPlaylistMenu(sub).subscribe());

        toolbar.setOnMenuItemClickListener(toolbarListener);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setRecyclerListener(new RecyclerListener());
        recyclerView.setAdapter(adapter);

        itemTouchHelper = new ItemTouchHelper(new ItemTouchHelperCallback(
                (fromPosition, toPosition) ->
                        adapter.moveItem(fromPosition, toPosition), mediaManager::moveQueueItem,
                () -> {
                    // Nothing to do
                }));

        itemTouchHelper.attachToRecyclerView(recyclerView);

        disposables.add(Aesthetic.get(getContext())
                .colorPrimary()
                .subscribe(color -> {
                    boolean isLight = Util.isColorLight(color);
                    lineOne.setTextColor(isLight ? Color.BLACK : Color.WHITE);
                    lineTwo.setTextColor(isLight ? Color.BLACK : Color.WHITE);
                }));

        // In landscape, we need to adjust the status bar's translation depending on the slide offset of the sheet
        if (ShuttleUtils.isLandscape()) {
            statusBarView.setTranslationY(ResourceUtils.toPixels(16));

            disposables.add(multiSheetSlideEventRelay.getEvents()
                    .filter(multiSheetEvent -> multiSheetEvent.sheet == MultiSheetView.Sheet.SECOND)
                    .filter(multiSheetEvent -> multiSheetEvent.slideOffset >= 0)
                    .subscribe(multiSheetEvent -> {
                        statusBarView.setTranslationY((1 - multiSheetEvent.slideOffset) * ResourceUtils.toPixels(16));
                    }));
        }

        setupContextualToolbar();
        return rootView;
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
        if (loadDataDisposable != null) {
            loadDataDisposable.dispose();
        }
        playerPresenter.unbindView(playerViewAdapter);
        queuePresenter.unbindView(this);
    }

    @Override
    public void onDestroyView() {
        disposables.clear();
        unbinder.unbind();
        super.onDestroyView();
    }

    private void setupContextualToolbar() {
        cabToolbar.getMenu().clear();
        cabToolbar.inflateMenu(R.menu.context_menu_queue);

        SubMenu sub = cabToolbar.getMenu().findItem(R.id.queue_add_to_playlist).getSubMenu();
        disposables.add(PlaylistUtils.createUpdatingPlaylistMenu(sub).subscribe());
        cabToolbar.setOnMenuItemClickListener(SongMenuUtils.getQueueMenuClickListener(getContext(), Single.fromCallable(() -> cabHelper.getItems()), songMenuFragmentHelper.getSongMenuCallbacks()));

        cabHelper = new ContextualToolbarHelper<>(cabToolbar, new Callback() {
            @Override
            public void notifyItemChanged(int position, SelectableViewModel viewModel) {
                adapter.notifyItemChanged(position, 0);
            }

            @Override
            public void notifyDatasetChanged() {
                adapter.notifyItemRangeChanged(0, adapter.items.size(), 0);
            }
        });
    }

    @Override
    protected String screenName() {
        return TAG;
    }

    @Override
    public void setData(List<Song> songs, int position) {
        PermissionUtils.RequestStoragePermissions(() -> {
            if (getActivity() != null && isAdded()) {
                if (loadDataDisposable != null) {
                    loadDataDisposable.dispose();
                }

                List<ViewModel> songViews = Stream.of(songs).map(song -> {
                    SongView songView = new SongView(song, requestManager) {
                        @Override
                        public boolean equals(Object o) {
                            // It's possible to have multiple SongViews with the same (duplicate) songs in the queue.
                            // When that occurs, there's not currently a way to tell the two SongViews apart - which
                            // can result in an adapter inconsistency. This fix just ensures no two SongViews in the queue
                            // are considered to be the same. We lose some RV optimisations here, but at least we don't crash.
                            return false;
                        }
                    };
                    songView.setClickListener(songClickListener);
                    songView.showAlbumArt(true);
                    songView.setEditable(true);

                    return (ViewModel) songView;
                }).toList();

                loadDataDisposable = adapter.setItems(songViews, new CompletionListUpdateCallbackAdapter() {
                    @Override
                    public void onComplete() {
                        updateQueuePosition(position, false);
                        if (recyclerView != null) {
                            recyclerView.scrollToPosition(position);
                        }
                    }
                });
            }
        });
    }

    private SongView.ClickListener songClickListener = new SongView.ClickListener() {
        @Override
        public void onSongClick(int position, SongView songView) {
            if (!cabHelper.handleClick(position, songView, songView.song)) {
                queuePresenter.onSongClick(position);
            }
        }

        @Override
        public boolean onSongLongClick(int position, SongView songView) {
            return cabHelper.handleLongClick(position, songView, songView.song);
        }

        @Override
        public void onSongOverflowClick(int position, View v, Song song) {
            PopupMenu menu = new PopupMenu(v.getContext(), v);
            SongMenuUtils.setupSongMenu(menu, true);
            menu.setOnMenuItemClickListener(SongMenuUtils.getSongMenuClickListener(v.getContext(), mediaManager, position, song, songMenuFragmentHelper.getSongMenuCallbacks()));
            menu.show();
        }

        @Override
        public void onStartDrag(SongView.ViewHolder holder) {
            itemTouchHelper.startDrag(holder);
        }
    };

    @Override
    public void showToast(String message, int duration) {
        Toast.makeText(getContext(), message, duration).show();
    }

    @Override
    public void updateQueuePosition(int position, boolean fromUser) {
        if (adapter.items.isEmpty() || position >= adapter.items.size() || position < 0) {
            return;
        }
        if (recyclerView == null) {
            return;
        }
        if (!fromUser) {
            recyclerView.scrollToPosition(position);
        }
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

    @Override
    public void showTaggerDialog(TaggerDialog taggerDialog) {
        taggerDialog.show(getChildFragmentManager());
    }

    @Override
    public void showDeleteDialog(DeleteDialog deleteDialog) {
        deleteDialog.show(getChildFragmentManager());
    }

    @Override
    public void onRemovedFromQueue(int position) {
        adapter.removeItem(position);
    }

    @Override
    public void showUpgradeDialog() {
        UpgradeDialog.getUpgradeDialog(getActivity()).show();
    }

    private final PlayerViewAdapter playerViewAdapter = new PlayerViewAdapter() {
        @Override
        public void trackInfoChanged(@Nullable Song song) {
            if (song != null) {
                lineOne.setText(song.name);
                if (song.albumArtistName != null && song.albumName != null) {
                    lineTwo.setText(String.format("%s | %s", song.albumArtistName, song.albumName));
                }
            }
        }

        @Override
        public void showUpgradeDialog(MaterialDialog dialog) {
            dialog.show();
        }
    };

    Toolbar.OnMenuItemClickListener toolbarListener = item -> {
        switch (item.getItemId()) {
            case R.id.menu_clear:
                queuePresenter.clearQueue();
                return true;
            case MediaManager.NEW_PLAYLIST:
                queuePresenter.saveQueue(getContext());
                return true;
            case MediaManager.PLAYLIST_SELECTED:
                queuePresenter.saveQueue(getContext(), item);
                return true;
        }
        return false;
    };

    private final SongMenuUtils.CallbacksAdapter songMenuCallbacks = new SongMenuUtils.CallbacksAdapter() {
        @Override
        public void onSongRemoved(int position, Song song) {
            super.onSongRemoved(position, song);

            queuePresenter.removeFromQueue(position);
        }
    };
}
