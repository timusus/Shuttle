package com.simplecity.amp_library.ui.views;


import android.support.annotation.Nullable;

import com.afollestad.materialdialogs.MaterialDialog;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.tagger.TaggerDialog;

public abstract class PlayerViewAdapter implements PlayerView {

    @Override
    public void setSeekProgress(int progress) {

    }

    @Override
    public void currentTimeVisibilityChanged(boolean visible) {

    }

    @Override
    public void currentTimeChanged(long seconds) {

    }

    @Override
    public void totalTimeChanged(long seconds){

    }

    @Override
    public void queueChanged(int queuePosition, int queueLength) {

    }

    @Override
    public void playbackChanged(boolean isPlaying) {

    }

    @Override
    public void shuffleChanged(int shuffleMode) {

    }

    @Override
    public void repeatChanged(int repeatMode) {

    }

    @Override
    public void favoriteChanged(boolean isFavorite) {

    }

    @Override
    public void trackInfoChanged(@Nullable Song song) {

    }

    @Override
    public void showToast(String message, int duration) {

    }

    @Override
    public void showLyricsDialog(MaterialDialog dialog) {

    }

    @Override
    public void showTaggerDialog(TaggerDialog taggerDialog) {

    }

    @Override
    public void showSongInfoDialog(MaterialDialog dialog) {

    }

}