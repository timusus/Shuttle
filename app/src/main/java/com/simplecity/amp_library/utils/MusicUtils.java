package com.simplecity.amp_library.utils;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.model.Album;
import com.simplecity.amp_library.model.AlbumArtist;
import com.simplecity.amp_library.model.Genre;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.playback.MediaManager;
import com.simplecity.amp_library.playback.QueueManager;
import com.simplecity.amp_library.ui.queue.QueueItem;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;

public class MusicUtils implements MediaManager {

    private static final String TAG = "MusicUtils";

    @NonNull
    public Disposable playAll(@NonNull Single<List<Song>> songsSingle, @NotNull Function1<? super String, Unit> onEmpty) {
        return songsSingle
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(songs -> playAll(songs, 0, true, onEmpty));
    }

    public void playAll(@NonNull List<Song> songs, int position, boolean canClearShuffle, @NotNull Function1<? super String, Unit> onEmpty) {

        if (canClearShuffle && !SettingsManager.getInstance().getRememberShuffle()) {
            setShuffleMode(QueueManager.ShuffleMode.OFF);
        }

        if (songs.size() == 0
                || MusicServiceConnectionUtils.serviceBinder == null
                || MusicServiceConnectionUtils.serviceBinder.getService() == null) {

            onEmpty.invoke(ShuttleApplication.getInstance().getResources().getString(R.string.empty_playlist));
            return;
        }

        if (position < 0) {
            position = 0;
        }

        MusicServiceConnectionUtils.serviceBinder.getService().open(songs, position);
        MusicServiceConnectionUtils.serviceBinder.getService().play();
    }

    @NonNull
    public Disposable shuffleAll(@NonNull Single<List<Song>> songsSingle, @NotNull Function1<? super String, Unit> onEmpty) {
        return songsSingle
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        songs -> shuffleAll(songs, onEmpty),
                        e -> LogUtils.logException(TAG, "Shuffle all error", e));
    }

    /**
     * {@inheritDoc}
     */
    public void shuffleAll(@NotNull List<Song> songs, @NotNull Function1<? super String, Unit> onEmpty) {
        setShuffleMode(QueueManager.ShuffleMode.ON);
        if (!songs.isEmpty()) {
            playAll(songs, new Random().nextInt(songs.size()), false, onEmpty);
        }
    }

    public void playFile(final Uri uri) {
        if (uri == null
                || MusicServiceConnectionUtils.serviceBinder == null
                || MusicServiceConnectionUtils.serviceBinder.getService() == null) {
            return;
        }

        // If this is a file:// URI, just use the path directly instead
        // of going through the open-from-filedescriptor codepath.
        String filename;
        final String scheme = uri.getScheme();
        if ("file".equals(scheme)) {
            filename = uri.getPath();
        } else {
            filename = uri.toString();
        }

        MusicServiceConnectionUtils.serviceBinder.getService().stop();
        MusicServiceConnectionUtils.serviceBinder.getService().openFile(filename, () ->
                MusicServiceConnectionUtils.serviceBinder.getService().play());
    }

    @Nullable
    public String getFilePath() {
        if (MusicServiceConnectionUtils.serviceBinder != null && MusicServiceConnectionUtils.serviceBinder.getService() != null) {
            Song song = MusicServiceConnectionUtils.serviceBinder.getService().getSong();
            if (song != null) {
                return song.path;
            }
        }
        return null;
    }

    public boolean isPlaying() {
        return MusicServiceConnectionUtils.serviceBinder != null && MusicServiceConnectionUtils.serviceBinder.getService() != null && MusicServiceConnectionUtils.serviceBinder.getService()
                .isPlaying();
    }

    public int getShuffleMode() {
        if (MusicServiceConnectionUtils.serviceBinder != null && MusicServiceConnectionUtils.serviceBinder.getService() != null) {
            return MusicServiceConnectionUtils.serviceBinder.getService().getShuffleMode();
        }
        return 0;
    }

    public void setShuffleMode(int mode) {
        if (MusicServiceConnectionUtils.serviceBinder != null && MusicServiceConnectionUtils.serviceBinder.getService() != null) {
            MusicServiceConnectionUtils.serviceBinder.getService().setShuffleMode(mode);
        }
    }

    /**
     * @return The current repeat mode
     */
    public int getRepeatMode() {
        if (MusicServiceConnectionUtils.serviceBinder != null && MusicServiceConnectionUtils.serviceBinder.getService() != null) {
            return MusicServiceConnectionUtils.serviceBinder.getService().getRepeatMode();
        }
        return 0;
    }

    /**
     * Changes to the next track
     */
    public void next() {
        if (MusicServiceConnectionUtils.serviceBinder != null && MusicServiceConnectionUtils.serviceBinder.getService() != null) {
            MusicServiceConnectionUtils.serviceBinder.getService().gotoNext(true);
        }
    }

    /**
     * Changes to the previous track
     *
     * @param allowTrackRestart if true, the track will restart if the track position is > 2 seconds
     */
    public void previous(boolean allowTrackRestart) {
        if (allowTrackRestart && getPosition() > 2000) {
            seekTo(0);
            if (MusicServiceConnectionUtils.serviceBinder != null && MusicServiceConnectionUtils.serviceBinder.getService() != null) {
                MusicServiceConnectionUtils.serviceBinder.getService().play();
            }
        } else {
            if (MusicServiceConnectionUtils.serviceBinder != null && MusicServiceConnectionUtils.serviceBinder.getService() != null) {
                MusicServiceConnectionUtils.serviceBinder.getService().previous();
            }
        }
    }

    /**
     * Plays or pauses the music depending on the current state.
     */
    public void playOrPause() {
        try {
            if (MusicServiceConnectionUtils.serviceBinder != null && MusicServiceConnectionUtils.serviceBinder.getService() != null) {
                if (MusicServiceConnectionUtils.serviceBinder.getService().isPlaying()) {
                    MusicServiceConnectionUtils.serviceBinder.getService().pause();
                } else {
                    MusicServiceConnectionUtils.serviceBinder.getService().play();
                }
            }
        } catch (final Exception ignored) {
        }
    }

    public int getAudioSessionId() {
        if (MusicServiceConnectionUtils.serviceBinder != null && MusicServiceConnectionUtils.serviceBinder.getService() != null) {
            return MusicServiceConnectionUtils.serviceBinder.getService().getAudioSessionId();
        }
        return 0;
    }

    /**
     * Note: This does not return a fully populated album artist.
     *
     * @return a partial {@link AlbumArtist} containing a partial {@link Album}
     * which contains the current song.
     */
    public AlbumArtist getAlbumArtist() {
        if (MusicServiceConnectionUtils.serviceBinder != null && MusicServiceConnectionUtils.serviceBinder.getService() != null) {
            if (getSong() != null) {
                return getSong().getAlbumArtist();
            }
        }
        return null;
    }

    /**
     * Note: This does not return a fully populated album.
     *
     * @return a partial {@link Album} containing this song.
     */
    public Album getAlbum() {
        if (MusicServiceConnectionUtils.serviceBinder != null && MusicServiceConnectionUtils.serviceBinder.getService() != null) {
            if (getSong() != null) {
                return getSong().getAlbum();
            }
        }
        return null;
    }

    public Song getSong() {
        if (MusicServiceConnectionUtils.serviceBinder != null && MusicServiceConnectionUtils.serviceBinder.getService() != null) {
            return MusicServiceConnectionUtils.serviceBinder.getService().getSong();
        }
        return null;
    }

    @NonNull
    public Single<Genre> getGenre() {
        if (MusicServiceConnectionUtils.serviceBinder != null && MusicServiceConnectionUtils.serviceBinder.getService() != null) {
            if (getSong() != null) {
                return getSong().getGenre();
            }
        }
        return Single.error(new IllegalStateException("Genre not found"));
    }

    public long getPosition() {
        if (MusicServiceConnectionUtils.serviceBinder != null && MusicServiceConnectionUtils.serviceBinder.getService() != null) {
            try {
                return MusicServiceConnectionUtils.serviceBinder.getService().getSeekPosition();
            } catch (final Exception ignored) {
            }
        }
        return 0;
    }

    /**
     * Method duration.
     *
     * @return {@link long}
     */
    public long getDuration() {
        if (MusicServiceConnectionUtils.serviceBinder != null && MusicServiceConnectionUtils.serviceBinder.getService() != null) {
            Song song = MusicServiceConnectionUtils.serviceBinder.getService().getSong();
            if (song != null) {
                return song.duration;
            }
        }
        return 0;
    }

    /**
     * Method seekTo.
     *
     * @param position the {@link long} position to seek to
     */
    public void seekTo(final long position) {
        if (MusicServiceConnectionUtils.serviceBinder != null && MusicServiceConnectionUtils.serviceBinder.getService() != null) {
            MusicServiceConnectionUtils.serviceBinder.getService().seekTo(position);
        }
    }

    public void moveQueueItem(final int from, final int to) {
        if (MusicServiceConnectionUtils.serviceBinder != null && MusicServiceConnectionUtils.serviceBinder.getService() != null) {
            MusicServiceConnectionUtils.serviceBinder.getService().moveQueueItem(from, to);
        }
    }

    public void toggleShuffleMode() {
        if (MusicServiceConnectionUtils.serviceBinder.getService() == null) {
            return;
        }
        MusicServiceConnectionUtils.serviceBinder.getService().toggleShuffleMode();
    }

    public void cycleRepeat() {
        if (MusicServiceConnectionUtils.serviceBinder.getService() == null) {
            return;
        }
        MusicServiceConnectionUtils.serviceBinder.getService().toggleRepeat();
    }

    public void addToQueue(@NonNull List<Song> songs, @NotNull Function1<? super String, Unit> onAdded) {
        if (MusicServiceConnectionUtils.serviceBinder.getService() == null) {
            return;
        }
        MusicServiceConnectionUtils.serviceBinder.getService().enqueue(songs, QueueManager.EnqueueAction.LAST);
        onAdded.invoke(ShuttleApplication.getInstance().getResources().getQuantityString(R.plurals.NNNtrackstoqueue, songs.size(), songs.size()));
    }

    @Nullable
    public Disposable playNext(@NonNull Single<List<Song>> songsSingle, @NotNull Function1<? super String, Unit> onAdded) {
        if (MusicServiceConnectionUtils.serviceBinder.getService() == null) {
            return null;
        }
        return songsSingle
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(songs -> playNext(songs, onAdded));
    }

    public void playNext(@NonNull List<Song> songs, @NotNull Function1<? super String, Unit> onAdded) {
        if (MusicServiceConnectionUtils.serviceBinder.getService() == null) {
            return;
        }
        MusicServiceConnectionUtils.serviceBinder.getService().enqueue(songs, QueueManager.EnqueueAction.NEXT);
        onAdded.invoke(ShuttleApplication.getInstance().getResources().getQuantityString(R.plurals.NNNtrackstoqueue, songs.size(), songs.size()));
    }

    @Override
    public void moveToNext(@NotNull QueueItem queueItem) {
        if (MusicServiceConnectionUtils.serviceBinder.getService() == null) {
            return;
        }
        MusicServiceConnectionUtils.serviceBinder.getService().moveToNext(queueItem);
    }

    public void setQueuePosition(final int position) {
        if (MusicServiceConnectionUtils.serviceBinder != null && MusicServiceConnectionUtils.serviceBinder.getService() != null) {
            MusicServiceConnectionUtils.serviceBinder.getService().setQueuePosition(position);
        }
    }

    public void clearQueue() {
        MusicServiceConnectionUtils.serviceBinder.getService().clearQueue();
    }

    @NonNull
    public List<QueueItem> getQueue() {
        if (MusicServiceConnectionUtils.serviceBinder != null && MusicServiceConnectionUtils.serviceBinder.getService() != null) {
            return MusicServiceConnectionUtils.serviceBinder.getService().getQueue();
        }
        return new ArrayList<>();
    }

    public int getQueuePosition() {
        if (MusicServiceConnectionUtils.serviceBinder != null && MusicServiceConnectionUtils.serviceBinder.getService() != null) {
            return MusicServiceConnectionUtils.serviceBinder.getService().getQueuePosition();
        }
        return 0;
    }

    public void removeFromQueue(@NonNull QueueItem queueItem) {
        if (MusicServiceConnectionUtils.serviceBinder != null && MusicServiceConnectionUtils.serviceBinder.getService() != null) {
            MusicServiceConnectionUtils.serviceBinder.getService().removeQueueItem(queueItem);
        }
    }

    public void removeFromQueue(@NonNull List<QueueItem> queueItems) {
        if (MusicServiceConnectionUtils.serviceBinder != null && MusicServiceConnectionUtils.serviceBinder.getService() != null) {
            MusicServiceConnectionUtils.serviceBinder.getService().removeQueueItems(queueItems);
        }
    }

    @Override
    public void removeSongsFromQueue(@NotNull List<Song> songs) {
        if (MusicServiceConnectionUtils.serviceBinder != null && MusicServiceConnectionUtils.serviceBinder.getService() != null) {
            MusicServiceConnectionUtils.serviceBinder.getService().removeSongs(songs);
        }
    }

    public void toggleFavorite() {
        if (MusicServiceConnectionUtils.serviceBinder != null && MusicServiceConnectionUtils.serviceBinder.getService() != null) {
            MusicServiceConnectionUtils.serviceBinder.getService().toggleFavorite();
        }
    }

    public void closeEqualizerSessions(boolean internal, int audioSessionId) {
        if (MusicServiceConnectionUtils.serviceBinder != null && MusicServiceConnectionUtils.serviceBinder.getService() != null) {
            MusicServiceConnectionUtils.serviceBinder.getService().closeEqualizerSessions(internal, audioSessionId);
        }
    }

    public void openEqualizerSession(boolean internal, int audioSessionId) {
        if (MusicServiceConnectionUtils.serviceBinder != null && MusicServiceConnectionUtils.serviceBinder.getService() != null) {
            MusicServiceConnectionUtils.serviceBinder.getService().openEqualizerSession(internal, audioSessionId);
        }
    }

    public void updateEqualizer() {
        if (MusicServiceConnectionUtils.serviceBinder != null && MusicServiceConnectionUtils.serviceBinder.getService() != null) {
            MusicServiceConnectionUtils.serviceBinder.getService().updateEqualizer();
        }
    }
}
