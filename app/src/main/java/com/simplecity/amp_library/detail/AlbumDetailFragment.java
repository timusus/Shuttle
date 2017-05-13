package com.simplecity.amp_library.detail;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.bignerdranch.android.multiselector.MultiSelector;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.glide.utils.AlwaysCrossFade;
import com.simplecity.amp_library.glide.utils.GlideUtils;
import com.simplecity.amp_library.model.Album;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.tagger.TaggerDialog;
import com.simplecity.amp_library.ui.modelviews.SongView;
import com.simplecity.amp_library.ui.modelviews.SubheaderView;
import com.simplecity.amp_library.utils.ArtworkDialog;
import com.simplecity.amp_library.utils.MusicUtils;
import com.simplecity.amp_library.utils.ResourceUtils;
import com.simplecity.amp_library.utils.SortManager;
import com.simplecity.amp_library.utils.StringUtils;
import com.simplecityapps.recycler_adapter.adapter.ViewModelAdapter;
import com.simplecityapps.recycler_adapter.model.ViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class AlbumDetailFragment extends BaseDetailFragment implements Toolbar.OnMenuItemClickListener, SongView.ClickListener {

    public static String ARG_ALBUM = "album";

    private static final String ARG_TRANSITION_NAME = "transition_name";

    private Album album;

    private MultiSelector multiSelector = new MultiSelector();

    public static AlbumDetailFragment newInstance(Album album, String transitionName) {

        Bundle args = new Bundle();

        AlbumDetailFragment fragment = new AlbumDetailFragment();
        args.putSerializable(ARG_ALBUM, album);
        args.putString(ARG_TRANSITION_NAME, transitionName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        album = (Album) getArguments().getSerializable(ARG_ALBUM);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = super.onCreateView(inflater, container, savedInstanceState);

        loadBackgroundImage();

        return rootView;
    }

    @Override
    protected SongsProvider getSongProvider() {
        return new SongsProvider() {
            @NonNull
            @Override
            public Observable<List<Song>> getSongs() {
                return album.getSongsObservable();
            }

            @Override
            public List<ViewModel> getAdaptableItems(List<Song> songs) {
                List<ViewModel> items = new ArrayList<>();

                boolean songsAscending = SortManager.getInstance().getAlbumDetailSongsAscending();
                @SortManager.SongSort int songSort = SortManager.getInstance().getAlbumDetailSongsSortOrder();

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
                            songView.setClickListener(AlbumDetailFragment.this);
                            songView.setShowTrackNumber(songSort == SortManager.SongSort.DETAIL_DEFAULT || songSort == SortManager.SongSort.TRACK_NUMBER);
                            return songView;
                        }).collect(Collectors.toList()));

                return items;
            }
        };
    }

    @Override
    protected AlbumsProvider getAlbumsProvider() {
        return null;
    }

    private void loadBackgroundImage() {
        int width = ResourceUtils.getScreenSize().width + ResourceUtils.toPixels(60);
        int height = getResources().getDimensionPixelSize(R.dimen.header_view_height);

        requestManager.load(album)
                // Need to override the height/width, as the shared element transition tricks Glide into thinking this ImageView has
                // the same dimensions as the ImageView that the transition starts with.
                // So we'll set it to screen width (plus a little extra, which might fix an issue on some devices..)
                .override(width, height)
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .priority(Priority.HIGH)
                .placeholder(GlideUtils.getPlaceHolderDrawable(album.name, false))
                .centerCrop()
                .animate(new AlwaysCrossFade(false))
                .into(headerImageView);

    }

    @Override
    protected ViewModelAdapter createAdapter() {
        return new ViewModelAdapter();
    }

    @Override
    protected String getToolbarTitle() {
        return album.name;
    }

    @Override
    protected String getToolbarSubtitle() {
        return album.albumArtistName;
    }

    @Override
    protected MaterialDialog getArtworkDialog() {
        return ArtworkDialog.build(getContext(), album);
    }

    @Override
    protected TaggerDialog getTaggerDialog() {
        return TaggerDialog.newInstance(album);
    }

    @Override
    void inflateSortMenus(Toolbar toolbar, MenuItem item) {
        item.setVisible(true);
        getActivity().getMenuInflater().inflate(R.menu.menu_detail_sort_songs, item.getSubMenu());
        super.inflateSortMenus(toolbar, item);
    }

    void handleSortMenuClicks(MenuItem item) {

        boolean sortChanged = false;

        switch (item.getItemId()) {
            //Songs
            case R.id.sort_song_default:
                SortManager.getInstance().setAlbumDetailSongsSortOrder(SortManager.SongSort.DETAIL_DEFAULT);
                sortChanged = true;
                break;
            case R.id.sort_song_name:
                SortManager.getInstance().setAlbumDetailSongsSortOrder(SortManager.SongSort.NAME);
                sortChanged = true;
                break;
            case R.id.sort_song_track_number:
                SortManager.getInstance().setAlbumDetailSongsSortOrder(SortManager.SongSort.TRACK_NUMBER);
                sortChanged = true;
                break;
            case R.id.sort_song_duration:
                SortManager.getInstance().setAlbumDetailSongsSortOrder(SortManager.SongSort.DURATION);
                sortChanged = true;
                break;
            case R.id.sort_song_year:
                SortManager.getInstance().setAlbumDetailSongsSortOrder(SortManager.SongSort.YEAR);
                sortChanged = true;
                break;
            case R.id.sort_song_date:
                SortManager.getInstance().setAlbumDetailSongsSortOrder(SortManager.SongSort.DATE);
                sortChanged = true;
                break;
            case R.id.sort_song_album_name:
                SortManager.getInstance().setAlbumDetailSongsSortOrder(SortManager.SongSort.ALBUM_NAME);
                sortChanged = true;
                break;
            case R.id.sort_songs_ascending:
                boolean ascending = !item.isChecked();
                SortManager.getInstance().setAlbumDetailSongsAscending(ascending);
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
        switch (SortManager.getInstance().getAlbumDetailSongsSortOrder()) {
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

        toolbar.getMenu().findItem(R.id.sort_songs_ascending).setChecked(SortManager.getInstance().getAlbumDetailSongsAscending());
    }

    @Override
    protected String screenName() {
        return "AlbumDetailFragment";
    }

    @Override
    public void onItemClick(Song song, SongView.ViewHolder holder) {
        subscriptions.add(getSongProvider().getSongs()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
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
}
