package com.simplecity.amp_library.playback;

import android.Manifest;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;
import com.simplecity.amp_library.data.Repository;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.playback.constants.InternalIntents;
import com.simplecity.amp_library.services.Equalizer;
import com.simplecity.amp_library.ui.screens.queue.QueueItem;
import com.simplecity.amp_library.ui.screens.queue.QueueItemKt;
import com.simplecity.amp_library.utils.LogUtils;
import com.simplecity.amp_library.utils.SettingsManager;
import com.simplecity.amp_library.utils.SleepTimer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import java.util.List;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class PlaybackManager implements Playback.Callbacks {

    private static final String TAG = "PlaybackManager";

    private Context context;

    private QueueManager queueManager;

    private PlaybackSettingsManager playbackSettingsManager;

    private MediaSessionManager mediaSessionManager;

    private Equalizer equalizer;

    private long lastPlayedTime;

    private CompositeDisposable disposables = new CompositeDisposable();

    private boolean pauseOnTrackFinish = false;

    private MusicService.Callbacks musicServiceCallbacks;

    private Repository.SongsRepository songsRepository;

    private SettingsManager settingsManager;

    private boolean playOnQueueReload = false;

    @NonNull
    Playback playback;

    PlaybackManager(
            Context context,
            QueueManager queueManager,
            PlaybackSettingsManager playbackSettingsManager,
            Repository.SongsRepository songsRepository,
            Repository.AlbumsRepository albumsRepository,
            Repository.AlbumArtistsRepository albumArtistsRepository,
            Repository.GenresRepository genresRepository,
            Repository.PlaylistsRepository playlistsRepository,
            MusicService.Callbacks musicServiceCallbacks,
            SettingsManager settingsManager
    ) {

        playback = new MediaPlayerPlayback(context);
        playback.setCallbacks(this);

        this.context = context.getApplicationContext();

        this.queueManager = queueManager;

        this.playbackSettingsManager = playbackSettingsManager;

        this.musicServiceCallbacks = musicServiceCallbacks;

        this.songsRepository = songsRepository;

        this.settingsManager = settingsManager;

        mediaSessionManager = new MediaSessionManager(
                context, queueManager,
                this,
                playbackSettingsManager,
                settingsManager,
                songsRepository,
                albumsRepository,
                albumArtistsRepository,
                genresRepository,
                playlistsRepository
        );

        equalizer = new Equalizer(context, settingsManager);

        disposables.add(SleepTimer.getInstance().getCurrentTimeObservable()
                .subscribe(remainingTime -> {
                    if (remainingTime == 0) {
                        if (SleepTimer.getInstance().playToEnd) {
                            pauseOnTrackFinish = true;
                        } else {
                            stop(true);
                        }
                    }
                }, throwable -> LogUtils.logException(TAG, "Error consuming SleepTimer observable", throwable)));
    }

    @NonNull
    public Playback getPlayback() {
        return playback;
    }

    public void setQueuePosition(int position) {
        stop(false);
        queueManager.queuePosition = position;
        load(true, true, 0);
    }

    void clearQueue() {
        stop(true);
        queueManager.clearQueue();
    }

    void reloadQueue() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        disposables.add(queueManager.reloadQueue(() -> {
                    load(true, playOnQueueReload, playbackSettingsManager.getSeekPosition());
                    playOnQueueReload = false;
                    return Unit.INSTANCE;
                })
        );
    }

    void removeQueueItems(List<QueueItem> queueItems) {
        queueManager.removeQueueItems(queueItems, () -> stop(true), () -> {
            boolean wasPlaying = isPlaying();
            stop(false);
            load(true, wasPlaying, 0);
        });
    }

    void removeSongs(List<Song> songs) {
        queueManager.removeSongs(songs, () -> stop(true), () -> {
            boolean wasPlaying = isPlaying();
            stop(false);
            load(true, wasPlaying, 0);
        });
    }

    void removeQueueItem(QueueItem queueItem) {
        queueManager.removeQueueItem(queueItem, () -> stop(true), () -> {
            boolean wasPlaying = isPlaying();
            stop(false);
            load(true, wasPlaying, 0);
        });
    }

    void moveQueueItem(int from, int to) {
        queueManager.moveQueueItem(from, to);
    }

    private void notifyChange(String what) {
        musicServiceCallbacks.notifyChange(what);
    }

    void playAutoShuffleList() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            disposables.add(songsRepository.getSongs((Function1<? super Song, Boolean>) null)
                    .firstOrError()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(songs -> {
                        queueManager.playlist = QueueItemKt.toQueueItems(songs);
                        queueManager.queuePosition = -1;
                        queueManager.makeShuffleList();
                        queueManager.setShuffleMode(QueueManager.ShuffleMode.ON);
                        notifyChange(InternalIntents.QUEUE_CHANGED);
                        queueManager.queuePosition = 0;
                        load(true, true, 0);
                    }, error -> LogUtils.logException(TAG, "Error playing auto shuffle list", error)));
        } else {
            queueManager.shuffleMode = QueueManager.ShuffleMode.OFF;
            queueManager.saveQueue(false);
        }
    }

    MediaSessionCompat.Token getMediaSessionToken() {
        return mediaSessionManager.getSessionToken();
    }

    void closeEqualizerSessions(boolean internal, int audioSessionId) {
        equalizer.closeEqualizerSessions(internal, audioSessionId);
    }

    void openEqualizerSession(boolean internal, int audioSessionId) {
        equalizer.openEqualizerSession(internal, audioSessionId);
    }

    void updateEqualizer() {
        equalizer.update();
    }

    /**
     * @param force true to ignore the current repeat mode.
     * @return The next position to play, or -1 of playback should complete.
     */
    private int getNextPosition(boolean force) {
        return queueManager.getNextPosition(force);
    }

    public void load(@NonNull List<Song> songs, int queuePosition, Boolean playWhenReady, long seekPosition) {
        queueManager.load(
                songs,
                queuePosition,
                () -> load(true, playWhenReady, seekPosition)
        );
    }

    private void load(boolean setNext, Boolean playWhenReady, long seekPosition) {
        if (queueManager.getCurrentPlaylist().isEmpty() || queueManager.queuePosition < 0 || queueManager.queuePosition >= queueManager.getCurrentPlaylist().size()) {
            return;
        }

        stop(false);

        loadAttempt(1, playWhenReady, seekPosition, success -> {
            if (success) {

                notifyChange(InternalIntents.QUEUE_CHANGED);
                notifyChange(InternalIntents.META_CHANGED);

                restoreBookmark();

                if (setNext) {
                    setNextTrack();
                }
            } else {
                musicServiceCallbacks.scheduleDelayedShutdown();
            }
            return Unit.INSTANCE;
        });
    }

    private void loadAttempt(int attempt, Boolean playWhenReady, long seekPosition, @Nullable Function1<Boolean, Unit> completion) {

        Song song = queueManager.getCurrentSong();
        if (song == null) {
            if (completion != null) {
                completion.invoke(false);
            }
            return;
        }

        load(song, playWhenReady, seekPosition, success -> {
            if (success) {
                if (completion != null) {
                    completion.invoke(true);
                }
            } else {
                if (attempt < 10) {
                    int position = getNextPosition(false);
                    if (position < 0) {
                        if (completion != null) {
                            completion.invoke(false);
                        }
                    } else {
                        queueManager.queuePosition = position;
                        queueManager.saveQueue(false);
                        loadAttempt(attempt + 1, playWhenReady, seekPosition, completion);
                    }
                } else {
                    if (completion != null) {
                        completion.invoke(false);
                    }
                }
            }
            return Unit.INSTANCE;
        });
    }

    private void load(@NonNull Song song, Boolean playWhenReady, long seekPosition, @Nullable Function1<Boolean, Unit> completion) {
        playback.load(song, playWhenReady, seekPosition, success -> {
            if (success) {
                if (playWhenReady) {
                    musicServiceCallbacks.cancelShutdown();
                }
                if (completion != null) {
                    completion.invoke(true);
                }
            } else {
                stop(true);
                if (completion != null) {
                    completion.invoke(false);
                }
            }
            return Unit.INSTANCE;
        });
    }

    void loadFile(String path, Boolean playWhenReady) {
        if (path == null) {
            return;
        }

        Uri uri = Uri.parse(path);
        long id = -1;
        try {
            id = Long.valueOf(uri.getLastPathSegment());
        } catch (NumberFormatException ignored) {
        }

        Function1<? super Song, Boolean> predicate;

        long finalId = id;
        if (finalId != -1 && (path.startsWith(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString()) || path.startsWith(MediaStore.Files.getContentUri("external").toString()))) {
            predicate = song -> song.id == finalId;
        } else {
            if (uri != null && path.startsWith("content://")) {
                path = uri.getPath();
            }
            String finalPath = path;
            predicate = song -> song.path.contains(finalPath);
        }

        disposables.add(songsRepository.getSongs(predicate)
                .firstOrError()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(songs -> {
                    if (!songs.isEmpty() && queueManager.getCurrentSong() != null) {
                        load(queueManager.getCurrentSong(), playWhenReady, (long) 0, null);
                    }
                }, error -> LogUtils.logException(TAG, "Error opening file", error)));
    }

    private void restoreBookmark() {
        // Go to bookmark if needed
        if (queueManager.getCurrentSong() != null && queueManager.getCurrentSong().isPodcast) {
            long bookmark = queueManager.getCurrentSong().bookMark;
            // Start playing a little bit before the bookmark, so it's easier to get back in to the narrative.
            seekTo(bookmark - 5000);
        }
    }

    void setNextTrack() {
        queueManager.nextPlayPos = getNextPosition(false);
        if (queueManager.nextPlayPos >= 0
                && !queueManager.getCurrentPlaylist().isEmpty()
                && queueManager.nextPlayPos < queueManager.getCurrentPlaylist().size()) {
            final Song nextSong = queueManager.getCurrentPlaylist().get(queueManager.nextPlayPos).getSong();
            playback.setNextDataSource(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI + "/" + nextSong.id);
        } else {
            playback.setNextDataSource(null);
        }
    }

    void enqueue(List<Song> songs, int action) {
        queueManager.enqueue(
                songs,
                action,
                this::setNextTrack,
                () -> load(true, true, 0));
    }

    void saveState() {
        if (playback.isInitialized()) {
            playbackSettingsManager.setSeekPosition(playback.getPosition());
        }
    }

    void release() {
        if (isPlaying() || playback.willResumePlayback()) {
            return;
        }

        mediaSessionManager.setActive(false);
    }

    void destroy() {

        playback.stop();

        // Release all MediaPlayer resources, including the native player and wakelocks
        playback.release();

        disposables.clear();

        mediaSessionManager.destroy();

        equalizer.release();
        equalizer.closeEqualizerSessions(true, getAudioSessionId());
    }

    boolean recentlyPlayed() {
        return isPlaying() || System.currentTimeMillis() - lastPlayedTime < 5 * 60 * 1000 /* 5 mins */;
    }

    int getAudioSessionId() {
        return playback.getAudioSessionId();
    }

    public void seekTo(long position) {
        if (position < 0) {
            position = 0;
        } else if (position > playback.getDuration()) {
            position = playback.getDuration();
        }

        playback.seekTo(position);

        notifyChange(InternalIntents.POSITION_CHANGED);
    }

    long getSeekPosition() {
        return playback.getPosition();
    }

    public void pause(boolean fade) {
        if (isPlaying()) {
            updateLastPlayedTime();
            saveBookmarkIfNeeded();
        }
        playback.pause(fade);
        equalizer.closeEqualizerSessions(false, getAudioSessionId());
        notifyChange(InternalIntents.PLAY_STATE_CHANGED);
        musicServiceCallbacks.scheduleDelayedShutdown();
    }

    private void updateLastPlayedTime() {
        lastPlayedTime = System.currentTimeMillis();
    }

    public boolean isPlaying() {
        return playback.isPlaying();
    }

    boolean willResumePlayback() {
        return playback.willResumePlayback();
    }

    public void stop(boolean goToIdle) {

        if (isPlaying()) {
            updateLastPlayedTime();
            saveBookmarkIfNeeded();
        }

        playback.stop();

        if (goToIdle) {
            musicServiceCallbacks.scheduleDelayedShutdown();
            lastPlayedTime = System.currentTimeMillis();
        } else {
            musicServiceCallbacks.stopForegroundImpl(false, true);
        }
    }

    /**
     * @param force true to ignore the repeat mode.
     * @return true if the we've successfully moved to the next track.
     */
    public boolean next(boolean force) {
        notifyChange(InternalIntents.TRACK_ENDING);

        int nextPosition = getNextPosition(force);
        if (nextPosition < 0) {
            musicServiceCallbacks.scheduleDelayedShutdown();
            return false;
        }

        setQueuePosition(nextPosition);
        return true;
    }

    /**
     * @param force true to ignore the current seek position & repeat mode.
     */
    public void previous(boolean force) {
        if (force || getSeekPosition() <= 2000) {
            queueManager.previous();
            stop(false);
            load(false, true, 0);
        } else {
            seekTo(0);
            play();
        }
    }

    public void play() {
        if (settingsManager.getEqualizerEnabled()) {
            //Shutdown any existing external audio sessions
            equalizer.closeEqualizerSessions(false, getAudioSessionId());

            //Start internal equalizer session (will only turn on if enabled)
            equalizer.openEqualizerSession(true, getAudioSessionId());
        } else {
            equalizer.openEqualizerSession(false, getAudioSessionId());
        }

        mediaSessionManager.setActive(true);

        if (playback.isInitialized()) {
            // If we are at the end of the song, go to the next song first
            long duration = playback.getDuration();
            if (queueManager.repeatMode != QueueManager.RepeatMode.ONE && duration > 2000 && playback.getPosition() >= duration - 2000) {
                next(true);
            } else {
                playback.start();
            }

            musicServiceCallbacks.cancelShutdown();
            musicServiceCallbacks.updateNotification();
        } else if (queueManager.getCurrentPlaylist().isEmpty()) {
            // This is mostly so that if you press 'play' on a bluetooth headset without ever having played anything before, it will still play something.
            if (queueManager.queueReloading) {
                playOnQueueReload = true;
            } else {
                playAutoShuffleList();
            }
        }

        notifyChange(InternalIntents.PLAY_STATE_CHANGED);
    }

    void togglePlayback() {
        if (isPlaying()) {
            pause(true);
        } else {
            play();
        }
    }

    private void saveBookmarkIfNeeded() {
        Song currentSong = queueManager.getCurrentSong();
        if (currentSong != null && currentSong.isPodcast) {
            long pos = getSeekPosition();
            long duration = queueManager.getCurrentSong().duration;
            if (pos < 5000 || (pos + 5000) > duration) {
                // If we're near the start or end, clear the bookmark
                pos = 0;
            }

            currentSong.bookMark = pos;

            try {
                // Write 'pos' to the bookmark field
                ContentValues values = new ContentValues();
                values.put(MediaStore.Audio.Media.BOOKMARK, pos);
                Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, queueManager.getCurrentSong().id);
                if (uri != null) {
                    context.getContentResolver().update(uri, values, null, null);
                } else {
                    Log.e(TAG, "Save bookmark failed (uri null)");
                }
            } catch (SQLiteException error) {
                Log.e(TAG, "Save bookmark failed, error: " + error.getLocalizedMessage());
            }
        }
    }

    @Override
    public void onTrackEnded(@NonNull Playback playback, boolean trackDidChange) {
        if (getPlayback() != playback) return;

        notifyChange(InternalIntents.TRACK_ENDING);

        if (pauseOnTrackFinish) {
            pause(false);
            pauseOnTrackFinish = false;
            return;
        }

        if (queueManager.repeatMode == QueueManager.RepeatMode.ONE) {
            seekTo(0);
            play();
            return;
        }

        if (trackDidChange) {
            queueManager.queuePosition = queueManager.nextPlayPos;
            notifyChange(InternalIntents.META_CHANGED);
            setNextTrack();
        } else {
            if (!next(false)) {
                // If we failed to move to the next track, then playback is complete.
                notifyChange(InternalIntents.PLAY_STATE_CHANGED);
            }
        }
    }

    @Override
    public void onPlayStateChanged(@NonNull Playback playback) {
        if (getPlayback() != playback) return;

        notifyChange(InternalIntents.PLAY_STATE_CHANGED);
    }

    @Override
    public void onError(@NonNull Playback playback, @NonNull String message) {
        if (getPlayback() != playback) return;

        if (isPlaying()) {
            next(true);
        } else {
            load(true, false, 0);
        }
    }

    public void switchToPlayback(@NonNull Playback playback, long seekPosition) {
        Playback oldPlayback = this.playback;
        boolean wasPlaying = oldPlayback.isPlaying();

        this.playback = playback;

        playback.setCallbacks(this);
        playback.seekTo(seekPosition);

        boolean playWhenReady = wasPlaying && playback.getResumeWhenSwitched();

        if (wasPlaying && !playWhenReady) {
            // If we were playing, and now we're not, we need to update the playback state
            notifyChange(InternalIntents.PLAY_STATE_CHANGED);
        }

        oldPlayback.stop();

        Song song = queueManager.getCurrentSong();
        if (song != null) {
            playback.load(song, playWhenReady, seekPosition, null);
        } else {
            Log.e(TAG, "Current song null");
        }
    }
}