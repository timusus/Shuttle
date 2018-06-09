package com.simplecity.amp_library.playback;

import android.Manifest;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteException;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.net.Uri;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;
import com.annimon.stream.function.Predicate;
import com.crashlytics.android.core.CrashlyticsCore;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.CastException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.NoConnectionException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.TransientNetworkDisconnectionException;
import com.simplecity.amp_library.http.HttpServer;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.playback.constants.InternalIntents;
import com.simplecity.amp_library.playback.constants.PlayerHandler;
import com.simplecity.amp_library.services.Equalizer;
import com.simplecity.amp_library.ui.queue.QueueItem;
import com.simplecity.amp_library.ui.queue.QueueItemKt;
import com.simplecity.amp_library.utils.DataManager;
import com.simplecity.amp_library.utils.LogUtils;
import com.simplecity.amp_library.utils.SettingsManager;
import com.simplecity.amp_library.utils.ShuttleUtils;
import com.simplecity.amp_library.utils.SleepTimer;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Action;
import io.reactivex.schedulers.Schedulers;
import java.util.List;

public class PlaybackManager {

    private static final String TAG = "PlaybackManager";

    @interface PlaybackLocation {
        int REMOTE = 0;
        int LOCAL = 1;
    }

    @interface PlaybackState {
        int PLAYING = 0;
        int PAUSED = 1;
        int STOPPED = 2;
    }

    private Context context;

    private HandlerThread handlerThread;

    private QueueManager queueManager;

    private ChromecastManager chromecastManager;

    private MediaSessionManager mediaSessionManager;

    private boolean playOnQueueLoad;

    @PlaybackState
    private int playbackState;

    private Equalizer equalizer;

    @PlaybackLocation
    private int playbackLocation = PlaybackLocation.LOCAL;

    @Nullable
    private AudioFocusRequest audioFocusRequest;

    @Nullable
    MultiPlayer player;

    private AudioManager audioManager;

    private int openFailedCounter = 0;

    private boolean isSupposedToBePlaying = false;

    private long lastPlayedTime;

    private MediaPlayerHandler playerHandler;

    // Used to track what type of audio focus loss caused the playback to pause
    boolean pausedByTransientLossOfFocus = false;

    private CompositeDisposable disposables = new CompositeDisposable();

    boolean pauseOnTrackFinish = false;

    private MusicService.Callbacks musicServiceCallbacks;

    private AudioManager.OnAudioFocusChangeListener audioFocusListener = new AudioManager.OnAudioFocusChangeListener() {
        public void onAudioFocusChange(final int focusChange) {
            playerHandler.obtainMessage(PlayerHandler.FOCUS_CHANGE, focusChange, 0).sendToTarget();
        }
    };

    PlaybackManager(Context context, QueueManager queueManager, MusicService.Callbacks musicServiceCallbacks) {

        this.context = context.getApplicationContext();

        this.queueManager = queueManager;

        this.musicServiceCallbacks = musicServiceCallbacks;

        mediaSessionManager = new MediaSessionManager(context, queueManager, this, musicServiceCallbacks);

        chromecastManager = new ChromecastManager(context, queueManager, this);

        // Start up the thread running the service. Note that we create a separate thread because the service normally runs in the process's
        // main thread, which we don't want to block. We also make it background priority so CPU-intensive work will not disrupt the UI.
        handlerThread = new HandlerThread("MusicPlayerHandler", android.os.Process.THREAD_PRIORITY_BACKGROUND);
        handlerThread.start();

        playerHandler = new MediaPlayerHandler(this, queueManager, handlerThread.getLooper());

        playbackState = PlaybackState.STOPPED;

        player = new MultiPlayer(context);
        player.setHandler(playerHandler);

        equalizer = new Equalizer(context);

        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        chromecastManager.init();

        disposables.add(SleepTimer.getInstance().getCurrentTimeObservable()
                .subscribe(remainingTime -> {
                    if (remainingTime == 0) {
                        if (SleepTimer.getInstance().playToEnd) {
                            pauseOnTrackFinish = true;
                        } else {
                            playerHandler.sendEmptyMessage(PlayerHandler.FADE_DOWN_STOP);
                        }
                    }
                }, throwable -> LogUtils.logException(TAG, "Error consuming SleepTimer observable", throwable)));
    }

    public void setPausedByTransientLossOfFocus(boolean pausedByTransientLossOfFocus) {
        this.pausedByTransientLossOfFocus = pausedByTransientLossOfFocus;
    }

    public void setQueuePosition(int position) {
        stop(false);
        queueManager.queuePosition = position;
        openCurrentAndNext();
        play();
        notifyChange(InternalIntents.META_CHANGED);
    }

    public void clearQueue() {
        stop(true);
        queueManager.clearQueue();
    }

    public void reloadQueue() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        queueManager.reloadQueue(this::reloadComplete,
                () -> {
                    setOpenFailedCounter(20);
                    openCurrentAndNext();
                },
                seekPos -> {
                    if (queueManager.getCurrentSong() != null) {
                        seekTo(seekPos < queueManager.getCurrentSong().duration ? seekPos : 0);
                    }
                });
    }

    public void removeQueueItems(List<QueueItem> queueItems) {
        queueManager.removeQueueItems(queueItems, () -> stop(true), () -> {
            final boolean wasPlaying = isPlaying();
            stop(false);
            openCurrentAndNext();
            if (wasPlaying) {
                play();
            }
        });
    }

    public void removeSongs(List<Song> songs) {
        queueManager.removeSongs(songs, () -> stop(true), () -> {
            final boolean wasPlaying = isPlaying();
            stop(false);
            openCurrentAndNext();
            if (wasPlaying) {
                play();
            }
        });
    }

    public void removeQueueItem(QueueItem queueItem) {
        queueManager.removeQueueItem(queueItem, () -> stop(true), () -> {
            final boolean wasPlaying = isPlaying();
            stop(false);
            openCurrentAndNext();
            if (wasPlaying) {
                play();
            }
        });
    }

    public void moveQueueItem(int from, int to) {
        queueManager.moveQueueItem(from, to);
    }

    void notifyChange(String what) {
        musicServiceCallbacks.notifyChange(what);
    }

    public void setVolume(float volume) {
        if (player != null) {
            player.setVolume(volume);
        }
    }

    public void playAutoShuffleList() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            disposables.add(DataManager.getInstance().getSongsRelay()
                    .firstOrError()
                    .subscribeOn(Schedulers.io())
                    .subscribe(songs -> {
                        queueManager.playlist = QueueItemKt.toQueueItems(songs);
                        queueManager.queuePosition = -1;
                        queueManager.makeShuffleList();
                        queueManager.setShuffleMode(QueueManager.ShuffleMode.ON);
                        notifyChange(InternalIntents.QUEUE_CHANGED);
                        queueManager.queuePosition = 0;
                        openCurrentAndNext();
                        play();
                        notifyChange(InternalIntents.META_CHANGED);
                        queueManager.saveQueue(false);
                    }, error -> LogUtils.logException(TAG, "Error playing auto shuffle list", error)));
        } else {
            queueManager.shuffleMode = QueueManager.ShuffleMode.OFF;
            queueManager.saveQueue(false);
        }
    }

    void notifyTrackEnded() {
        playerHandler.sendEmptyMessage(PlayerHandler.TRACK_ENDED);
    }

    private void reloadComplete() {
        if (playOnQueueLoad) {
            play();
            playOnQueueLoad = false;
        }
    }

    public MediaSessionCompat.Token getMediaSessionToken() {
        return mediaSessionManager.getSessionToken();
    }

    public boolean hasTrackEndedMessage() {
        return playerHandler.hasMessages(PlayerHandler.TRACK_ENDED);
    }

    public void closeEqualizerSessions(boolean internal, int audioSessionId) {
        equalizer.closeEqualizerSessions(internal, audioSessionId);
    }

    public void openEqualizerSession(boolean internal, int audioSessionId) {
        equalizer.openEqualizerSession(internal, audioSessionId);
    }

    public void updateEqualizer() {
        equalizer.update();
    }

    void releaseWakelock() {
        if (player != null) {
            player.releaseWakelock();
        }
    }

    private void setOpenFailedCounter(int openFailedCounter) {
        this.openFailedCounter = openFailedCounter;
    }

    /**
     * @param force true to force the player onto the track next, false otherwise.
     * @return The next position to play.
     */
    private int getNextPosition(final boolean force) {
        return queueManager.getNextPosition(force);
    }

    public void open(@NonNull List<Song> songs, int position) {
        queueManager.open(songs, position, this::openCurrentAndNext);
    }

    private void openCurrent() {
        openCurrentAndMaybeNext(false);
    }

    public void openCurrentAndNext() {
        openCurrentAndMaybeNext(true);
    }

    private void openCurrentAndMaybeNext(boolean openNext) {
        if (queueManager.getCurrentPlaylist().isEmpty() || queueManager.queuePosition < 0 || queueManager.queuePosition >= queueManager.getCurrentPlaylist().size()) {
            return;
        }

        stop(false);

        boolean shutdown = false;

        while (true) {
            if (queueManager.getCurrentSong() != null && open(queueManager.getCurrentSong())) {
                break;
            }
            // If we get here then opening the file failed.
            if (openFailedCounter++ < 10 && queueManager.getCurrentPlaylist().size() > 1) {
                final int pos = getNextPosition(false);
                if (pos < 0) {
                    musicServiceCallbacks.scheduleDelayedShutdown();
                    if (isSupposedToBePlaying) {
                        isSupposedToBePlaying = false;
                        notifyChange(InternalIntents.PLAY_STATE_CHANGED);
                    }
                    return;
                }
                queueManager.queuePosition = pos;
                stop(false);
                queueManager.queuePosition = pos;
            } else {
                openFailedCounter = 0;
                shutdown = true;
                break;
            }
        }
        // Go to bookmark if needed
        if (queueManager.getCurrentSong() != null && queueManager.getCurrentSong().isPodcast) {
            long bookmark = queueManager.getCurrentSong().bookMark;
            // Start playing a little bit before the bookmark,
            // so it's easier to get back in to the narrative.
            seekTo(bookmark - 5000);
        }

        if (shutdown) {
            musicServiceCallbacks.scheduleDelayedShutdown();
            if (isSupposedToBePlaying) {
                isSupposedToBePlaying = false;
                notifyChange(InternalIntents.PLAY_STATE_CHANGED);
            }
        }
        if (openNext) {
            setNextTrack();
        }
    }

    public boolean open(@NonNull Song song) {
        if (player != null) {
            player.setDataSource(song.path);
            if (player != null && player.isInitialized()) {
                openFailedCounter = 0;
                return true;
            }
        }

        stop(true);
        return false;
    }

    public void openFile(String path, @Nullable Action completion) {
        if (path == null) {
            return;
        }

        Uri uri = Uri.parse(path);
        long id = -1;
        try {
            id = Long.valueOf(uri.getLastPathSegment());
        } catch (NumberFormatException ignored) {
        }

        Predicate<Song> predicate;

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

        disposables.add(DataManager.getInstance().getSongsObservable(predicate)
                .firstOrError()
                .subscribe(songs -> {
                    if (!songs.isEmpty() && queueManager.getCurrentSong() != null) {
                        open(queueManager.getCurrentSong());
                        if (completion != null) {
                            completion.run();
                        }
                    }
                }, error -> LogUtils.logException(TAG, "Error opening file", error)));
    }

    public void setNextTrack() {
        queueManager.nextPlayPos = getNextPosition(false);
        if (queueManager.nextPlayPos >= 0
                && !queueManager.getCurrentPlaylist().isEmpty()
                && queueManager.nextPlayPos < queueManager.getCurrentPlaylist().size()) {
            final Song nextSong = queueManager.getCurrentPlaylist().get(queueManager.nextPlayPos).getSong();
            try {
                if (player != null) {
                    player.setNextDataSource(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI + "/" + nextSong.id);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error: " + e.getMessage());
                CrashlyticsCore.getInstance().log("setNextTrack() with id failed. error: " + e.getLocalizedMessage());
            }
        } else {
            try {
                if (player != null) {
                    player.setNextDataSource(null);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error: " + e.getMessage());
                CrashlyticsCore.getInstance().log("setNextTrack() failed with null id. error: " + e.getLocalizedMessage());
            }
        }
    }

    public void enqueue(List<Song> songs, int action) {
        queueManager.enqueue(
                songs,
                action,
                this::setNextTrack,
                () -> {
                    openCurrentAndNext();
                    play();
                });
    }

    public void saveState() {
        if (player != null && player.isInitialized()) {
            PlaybackSettingsManager.INSTANCE.setSeekPosition(player.getPosition());
        }
    }

    void release() {
        if (isPlaying() || pausedByTransientLossOfFocus || playerHandler.hasMessages(PlayerHandler.TRACK_ENDED)) {
            return;
        }

        if (ShuttleUtils.hasOreo()) {
            if (audioFocusRequest != null) {
                audioManager.abandonAudioFocusRequest(audioFocusRequest);
            }
        } else {
            audioManager.abandonAudioFocus(audioFocusListener);
        }

        mediaSessionManager.setActive(false);
    }

    public void destroy() {

        disposables.clear();

        chromecastManager.release();

        mediaSessionManager.destroy();

        // Release all MediaPlayer resources, including the native player and wakelocks
        if (player != null) {
            player.release();
            player.releaseWakelock();
            player = null;
        }

        // Remove the audio focus listener and lock screen controls
        if (ShuttleUtils.hasOreo()) {
            if (audioFocusRequest != null) {
                audioManager.abandonAudioFocusRequest(audioFocusRequest);
            }
        } else {
            audioManager.abandonAudioFocus(audioFocusListener);
        }

        playerHandler.removeCallbacksAndMessages(null);

        equalizer.releaseEffects();
        equalizer.closeEqualizerSessions(true, getAudioSessionId());

        // Quit the thread so that anything that gets posted won't run
        handlerThread.quitSafely();
    }

    public boolean recentlyPlayed() {
        return isPlaying() || System.currentTimeMillis() - lastPlayedTime < 5 * 60 * 1000 /* 5 mins */;
    }

    public int getAudioSessionId() {
        if (player != null) {
            return player.getAudioSessionId();
        } else {
            return 0;
        }
    }

    public void seekTo(long position) {
        if (player != null && player.isInitialized()) {
            if (position < 0) {
                position = 0;
            } else if (position > player.getDuration()) {
                position = player.getDuration();
            }

            player.seekTo(position);

            if (playbackLocation == PlaybackManager.PlaybackLocation.REMOTE) {
                try {
                    chromecastManager.castManager.seek((int) position);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            notifyChange(InternalIntents.POSITION_CHANGED);
        }
    }

    public long getSeekPosition() {
        switch (playbackLocation) {
            case PlaybackLocation.LOCAL: {
                if (player != null) {
                    return player.getPosition();
                }
                break;
            }
            case PlaybackLocation.REMOTE: {
                try {
                    return (int) chromecastManager.castManager.getCurrentMediaPosition();
                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                    if (player != null) {
                        return player.getPosition();
                    }
                }

                break;
            }
        }
        return 0;
    }

    @PlaybackLocation
    int getPlaybackLocation() {
        return playbackLocation;
    }

    void setPlaybackLocation(@PlaybackLocation int playbackLocation) {
        this.playbackLocation = playbackLocation;
    }

    public void pause() {
        switch (playbackLocation) {
            case PlaybackLocation.LOCAL: {
                playerHandler.removeMessages(PlayerHandler.FADE_UP);
                if (isSupposedToBePlaying) {
                    equalizer.closeEqualizerSessions(false, getAudioSessionId());
                    if (player != null) {
                        player.pause();
                    }
                    setIsSupposedToBePlaying(false, true);
                    notifyChange(InternalIntents.PLAY_STATE_CHANGED);
                    saveBookmarkIfNeeded();
                }
                break;
            }
            case PlaybackLocation.REMOTE: {
                try {
                    if (player != null) {
                        player.seekTo(chromecastManager.castManager.getCurrentMediaPosition());
                    }
                    chromecastManager.castManager.pause();
                    playbackState = PlaybackState.PAUSED;
                    musicServiceCallbacks.scheduleDelayedShutdown();
                    isSupposedToBePlaying = false;
                    notifyChange(InternalIntents.PLAY_STATE_CHANGED);
                    saveBookmarkIfNeeded();
                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                }
                break;
            }
        }
    }

    public boolean isPlaying() {
        switch (playbackLocation) {
            case PlaybackLocation.LOCAL: {
                return isSupposedToBePlaying;
            }
            case PlaybackLocation.REMOTE: {
                return playbackState == PlaybackState.PLAYING;
            }
        }

        return false;
    }

    public void stop(boolean goToIdle) {
        switch (playbackLocation) {
            case PlaybackLocation.LOCAL: {
                if (player != null && player.isInitialized()) {
                    player.stop();
                }
                if (goToIdle) {
                    setIsSupposedToBePlaying(false, false);
                } else {
                    musicServiceCallbacks.stopForegroundImpl(false, true);
                }
                break;
            }
            case PlaybackLocation.REMOTE: {
                try {
                    if (player != null && player.isInitialized()) {
                        player.seekTo(chromecastManager.castManager.getCurrentMediaPosition());
                        player.stop();
                    }
                    playbackState = PlaybackState.STOPPED;
                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                }
                if (goToIdle) {
                    if (ShuttleUtils.isUpgraded() && chromecastManager.castManager != null) {
                        HttpServer.getInstance().stop();
                    }
                    setIsSupposedToBePlaying(false, false);
                } else {
                    musicServiceCallbacks.stopForegroundImpl(false, true);
                }
                break;
            }
        }
    }

    public void next(boolean force) {
        notifyChange(InternalIntents.TRACK_ENDING);

        if (queueManager.getCurrentPlaylist().size() == 0) {
            musicServiceCallbacks.scheduleDelayedShutdown();
            return;
        }

        final int pos = getNextPosition(force);
        if (pos < 0) {
            setIsSupposedToBePlaying(false, true);
            return;
        }

        queueManager.queuePosition = pos;
        saveBookmarkIfNeeded();
        stop(false);
        queueManager.queuePosition = pos;
        openCurrentAndNext();
        play();
        notifyChange(InternalIntents.META_CHANGED);
    }

    public void previous() {
        queueManager.previous();
        stop(false);
        openCurrent();
        play();
        notifyChange(InternalIntents.META_CHANGED);
    }

    /**
     * Helper function to wrap the logic around isSupposedToBePlaying for consistency
     *
     * @param supposedToBePlaying to set isSupposedToBePlaying to
     * @param notify whether we want to fire PLAY_STATE_CHANGED event
     */
    void setIsSupposedToBePlaying(boolean supposedToBePlaying, boolean notify) {
        if (isSupposedToBePlaying != supposedToBePlaying) {
            isSupposedToBePlaying = supposedToBePlaying;

            // Update lastPlayed time first and notify afterwards, as the notification listener method needs the up-to-date value
            // for the recentlyPlayed() method to work
            if (!isSupposedToBePlaying) {
                musicServiceCallbacks.scheduleDelayedShutdown();
                lastPlayedTime = System.currentTimeMillis();
            }

            if (notify) {
                notifyChange(InternalIntents.PLAY_STATE_CHANGED);
            }
        }
    }

    public void play() {
        int status;

        if (ShuttleUtils.hasOreo()) {
            AudioFocusRequest audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setOnAudioFocusChangeListener(audioFocusListener)
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build())
                    .build();
            this.audioFocusRequest = audioFocusRequest;
            status = audioManager.requestAudioFocus(audioFocusRequest);
        } else {
            status = audioManager.requestAudioFocus(audioFocusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }

        if (status != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            return;
        }

        if (playbackLocation == PlaybackLocation.LOCAL) {
            if (SettingsManager.getInstance().getEqualizerEnabled()) {
                //Shutdown any existing external audio sessions
                equalizer.closeEqualizerSessions(false, getAudioSessionId());

                //Start internal equalizer session (will only turn on if enabled)
                equalizer.openEqualizerSession(true, getAudioSessionId());
            } else {
                equalizer.openEqualizerSession(false, getAudioSessionId());
            }
        }

        mediaSessionManager.setActive(true);

        switch (playbackLocation) {
            case PlaybackLocation.LOCAL: {
                if (player != null && player.isInitialized()) {
                    // If we are at the end of the song, go to the next song first
                    long duration = player.getDuration();
                    if (queueManager.repeatMode != QueueManager.RepeatMode.ONE && duration > 2000 && player.getPosition() >= duration - 2000) {
                        next(true);
                    }
                    player.start();
                    // Make sure we fade in, in case a previous fadein was stopped because of another focus loss
                    playerHandler.removeMessages(PlayerHandler.FADE_DOWN);
                    playerHandler.sendEmptyMessage(PlayerHandler.FADE_UP);

                    setIsSupposedToBePlaying(true, true);

                    musicServiceCallbacks.cancelShutdown();
                    musicServiceCallbacks.updateNotification();
                } else if (queueManager.getCurrentPlaylist().size() == 0) {
                    // This is mostly so that if you press 'play' on a bluetooth headset without ever having played anything before, it will still play something.
                    if (queueManager.queueReloading) {
                        playOnQueueLoad = true;
                    } else {
                        playAutoShuffleList();
                    }
                }
                break;
            }
            case PlaybackLocation.REMOTE: {
                // If we are at the end of the song, go to the next song first
                long duration = player != null ? player.getDuration() : 0;

                if (queueManager.repeatMode != QueueManager.RepeatMode.ONE && duration > 2000 && player.getPosition() >= duration - 2000) {
                    next(true);
                }

                if (!isSupposedToBePlaying) {
                    isSupposedToBePlaying = true;
                    notifyChange(InternalIntents.PLAY_STATE_CHANGED);
                }

                musicServiceCallbacks.cancelShutdown();
                musicServiceCallbacks.updateNotification();

                switch (playbackState) {
                    case PlaybackState.STOPPED: {
                        try {
                            if (queueManager.getCurrentSong() != null) {
                                chromecastManager.castManager.checkConnectivity();
                                chromecastManager.prepareChromeCastLoad(queueManager.getCurrentSong(), 0, true);
                                playbackState = PlaybackState.PLAYING;
                                musicServiceCallbacks.updateNotification();
                            }
                        } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
                            Log.e(TAG, "Play() called & failed. State: Stopped " + e.toString());
                            playbackState = PlaybackState.STOPPED;
                            musicServiceCallbacks.updateNotification();
                        }
                        break;
                    }
                    case PlaybackState.PAUSED: {
                        try {
                            chromecastManager.castManager.checkConnectivity();
                            chromecastManager.castManager.play();
                            playbackState = PlaybackState.PLAYING;
                            musicServiceCallbacks.updateNotification();
                        } catch (TransientNetworkDisconnectionException | NoConnectionException | CastException e) {
                            Log.e(TAG, "Play() called & failed. State: Paused " + e.toString());
                            playbackState = PlaybackState.PAUSED;
                            musicServiceCallbacks.updateNotification();
                        }
                        break;
                    }
                }

                if (queueManager.getCurrentPlaylist().size() == 0) {
                    // This is mostly so that if you press 'play' on a bluetooth headset without every having played anything before, it will still play something.
                    if (queueManager.queueReloading) {
                        playOnQueueLoad = true;
                    } else {
                        playAutoShuffleList();
                    }
                }
            }
        }
    }

    private void saveBookmarkIfNeeded() {
        try {
            if (queueManager.getCurrentSong() != null) {
                if (queueManager.getCurrentSong().isPodcast) {
                    long pos = getSeekPosition();
                    long bookmark = queueManager.getCurrentSong().bookMark;
                    long duration = queueManager.getCurrentSong().duration;
                    if ((pos < bookmark && (pos + 10000) > bookmark) || (pos > bookmark && (pos - 10000) < bookmark)) {
                        // The existing bookmark is close to the current position, so don't update it.
                        return;
                    }
                    if (pos < 15000 || (pos + 10000) > duration) {
                        // If we're near the start or end, clear the bookmark
                        pos = 0;
                    }

                    // Write 'pos' to the bookmark field
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Audio.Media.BOOKMARK, pos);
                    Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, queueManager.getCurrentSong().id);
                    if (uri != null) {
                        context.getContentResolver().update(uri, values, null, null);
                    }
                }
            }
        } catch (SQLiteException ignored) {
        }
    }

    void setPlaybackState(@PlaybackState int playbackState) {
        this.playbackState = playbackState;
    }

    boolean getIsSupposedToBePlaying() {
        return isSupposedToBePlaying;
    }
}
