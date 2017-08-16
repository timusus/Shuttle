package com.simplecity.amp_library.ui.detail;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.simplecity.amp_library.model.Album;
import com.simplecity.amp_library.model.Playlist;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.utils.Operators;
import com.simplecity.amp_library.utils.PlaceholderProvider;
import com.simplecity.amp_library.utils.SortManager;

import java.util.Collections;
import java.util.List;

import io.reactivex.Single;

public class PlaylistDetailFragment extends BaseDetailFragment {

    public static String ARG_PLAYLIST = "playlist";

    private Playlist playlist;

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
    boolean canPlaySlideshow() {
        return true;
    }

    @Override
    void setSongSortOrder(int sortOrder) {
        SortManager.getInstance().setPlaylistDetailSongsSortOrder(sortOrder);
    }

    @Override
    int getSongSortOrder() {
        return SortManager.getInstance().getPlaylistDetailSongsSortOrder();
    }

    @Override
    void setSongsAscending(boolean ascending) {
        SortManager.getInstance().setPlaylistDetailSongsAscending(ascending);
    }

    @Override
    boolean getSongsAscending() {
        return SortManager.getInstance().getPlaylistDetailSongsAscending();
    }

    @Override
    void setAlbumSortOrder(int sortOrder) {
        SortManager.getInstance().setPlaylistDetailAlbumsSortOrder(sortOrder);
    }

    @Override
    int getAlbumSort() {
        return SortManager.getInstance().getPlaylistDetailAlbumsSortOrder();
    }

    @Override
    void setAlbumsAscending(boolean ascending) {
        SortManager.getInstance().setPlaylistDetailAlbumsAscending(ascending);
    }

    @Override
    boolean getAlbumsAscending() {
        return SortManager.getInstance().getPlaylistDetailAlbumsAscending();
    }

    @NonNull
    @Override
    public Single<List<Song>> getSongs() {
        return playlist.getSongsObservable().first(Collections.emptyList());
    }

    @NonNull
    @Override
    public Single<List<Album>> getAlbums() {
        return getSongs().map(Operators::songsToAlbums);
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
    protected String screenName() {
        return "PlaylistDetailFragment";
    }
}