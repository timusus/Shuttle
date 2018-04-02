package com.simplecity.amp_library.utils;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.support.annotation.Nullable;

import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.model.Album;
import com.simplecity.amp_library.model.AlbumArtist;
import com.simplecity.amp_library.model.Genre;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.playback.QueueManager;
import com.simplecity.amp_library.rx.UnsafeConsumer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class MusicUtils {

    private static final String TAG = "MusicUtils";

    public interface Defs {
        int ADD_TO_PLAYLIST = 0;
        int PLAYLIST_SELECTED = 1;
        int NEW_PLAYLIST = 2;
    }

    /**
     * Sends a list of songs to the MusicService for playback
     */
    @SuppressLint("CheckResult")
    public static void playAll(Single<List<Song>> songsSingle, UnsafeConsumer<String> onEmpty) {
        songsSingle
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(songs -> playAll(songs, 0, true, onEmpty));
    }

    /**
     * Sends a list of songs to the MusicService for playback
     */
    public static void playAll(List<Song> songs, int position, boolean canClearShuffle, UnsafeConsumer<String> onEmpty) {

        if (canClearShuffle && !SettingsManager.getInstance().getRememberShuffle()) {
            setShuffleMode(QueueManager.ShuffleMode.OFF);
        }

        if (songs.size() == 0
                || MusicServiceConnectionUtils.serviceBinder == null
                || MusicServiceConnectionUtils.serviceBinder.getService() == null) {

            onEmpty.accept(ShuttleApplication.getInstance().getResources().getString(R.string.empty_playlist));
            return;
        }

        if (position < 0) {
            position = 0;
        }

        MusicServiceConnectionUtils.serviceBinder.getService().open(songs, position);
        MusicServiceConnectionUtils.serviceBinder.getService().play();
    }

    /**
     * Shuffles all songs in a given song list
     */
    @SuppressLint("CheckResult")
    public static void shuffleAll(Single<List<Song>> songsSingle, UnsafeConsumer<String> onEmpty) {
        setShuffleMode(QueueManager.ShuffleMode.ON);
        songsSingle
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        songs -> {
                            if (!songs.isEmpty()) {
                                playAll(songs, new Random().nextInt(songs.size()), false, onEmpty);
                            }
                        },
                        e -> LogUtils.logException(TAG, "Shuffle all error", e));
    }

    /**
     * @param uri The source of the file
     */
    public static void playFile(final Uri uri) {
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

    /**
     * @return {@link String} The path to the currently playing file
     */
    @Nullable
    public static String getFilePath() {
        if (MusicServiceConnectionUtils.serviceBinder != null && MusicServiceConnectionUtils.serviceBinder.getService() != null) {
            Song song = MusicServiceConnectionUtils.serviceBinder.getService().getSong();
            if (song != null) {
                return song.path;
            }
        }
        return null;
    }

    /**
     * @return True if we're playing music, false otherwise.
     */
    public static boolean isPlaying() {
        if (MusicServiceConnectionUtils.serviceBinder != null && MusicServiceConnectionUtils.serviceBinder.getService() != null) {
            return MusicServiceConnectionUtils.serviceBinder.getService().isPlaying();
        }
        return false;
    }

    /**
     * @return The current shuffle mode
     */
    public static int getShuffleMode() {
        if (MusicServiceConnectionUtils.serviceBinder != null && MusicServiceConnectionUtils.serviceBinder.getService() != null) {
            return MusicServiceConnectionUtils.serviceBinder.getService().getShuffleMode();
        }
        return 0;
    }

    /**
     * Sets the shuffle mode
     */
    public static void setShuffleMode(int mode) {
        if (MusicServiceConnectionUtils.serviceBinder != null && MusicServiceConnectionUtils.serviceBinder.getService() != null) {
            MusicServiceConnectionUtils.serviceBinder.getService().setShuffleMode(mode);
        }
    }

    /**
     * @return The current repeat mode
     */
    public static int getRepeatMode() {
        if (MusicServiceConnectionUtils.serviceBinder != null && MusicServiceConnectionUtils.serviceBinder.getService() != null) {
            return MusicServiceConnectionUtils.serviceBinder.getService().getRepeatMode();
        }
        return 0;
    }

    /**
     * Changes to the next track
     */
    public static void next() {
        if (MusicServiceConnectionUtils.serviceBinder != null && MusicServiceConnectionUtils.serviceBinder.getService() != null) {
            MusicServiceConnectionUtils.serviceBinder.getService().next();
        }
    }

    /**
     * Changes to the previous track
     *
     * @param allowTrackRestart if true, the track will restart if the track position is > 2 seconds
     */
    public static void previous(boolean allowTrackRestart) {
        if (allowTrackRestart && getPosition() > 2000) {
            seekTo(0);
            if (MusicServiceConnectionUtils.serviceBinder != null && MusicServiceConnectionUtils.serviceBinder.getService() != null) {
                MusicServiceConnectionUtils.serviceBinder.getService().play();
            }
        } else {
            if (MusicServiceConnectionUtils.serviceBinder != null && MusicServiceConnectionUtils.serviceBinder.getService() != null) {
                MusicServiceConnectionUtils.serviceBinder.getService().prev();
            }
        }
    }

    /**
     * Plays or pauses the music depending on the current state.
     */
    public static void playOrPause() {
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

    public static int getAudioSessionId() {
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
    public static AlbumArtist getAlbumArtist() {
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
    public static Album getAlbum() {
        if (MusicServiceConnectionUtils.serviceBinder != null && MusicServiceConnectionUtils.serviceBinder.getService() != null) {
            if (getSong() != null) {
                return getSong().getAlbum();
            }
        }
        return null;
    }

    public static Song getSong() {
        if (MusicServiceConnectionUtils.serviceBinder != null && MusicServiceConnectionUtils.serviceBinder.getService() != null) {
            return MusicServiceConnectionUtils.serviceBinder.getService().getSong();
        }
        return null;
    }

    public static Single<Genre> getGenre() {
        if (MusicServiceConnectionUtils.serviceBinder != null && MusicServiceConnectionUtils.serviceBinder.getService() != null) {
            if (getSong() != null) {
                return getSong().getGenre();
            }
        }
        return Single.error(new IllegalStateException("Genre not found"));
    }

    /**
     * Method getPosition.
     *
     * @return {@link long}
     */
    public static long getPosition() {
        if (MusicServiceConnectionUtils.serviceBinder != null && MusicServiceConnectionUtils.serviceBinder.getService() != null) {
            try {
                return MusicServiceConnectionUtils.serviceBinder.getService().getPosition();
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
    public static long getDuration() {
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
    public static void seekTo(final long position) {
        if (MusicServiceConnectionUtils.serviceBinder != null && MusicServiceConnectionUtils.serviceBinder.getService() != null) {
            MusicServiceConnectionUtils.serviceBinder.getService().seekTo(position);
        }
    }

    public static void moveQueueItem(final int from, final int to) {
        if (MusicServiceConnectionUtils.serviceBinder != null && MusicServiceConnectionUtils.serviceBinder.getService() != null) {
            MusicServiceConnectionUtils.serviceBinder.getService().moveQueueItem(from, to);
        }
    }

    public static void toggleShuffleMode() {
        if (MusicServiceConnectionUtils.serviceBinder.getService() == null) {
            return;
        }
        MusicServiceConnectionUtils.serviceBinder.getService().toggleShuffleMode();
    }

    public static void cycleRepeat() {
        if (MusicServiceConnectionUtils.serviceBinder.getService() == null) {
            return;
        }
        MusicServiceConnectionUtils.serviceBinder.getService().toggleRepeat();
    }

    public static void addToQueue(List<Song> songs, UnsafeConsumer<String> onAdded) {
        if (MusicServiceConnectionUtils.serviceBinder.getService() == null) {
            return;
        }
        MusicServiceConnectionUtils.serviceBinder.getService().enqueue(songs, QueueManager.EnqueueAction.LAST);
        onAdded.accept(ShuttleApplication.getInstance().getResources().getQuantityString(R.plurals.NNNtrackstoqueue, songs.size(), songs.size()));
    }

    public static void playNext(Single<List<Song>> songsSingle, UnsafeConsumer<String> onAdded) {
        if (MusicServiceConnectionUtils.serviceBinder.getService() == null) {
            return;
        }
        songsSingle
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(songs -> playNext(songs, onAdded));
    }

    public static void playNext(List<Song> songs, UnsafeConsumer<String> onAdded) {
        if (MusicServiceConnectionUtils.serviceBinder.getService() == null) {
            return;
        }
        MusicServiceConnectionUtils.serviceBinder.getService().enqueue(songs, QueueManager.EnqueueAction.NEXT);
        onAdded.accept(ShuttleApplication.getInstance().getResources().getQuantityString(R.plurals.NNNtrackstoqueue, songs.size(), songs.size()));
    }

    public static void setQueuePosition(final int position) {
        if (MusicServiceConnectionUtils.serviceBinder != null && MusicServiceConnectionUtils.serviceBinder.getService() != null) {
            MusicServiceConnectionUtils.serviceBinder.getService().setQueuePosition(position);
        }
    }

    public static void clearQueue() {
        MusicServiceConnectionUtils.serviceBinder.getService().clearQueue();
    }

    public static List<Song> getQueue() {
        if (MusicServiceConnectionUtils.serviceBinder != null && MusicServiceConnectionUtils.serviceBinder.getService() != null) {
            return MusicServiceConnectionUtils.serviceBinder.getService().getQueue();
        }
        return new ArrayList<>();
    }

    public static int getQueuePosition() {
        if (MusicServiceConnectionUtils.serviceBinder != null && MusicServiceConnectionUtils.serviceBinder.getService() != null) {
            return MusicServiceConnectionUtils.serviceBinder.getService().getQueuePosition();
        }
        return 0;
    }

    public static void removeFromQueue(int position) {
        if (MusicServiceConnectionUtils.serviceBinder != null && MusicServiceConnectionUtils.serviceBinder.getService() != null) {
            MusicServiceConnectionUtils.serviceBinder.getService().removeSong(position);
        }
    }

    public static void removeFromQueue(final List<Song> songs) {
        if (MusicServiceConnectionUtils.serviceBinder != null && MusicServiceConnectionUtils.serviceBinder.getService() != null) {
            MusicServiceConnectionUtils.serviceBinder.getService().removeSongs(songs);
        }
    }

    public static void toggleFavorite() {
        if (MusicServiceConnectionUtils.serviceBinder != null && MusicServiceConnectionUtils.serviceBinder.getService() != null) {
            MusicServiceConnectionUtils.serviceBinder.getService().toggleFavorite();
        }
    }

    public static void closeEqualizerSessions(boolean internal, int audioSessionId) {
        if (MusicServiceConnectionUtils.serviceBinder != null && MusicServiceConnectionUtils.serviceBinder.getService() != null) {
            MusicServiceConnectionUtils.serviceBinder.getService().closeEqualizerSessions(internal, audioSessionId);
        }
    }

    public static void openEqualizerSession(boolean internal, int audioSessionId) {
        if (MusicServiceConnectionUtils.serviceBinder != null && MusicServiceConnectionUtils.serviceBinder.getService() != null) {
            MusicServiceConnectionUtils.serviceBinder.getService().openEqualizerSession(internal, audioSessionId);
        }
    }

    public static void updateEqualizer() {
        if (MusicServiceConnectionUtils.serviceBinder != null && MusicServiceConnectionUtils.serviceBinder.getService() != null) {
            MusicServiceConnectionUtils.serviceBinder.getService().updateEqualizer();
        }
    }
}
