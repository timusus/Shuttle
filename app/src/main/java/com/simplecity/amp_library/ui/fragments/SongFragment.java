package com.simplecity.amp_library.ui.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
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
import com.simplecity.amp_library.model.Playlist;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.sql.databases.BlacklistHelper;
import com.simplecity.amp_library.ui.adapters.SectionedAdapter;
import com.simplecity.amp_library.ui.modelviews.EmptyView;
import com.simplecity.amp_library.ui.modelviews.SelectableViewModel;
import com.simplecity.amp_library.ui.modelviews.ShuffleView;
import com.simplecity.amp_library.ui.modelviews.SongView;
import com.simplecity.amp_library.ui.views.ContextualToolbar;
import com.simplecity.amp_library.utils.ContextualToolbarHelper;
import com.simplecity.amp_library.utils.DataManager;
import com.simplecity.amp_library.utils.DialogUtils;
import com.simplecity.amp_library.utils.MenuUtils;
import com.simplecity.amp_library.utils.MusicUtils;
import com.simplecity.amp_library.utils.PermissionUtils;
import com.simplecity.amp_library.utils.PlaylistUtils;
import com.simplecity.amp_library.utils.ShuttleUtils;
import com.simplecity.amp_library.utils.SortManager;
import com.simplecityapps.recycler_adapter.model.ViewModel;
import com.simplecityapps.recycler_adapter.recyclerview.RecyclerListener;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import java.util.Collections;
import java.util.List;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

public class SongFragment extends BaseFragment implements
        MusicUtils.Defs,
        SongView.ClickListener,
        ShuffleView.ShuffleClickListener,
        Toolbar.OnMenuItemClickListener,
        PageSelectedListener {

    private static final String TAG = "SongFragment";

    private static final String ARG_PAGE_TITLE = "page_title";

    private FastScrollRecyclerView recyclerView;

    private SectionedAdapter songsAdapter;

    private boolean sortOrderChanged = false;

    private Subscription subscription;

    private ShuffleView shuffleView;

    private ContextualToolbarHelper<Song> contextualToolbarHelper;

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

        songsAdapter = new SectionedAdapter();

        shuffleView = new ShuffleView();
        shuffleView.setClickListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        recyclerView = (FastScrollRecyclerView) inflater.inflate(R.layout.fragment_recycler, container, false);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setRecyclerListener(new RecyclerListener());
        recyclerView.setAdapter(songsAdapter);
        return recyclerView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ContextualToolbar contextualToolbar = ContextualToolbar.findContextualToolbar(this);
        contextualToolbar.inflateMenu(R.menu.context_menu_songs);
        SubMenu sub = contextualToolbar.getMenu().findItem(R.id.menu_add_to_playlist).getSubMenu();
        PlaylistUtils.makePlaylistMenu(getActivity(), sub, SONG_FRAGMENT_GROUP_ID);
        contextualToolbar.setOnMenuItemClickListener(this);
        contextualToolbarHelper = new ContextualToolbarHelper<>(contextualToolbar, new ContextualToolbarHelper.Callback() {
            @Override
            public void notifyItemChanged(int position) {
                songsAdapter.notifyItemChanged(position, 0);
            }

            @Override
            public void notifyDatasetChanged() {
                songsAdapter.notifyItemRangeChanged(0, songsAdapter.items.size(), 0);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshAdapterItems();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
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
                                            .map(song -> {

                                                // Look for an existing SongView wrapping the song, we'll reuse it if it exists.
                                                SongView songView = (SongView) Stream.of(songsAdapter.items)
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
                                        songsAdapter.setItems(Collections.singletonList(new EmptyView(R.string.empty_songlist)));
                                    } else {
                                        items.add(0, shuffleView);
                                        songsAdapter.setItems(items);
                                    }

                                    //Move the RV back to the top if we've had a sort order change.
                                    if (sortOrderChanged) {
                                        recyclerView.scrollToPosition(0);
                                    }

                                    sortOrderChanged = false;
                                });
                    }
                }
        );
    }

    @Override
    public void onPause() {

        if (subscription != null) {
            subscription.unsubscribe();
        }

        super.onPause();
    }

    @Override
    public void onDestroy() {
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
    public void onSongClick(int position, SongView songView) {
        if (!contextualToolbarHelper.handleClick(position, songView)) {
            List<Song> songs = Stream.of(songsAdapter.items)
                    .filter(adaptableItem -> adaptableItem instanceof SongView)
                    .map(adaptableItem -> ((SongView) adaptableItem).song)
                    .collect(Collectors.toList());

            MusicUtils.playAll(songs, songs.indexOf(songView.song), (String message) ->
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    public void onSongOverflowClick(View v, Song song) {
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
    public boolean onMenuItemClick(MenuItem item) {

        List<Song> songs = Stream.of(contextualToolbarHelper.getItems())
                .map(SelectableViewModel::getItem)
                .collect(Collectors.toList());

        switch (item.getItemId()) {
            case NEW_PLAYLIST:
                PlaylistUtils.createPlaylistDialog(getActivity(), songs);
                break;
            case PLAYLIST_SELECTED:
                Playlist playlist = (Playlist) item.getIntent().getSerializableExtra(ShuttleUtils.ARG_PLAYLIST);
                PlaylistUtils.addToPlaylist(getContext(), playlist, songs);
                break;
            case R.id.delete:
                new DialogUtils.DeleteDialogBuilder()
                        .context(getContext())
                        .singleMessageId(R.string.delete_song_desc)
                        .multipleMessage(R.string.delete_song_desc_multiple)
                        .itemNames(Stream.of(songs)
                                .map(song -> song.name)
                                .collect(Collectors.toList()))
                        .songsToDelete(Observable.just(songs))
                        .build()
                        .show();
                contextualToolbarHelper.finish();
                break;
            case R.id.menu_add_to_queue:
                MusicUtils.addToQueue(songs, message ->
                        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show());
                break;
        }

        return true;
    }

    @Override
    protected String screenName() {
        return TAG;
    }

    @Override
    public void onPageSelected() {
        // Nothing to do
    }

    @Override
    public void onPageDeselected() {
        new Handler().postDelayed(() -> {
            if (contextualToolbarHelper != null) {
                contextualToolbarHelper.finish();
            }
        }, 250);
    }
}