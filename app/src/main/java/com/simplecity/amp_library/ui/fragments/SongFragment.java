package com.simplecity.amp_library.ui.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.annimon.stream.Stream;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.ui.adapters.SectionedAdapter;
import com.simplecity.amp_library.ui.modelviews.EmptyView;
import com.simplecity.amp_library.ui.modelviews.SelectableViewModel;
import com.simplecity.amp_library.ui.modelviews.ShuffleView;
import com.simplecity.amp_library.ui.modelviews.SongView;
import com.simplecity.amp_library.ui.views.ContextualToolbar;
import com.simplecity.amp_library.utils.ContextualToolbarHelper;
import com.simplecity.amp_library.utils.DataManager;
import com.simplecity.amp_library.utils.LogUtils;
import com.simplecity.amp_library.utils.PermissionUtils;
import com.simplecity.amp_library.utils.PlaylistUtils;
import com.simplecity.amp_library.utils.SettingsManager;
import com.simplecity.amp_library.utils.menu.song.SongMenuCallbacksAdapter;
import com.simplecity.amp_library.utils.menu.song.SongMenuUtils;
import com.simplecity.amp_library.utils.sorting.SongSortHelper;
import com.simplecity.amp_library.utils.sorting.SortManager;
import com.simplecityapps.recycler_adapter.model.ViewModel;
import com.simplecityapps.recycler_adapter.recyclerview.RecyclerListener;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import kotlin.Unit;

public class SongFragment extends BaseFragment implements
        SongView.ClickListener,
        ShuffleView.ShuffleClickListener {

    private static final String TAG = "SongFragment";

    private static final String ARG_PAGE_TITLE = "page_title";

    private FastScrollRecyclerView recyclerView;

    SectionedAdapter adapter;

    private boolean sortOrderChanged = false;

    private ShuffleView shuffleView;

    private ContextualToolbarHelper<Song> contextualToolbarHelper;

    @Nullable
    private Disposable refreshDisposable;

    @Nullable
    private Disposable playlistMenuDisposable;

    private CompositeDisposable menuDisposables = new CompositeDisposable();

    private RequestManager requestManager;

    private SongMenuCallbacksAdapter songMenuCallbacksAdapter = new SongMenuCallbacksAdapter(this, menuDisposables);

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

        adapter = new SectionedAdapter();

        shuffleView = new ShuffleView();
        shuffleView.setClickListener(this);

        requestManager = Glide.with(this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (recyclerView == null) {
            recyclerView = (FastScrollRecyclerView) inflater.inflate(R.layout.fragment_recycler, container, false);
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            recyclerView.setRecyclerListener(new RecyclerListener());
        }
        if (recyclerView.getAdapter() != adapter) {
            recyclerView.setAdapter(adapter);
        }
        return recyclerView;
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

                        boolean ascending = SortManager.getInstance().getSongsAscending();
                        boolean showArtwork = SettingsManager.getInstance().showArtworkInSongList();

                        refreshDisposable = DataManager.getInstance().getSongsRelay()
                                .skipWhile(songs -> !force && Stream.of(adapter.items).filter(viewModel -> viewModel instanceof SongView).count() == songs.size())
                                .debounce(150, TimeUnit.MILLISECONDS)
                                .flatMapSingle(songs -> {
                                    //Sort
                                    SortManager.getInstance().sortSongs(songs);
                                    //Reverse if required
                                    if (!ascending) {
                                        Collections.reverse(songs);
                                    }
                                    return Observable.fromIterable(songs)
                                            .map(song -> {
                                                // Look for an existing SongView wrapping the song, we'll reuse it if it exists.
                                                SongView songView = (SongView) Stream.of(adapter.items)
                                                        .filter(viewModel -> viewModel instanceof SongView
                                                                && (((SongView) viewModel).song.equals(song))
                                                                && ((SongView) viewModel).getShowAlbumArt() == showArtwork)
                                                        .findFirst()
                                                        .orElse(null);

                                                if (songView == null) {
                                                    songView = new SongView(song, requestManager);
                                                    songView.setClickListener(this);
                                                }
                                                songView.showAlbumArt(showArtwork);

                                                return (ViewModel) songView;
                                            })
                                            .toList();
                                })
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(items -> {

                                    if (items.isEmpty()) {
                                        adapter.setItems(Collections.singletonList(new EmptyView(R.string.empty_songlist)));
                                    } else {
                                        items.add(0, shuffleView);
                                        adapter.setItems(items);
                                    }

                                    //Move the RV back to the top if we've had a sort order change.
                                    if (sortOrderChanged) {
                                        recyclerView.scrollToPosition(0);
                                    }

                                    sortOrderChanged = false;
                                }, error -> LogUtils.logException(TAG, "Error refreshing adapter items", error));
                    }
                }
        );
    }

    @Override
    public void onPause() {

        if (refreshDisposable != null) {
            refreshDisposable.dispose();
        }

        if (playlistMenuDisposable != null) {
            playlistMenuDisposable.dispose();
        }

        menuDisposables.clear();

        super.onPause();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.menu_sort_songs, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        SongSortHelper.updateSongSortMenuItems(menu, SortManager.getInstance().getSongsSortOrder(), SortManager.getInstance().getSongsAscending());

        menu.findItem(R.id.showArtwork).setChecked(SettingsManager.getInstance().showArtworkInSongList());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Integer songSortOder = SongSortHelper.handleSongMenuSortOrderClicks(item);
        if (songSortOder != null) {
            SortManager.getInstance().setSongsSortOrder(songSortOder);
            refreshAdapterItems(true);
            getActivity().invalidateOptionsMenu();
            return true;
        }
        Boolean songsAsc = SongSortHelper.handleSongDetailMenuSortOrderAscClicks(item);
        if (songsAsc != null) {
            SortManager.getInstance().setSongsAscending(songsAsc);
            refreshAdapterItems(true);
            getActivity().invalidateOptionsMenu();
            return true;
        }

        if (item.getItemId() == R.id.showArtwork) {
            SettingsManager.getInstance().setShowArtworkInSongList(!item.isChecked());
            refreshAdapterItems(true);
            getActivity().invalidateOptionsMenu();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSongClick(int position, SongView songView) {
        if (!contextualToolbarHelper.handleClick(songView, songView.song)) {
            List<Song> songs = Stream.of(adapter.items)
                    .filter(adaptableItem -> adaptableItem instanceof SongView)
                    .map(adaptableItem -> ((SongView) adaptableItem).song)
                    .toList();

            mediaManager.playAll(songs, songs.indexOf(songView.song), true, message -> {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                return Unit.INSTANCE;
            });
        }
    }

    @Override
    public void onSongOverflowClick(int position, View view, Song song) {
        PopupMenu menu = new PopupMenu(getContext(), view);
        SongMenuUtils.INSTANCE.setupSongMenu(menu, false);
        menu.setOnMenuItemClickListener(SongMenuUtils.INSTANCE.getSongMenuClickListener(song, songMenuCallbacksAdapter));
        menu.show();
    }

    @Override
    public boolean onSongLongClick(int position, SongView songView) {
        return contextualToolbarHelper.handleLongClick(songView, songView.song);
    }

    @Override
    public void onStartDrag(SongView.ViewHolder viewHolder) {
        // Nothing to do
    }

    @Override
    public void onShuffleItemClick() {
        mediaManager.shuffleAll(DataManager.getInstance().getSongsRelay().firstOrError(), message -> {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            return Unit.INSTANCE;
        });
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

            contextualToolbar.setOnMenuItemClickListener(
                    SongMenuUtils.INSTANCE.getSongMenuClickListener(Single.defer(() -> Single.just(contextualToolbarHelper.getItems())), songMenuCallbacksAdapter));
        }
    }

    @Override
    protected String screenName() {
        return TAG;
    }
}
