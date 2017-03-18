package com.simplecity.amp_library.ui.views;


import android.support.annotation.Nullable;

import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.playback.MusicService;

public interface PlayerView {

    void setSeekProgress(int progress);

    void currentTimeVisibilityChanged(boolean visible);

    void currentTimeChanged(long seconds);

    void queueChanged(int queuePosition, int queueLength);

    void playbackChanged(boolean isPlaying);

    void shuffleChanged(@MusicService.ShuffleMode int shuffleMode);

    void repeatChanged(@MusicService.RepeatMode int repeatMode);

    void favoriteChanged();

    void trackInfoChanged(@Nullable Song song);
}