package com.simplecity.amp_library.detail;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.model.Album;
import com.simplecity.amp_library.model.Genre;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.ui.modelviews.AlbumView;
import com.simplecity.amp_library.ui.modelviews.HorizontalAlbumView;
import com.simplecity.amp_library.ui.modelviews.HorizontalRecyclerView;
import com.simplecity.amp_library.ui.modelviews.SongView;
import com.simplecity.amp_library.ui.modelviews.SubheaderView;
import com.simplecity.amp_library.utils.MusicUtils;
import com.simplecity.amp_library.utils.Operators;
import com.simplecity.amp_library.utils.SortManager;
import com.simplecity.amp_library.utils.StringUtils;
import com.simplecityapps.recycler_adapter.adapter.ViewModelAdapter;
import com.simplecityapps.recycler_adapter.model.ViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rx.Observable;

public class GenreDetailFragment extends BaseDetailFragment implements
        SongView.ClickListener,
        AlbumView.ClickListener {

    public static String ARG_GENRE = "genre";

    private Genre genre;

    private HorizontalRecyclerView horizontalRecyclerView;

    public static GenreDetailFragment newInstance(Genre genre) {

        Bundle args = new Bundle();

        GenreDetailFragment fragment = new GenreDetailFragment();
        args.putSerializable(ARG_GENRE, genre);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        genre = (Genre) getArguments().getSerializable(ARG_GENRE);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        horizontalRecyclerView = new HorizontalRecyclerView();
    }

    @Override
    protected SongsProvider getSongProvider() {
        return new SongsProvider() {
            @NonNull
            @Override
            public Observable<List<Song>> getSongs() {
                return genre.getSongsObservable();
            }

            @Override
            public List<ViewModel> getAdaptableItems(List<Song> songs) {
                List<ViewModel> items = new ArrayList<>();

                boolean songsAscending = SortManager.getInstance().getGenreDetailSongsAscending();
                @SortManager.SongSort int songSort = SortManager.getInstance().getGenreDetailSongsSortOrder();

                //If we're not looking at a playlist, or we are, but it's not sorted by 'default',
                //then we just leave the songs in what ever sort order they came in
                if (songSort != SortManager.SongSort.DETAIL_DEFAULT) {
                    SortManager.getInstance().sortSongs(songs, songSort);
                    if (!songsAscending) {
                        Collections.reverse(songs);
                    }
                }

                items.add(new SubheaderView(StringUtils.makeSongsLabel(getContext(), songs.size())));

                items.addAll(Stream.of(songs)
                        .map(song -> {
                            SongView songView = new SongView(song, requestManager);
                            songView.setClickListener(GenreDetailFragment.this);
                            return songView;
                        }).collect(Collectors.toList()));

                return items;
            }
        };
    }

    @Override
    protected AlbumsProvider getAlbumsProvider() {
        return songs -> {
            List<ViewModel> items = new ArrayList<>();

            boolean albumsAscending = SortManager.getInstance().getGenreDetailAlbumsAscending();
            @SortManager.AlbumSort int albumSort = SortManager.getInstance().getGenreDetailAlbumsSortOrder();

            List<Album> albums = Stream.of(Operators.songsToAlbums(songs))
                    .collect(Collectors.toList());

            SortManager.getInstance().sortAlbums(albums, albumSort);
            if (!albumsAscending) {
                Collections.reverse(albums);
            }

            horizontalRecyclerView.setItems(Stream.of(albums)
                    .map(album -> {
                        HorizontalAlbumView horizontalAlbumView = new HorizontalAlbumView(album, requestManager);
                        horizontalAlbumView.setClickListener(GenreDetailFragment.this);
                        horizontalAlbumView.setShowYear(true);
                        return horizontalAlbumView;
                    })
                    .collect(Collectors.toList()));

            items.add(new SubheaderView(StringUtils.makeAlbumsLabel(getContext(), albums.size())));
            items.add(horizontalRecyclerView);

            return items;
        };
    }

    @Override
    protected ViewModelAdapter createAdapter() {
        return new ViewModelAdapter();
    }

    @Override
    protected String getToolbarTitle() {
        return genre.name;
    }

    @Override
    public void onItemClick(Song song, SongView.ViewHolder holder) {
        subscriptions.add(getSongProvider().getSongs()
                .subscribe(songs -> {
                    int position = songs.indexOf(song);
                    MusicUtils.playAll(songs, position, message -> Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show());
                }));
    }

    @Override
    public boolean onItemLongClick(Song song) {
        return false;
    }

    @Override
    public void onOverflowClick(View v, Song song) {

    }

    @Override
    public void onStartDrag(SongView.ViewHolder viewHolder) {

    }

    @Override
    public void onAlbumClick(Album album, AlbumView.ViewHolder holder) {
        pushDetailController(AlbumDetailFragment.newInstance(album, ViewCompat.getTransitionName(holder.imageOne)), "AlbumDetailFragment", holder.imageOne);
    }

    @Override
    public boolean onAlbumLongClick(Album album) {
        return false;
    }

    @Override
    public void onAlbumOverflowClicked(View v, Album album) {

    }

    @Override
    void inflateSortMenus(Toolbar toolbar, MenuItem item) {
        item.setVisible(true);
        getActivity().getMenuInflater().inflate(R.menu.menu_detail_sort_albums, item.getSubMenu());
        getActivity().getMenuInflater().inflate(R.menu.menu_detail_sort_songs, item.getSubMenu());
        super.inflateSortMenus(toolbar, item);
    }

    @Override
    void handleSortMenuClicks(MenuItem item) {

        boolean sortChanged = false;

        switch (item.getItemId()) {
            //Songs
            case R.id.sort_song_default:
                SortManager.getInstance().setGenreDetailSongsSortOrder(SortManager.SongSort.DETAIL_DEFAULT);
                sortChanged = true;
                break;
            case R.id.sort_song_name:
                SortManager.getInstance().setGenreDetailSongsSortOrder(SortManager.SongSort.NAME);
                sortChanged = true;
                break;
            case R.id.sort_song_track_number:
                SortManager.getInstance().setGenreDetailSongsSortOrder(SortManager.SongSort.TRACK_NUMBER);
                sortChanged = true;
                break;
            case R.id.sort_song_duration:
                SortManager.getInstance().setGenreDetailSongsSortOrder(SortManager.SongSort.DURATION);
                sortChanged = true;
                break;
            case R.id.sort_song_year:
                SortManager.getInstance().setGenreDetailSongsSortOrder(SortManager.SongSort.YEAR);
                sortChanged = true;
                break;
            case R.id.sort_song_date:
                SortManager.getInstance().setGenreDetailSongsSortOrder(SortManager.SongSort.DATE);
                sortChanged = true;
                break;
            case R.id.sort_song_album_name:
                SortManager.getInstance().setGenreDetailSongsSortOrder(SortManager.SongSort.ALBUM_NAME);
                sortChanged = true;
                break;
            case R.id.sort_songs_ascending:
                boolean ascending = !item.isChecked();
                SortManager.getInstance().setGenreDetailSongsAscending(ascending);
                sortChanged = true;
                break;

            //Albums
            case R.id.sort_album_default:
                SortManager.getInstance().setGenreDetailAlbumsSortOrder(SortManager.AlbumSort.DEFAULT);
                sortChanged = true;
                break;
            case R.id.sort_album_name:
                SortManager.getInstance().setGenreDetailAlbumsSortOrder(SortManager.AlbumSort.NAME);
                sortChanged = true;
                break;
            case R.id.sort_album_year:
                SortManager.getInstance().setGenreDetailAlbumsSortOrder(SortManager.AlbumSort.YEAR);
                sortChanged = true;
                break;
            case R.id.sort_albums_ascending:
                SortManager.getInstance().setGenreDetailAlbumsAscending(!item.isChecked());
                sortChanged = true;
                break;
        }

        updateMenuItems(toolbar);

        if (sortChanged) {
            detailPresenter.loadData();
        }
    }

    @Override
    protected void updateMenuItems(Toolbar toolbar) {

        // Songs
        switch (SortManager.getInstance().getGenreDetailSongsSortOrder()) {
            case SortManager.SongSort.DETAIL_DEFAULT:
                toolbar.getMenu().findItem(R.id.sort_song_default).setChecked(true);
                break;
            case SortManager.SongSort.NAME:
                toolbar.getMenu().findItem(R.id.sort_song_name).setChecked(true);
                break;
            case SortManager.SongSort.TRACK_NUMBER:
                toolbar.getMenu().findItem(R.id.sort_song_track_number).setChecked(true);
                break;
            case SortManager.SongSort.DURATION:
                toolbar.getMenu().findItem(R.id.sort_song_duration).setChecked(true);
                break;
            case SortManager.SongSort.DATE:
                toolbar.getMenu().findItem(R.id.sort_song_date).setChecked(true);
                break;
            case SortManager.SongSort.YEAR:
                toolbar.getMenu().findItem(R.id.sort_song_year).setChecked(true);
                break;
            case SortManager.SongSort.ALBUM_NAME:
                toolbar.getMenu().findItem(R.id.sort_song_album_name).setChecked(true);
                break;
            case SortManager.SongSort.ARTIST_NAME:
                toolbar.getMenu().findItem(R.id.sort_song_artist_name).setChecked(true);
                break;
        }

        toolbar.getMenu().findItem(R.id.sort_songs_ascending).setChecked(SortManager.getInstance().getGenreDetailSongsAscending());

        //Albums
        switch (SortManager.getInstance().getGenreDetailAlbumsSortOrder()) {
            case SortManager.AlbumSort.DEFAULT:
                toolbar.getMenu().findItem(R.id.sort_album_default).setChecked(true);
                break;
            case SortManager.AlbumSort.NAME:
                toolbar.getMenu().findItem(R.id.sort_album_name).setChecked(true);
                break;
            case SortManager.AlbumSort.YEAR:
                toolbar.getMenu().findItem(R.id.sort_album_year).setChecked(true);
                break;
            case SortManager.AlbumSort.ARTIST_NAME:
                toolbar.getMenu().findItem(R.id.sort_album_artist_name).setChecked(true);
                break;
        }

        toolbar.getMenu().findItem(R.id.sort_albums_ascending).setChecked(SortManager.getInstance().getGenreDetailAlbumsAscending());
    }

    @Override
    protected String screenName() {
        return "GenreDetailFragment";
    }
}
