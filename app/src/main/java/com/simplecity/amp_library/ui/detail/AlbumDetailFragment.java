package com.simplecity.amp_library.ui.detail;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.afollestad.materialdialogs.MaterialDialog;
import com.simplecity.amp_library.glide.utils.GlideUtils;
import com.simplecity.amp_library.model.Album;
import com.simplecity.amp_library.model.ArtworkProvider;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.tagger.TaggerDialog;
import com.simplecity.amp_library.utils.ArtworkDialog;
import com.simplecity.amp_library.utils.SortManager;

import java.util.List;

import rx.Observable;

public class AlbumDetailFragment extends BaseDetailFragment {

    public static String ARG_ALBUM = "album";

    private static final String ARG_TRANSITION_NAME = "transition_name";

    private Album album;

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
    boolean showAlbumMenu() {
        return false;
    }

    @Override
    int getSongSortOrder() {
        return SortManager.getInstance().getAlbumDetailSongsSortOrder();
    }

    @Override
    void setSongSortOrder(int sortOrder) {
        SortManager.getInstance().setAlbumDetailSongsSortOrder(sortOrder);
    }

    @Override
    void setSongsAscending(boolean ascending) {
        SortManager.getInstance().setAlbumDetailSongsAscending(ascending);
    }

    @Override
    boolean getSongsAscending() {
        return SortManager.getInstance().getAlbumDetailSongsAscending();
    }

    @NonNull
    @Override
    public Observable<List<Song>> getSongs() {
        return album.getSongsObservable();
    }

    @NonNull
    @Override
    protected String getToolbarTitle() {
        return album.name;
    }

    @Override
    protected String getToolbarSubtitle() {
        return album.albumArtistName;
    }

    @Override
    ArtworkProvider getArtworkProvider() {
        return album;
    }

    @Override
    protected MaterialDialog getArtworkDialog() {
        return ArtworkDialog.build(getContext(), album);
    }

    @Override
    protected TaggerDialog getTaggerDialog() {
        return TaggerDialog.newInstance(album);
    }

    @NonNull
    @Override
    Drawable getPlaceHolderDrawable() {
        return GlideUtils.getPlaceHolderDrawable(album.name, true);
    }

    @Override
    protected String screenName() {
        return "AlbumDetailFragment";
    }
}