package com.simplecity.amp_library.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;

import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.model.Album;
import com.simplecity.amp_library.model.AlbumArtist;
import com.simplecity.amp_library.model.Genre;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.playback.MusicService;
import com.simplecity.amp_library.rx.UnsafeConsumer;

import java.util.ArrayList;
import java.util.List;

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
     * Passes along a list of songs, wrapped in a Single obj, to be played, starting at position 0.
     */
    public static void playAll(Single<List<Song>> songsSingle, UnsafeConsumer<String> onEmpty) {
        songsSingle.observeOn(AndroidSchedulers.mainThread())
                .subscribe(songs -> {
                    setShuffleMode(MusicService.ShuffleMode.OFF);
                    playAll(songs, onEmpty);
                });
    }

    /**
     * Play a list of songs starting at a given position.
     */
    public static void playAll(List<Song> songs, int position, UnsafeConsumer<String> onEmpty) {
        setShuffleMode(MusicService.ShuffleMode.OFF);
        playAll(songs, position, MusicService.ShuffleMode.OFF, onEmpty);
    }

    /**
     * Passes along a list of songs to be played, starting at position 0.
     */
    private static void playAll(List<Song> songs, UnsafeConsumer<String> onEmpty) {
        playAll(songs, 0, MusicService.ShuffleMode.OFF, onEmpty);
    }

    /**
     * Sends a list of songs to the MusicService for playback.
     */
    private static void playAll(List<Song> songs, int position, int shuffleMode, UnsafeConsumer<String> onEmpty) {
        if (songs.size() == 0
                || MusicServiceConnectionUtils.serviceBinder == null
                || MusicServiceConnectionUtils.serviceBinder.getService() == null) {

            onEmpty.accept(ShuttleApplication.getInstance().getResources().getString(R.string.empty_playlist));
            return;
        }

        if (position < 0) {
            position = 0;
        }

        MusicServiceConnectionUtils.serviceBinder.getService().open(songs, position, shuffleMode);
        MusicServiceConnectionUtils.serviceBinder.getService().play();
    }

    /**
     * Shuffles all songs on device in album shuffle mode
     */
    public static void shuffleAll(UnsafeConsumer<String> onEmpty) {
        shuffleAll(DataManager.getInstance().getSongsRelay().firstOrError(), onEmpty);
    }

    /**
     * Shuffles all songs in a given song list
     */
    public static void shuffleAll(Single<List<Song>> songsSingle, UnsafeConsumer<String> onEmpty) {
        songsSingle.observeOn(AndroidSchedulers.mainThread())
                .subscribe(songs -> {
                    setShuffleMode(MusicService.ShuffleMode.ON);
                    playAll(songs, 0, getShuffleMode(), onEmpty);
                }, e -> LogUtils.logException(TAG, "Shuffle all error", e));
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
     * Method getIntPref.
     *
     * @param context Context
     * @param name    String
     * @param def     int
     * @return int
     */
    public static int getIntPref(Context context, String name, int def) {
        final SharedPreferences prefs = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        return prefs.getInt(name, def);
    }

    /**
     * Method setIntPref.
     *
     * @param context Context
     * @param name    String
     * @param value   int
     */
    static void setIntPref(Context context, String name, int value) {
        final SharedPreferences prefs = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        final Editor editor = prefs.edit();
        editor.putInt(name, value);
        editor.apply();
    }

    /**
     * @return {@link String} The path to the currently playing file
     */
    public static String getFilePath() {
        if (MusicServiceConnectionUtils.serviceBinder != null && MusicServiceConnectionUtils.serviceBinder.getService() != null) {
            return MusicServiceConnectionUtils.serviceBinder.getService().getPath();
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

    public static String getAlbumName() {
        if (MusicServiceConnectionUtils.serviceBinder != null && MusicServiceConnectionUtils.serviceBinder.getService() != null) {
            return MusicServiceConnectionUtils.serviceBinder.getService().getAlbumName();
        }
        return null;
    }

    public static String getAlbumArtistName() {
        if (MusicServiceConnectionUtils.serviceBinder != null && MusicServiceConnectionUtils.serviceBinder.getService() != null) {
            return MusicServiceConnectionUtils.serviceBinder.getService().getAlbumArtistName();
        }
        return null;
    }

    public static String getSongName() {
        if (MusicServiceConnectionUtils.serviceBinder != null && MusicServiceConnectionUtils.serviceBinder.getService() != null) {
            return MusicServiceConnectionUtils.serviceBinder.getService().getSongName();
        }
        return null;
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
            try {
                return MusicServiceConnectionUtils.serviceBinder.getService().getDuration();
            } catch (Exception ignored) {
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
        MusicServiceConnectionUtils.serviceBinder.getService().enqueue(songs, MusicService.EnqueueAction.LAST);
        onAdded.accept(ShuttleApplication.getInstance().getResources().getQuantityString(R.plurals.NNNtrackstoqueue, songs.size(), songs.size()));
    }

    public static void playNext(List<Song> songs, UnsafeConsumer<String> onAdded) {
        if (MusicServiceConnectionUtils.serviceBinder.getService() == null) {
            return;
        }
        MusicServiceConnectionUtils.serviceBinder.getService().enqueue(songs, MusicService.EnqueueAction.NEXT);
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
}
