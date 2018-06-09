package com.simplecity.amp_library.playback;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.Toast;
import com.crashlytics.android.Crashlytics;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.notifications.MusicNotificationHelper;
import com.simplecity.amp_library.playback.constants.ExternalIntents;
import com.simplecity.amp_library.playback.constants.InternalIntents;
import com.simplecity.amp_library.playback.constants.MediaButtonCommand;
import com.simplecity.amp_library.playback.constants.ServiceCommand;
import com.simplecity.amp_library.playback.constants.ShortcutCommands;
import com.simplecity.amp_library.playback.constants.WidgetManager;
import com.simplecity.amp_library.services.Equalizer;
import com.simplecity.amp_library.ui.queue.QueueItem;
import com.simplecity.amp_library.utils.LogUtils;
import com.simplecity.amp_library.utils.MediaButtonIntentReceiver;
import com.simplecity.amp_library.utils.PlaylistUtils;
import com.simplecity.amp_library.utils.ShuttleUtils;
import io.reactivex.Completable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Action;
import io.reactivex.schedulers.Schedulers;
import java.util.ConcurrentModificationException;
import java.util.List;

@SuppressLint("InlinedApi")
public class MusicService extends Service {

    @interface NotifyMode {
        int NONE = 0;
        int FOREGROUND = 1;
        int BACKGROUND = 2;
    }

    private static final String TAG = "MusicService";

    private MusicServiceCallbacks musicServiceCallbacks = new MusicServiceCallbacks();

    private QueueManager queueManager;

    private PlaybackManager playbackManager;

    private BluetoothManager bluetoothManager;

    private HeadsetManager headsetManager;

    private WidgetManager widgetManager = new WidgetManager();

    private ScrobbleManager scrobbleManager = new ScrobbleManager();

    private final IBinder binder = new LocalBinder(this);

    private BroadcastReceiver unmountReceiver = null;

    private int serviceStartId = -1;

    private boolean serviceInUse = false;

    private MusicNotificationHelper notificationHelper;

    private static NotificationStateHandler notificationStateHandler;

    private AlarmManager alarmManager;

    private PendingIntent shutdownIntent;

    private boolean shutdownScheduled;

    private CompositeDisposable disposables = new CompositeDisposable();

    @SuppressLint("InlinedApi")
    @Override
    public void onCreate() {
        super.onCreate();

        queueManager = new QueueManager(musicServiceCallbacks);

        playbackManager = new PlaybackManager(this, queueManager, musicServiceCallbacks);

        bluetoothManager = new BluetoothManager(playbackManager, musicServiceCallbacks);

        headsetManager = new HeadsetManager(playbackManager);

        notificationHelper = new MusicNotificationHelper(this);

        notificationStateHandler = new NotificationStateHandler(this);

        headsetManager.registerHeadsetPlugReceiver(this);
        bluetoothManager.registerBluetoothReceiver(this);
        bluetoothManager.registerA2dpServiceListener(this);

        registerExternalStorageListener();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ServiceCommand.SERVICE_COMMAND);
        intentFilter.addAction(ServiceCommand.TOGGLE_PAUSE_ACTION);
        intentFilter.addAction(ServiceCommand.PAUSE_ACTION);
        intentFilter.addAction(ServiceCommand.NEXT_ACTION);
        intentFilter.addAction(ServiceCommand.PREV_ACTION);
        intentFilter.addAction(ServiceCommand.STOP_ACTION);
        intentFilter.addAction(ServiceCommand.SHUFFLE_ACTION);
        intentFilter.addAction(ServiceCommand.REPEAT_ACTION);
        intentFilter.addAction(ExternalIntents.PLAY_STATUS_REQUEST);
        registerReceiver(intentReceiver, intentFilter);

        // Initialize the delayed shutdown intent
        Intent shutdownIntent = new Intent(this, MusicService.class);
        shutdownIntent.setAction(ServiceCommand.SHUTDOWN);

        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        this.shutdownIntent = PendingIntent.getService(this, 0, shutdownIntent, 0);

        // Listen for the idle state
        scheduleDelayedShutdown();

        reloadQueue();
    }

    @Override
    public IBinder onBind(final Intent intent) {
        cancelShutdown();
        serviceInUse = true;
        return binder;
    }

    @Override
    public void onRebind(Intent intent) {
        cancelShutdown();
        serviceInUse = true;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        serviceInUse = false;
        saveState(true);

        if (playbackManager.getIsSupposedToBePlaying() || playbackManager.pausedByTransientLossOfFocus) {
            // Something is currently playing, or will be playing once an in-progress action requesting audio focus ends, so don't stop the service now.
            return true;

            // If there is a playlist but playback is paused, then wait a while before stopping the service, so that pause/resume isn't slow.
            // Also delay stopping the service if we're transitioning between tracks.
        } else if (queueManager.playlist.size() > 0 || queueManager.shuffleList.size() > 0 || playbackManager.hasTrackEndedMessage()) {
            scheduleDelayedShutdown();
            return true;
        }

        stopSelf(serviceStartId);

        // Shutdown the EQ
        Intent shutdownEqualizer = new Intent(MusicService.this, Equalizer.class);
        stopService(shutdownEqualizer);
        return true;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        //If nothing is playing, and won't be playing any time soon, we can stop the service.
        //Presumably this is what the user wanted.
        if (!isPlaying() && !playbackManager.pausedByTransientLossOfFocus) {
            stopSelf();
        }

        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        saveState(true);

        //Shutdown the EQ
        Intent shutdownEqualizer = new Intent(MusicService.this, Equalizer.class);
        stopService(shutdownEqualizer);

        alarmManager.cancel(shutdownIntent);

        // Remove any callbacks from the handlers
        notificationStateHandler.removeCallbacksAndMessages(null);

        headsetManager.unregisterHeadsetPlugReceiver(this);
        bluetoothManager.unregisterBluetoothReceiver(this);
        bluetoothManager.unregisterA2dpServiceListener(this);

        unregisterReceiver(intentReceiver);
        if (unmountReceiver != null) {
            unregisterReceiver(unmountReceiver);
            unmountReceiver = null;
        }

        playbackManager.destroy();

        disposables.clear();

        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        serviceStartId = startId;

        if (intent != null) {
            final String action = intent.getAction();
            String cmd = intent.getStringExtra("command");

            if (MediaButtonCommand.NEXT.equals(cmd) || ServiceCommand.NEXT_ACTION.equals(action)) {
                gotoNext(true);
            } else if (MediaButtonCommand.PREVIOUS.equals(cmd) || ServiceCommand.PREV_ACTION.equals(action)) {
                if (getSeekPosition() < 2000) {
                    previous();
                } else {
                    seekTo(0);
                    play();
                }
            } else if (MediaButtonCommand.TOGGLE_PAUSE.equals(cmd)
                    || ServiceCommand.TOGGLE_PAUSE_ACTION.equals(action)) {
                if (isPlaying()) {
                    pause();
                } else {
                    play();
                }
            } else if (MediaButtonCommand.PAUSE.equals(cmd) || ServiceCommand.PAUSE_ACTION.equals(action)) {
                pause();
            } else if (MediaButtonCommand.PLAY.equals(cmd)) {
                play();
            } else if (ServiceCommand.STOP_ACTION.equals(action) || MediaButtonCommand.STOP.equals(action)) {
                pause();
                releaseServiceUiAndStop();
                notificationStateHandler.removeCallbacksAndMessages(null);
                //For some reason, the notification will only fuck off if this call is delayed.
                new Handler().postDelayed(() -> stopForegroundImpl(true, false), 150);
            } else if (ServiceCommand.SHUFFLE_ACTION.equals(action)) {
                toggleShuffleMode();
            } else if (ServiceCommand.REPEAT_ACTION.equals(action)) {
                toggleRepeat();
            } else if (MediaButtonCommand.TOGGLE_FAVORITE.equals(action) || ServiceCommand.TOGGLE_FAVORITE.equals(action)) {
                toggleFavorite();
            } else if (ExternalIntents.PLAY_STATUS_REQUEST.equals(action)) {
                notifyChange(ExternalIntents.PLAY_STATUS_RESPONSE);
            } else if (ServiceCommand.SHUTDOWN.equals(action)) {
                shutdownScheduled = false;
                releaseServiceUiAndStop();
                return START_NOT_STICKY;
            }

            if (action != null) {
                switch (action) {
                    case ShortcutCommands.PLAY:
                        play();
                        break;
                    case ShortcutCommands.SHUFFLE_ALL:
                        queueManager.makeShuffleList();
                        playAutoShuffleList();
                        break;
                }
            }
        }

        // Make sure the service will shut down on its own if it was  just started but not bound to and nothing is playing
        scheduleDelayedShutdown();

        if (intent != null && intent.getBooleanExtra(MediaButtonCommand.FROM_MEDIA_BUTTON, false)) {
            MediaButtonIntentReceiver.completeWakefulIntent(intent);
        }

        return START_STICKY;
    }

    private final BroadcastReceiver intentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String action = intent.getAction();
            final String command = intent.getStringExtra("command");

            if (MediaButtonCommand.NEXT.equals(command) || ServiceCommand.NEXT_ACTION.equals(action)) {
                gotoNext(true);
            } else {
                if (MediaButtonCommand.PREVIOUS.equals(command) || ServiceCommand.PREV_ACTION.equals(action)) {
                    if (getSeekPosition() < 2000) {
                        previous();
                    } else {
                        seekTo(0);
                        play();
                    }
                } else {
                    if (MediaButtonCommand.TOGGLE_PAUSE.equals(command) || ServiceCommand.TOGGLE_PAUSE_ACTION.equals(action)) {
                        if (isPlaying()) {
                            pause();
                        } else {
                            play();
                        }
                    } else if (MediaButtonCommand.PAUSE.equals(command)
                            || ServiceCommand.PAUSE_ACTION.equals(action)) {
                        pause();
                    } else if (MediaButtonCommand.PLAY.equals(command)) {
                        play();
                    } else if (MediaButtonCommand.STOP.equals(command)) {
                        pause();
                        releaseServiceUiAndStop();
                    } else if (MediaButtonCommand.TOGGLE_FAVORITE.equals(command)) {
                        toggleFavorite();
                    }
                    widgetManager.processCommand(MusicService.this, intent, command);
                }
            }
        }
    };

    /**
     * Release resources and destroy the service.
     */
    void releaseServiceUiAndStop() {
        playbackManager.release();

        cancelNotification();

        if (!serviceInUse) {
            saveState(true);

            //Shutdown the EQ
            Intent shutdownEqualizer = new Intent(MusicService.this, Equalizer.class);
            stopService(shutdownEqualizer);

            stopSelf(serviceStartId);
        }
    }

    /**
     * Called when we receive a ACTION_MEDIA_EJECT notification.
     */
    public void closeExternalStorageFiles() {
        // Stop playback and clean up if the SD card is going to be unmounted.
        stop();
        notifyChange(InternalIntents.QUEUE_CHANGED);
        notifyChange(InternalIntents.META_CHANGED);
    }

    /**
     * Registers an intent to listen for ACTION_MEDIA_EJECT notifications. The intent will call closeExternalStorageFiles() if the external
     * media is going to be ejected, so applications can clean up any files they have open.
     */
    public void registerExternalStorageListener() {
        if (unmountReceiver == null) {
            unmountReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    final String action = intent.getAction();
                    if (Intent.ACTION_MEDIA_EJECT.equals(action)) {
                        saveState(true);
                        queueManager.queueIsSaveable = false;
                        closeExternalStorageFiles();
                    } else if (Intent.ACTION_MEDIA_MOUNTED.equals(action)) {
                        queueManager.queueIsSaveable = true;
                        reloadQueue();
                    }
                }
            };
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Intent.ACTION_MEDIA_EJECT);
            intentFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
            intentFilter.addDataScheme("file");
            registerReceiver(unmountReceiver, intentFilter);
        }
    }

    /**
     * Save the current state of the player in preferences. This stores the player's seek position, and calls {@link QueueManager#saveQueue(boolean)}
     * to save our current playback position, shuffle mode, etc.
     *
     * @param saveQueue whether to save the queue as well
     */
    void saveState(boolean saveQueue) {
        playbackManager.saveState();
        queueManager.saveQueue(saveQueue);
    }

    /**
     * Queues a new list for playback
     *
     * @param songs The list to queue
     * @param action The action to take
     */
    public void enqueue(List<Song> songs, @QueueManager.EnqueueAction final int action) {
        synchronized (this) {
            playbackManager.enqueue(songs, action);
        }
    }

    public void moveToNext(QueueItem queueItem) {
        synchronized (this) {
            List<QueueItem> playlist = queueManager.getCurrentPlaylist();
            int fromIndex = playlist.indexOf(queueItem);

            QueueItem currentQueueItem = queueManager.getCurrentQueueItem();
            int toIndex = playlist.indexOf(currentQueueItem) + 1;

            if (fromIndex != toIndex) {
                playbackManager.moveQueueItem(fromIndex, toIndex);
            }
        }
    }

    /**
     * Sets the track to be played
     */
    protected void setNextTrack() {
        synchronized (this) {
            playbackManager.setNextTrack();
        }
    }

    /**
     * Opens a list of songs for playback
     *
     * @param songs The list of songs to open
     * @param position The position to start playback at
     */
    public void open(@NonNull List<Song> songs, final int position) {
        synchronized (this) {
            playbackManager.open(songs, position);
        }
    }

    /**
     * Opens a song and prepares it for playback
     *
     * @param song the song to open
     * @return true if the song as successfully opened
     */
    public boolean open(@NonNull Song song) {
        synchronized (this) {
            return playbackManager.open(song);
        }
    }

    /**
     * Opens a file and prepares it for playback
     *
     * @param path The path of the file to open
     */
    public void openFile(String path, @Nullable Action completion) {
        synchronized (this) {
            playbackManager.openFile(path, completion);
        }
    }

    /**
     * Starts playback of a previously opened file
     */
    public void play() {
        synchronized (this) {
            playbackManager.play();
        }
    }

    /**
     * Stops playback
     */
    public void stop() {
        synchronized (this) {
            playbackManager.stop(true);
        }
    }

    /**
     * Pauses playback
     */
    public void pause() {
        synchronized (this) {
            playbackManager.pause();
        }
    }

    /**
     * Returns whether something is currently playing
     *
     * @return true if something is playing (or will be playing shortly, in case we're currently transitioning between tracks), false if not
     */
    public boolean isPlaying() {
        synchronized (this) {
            return playbackManager.isPlaying();
        }
    }

    /**
     * @return true if is playing or has played recently
     */
    private boolean recentlyPlayed() {
        synchronized (this) {
            return playbackManager.recentlyPlayed();
        }
    }

    /**
     * Changes from the current track to the previous track
     */
    public void previous() {
        synchronized (this) {
            playbackManager.previous();
        }
    }

    /**
     * Changes from the current track to the next track
     *
     * @param force true to move to the next song regardless of repeat mode.
     */
    public void gotoNext(boolean force) {
        synchronized (this) {
            playbackManager.next(force);
        }
    }

    /**
     * Returns the current playback position in milliseconds
     */
    public long getSeekPosition() {
        synchronized (this) {
            return playbackManager.getSeekPosition();
        }
    }

    /**
     * Seeks to the position specified.
     *
     * @param position The position to seek to, in milliseconds
     */
    public void seekTo(long position) {
        synchronized (this) {
            playbackManager.seekTo(position);
        }
    }

    /**
     * @return int the audio session ID.
     */
    public int getAudioSessionId() {
        synchronized (this) {
            return playbackManager.getAudioSessionId();
        }
    }

    /**
     * Creates a shuffled list of all songs and begins playback
     */
    public void playAutoShuffleList() {
        synchronized (this) {
            playbackManager.playAutoShuffleList();
        }
    }

    @QueueManager.ShuffleMode
    public int getShuffleMode() {
        return queueManager.shuffleMode;
    }

    public void setShuffleMode(@QueueManager.ShuffleMode int shufflemode) {
        synchronized (this) {
            queueManager.setShuffleMode(shufflemode);
        }
    }

    @QueueManager.RepeatMode
    public int getRepeatMode() {
        return queueManager.repeatMode;
    }

    public void setRepeatMode(@QueueManager.RepeatMode int repeatMode) {
        synchronized (this) {
            queueManager.setRepeatMode(repeatMode);
            setNextTrack();
        }
    }

    @Nullable
    public Song getSong() {
        synchronized (this) {
            return queueManager.getCurrentSong();
        }
    }

    public void toggleFavorite() {
        Song song = queueManager.getCurrentSong();
        if (song != null) {
            PlaylistUtils.toggleFavorite(song, isFavorite -> {
                if (isFavorite) {
                    Toast.makeText(MusicService.this, getString(R.string.song_to_favourites, song.name), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MusicService.this, getString(R.string.song_removed_from_favourites, song.name), Toast.LENGTH_SHORT).show();
                }
                notifyChange(InternalIntents.FAVORITE_CHANGED);
            });
        }
    }

    private void showToast(int resId) {
        Toast.makeText(getBaseContext(), resId, Toast.LENGTH_SHORT).show();
    }

    public void toggleShuffleMode() {
        switch (getShuffleMode()) {
            case QueueManager.ShuffleMode.OFF:
                setShuffleMode(QueueManager.ShuffleMode.ON);
                notifyChange(InternalIntents.SHUFFLE_CHANGED);
                queueManager.makeShuffleList();
                notifyChange(InternalIntents.QUEUE_CHANGED);
                if (getRepeatMode() == QueueManager.RepeatMode.ONE) {
                    setRepeatMode(QueueManager.RepeatMode.ALL);
                }
                showToast(R.string.shuffle_on_notif);
                break;
            case QueueManager.ShuffleMode.ON:
                setShuffleMode(QueueManager.ShuffleMode.OFF);
                notifyChange(InternalIntents.SHUFFLE_CHANGED);
                if (this.queueManager.queuePosition >= 0 && this.queueManager.queuePosition < queueManager.shuffleList.size()) {
                    int playPos = queueManager.playlist.indexOf(queueManager.shuffleList.get(this.queueManager.queuePosition));
                    if (playPos != -1) {
                        this.queueManager.queuePosition = playPos;
                    }
                }
                notifyChange(InternalIntents.QUEUE_CHANGED);
                showToast(R.string.shuffle_off_notif);
                break;
        }
    }

    public void toggleRepeat() {
        switch (getRepeatMode()) {
            case QueueManager.RepeatMode.OFF:
                setRepeatMode(QueueManager.RepeatMode.ALL);
                showToast(R.string.repeat_all_notif);
                break;
            case QueueManager.RepeatMode.ALL:
                setRepeatMode(QueueManager.RepeatMode.ONE);
                showToast(R.string.repeat_current_notif);
                break;
            case QueueManager.RepeatMode.ONE:
                setRepeatMode(QueueManager.RepeatMode.OFF);
                showToast(R.string.repeat_off_notif);
                break;
        }
        notifyChange(InternalIntents.REPEAT_CHANGED);
    }

    public void clearQueue() {
        synchronized (this) {
            playbackManager.clearQueue();
        }
    }

    void reloadQueue() {
        synchronized (this) {
            playbackManager.reloadQueue();
        }
    }

    public List<QueueItem> getQueue() {
        synchronized (this) {
            return queueManager.getCurrentPlaylist();
        }
    }

    /**
     * @return the position in the queue
     */
    public int getQueuePosition() {
        synchronized (this) {
            return queueManager.queuePosition;
        }
    }

    /**
     * Starts playing the track at the given position in the queue.
     *
     * @param position The position in the queue of the track that will be played.
     */
    public void setQueuePosition(int position) {
        synchronized (this) {
            playbackManager.setQueuePosition(position);
        }
    }

    public void removeQueueItems(List<QueueItem> queueItems) {
        synchronized (this) {
            playbackManager.removeQueueItems(queueItems);
        }
    }

    public void removeSongs(List<Song> songs) {
        synchronized (this) {
            playbackManager.removeSongs(songs);
        }
    }

    public void removeQueueItem(QueueItem queueItem) {
        synchronized (this) {
            playbackManager.removeQueueItem(queueItem);
        }
    }

    public void moveQueueItem(int from, int to) {
        synchronized (this) {
            playbackManager.moveQueueItem(from, to);
        }
    }

    // EQ

    public void closeEqualizerSessions(boolean internal, int audioSessionId) {
        playbackManager.closeEqualizerSessions(internal, audioSessionId);
    }

    public void openEqualizerSession(boolean internal, int audioSessionId) {
        playbackManager.openEqualizerSession(internal, audioSessionId);
    }

    public void updateEqualizer() {
        playbackManager.updateEqualizer();
    }

    private void scheduleDelayedShutdown() {
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 5 * 60 * 1000 /* 5 mins */, shutdownIntent);
        shutdownScheduled = true;
    }

    private void cancelShutdown() {
        if (shutdownScheduled) {
            alarmManager.cancel(shutdownIntent);
            shutdownScheduled = false;
        }
    }

    // Notifications

    void updateNotification() {

        final int notifyMode;

        if (isPlaying()) {
            notifyMode = NotifyMode.FOREGROUND;
        } else if (recentlyPlayed()) {
            notifyMode = NotifyMode.BACKGROUND;
        } else {
            notifyMode = NotifyMode.NONE;
        }

        switch (notifyMode) {
            case NotifyMode.FOREGROUND:
                startForegroundImpl();
                break;
            case NotifyMode.BACKGROUND:
                try {
                    if (queueManager.getCurrentSong() != null) {
                        notificationHelper.notify(this, queueManager.getCurrentSong(), isPlaying(), playbackManager.getMediaSessionToken());
                    }
                } catch (ConcurrentModificationException e) {
                    LogUtils.logException(TAG, "Exception while attempting to show notification", e);
                }
                stopForegroundImpl(false, false);
                break;
            case NotifyMode.NONE:
                stopForegroundImpl(false, false);
                notificationHelper.cancel();
                break;
        }
    }

    private void cancelNotification() {
        stopForegroundImpl(true, true);
        notificationHelper.cancel();
    }

    public static PendingIntent retrievePlaybackAction(Context context, final String action) {
        final ComponentName serviceName = new ComponentName(context, MusicService.class);
        Intent intent = new Intent(action);
        intent.setComponent(serviceName);

        return PendingIntent.getService(context, 0, intent, 0);
    }

    /**
     * Starts the foreground notification, and cancels any stop messages
     */
    private void startForegroundImpl() {
        try {
            notificationStateHandler.sendEmptyMessage(NotificationStateHandler.START_FOREGROUND);
            Song song = queueManager.getCurrentSong();
            if (song != null) {
                notificationHelper.startForeground(this, queueManager.getCurrentSong(), isPlaying(), playbackManager.getMediaSessionToken());
            }
        } catch (NullPointerException | ConcurrentModificationException e) {
            Crashlytics.log("startForegroundImpl error: " + e.getMessage());
        }
    }

    /**
     * Stops the foreground notification
     *
     * @param removeNotification true to remove the notification as well as stop the service running in the foreground
     * @param withDelay true to delay the stop call by 1.5 seconds, allowing subsequent start calls to cancel this call
     */
    void stopForegroundImpl(boolean removeNotification, boolean withDelay) {
        if (withDelay) {
            notificationStateHandler.sendEmptyMessageDelayed(NotificationStateHandler.STOP_FOREGROUND, 1500);
        } else {
            stopForeground(removeNotification);
        }
    }

    // Event management

    private Bundle getExtras(@NonNull Song song) {
        Bundle extras = new Bundle();
        extras.putLong("id", song.id);
        extras.putString("artist", song.artistName);
        extras.putString("album", song.albumName);
        extras.putString("track", song.name);
        extras.putInt("shuffleMode", getShuffleMode());
        extras.putInt("repeatMode", getRepeatMode());
        extras.putBoolean("playing", isPlaying());
        extras.putLong("duration", song.duration);
        extras.putLong("position", getSeekPosition());
        extras.putLong("ListSize", queueManager.getCurrentPlaylist().size());
        return extras;
    }

    private Intent getTaskerIntent(@NonNull Song song) {
        Intent intent = new Intent(ExternalIntents.TASKER);
        intent.putExtra("%MTRACK", isPlaying() ? song.name : "");
        return intent;
    }

    private Intent getPebbleIntent(@NonNull Song song) {
        Intent intent = new Intent(ExternalIntents.PEBBLE);
        intent.putExtra("artist", song.artistName);
        intent.putExtra("album", song.albumName);
        intent.putExtra("track", song.name);
        return intent;
    }

    void notifyChange(String action) {
        switch (action) {
            case InternalIntents.TRACK_ENDING:
                onTrackEnded();
                return;
            case InternalIntents.FAVORITE_CHANGED:
                updateNotification();
                return;
            case InternalIntents.PLAY_STATE_CHANGED:
                onPlayStateChanged();
                break;
            case InternalIntents.META_CHANGED:
                onMetaChanged();
                break;
            case InternalIntents.QUEUE_CHANGED:
                onQueueChanged();
                break;
        }

        Intent intent = new Intent(action);
        Song currentSong = queueManager.getCurrentSong();
        if (currentSong != null) {
            intent.putExtras(getExtras(currentSong));
        }
        sendBroadcast(intent);

        widgetManager.notifyChange(this, action);

        saveState(false);
    }

    private void onQueueChanged() {
        if (isPlaying()) {
            setNextTrack();
        }
    }

    private void onMetaChanged() {
        updateNotification();

        if (queueManager.getCurrentSong() != null) {
            queueManager.getCurrentSong().setStartTime();

            sendBroadcast(getTaskerIntent(queueManager.getCurrentSong()));

            sendBroadcast(getPebbleIntent(queueManager.getCurrentSong()));

            bluetoothManager.sendMetaChangedIntent(this, getExtras(queueManager.getCurrentSong()));

            scrobbleManager.scrobbleBroadcast(this, ScrobbleManager.ScrobbleStatus.START, queueManager.getCurrentSong());
        }
    }

    private void onPlayStateChanged() {
        updateNotification();

        if (queueManager.getCurrentSong() != null) {

            bluetoothManager.sendPlayStateChangedIntent(this, getExtras(queueManager.getCurrentSong()));

            sendBroadcast(getTaskerIntent(queueManager.getCurrentSong()));

            if (isPlaying()) {
                queueManager.getCurrentSong().setResumed();
                sendBroadcast(getPebbleIntent(queueManager.getCurrentSong()));
            } else {
                queueManager.getCurrentSong().setPaused();
            }
            scrobbleManager.scrobbleBroadcast(this, isPlaying() ? ScrobbleManager.ScrobbleStatus.RESUME : ScrobbleManager.ScrobbleStatus.PAUSE, queueManager.getCurrentSong());
        }
    }

    private void onTrackEnded() {
        //We're just about to change tracks, so 'current song' is the song that just finished
        Song finishedSong = queueManager.getCurrentSong();
        if (finishedSong != null) {
            if (finishedSong.hasPlayed()) {
                disposables.add(
                        Completable.fromAction(() -> ShuttleUtils.incrementPlayCount(this, finishedSong))
                                .subscribeOn(Schedulers.io())
                                .subscribe(() -> {
                                    // Nothing to do
                                }, error -> LogUtils.logException(TAG, "Error incrementing play count", error))
                );
            }
            scrobbleManager.scrobbleBroadcast(this, ScrobbleManager.ScrobbleStatus.COMPLETE, finishedSong);
        }
    }

    public interface Callbacks {

        void notifyChange(String action);

        void scheduleDelayedShutdown();

        void cancelShutdown();

        void updateNotification();

        void releaseServiceUiAndStop();

        void stopForegroundImpl(boolean removeNotification, boolean withDelay);
    }

    class MusicServiceCallbacks implements Callbacks {

        @Override
        public void notifyChange(String action) {
            MusicService.this.notifyChange(action);
        }

        @Override
        public void scheduleDelayedShutdown() {
            MusicService.this.scheduleDelayedShutdown();
        }

        @Override
        public void cancelShutdown() {
            MusicService.this.cancelShutdown();
        }

        @Override
        public void updateNotification() {
            MusicService.this.updateNotification();
        }

        @Override
        public void releaseServiceUiAndStop() {
            MusicService.this.releaseServiceUiAndStop();
        }

        @Override
        public void stopForegroundImpl(boolean removeNotification, boolean withDelay) {
            MusicService.this.stopForegroundImpl(removeNotification, withDelay);
        }
    }
}