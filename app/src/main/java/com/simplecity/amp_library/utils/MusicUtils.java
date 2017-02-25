package com.simplecity.amp_library.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.widget.Toast;

import com.simplecity.amp_library.R;
import com.simplecity.amp_library.model.Album;
import com.simplecity.amp_library.model.AlbumArtist;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.playback.MusicService;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;

public class MusicUtils {

    private static final String TAG = "MusicUtils";

    public interface Defs {
        int PLAY_NEXT = 1;
        int ADD_TO_PLAYLIST = 2;
        int USE_AS_RINGTONE = 3;
        int PLAYLIST_SELECTED = 4;
        int NEW_PLAYLIST = 5;
        int PLAY_SELECTION = 6;
        int SHUFFLE_ALL = 9;
        int TIMER = 10;
        int LYRICS = 11;
        int QUEUE = 13;
        int CHILD_MENU_BASE = 15;
        int REMOVE = 16;
        int DELETE_ITEM = 18;
        int SET_INITIAL_DIR = 20;
        int RENAME = 21;
        int TAGGER = 22;
        int EQUALIZER = 24;
        int CLEAR_QUEUE = 26;
        int RESCAN = 28;
        int VIEW_AS = 30;
        int OPTIONS = 31;
        int BLACKLIST = 42;
        int GO_TO = 43;
        int GO_TO_ARTIST = 44;
        int GO_TO_ALBUM = 45;
        int TOGGLE_QUEUE = 46;
        int VIEW_INFO = 47;
        int EDIT_ARTWORK = 48;
        int SHARE = 29;

        int ARTIST_FRAGMENT_GROUP_ID = 2;
        int ALBUM_FRAGMENT_GROUP_ID = 3;
        int SONG_FRAGMENT_GROUP_ID = 4;
        int PLAYLIST_FRAGMENT_GROUP_ID = 5;
        int FOLDER_FRAGMENT_GROUP_ID = 6;
        int QUEUE_FRAGMENT_GROUP_ID = 7;
        int SUGGESTED_FRAGMENT_GROUP_ID = 8;
    }

    public interface PlaylistIds {
        long RECENTLY_ADDED_PLAYLIST = -2;
        long MOST_PLAYED_PLAYLIST = -3;
        long PODCASTS_PLAYLIST = -4;
        long RECENTLY_PLAYED_PLAYLIST = -5;
    }

    public interface PlaylistMenuOrder {
        int DELETE_PLAYLIST = Defs.CHILD_MENU_BASE + 1;
        int CLEAR_PLAYLIST = Defs.CHILD_MENU_BASE + 2;
        int EDIT_PLAYLIST = Defs.CHILD_MENU_BASE + 3;
        int RENAME_PLAYLIST = Defs.CHILD_MENU_BASE + 4;
        int EXPORT_PLAYLIST = Defs.CHILD_MENU_BASE + 5;
    }

    public static void playAll(Context context, Observable<List<Song>> songsObservable) {
        songsObservable.observeOn(AndroidSchedulers.mainThread())
                .subscribe(songs -> playAll(songs, () -> {
                    final String message = context.getString(R.string.emptyplaylist);
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                }));
    }

    /**
     * @param songs list of songs to play
     */
    public static void playAll(List<Song> songs, Action0 onEmpty) {
        playAll(songs, 0, false, onEmpty);
    }

    /**
     * @param songs    list of songs to play
     * @param position position of the pressed song
     */
    public static void playAll(List<Song> songs, int position, Action0 onEmpty) {
        playAll(songs, position, false, onEmpty);
    }

    /**
     * Method playAll.
     *
     * @param songs        List<Song>
     * @param position     int
     * @param forceShuffle boolean
     */
    public static void playAll(List<Song> songs, int position, boolean forceShuffle, Action0 onEmpty) {

        if (songs.size() == 0
                || MusicServiceConnectionUtils.sServiceBinder == null
                || MusicServiceConnectionUtils.sServiceBinder.getService() == null) {

            onEmpty.call();
            return;
        }

        if (position < 0) {
            position = 0;
        }

        MusicServiceConnectionUtils.sServiceBinder.getService().open(songs, forceShuffle ? -1 : position);
        MusicServiceConnectionUtils.sServiceBinder.getService().play();
    }

    /**
     * Shuffles the passed in song list
     */
    public static void shuffleAll(Context context, Observable<List<Song>> songsObservable) {
        songsObservable.observeOn(AndroidSchedulers.mainThread())
                .subscribe(songs -> {
                    setShuffleMode(MusicService.ShuffleMode.ON);
                    playAll(songs, 0, true, () -> {
                        final String message = context.getString(R.string.emptyplaylist);
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                    });
                });
    }

    /**
     * Shuffles all songs on the device
     */
    public static void shuffleAll(Context context) {
        shuffleAll(context, DataManager.getInstance().getSongsRelay().first());
    }

    /**
     * @param uri The source of the file
     */
    public static void playFile(final Uri uri) {
        if (uri == null
                || MusicServiceConnectionUtils.sServiceBinder == null
                || MusicServiceConnectionUtils.sServiceBinder.getService() == null) {
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

        MusicServiceConnectionUtils.sServiceBinder.getService().stop();
        MusicServiceConnectionUtils.sServiceBinder.getService().openFile(filename, () ->
                MusicServiceConnectionUtils.sServiceBinder.getService().play());
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
        if (MusicServiceConnectionUtils.sServiceBinder != null && MusicServiceConnectionUtils.sServiceBinder.getService() != null) {
            return MusicServiceConnectionUtils.sServiceBinder.getService().getPath();
        }
        return null;
    }

    /**
     * @return True if we're playing music, false otherwise.
     */
    public static boolean isPlaying() {
        if (MusicServiceConnectionUtils.sServiceBinder != null && MusicServiceConnectionUtils.sServiceBinder.getService() != null) {
            return MusicServiceConnectionUtils.sServiceBinder.getService().isPlaying();
        }
        return false;
    }

    /**
     * @return The current shuffle mode
     */
    public static int getShuffleMode() {
        if (MusicServiceConnectionUtils.sServiceBinder != null && MusicServiceConnectionUtils.sServiceBinder.getService() != null) {
            return MusicServiceConnectionUtils.sServiceBinder.getService().getShuffleMode();
        }
        return 0;
    }

    /**
     * Sets the shuffle mode
     */
    public static void setShuffleMode(int mode) {
        if (MusicServiceConnectionUtils.sServiceBinder != null && MusicServiceConnectionUtils.sServiceBinder.getService() != null) {
            MusicServiceConnectionUtils.sServiceBinder.getService().setShuffleMode(mode);
        }
    }

    /**
     * @return The current repeat mode
     */
    public static int getRepeatMode() {
        if (MusicServiceConnectionUtils.sServiceBinder != null && MusicServiceConnectionUtils.sServiceBinder.getService() != null) {
            return MusicServiceConnectionUtils.sServiceBinder.getService().getRepeatMode();
        }
        return 0;
    }

    /**
     * Changes to the next track
     */
    public static void next() {
        if (MusicServiceConnectionUtils.sServiceBinder != null && MusicServiceConnectionUtils.sServiceBinder.getService() != null) {
            MusicServiceConnectionUtils.sServiceBinder.getService().next();
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
            if (MusicServiceConnectionUtils.sServiceBinder != null && MusicServiceConnectionUtils.sServiceBinder.getService() != null) {
                MusicServiceConnectionUtils.sServiceBinder.getService().play();
            }
        } else {
            if (MusicServiceConnectionUtils.sServiceBinder != null && MusicServiceConnectionUtils.sServiceBinder.getService() != null) {
                MusicServiceConnectionUtils.sServiceBinder.getService().prev();
            }
        }
    }

    /**
     * Plays or pauses the music depending on the current state.
     */
    public static void playOrPause() {
        try {
            if (MusicServiceConnectionUtils.sServiceBinder != null && MusicServiceConnectionUtils.sServiceBinder.getService() != null) {
                if (MusicServiceConnectionUtils.sServiceBinder.getService().isPlaying()) {
                    MusicServiceConnectionUtils.sServiceBinder.getService().pause();
                } else {
                    MusicServiceConnectionUtils.sServiceBinder.getService().play();
                }
            }
        } catch (final Exception ignored) {
        }
    }

    /**
     * Method getArtistId.
     *
     * @return long
     */
    public static long getArtistId() {
        if (MusicServiceConnectionUtils.sServiceBinder != null && MusicServiceConnectionUtils.sServiceBinder.getService() != null) {
            return MusicServiceConnectionUtils.sServiceBinder.getService().getArtistId();
        }
        return -1;
    }

    /**
     * Method getAlbumId.
     *
     * @return long
     */
    public static long getAlbumId() {
        if (MusicServiceConnectionUtils.sServiceBinder != null && MusicServiceConnectionUtils.sServiceBinder.getService() != null) {
            return MusicServiceConnectionUtils.sServiceBinder.getService().getAlbumId();
        }
        return -1;
    }

    /**
     * Method getSong.
     *
     * @return long
     */
    public static long getSongId() {
        if (MusicServiceConnectionUtils.sServiceBinder != null && MusicServiceConnectionUtils.sServiceBinder.getService() != null) {
            return MusicServiceConnectionUtils.sServiceBinder.getService().getSongId();
        }
        return -1;
    }

    public static int getAudioSessionId() {
        if (MusicServiceConnectionUtils.sServiceBinder != null && MusicServiceConnectionUtils.sServiceBinder.getService() != null) {
            return MusicServiceConnectionUtils.sServiceBinder.getService().getAudioSessionId();
        }
        return 0;
    }

    public static String getAlbumName() {
        if (MusicServiceConnectionUtils.sServiceBinder != null && MusicServiceConnectionUtils.sServiceBinder.getService() != null) {
            return MusicServiceConnectionUtils.sServiceBinder.getService().getAlbumName();
        }
        return null;
    }

    public static String getAlbumArtistName() {
        if (MusicServiceConnectionUtils.sServiceBinder != null && MusicServiceConnectionUtils.sServiceBinder.getService() != null) {
            return MusicServiceConnectionUtils.sServiceBinder.getService().getAlbumArtistName();
        }
        return null;
    }

    public static String getSongName() {
        if (MusicServiceConnectionUtils.sServiceBinder != null && MusicServiceConnectionUtils.sServiceBinder.getService() != null) {
            return MusicServiceConnectionUtils.sServiceBinder.getService().getSongName();
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
        if (MusicServiceConnectionUtils.sServiceBinder != null && MusicServiceConnectionUtils.sServiceBinder.getService() != null) {
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
        if (MusicServiceConnectionUtils.sServiceBinder != null && MusicServiceConnectionUtils.sServiceBinder.getService() != null) {
            if (getSong() != null) {
                return getSong().getAlbum();
            }
        }
        return null;
    }

    public static Song getSong() {
        if (MusicServiceConnectionUtils.sServiceBinder != null && MusicServiceConnectionUtils.sServiceBinder.getService() != null) {
            return MusicServiceConnectionUtils.sServiceBinder.getService().getSong();
        }
        return null;
    }

    /**
     * Method getPosition.
     *
     * @return {@link long}
     */
    public static long getPosition() {
        if (MusicServiceConnectionUtils.sServiceBinder != null && MusicServiceConnectionUtils.sServiceBinder.getService() != null) {
            try {
                return MusicServiceConnectionUtils.sServiceBinder.getService().getPosition();
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
        if (MusicServiceConnectionUtils.sServiceBinder != null && MusicServiceConnectionUtils.sServiceBinder.getService() != null) {
            try {
                return MusicServiceConnectionUtils.sServiceBinder.getService().getDuration();
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
        if (MusicServiceConnectionUtils.sServiceBinder != null && MusicServiceConnectionUtils.sServiceBinder.getService() != null) {
            MusicServiceConnectionUtils.sServiceBinder.getService().seekTo(position);
        }
    }

    public static void moveQueueItem(final int from, final int to) {
        if (MusicServiceConnectionUtils.sServiceBinder != null && MusicServiceConnectionUtils.sServiceBinder.getService() != null) {
            MusicServiceConnectionUtils.sServiceBinder.getService().moveQueueItem(from, to);
        }
    }

    static Observable<Boolean> isFavorite() {
        if (MusicServiceConnectionUtils.sServiceBinder != null && MusicServiceConnectionUtils.sServiceBinder.getService() != null) {
            return MusicServiceConnectionUtils.sServiceBinder.getService().isFavorite();
        }
        return Observable.just(false);
    }

    public static void toggleLockscreenArtwork() {
        if (MusicServiceConnectionUtils.sServiceBinder.getService() == null) {
            return;
        }
        MusicServiceConnectionUtils.sServiceBinder.getService().notifyChange(MusicService.InternalIntents.META_CHANGED);
    }

    public static void toggleShuffleMode() {
        if (MusicServiceConnectionUtils.sServiceBinder.getService() == null) {
            return;
        }
        MusicServiceConnectionUtils.sServiceBinder.getService().toggleShuffleMode();
    }

    public static void cycleRepeat() {
        if (MusicServiceConnectionUtils.sServiceBinder.getService() == null) {
            return;
        }
        MusicServiceConnectionUtils.sServiceBinder.getService().toggleRepeat();
    }

    public static void addToQueue(Context context, List<Song> songs) {
        if (MusicServiceConnectionUtils.sServiceBinder.getService() == null) {
            return;
        }
        MusicServiceConnectionUtils.sServiceBinder.getService().enqueue(songs, MusicService.EnqueueAction.LAST);
        final String message = context.getResources().getQuantityString(R.plurals.NNNtrackstoqueue, songs.size(), songs.size());
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public static void playNext(Context context, List<Song> songs) {
        if (MusicServiceConnectionUtils.sServiceBinder.getService() == null) {
            return;
        }
        MusicServiceConnectionUtils.sServiceBinder.getService().enqueue(songs, MusicService.EnqueueAction.NEXT);
        final String message = context.getResources().getQuantityString(R.plurals.NNNtrackstoqueue, songs.size(), songs.size());
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public static void setQueuePosition(final int position) {
        if (MusicServiceConnectionUtils.sServiceBinder != null && MusicServiceConnectionUtils.sServiceBinder.getService() != null) {
            MusicServiceConnectionUtils.sServiceBinder.getService().setQueuePosition(position);
        }
    }

    public static void clearQueue() {
        MusicServiceConnectionUtils.sServiceBinder.getService().clearQueue();
    }

    public static void setTimer(int sleepNumber) {
        if (MusicServiceConnectionUtils.sServiceBinder == null || MusicServiceConnectionUtils.sServiceBinder.getService() == null) {
            return;
        }
        MusicServiceConnectionUtils.sServiceBinder.getService().sleep(sleepNumber);
    }

    public static void stopTimer() {
        if (MusicServiceConnectionUtils.sServiceBinder == null || MusicServiceConnectionUtils.sServiceBinder.getService() == null) {
            return;
        }
        MusicServiceConnectionUtils.sServiceBinder.getService().stopTimer();
    }

    public static List<Song> getQueue() {
        if (MusicServiceConnectionUtils.sServiceBinder != null && MusicServiceConnectionUtils.sServiceBinder.getService() != null) {
            return MusicServiceConnectionUtils.sServiceBinder.getService().getQueue();
        }
        return new ArrayList<>();
    }

    public static boolean getTimerActive() {
        if (MusicServiceConnectionUtils.sServiceBinder == null || MusicServiceConnectionUtils.sServiceBinder.getService() == null) {
            return false;
        }
        return MusicServiceConnectionUtils.sServiceBinder.getService().isTimerActive();
    }

    public static long getTimeRemaining() {
        if (MusicServiceConnectionUtils.sServiceBinder.getService() == null) {
            return 0;
        }
        return MusicServiceConnectionUtils.sServiceBinder.getService().timeRemaining();
    }

    public static int getQueuePosition() {
        if (MusicServiceConnectionUtils.sServiceBinder != null && MusicServiceConnectionUtils.sServiceBinder.getService() != null) {
            return MusicServiceConnectionUtils.sServiceBinder.getService().getQueuePosition();
        }
        return 0;
    }

    public static void removeFromQueue(final Song song, boolean notify) {
        if (MusicServiceConnectionUtils.sServiceBinder != null && MusicServiceConnectionUtils.sServiceBinder.getService() != null) {
            MusicServiceConnectionUtils.sServiceBinder.getService().removeTrack(song, notify);
        }
    }

    public static void removeFromQueue(final List<Song> songs, boolean notify) {
        if (MusicServiceConnectionUtils.sServiceBinder != null && MusicServiceConnectionUtils.sServiceBinder.getService() != null) {
            MusicServiceConnectionUtils.sServiceBinder.getService().removeTracks(songs, notify);
        }
    }
}
