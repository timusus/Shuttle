package com.simplecity.amp_library.ui.detail;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.annimon.stream.IntStream;
import com.annimon.stream.Stream;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.model.Album;
import com.simplecity.amp_library.model.Playlist;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.rx.UnsafeAction;
import com.simplecity.amp_library.ui.modelviews.SongView;
import com.simplecity.amp_library.ui.recyclerview.ItemTouchHelperCallback;
import com.simplecity.amp_library.utils.ComparisonUtils;
import com.simplecity.amp_library.utils.MenuUtils;
import com.simplecity.amp_library.utils.Operators;
import com.simplecity.amp_library.utils.PlaceholderProvider;
import com.simplecity.amp_library.utils.SortManager;
import com.simplecityapps.recycler_adapter.model.ViewModel;

import java.util.Collections;
import java.util.List;

import io.reactivex.Single;

public class PlaylistDetailFragment extends BaseDetailFragment {

    public static String ARG_PLAYLIST = "playlist";

    private Playlist playlist;

    private ItemTouchHelper itemTouchHelper;

    public static PlaylistDetailFragment newInstance(Playlist playlist) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_PLAYLIST, playlist);
        PlaylistDetailFragment fragment = new PlaylistDetailFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        playlist = (Playlist) getArguments().getSerializable(ARG_PLAYLIST);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = super.onCreateView(inflater, container, savedInstanceState);

        itemTouchHelper = new ItemTouchHelper(new ItemTouchHelperCallback(
                (fromPosition, toPosition) -> adapter.moveItem(fromPosition, toPosition),
                (fromPosition, toPosition) -> {
                    SongView from = (SongView) adapter.items.get(fromPosition);
                    SongView to = (SongView) adapter.items.get(toPosition);

                    List<SongView> songViews = Stream.of(adapter.items)
                            .filter(itemView -> itemView instanceof SongView)
                            .map(itemView -> ((SongView) itemView))
                            .toList();

                    int adjustedFrom = IntStream.range(0, songViews.size())
                            .filter(i -> from.equals(songViews.get(i)))
                            .findFirst()
                            .orElse(-1);

                    int adjustedTo = IntStream.range(0, songViews.size())
                            .filter(i -> to.equals(songViews.get(i)))
                            .findFirst()
                            .orElse(-1);

                    if (adjustedFrom != -1 && adjustedTo != -1) {
                        playlist.moveSong(adjustedFrom, adjustedTo);
                    }
                },
                () -> {
                    // Nothing to do
                }) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                if (viewHolder.getItemViewType() == target.getItemViewType()) {
                    return super.onMove(recyclerView, viewHolder, target);
                }
                return false;
            }
        });

        itemTouchHelper.attachToRecyclerView(recyclerView);

        return rootView;
    }

    @Override
    boolean canPlaySlideshow() {
        return true;
    }

    @Override
    boolean showSongOverflowRemoveButton() {
        return playlist.canEdit;
    }

    @Override
    void songRemoved(int position, Song song) {
        adapter.removeItem(position);
        playlist.removeSong(song, null);
    }

    @Override
    void setSongSortOrder(int sortOrder) {
        SortManager.getInstance().setPlaylistDetailSongsSortOrder(playlist, sortOrder);
    }

    @Override
    int getSongSortOrder() {
        return SortManager.getInstance().getPlaylistDetailSongsSortOrder(playlist);
    }

    @Override
    void setSongsAscending(boolean ascending) {
        SortManager.getInstance().setPlaylistDetailSongsAscending(playlist, ascending);
    }

    @Override
    boolean getSongsAscending() {
        return SortManager.getInstance().getPlaylistDetailSongsAscending(playlist);
    }

    @Override
    void setAlbumSortOrder(int sortOrder) {
        SortManager.getInstance().setPlaylistDetailAlbumsSortOrder(playlist, sortOrder);
    }

    @Override
    int getAlbumSort() {
        return SortManager.getInstance().getPlaylistDetailAlbumsSortOrder(playlist);
    }

    @Override
    void setAlbumsAscending(boolean ascending) {
        SortManager.getInstance().setPlaylistDetailAlbumsAscending(playlist, ascending);
    }

    @Override
    boolean getAlbumsAscending() {
        return SortManager.getInstance().getPlaylistDetailAlbumsAscending(playlist);
    }

    @NonNull
    @Override
    public Single<List<Song>> getSongs() {
        return playlist.getSongsObservable().first(Collections.emptyList()).map(songs -> {
            sortSongs(songs);

            int songSortOrder = getSongSortOrder();
            if (songSortOrder == SortManager.SongSort.DETAIL_DEFAULT) {
                if (playlist.type == Playlist.Type.MOST_PLAYED) {
                    Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(b.playCount, a.playCount));
                }
                if (playlist.canEdit) {
                    Collections.sort(songs, (a, b) -> ComparisonUtils.compareLong(a.playlistSongPlayOrder, b.playlistSongPlayOrder));
                }
            }
            return songs;
        });
    }

    @NonNull
    @Override
    public List<ViewModel> getSongViewModels(List<Song> songs) {
        List<ViewModel> viewModels = super.getSongViewModels(songs);
        return Stream.of(viewModels)
                .filter(viewModel -> viewModel instanceof SongView)
                .map(viewModel -> {
                    ((SongView) viewModel).showPlayCount(true);
                    if (playlist.canEdit && getSongSortOrder() == SortManager.SongSort.DETAIL_DEFAULT) {
                        ((SongView) viewModel).setEditable(true);
                    }
                    return viewModel;
                }).toList();
    }

    @NonNull
    @Override
    public Single<List<Album>> getAlbums() {
        return getSongs().map(Operators::songsToAlbums).map(albums -> {
            sortAlbums(albums);
            return albums;
        });
    }

    @Override
    protected void setupToolbarMenu(Toolbar toolbar) {
        super.setupToolbarMenu(toolbar);

        MenuUtils.setupPlaylistMenu(toolbar, playlist);

        toolbar.getMenu().findItem(R.id.playPlaylist).setVisible(false);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (MenuUtils.handleMenuItemClicks(getContext(), item, playlist, () -> getNavigationController().popViewController())) {
            return true;
        }
        return super.onMenuItemClick(item);
    }

    @NonNull
    @Override
    protected String getToolbarTitle() {
        return playlist.name;
    }

    @NonNull
    @Override
    Drawable getPlaceHolderDrawable() {
        return PlaceholderProvider.getInstance().getPlaceHolderDrawable(playlist.name, true);
    }

    @Override
    public void onStartDrag(SongView.ViewHolder holder) {
        super.onStartDrag(holder);

        itemTouchHelper.startDrag(holder);
    }

    @Override
    protected String screenName() {
        return "PlaylistDetailFragment";
    }
}