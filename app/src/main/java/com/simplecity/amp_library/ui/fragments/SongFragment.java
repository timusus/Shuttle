package com.simplecity.amp_library.ui.fragments;

import android.os.Bundle;
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

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
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
import com.simplecity.amp_library.utils.MenuUtils;
import com.simplecity.amp_library.utils.MusicUtils;
import com.simplecity.amp_library.utils.PermissionUtils;
import com.simplecity.amp_library.utils.PlaylistUtils;
import com.simplecity.amp_library.utils.SortManager;
import com.simplecityapps.recycler_adapter.model.ViewModel;
import com.simplecityapps.recycler_adapter.recyclerview.RecyclerListener;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import java.util.Collections;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

public class SongFragment extends BaseFragment implements
        MusicUtils.Defs,
        SongView.ClickListener,
        ShuffleView.ShuffleClickListener {

    private static final String TAG = "SongFragment";

    private static final String ARG_PAGE_TITLE = "page_title";

    private FastScrollRecyclerView recyclerView;

    private SectionedAdapter adapter;

    private boolean sortOrderChanged = false;

    private ShuffleView shuffleView;

    private ContextualToolbarHelper<Song> contextualToolbarHelper;

    @Nullable
    private Disposable disposable;

    @Nullable
    private Disposable playlistMenuDisposable;

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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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

                        disposable = DataManager.getInstance().getSongsRelay()
                                .skipWhile(songs -> !force && Stream.of(adapter.items).filter(viewModel -> viewModel instanceof SongView).count() == songs.size())
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
                                                        .filter(viewModel -> viewModel instanceof SongView && (((SongView) viewModel).song.equals(song)))
                                                        .findFirst()
                                                        .orElse(null);

                                                if (songView == null) {
                                                    songView = new SongView(song, null);
                                                    songView.setClickListener(this);
                                                }

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

        if (disposable != null) {
            disposable.dispose();
        }

        if (playlistMenuDisposable != null) {
            playlistMenuDisposable.dispose();
        }

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
            refreshAdapterItems(true);
            getActivity().invalidateOptionsMenu();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSongClick(int position, SongView songView) {
        if (!contextualToolbarHelper.handleClick(position, songView)) {
            List<Song> songs = Stream.of(adapter.items)
                    .filter(adaptableItem -> adaptableItem instanceof SongView)
                    .map(adaptableItem -> ((SongView) adaptableItem).song)
                    .toList();

            MusicUtils.playAll(songs, songs.indexOf(songView.song), (String message) ->
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    public void onSongOverflowClick(int position, View v, Song song) {
        PopupMenu menu = new PopupMenu(SongFragment.this.getActivity(), v);
        MenuUtils.setupSongMenu(menu, false);
        menu.setOnMenuItemClickListener(MenuUtils.getSongMenuClickListener(getContext(), song,
                taggerDialog -> taggerDialog.show(getFragmentManager()),
                null, null));
        menu.show();
    }

    @Override
    public boolean onSongLongClick(int position, SongView songView) {
        return contextualToolbarHelper.handleLongClick(position, songView);
    }

    @Override
    public void onStartDrag(SongView.ViewHolder viewHolder) {
        // Nothing to do
    }

    @Override
    public void onShuffleItemClick() {
        MusicUtils.shuffleAll(message -> Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show());
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

            if (playlistMenuDisposable != null) {
                playlistMenuDisposable.dispose();
            }
            playlistMenuDisposable = PlaylistUtils.createUpdatingPlaylistMenu(sub).subscribe();

            contextualToolbar.setOnMenuItemClickListener(MenuUtils.getSongMenuClickListener(getContext(), () -> Stream.of(contextualToolbarHelper.getItems())
                    .map(SelectableViewModel::getItem)
                    .collect(Collectors.toList())));
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
    }

    @Override
    protected String screenName() {
        return TAG;
    }
}