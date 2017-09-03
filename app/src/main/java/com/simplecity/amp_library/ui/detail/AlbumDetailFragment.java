package com.simplecity.amp_library.ui.detail;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;

import com.afollestad.materialdialogs.MaterialDialog;
import com.annimon.stream.Stream;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.model.Album;
import com.simplecity.amp_library.model.ArtworkProvider;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.tagger.TaggerDialog;
import com.simplecity.amp_library.ui.dialog.BiographyDialog;
import com.simplecity.amp_library.ui.modelviews.DiscNumberView;
import com.simplecity.amp_library.ui.modelviews.SongView;
import com.simplecity.amp_library.utils.ArtworkDialog;
import com.simplecity.amp_library.utils.PlaceholderProvider;
import com.simplecity.amp_library.utils.SortManager;
import com.simplecityapps.recycler_adapter.model.ViewModel;

import java.util.List;

import io.reactivex.Single;

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
    public Single<List<Song>> getSongs() {
        return album.getSongsSingle().map(songs -> {
            sortSongs(songs);
            return songs;
        });
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

    @Nullable
    @Override
    MaterialDialog getInfoDialog() {
        return BiographyDialog.getAlbumBiographyDialog(getContext(), album.albumArtistName, album.name);
    }

    @NonNull
    @Override
    Drawable getPlaceHolderDrawable() {
        return PlaceholderProvider.getInstance().getPlaceHolderDrawable(album.name, true);
    }

    @NonNull
    @Override
    public List<ViewModel> getSongViewModels(List<Song> songs) {
        int songSortOrder = getSongSortOrder();

        List<ViewModel> songViewModels = super.getSongViewModels(songs);
        Stream.of(songViewModels)
                .filter(viewModel -> viewModel instanceof SongView)
                .forEach(viewModel -> {
                    ((SongView) viewModel).showArtistName(false);
                    ((SongView) viewModel).showAlbumName(false);
                    ((SongView) viewModel).setShowTrackNumber(songSortOrder == SortManager.SongSort.TRACK_NUMBER || songSortOrder == SortManager.SongSort.DETAIL_DEFAULT);
                });

        if (album.numDiscs > 1 && (songSortOrder == SortManager.SongSort.DETAIL_DEFAULT || songSortOrder == SortManager.SongSort.TRACK_NUMBER)) {
            int discNumber = 0;
            int length = songViewModels.size();
            for (int i = 0; i < length; i++) {
                ViewModel viewModel = songViewModels.get(i);
                if (viewModel instanceof SongView) {
                    if (discNumber != ((SongView) viewModel).song.discNumber) {
                        discNumber = ((SongView) viewModel).song.discNumber;
                        songViewModels.add(i, new DiscNumberView(discNumber));
                    }
                }
            }
        }

        return songViewModels;
    }

    @Override
    protected void setupToolbarMenu(Toolbar toolbar) {
        super.setupToolbarMenu(toolbar);

        toolbar.getMenu().findItem(R.id.editTags).setVisible(true);
        toolbar.getMenu().findItem(R.id.info).setVisible(true);
        toolbar.getMenu().findItem(R.id.artwork).setVisible(true);
    }

    @Override
    protected String screenName() {
        return "AlbumDetailFragment";
    }
}