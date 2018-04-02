package com.simplecity.amp_library.playback;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.RemoteControlClient;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.widget.Toast;

import com.annimon.stream.function.Predicate;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.CastException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.NoConnectionException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.TransientNetworkDisconnectionException;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.http.HttpServer;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.notifications.MusicNotificationHelper;
import com.simplecity.amp_library.playback.constants.ExternalIntents;
import com.simplecity.amp_library.playback.constants.InternalIntents;
import com.simplecity.amp_library.playback.constants.MediaButtonCommand;
import com.simplecity.amp_library.playback.constants.PlayerHandler;
import com.simplecity.amp_library.playback.constants.ServiceCommand;
import com.simplecity.amp_library.playback.constants.ShortcutCommands;
import com.simplecity.amp_library.playback.constants.WidgetManager;
import com.simplecity.amp_library.rx.UnsafeAction;
import com.simplecity.amp_library.services.Equalizer;
import com.simplecity.amp_library.utils.DataManager;
import com.simplecity.amp_library.utils.LogUtils;
import com.simplecity.amp_library.utils.MediaButtonIntentReceiver;
import com.simplecity.amp_library.utils.PlaylistUtils;
import com.simplecity.amp_library.utils.SettingsManager;
import com.simplecity.amp_library.utils.ShuttleUtils;
import com.simplecity.amp_library.utils.SleepTimer;

import java.util.ConcurrentModificationException;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Action;
import io.reactivex.schedulers.Schedulers;

@SuppressLint("InlinedApi")
public class MusicService extends Service {

    private static final String TAG = "MusicService";

    private MusicServiceCallbacks musicServiceCallbacks = new MusicServiceCallbacks();

    QueueManager queueManager = new QueueManager(musicServiceCallbacks);

    private BluetoothManager bluetoothManager = new BluetoothManager(musicServiceCallbacks);

    private HeadsetManager headsetManager = new HeadsetManager(musicServiceCallbacks);

    private WidgetManager widgetManager = new WidgetManager();

    private ScrobbleManager scrobbleManager = new ScrobbleManager();

    private ChromecastManager chromecastManager = new ChromecastManager(this, queueManager, musicServiceCallbacks);

    private Equalizer equalizer;

    boolean playOnQueueLoad;

    /**
     * Idle time before stopping the foreground notification (5 minutes)
     */
    private static final int IDLE_DELAY = 5 * 60 * 1000;

    private int playbackState;

    @interface PlaybackLocation {
        int REMOTE = 0;
        int LOCAL = 1;
    }

    @PlaybackLocation
    private int playbackLocation = PlaybackLocation.LOCAL;

    public static final int PLAYING = 0;
    public static final int PAUSED = 1;
    public static final int STOPPED = 2;

    private final IBinder binder = new LocalBinder(this);

    @Nullable
    MultiPlayer player;

    private BroadcastReceiver unmountReceiver = null;

    private MediaSessionCompat mediaSession;

    private ComponentName mediaButtonReceiverComponent;


    //Todo:
    // Don't make this public. The MultiPlayer uses it. Just attach a listener to the MultiPlayer
    // to listen for onCompletion, and acquire the wakelock there.
    public WakeLock wakeLock;

    private int serviceStartId = -1;

    private boolean serviceInUse = false;

    private int openFailedCounter = 0;

    private boolean isSupposedToBePlaying = false;

    /**
     * Gets the last played time to determine whether we still want notifications or not
     */
    private long lastPlayedTime;

    private MusicNotificationHelper notificationHelper;

    private static final int NOTIFY_MODE_NONE = 0;
    private static final int NOTIFY_MODE_FOREGROUND = 1;
    private static final int NOTIFY_MODE_BACKGROUND = 2;

    private MediaPlayerHandler playerHandler;

    private HandlerThread handlerThread;

    private Handler mainHandler;

    private static NotificationStateHandler notificationStateHandler;

    // Used to track what type of audio focus loss caused the playback to pause
    boolean pausedByTransientLossOfFocus = false;

    private AudioManager audioManager;

    private OnAudioFocusChangeListener audioFocusListener = new OnAudioFocusChangeListener() {
        public void onAudioFocusChange(final int focusChange) {
            playerHandler.obtainMessage(PlayerHandler.FOCUS_CHANGE, focusChange, 0).sendToTarget();
        }
    };

    private AlarmManager alarmManager;

    private PendingIntent shutdownIntent;

    @Nullable
    private AudioFocusRequest audioFocusRequest;

    private boolean shutdownScheduled;

    private CompositeDisposable disposables = new CompositeDisposable();

    boolean pauseOnTrackFinish = false;


    private final BroadcastReceiver intentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String action = intent.getAction();
            final String cmd = intent.getStringExtra("command");

            if (MediaButtonCommand.NEXT.equals(cmd) || ServiceCommand.NEXT_ACTION.equals(action)) {
                gotoNext(true);
            } else {
                if (MediaButtonCommand.PREVIOUS.equals(cmd) || ServiceCommand.PREV_ACTION.equals(action)) {
                    if (getPosition() < 2000) {
                        prev();
                    } else {
                        seekTo(0);
                        play();
                    }
                } else {
                    if (MediaButtonCommand.TOGGLE_PAUSE.equals(cmd)
                            || ServiceCommand.TOGGLE_PAUSE_ACTION.equals(action)) {
                        if (isPlaying()) {
                            pause();
                            pausedByTransientLossOfFocus = false;
                        } else {
                            play();
                        }
                    } else if (MediaButtonCommand.PAUSE.equals(cmd)
                            || ServiceCommand.PAUSE_ACTION.equals(action)) {
                        pause();
                        pausedByTransientLossOfFocus = false;
                    } else if (MediaButtonCommand.PLAY.equals(cmd)) {
                        play();
                    } else if (MediaButtonCommand.STOP.equals(cmd)) {
                        pause();
                        pausedByTransientLossOfFocus = false;
                        releaseServiceUiAndStop();
                    } else if (MediaButtonCommand.TOGGLE_FAVORITE.equals(cmd)) {
                        toggleFavorite();
                    }
                    widgetManager.processCommand(MusicService.this, intent, cmd);
                }
            }
        }
    };

    @SuppressLint("InlinedApi")
    @Override
    public void onCreate() {
        super.onCreate();

        notificationHelper = new MusicNotificationHelper(this);

        // Start up the thread running the service. Note that we create a separate thread because the service normally runs in the process's
        // main thread, which we don't want to block. We also make it background priority so CPU-intensive work will not disrupt the UI.
        handlerThread = new HandlerThread("MusicPlayerHandler", android.os.Process.THREAD_PRIORITY_BACKGROUND);
        handlerThread.start();

        mainHandler = new Handler(Looper.getMainLooper());

        playerHandler = new MediaPlayerHandler(this, handlerThread.getLooper());

        notificationStateHandler = new NotificationStateHandler(this);

        headsetManager.registerHeadsetPlugReceiver(this);
        bluetoothManager.registerBluetoothReceiver(this);
        bluetoothManager.registerA2dpServiceListener(this);

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        mediaButtonReceiverComponent = new ComponentName(getPackageName(), MediaButtonIntentReceiver.class.getName());

        setupMediaSession();

        chromecastManager.init();

        playbackState = STOPPED;

        registerExternalStorageListener();

        player = new MultiPlayer(this);
        player.setHandler(playerHandler);

        equalizer = new Equalizer(this);

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

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
        wakeLock.setReferenceCounted(false);

        // Initialize the delayed shutdown intent
        Intent shutdownIntent = new Intent(this, MusicService.class);
        shutdownIntent.setAction(ServiceCommand.SHUTDOWN);

        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        this.shutdownIntent = PendingIntent.getService(this, 0, shutdownIntent, 0);

        // Listen for the idle state
        scheduleDelayedShutdown();

        reloadQueue();

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

    private void setupMediaSession() {
        mediaSession = new MediaSessionCompat(this, "Shuttle", mediaButtonReceiverComponent, null);
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPause() {
                pause();
                pausedByTransientLossOfFocus = false;
            }

            @Override
            public void onPlay() {
                play();
            }

            @Override
            public void onSeekTo(long pos) {
                seekTo(pos);
            }

            @Override
            public void onSkipToNext() {
                gotoNext(true);
            }

            @Override
            public void onSkipToPrevious() {
                prev();
            }

            @Override
            public void onStop() {
                pause();
                pausedByTransientLossOfFocus = false;
                releaseServiceUiAndStop();
            }

            @Override
            public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
                Log.e("MediaButtonReceiver", "OnMediaButtonEvent called");
                MediaButtonIntentReceiver.MediaButtonReceiverHelper.onReceive(MusicService.this, mediaButtonEvent);
                return true;
            }
        });
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS | MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS);

        //For some reason, MediaSessionCompat doesn't seem to pass all of the available 'actions' on as
        //transport control flags for the RCC, so we do that manually
        RemoteControlClient remoteControlClient = (RemoteControlClient) mediaSession.getRemoteControlClient();
        if (remoteControlClient != null) {
            remoteControlClient.setTransportControlFlags(
                    RemoteControlClient.FLAG_KEY_MEDIA_PAUSE
                            | RemoteControlClient.FLAG_KEY_MEDIA_PLAY
                            | RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE
                            | RemoteControlClient.FLAG_KEY_MEDIA_NEXT
                            | RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS
                            | RemoteControlClient.FLAG_KEY_MEDIA_STOP);
        }

    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        //If nothing is playing, and won't be playing any time soon, we can stop the service.
        //Presumably this is what the user wanted.
        if (!isPlaying() && !pausedByTransientLossOfFocus) {
            stopSelf();
        }

        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {

        saveState(true);

        chromecastManager.release();

        equalizer.releaseEffects();
        equalizer.closeEqualizerSessions(true, getAudioSessionId());

        //Shutdown the EQ
        Intent shutdownEqualizer = new Intent(MusicService.this, Equalizer.class);
        stopService(shutdownEqualizer);

        alarmManager.cancel(shutdownIntent);

        // Remove any callbacks from the handlers
        playerHandler.removeCallbacksAndMessages(null);
        notificationStateHandler.removeCallbacksAndMessages(null);

        // quit the thread so that anything that gets posted won't run
        handlerThread.quitSafely();

        mainHandler.removeCallbacksAndMessages(null);

        // release all MediaPlayer resources, including the native player and
        // wakelocks
        if (player != null) {
            player.release();
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
        mediaSession.release();

        headsetManager.unregisterHeadsetPlugReceiver(this);
        bluetoothManager.unregisterBluetoothReceiver(this);
        bluetoothManager.unregisterA2dpServiceListener(this);

        unregisterReceiver(intentReceiver);
        if (unmountReceiver != null) {
            unregisterReceiver(unmountReceiver);
            unmountReceiver = null;
        }

        wakeLock.release();

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
                if (getPosition() < 2000) {
                    prev();
                } else {
                    seekTo(0);
                    play();
                }
            } else if (MediaButtonCommand.TOGGLE_PAUSE.equals(cmd)
                    || ServiceCommand.TOGGLE_PAUSE_ACTION.equals(action)) {
                if (isPlaying()) {
                    pause();
                    pausedByTransientLossOfFocus = false;
                } else {
                    play();
                }
            } else if (MediaButtonCommand.PAUSE.equals(cmd) || ServiceCommand.PAUSE_ACTION.equals(action)) {
                pause();
                pausedByTransientLossOfFocus = false;
            } else if (MediaButtonCommand.PLAY.equals(cmd)) {
                play();
            } else if (ServiceCommand.STOP_ACTION.equals(action) || MediaButtonCommand.STOP.equals(action)) {
                pause();
                pausedByTransientLossOfFocus = false;
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

        // make sure the service will shut down on its own if it was
        // just started but not bound to and nothing is playing
        scheduleDelayedShutdown();

        if (intent != null && intent.getBooleanExtra(MediaButtonCommand.FROM_MEDIA_BUTTON, false)) {
            MediaButtonIntentReceiver.completeWakefulIntent(intent);
        }

        return START_STICKY;
    }

    void releaseServiceUiAndStop() {

        if (isPlaying()
                || pausedByTransientLossOfFocus
                || playerHandler.hasMessages(PlayerHandler.TRACK_ENDED)) {
            return;
        }

        cancelNotification();
        if (ShuttleUtils.hasOreo()) {
            if (audioFocusRequest != null) {
                audioManager.abandonAudioFocusRequest(audioFocusRequest);
            }
        } else {
            audioManager.abandonAudioFocus(audioFocusListener);
        }

        mediaSession.setActive(false);

        if (!serviceInUse) {
            saveState(true);

            //Shutdown the EQ
            Intent shutdownEqualizer = new Intent(MusicService.this, Equalizer.class);
            stopService(shutdownEqualizer);

            stopSelf(serviceStartId);
        }
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

        if (isSupposedToBePlaying || pausedByTransientLossOfFocus) {
            // Something is currently playing, or will be playing once
            // an in-progress action requesting audio focus ends, so don't stop
            // the service now.
            return true;

            // If there is a playlist but playback is paused, then wait a while
            // before stopping the service, so that pause/resume isn't slow.
            // Also delay stopping the service if we're transitioning between
            // tracks.
        } else if (queueManager.playlist.size() > 0 || queueManager.shuffleList.size() > 0 || playerHandler.hasMessages(PlayerHandler.TRACK_ENDED)) {
            scheduleDelayedShutdown();
            return true;
        }

        stopSelf(serviceStartId);

        //Shutdown the EQ
        Intent shutdownEqualizer = new Intent(MusicService.this, Equalizer.class);
        stopService(shutdownEqualizer);
        return true;
    }

    /**
     * Called when we receive a ACTION_MEDIA_EJECT notification.
     */
    public void closeExternalStorageFiles() {
        // stop playback and clean up if the SD card is going to be unmounted.
        stop(true);
        notifyChange(InternalIntents.QUEUE_CHANGED);
        notifyChange(InternalIntents.META_CHANGED);
    }

    /**
     * Registers an intent to listen for ACTION_MEDIA_EJECT notifications. The
     * intent will call closeExternalStorageFiles() if the external media is
     * going to be ejected, so applications can clean up any files they have
     * open.
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
        extras.putLong("position", getPosition());
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
            case InternalIntents.POSITION_CHANGED:
                updateMediaSession(action);
                return;
            case InternalIntents.FAVORITE_CHANGED:
                updateNotification();
                return;
            case InternalIntents.PLAY_STATE_CHANGED:
                onPlayStateChanged(action);
                break;
            case InternalIntents.META_CHANGED:
                onMetaChanged(action);
                break;
            case InternalIntents.QUEUE_CHANGED:
                onQueueChanged(action);
                break;
        }

        if (queueManager.getCurrentSong() != null) {
            Intent intent = new Intent(action);
            intent.putExtras(getExtras(queueManager.getCurrentSong()));
            sendBroadcast(intent);
        }

        widgetManager.notifyChange(this, action);

        saveState(false);
    }

    private void onQueueChanged(String action) {
        if (isPlaying()) {
            setNextTrack();
        }
        updateMediaSession(action);
    }

    private void onMetaChanged(String action) {

        updateMediaSession(action);

        if (queueManager.getCurrentSong() != null) {
            queueManager.getCurrentSong().setStartTime();

            sendBroadcast(getTaskerIntent(queueManager.getCurrentSong()));

            sendBroadcast(getPebbleIntent(queueManager.getCurrentSong()));

            bluetoothManager.sendMetaChangedIntent(this, getExtras(queueManager.getCurrentSong()));

            scrobbleManager.scrobbleBroadcast(this, ScrobbleManager.ScrobbleStatus.START, queueManager.getCurrentSong());
        }
    }

    private void onPlayStateChanged(String action) {
        updateMediaSession(action);

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

    /**
     * Save the current state of the player in preferences. This stores the player's seek position,
     * and calls {@link QueueManager#saveQueue(boolean)} to save our current playback position,
     * shuffle mode, etc.
     *
     * @param saveQueue whether to save the queue as well
     */
    void saveState(boolean saveQueue) {
        if (player != null && player.isInitialized()) {
            PlaybackSettingsManager.INSTANCE.setSeekPosition(player.getPosition());
        }
        queueManager.saveQueue(saveQueue);
    }

    /**
     * Queues a new list for playback
     *
     * @param songs  The list to queue
     * @param action The action to take
     */
    public void enqueue(List<Song> songs, @QueueManager.EnqueueAction final int action) {
        queueManager.enqueue(
                songs,
                action,
                this::setNextTrack,
                () -> {
                    openCurrentAndNext();
                    play();
                });
    }

    private void openCurrent() {
        openCurrentAndMaybeNext(false);
    }

    void openCurrentAndNext() {
        openCurrentAndMaybeNext(true);
    }

    /**
     * Called to open a new file as the current track and prepare the next for playback
     */
    private void openCurrentAndMaybeNext(final boolean openNext) {
        synchronized (this) {
            if (queueManager.getCurrentPlaylist() == null || queueManager.getCurrentPlaylist().isEmpty() || queueManager.queuePosition < 0 || queueManager.queuePosition >= queueManager.getCurrentPlaylist().size()) {
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
                        scheduleDelayedShutdown();
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
                scheduleDelayedShutdown();
                if (isSupposedToBePlaying) {
                    isSupposedToBePlaying = false;
                    notifyChange(InternalIntents.PLAY_STATE_CHANGED);
                }

            }
            if (openNext) {
                setNextTrack();
            }
        }
    }

    /**
     * Sets the track to be played
     */
    protected void setNextTrack() {
        queueManager.nextPlayPos = getNextPosition(false);
        if (queueManager.nextPlayPos >= 0
                && queueManager.getCurrentPlaylist() != null
                && !queueManager.getCurrentPlaylist().isEmpty()
                && queueManager.nextPlayPos < queueManager.getCurrentPlaylist().size()) {
            final Song nextSong = queueManager.getCurrentPlaylist().get(queueManager.nextPlayPos);
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

    public boolean open(@NonNull Song song) {
        synchronized (this) {
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
    }

    /**
     * Opens a file and prepares it for playback
     *
     * @param path The path of the file to open
     */
    public void openFile(String path, @Nullable Action completion) {
        synchronized (this) {

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
    }

    /**
     * Starts playback of a previously opened file.
     */
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

        if (mediaSession != null && !mediaSession.isActive()) {
            try {
                mediaSession.setActive(true);
            } catch (Exception e) {
                Log.e(TAG, "mSession.setActive() failed");
            }
        }

        switch (playbackLocation) {
            case PlaybackLocation.LOCAL: {
                if (player != null && player.isInitialized()) {
                    // if we are at the end of the song, go to the next song first
                    final long duration = player.getDuration();
                    if (queueManager.repeatMode != QueueManager.RepeatMode.ONE && duration > 2000
                            && player.getPosition() >= duration - 2000) {
                        gotoNext(true);
                    }
                    player.start();
                    // make sure we fade in, in case a previous fadein was stopped
                    // because of another focus loss
                    playerHandler.removeMessages(PlayerHandler.FADE_DOWN);
                    playerHandler.sendEmptyMessage(PlayerHandler.FADE_UP);

                    setIsSupposedToBePlaying(true, true);

                    cancelShutdown();
                    updateNotification();
                } else if (queueManager.getCurrentPlaylist().size() == 0) {
                    // This is mostly so that if you press 'play' on a bluetooth headset
                    // without ever having played anything before, it will still play
                    // something.
                    if (queueManager.queueReloading) {
                        playOnQueueLoad = true;
                    } else {
                        playAutoShuffleList();
                    }
                }
                break;
            }
            case PlaybackLocation.REMOTE: {
                // if we are at the end of the song, go to the next song first
                final long duration = player.getDuration();
                if (queueManager.repeatMode != QueueManager.RepeatMode.ONE && duration > 2000 && player.getPosition() >= duration - 2000) {
                    gotoNext(true);
                }

                if (!isSupposedToBePlaying) {
                    isSupposedToBePlaying = true;
                    notifyChange(InternalIntents.PLAY_STATE_CHANGED);
                }

                cancelShutdown();
                updateNotification();

                switch (playbackState) {
                    case STOPPED: {
                        try {
                            if (queueManager.getCurrentSong() != null) {
                                chromecastManager.castManager.checkConnectivity();
                                chromecastManager.prepareChromeCastLoad(queueManager.getCurrentSong(), 0, true);
                                playbackState = PLAYING;
                                updateNotification();
                            }
                        } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
                            Log.e(TAG, "Play() called & failed. State: Stopped " + e.toString());
                            playbackState = STOPPED;
                            updateNotification();
                        }
                        break;
                    }
                    case PAUSED: {
                        try {
                            chromecastManager.castManager.checkConnectivity();
                            chromecastManager.castManager.play();
                            playbackState = PLAYING;
                            updateNotification();
                        } catch (TransientNetworkDisconnectionException | NoConnectionException | CastException e) {
                            Log.e(TAG, "Play() called & failed. State: Paused " + e.toString());
                            playbackState = PAUSED;
                            updateNotification();
                        }
                        break;
                    }
                }

                if (queueManager.getCurrentPlaylist().size() == 0) {
                    // This is mostly so that if you press 'play' on a bluetooth headset
                    // without every having played anything before, it will still play
                    // something.

                    if (queueManager.queueReloading) {
                        playOnQueueLoad = true;
                    } else {
                        playAutoShuffleList();
                    }
                }
            }
        }
    }

    private void updateMediaSession(final String action) {

        int playState = isSupposedToBePlaying ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED;

        long playbackActions = getMediaSessionActions();

        if (action.equals(InternalIntents.PLAY_STATE_CHANGED) || action.equals(InternalIntents.POSITION_CHANGED)) {
            //noinspection WrongConstant
            mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                    .setActions(playbackActions)
                    .setState(playState, getPosition(), 1.0f)
                    .build());
        } else if (action.equals(InternalIntents.META_CHANGED) || action.equals(InternalIntents.QUEUE_CHANGED)) {

            if (queueManager.getCurrentSong() != null) {
                MediaMetadataCompat.Builder metaData = new MediaMetadataCompat.Builder()
                        .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, queueManager.getCurrentSong().artistName)
                        .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, queueManager.getCurrentSong().albumArtistName)
                        .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, queueManager.getCurrentSong().albumName)
                        .putString(MediaMetadataCompat.METADATA_KEY_TITLE, queueManager.getCurrentSong().name)
                        .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, queueManager.getCurrentSong().duration)
                        .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, (long) (getQueuePosition() + 1))
                        //Getting the genre is expensive.. let's not bother for now.
                        //.putString(MediaMetadataCompat.METADATA_KEY_GENRE, getGenreName())
                        .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, null)
                        .putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, (long) (getQueue().size()));

                if (SettingsManager.getInstance().showLockscreenArtwork()) {
                    //Glide has to be called from the main thread.
                    doOnMainThread(() -> Glide.with(MusicService.this)
                            .load(queueManager.getCurrentSong().getAlbum())
                            .asBitmap()
                            .override(1024, 1024)
                            .into(new SimpleTarget<Bitmap>() {
                                @Override
                                public void onResourceReady(Bitmap bitmap, GlideAnimation<? super Bitmap> glideAnimation) {
                                    if (bitmap != null) {
                                        metaData.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap);
                                    }
                                    try {
                                        mediaSession.setMetadata(metaData.build());
                                    } catch (NullPointerException e) {
                                        metaData.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, null);
                                        mediaSession.setMetadata(metaData.build());
                                    }
                                }

                                @Override
                                public void onLoadFailed(Exception e, Drawable errorDrawable) {
                                    super.onLoadFailed(e, errorDrawable);
                                    mediaSession.setMetadata(metaData.build());
                                }
                            }));
                } else {
                    mediaSession.setMetadata(metaData.build());
                }

                try {
                    mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                            .setActions(playbackActions)
                            .setState(playState, getPosition(), 1.0f)
                            .build());
                } catch (IllegalStateException e) {
                    LogUtils.logException(TAG, "Error setting playback state", e);
                }
            }
        }
    }

    void updateNotification() {

        final int notifyMode;

        if (isPlaying()) {
            notifyMode = NOTIFY_MODE_FOREGROUND;
        } else if (recentlyPlayed()) {
            notifyMode = NOTIFY_MODE_BACKGROUND;
        } else {
            notifyMode = NOTIFY_MODE_NONE;
        }

        switch (notifyMode) {
            case NOTIFY_MODE_FOREGROUND:
                startForegroundImpl();
                break;
            case NOTIFY_MODE_BACKGROUND:
                try {
                    if (queueManager.getCurrentSong() != null) {
                        notificationHelper.notify(this, queueManager.getCurrentSong(), isPlaying(), mediaSession);
                    }
                } catch (ConcurrentModificationException e) {
                    LogUtils.logException(TAG, "Exception while attempting to show notification", e);
                }
                stopForegroundImpl(false, false);
                break;
            case NOTIFY_MODE_NONE:
                stopForegroundImpl(false, false);
                notificationHelper.cancel();
                break;
        }
    }

    private void cancelNotification() {
        stopForegroundImpl(true, true);
        notificationHelper.cancel();
    }

    private void doOnMainThread(UnsafeAction action) {
        mainHandler.post(action::run);
    }

    public static PendingIntent retrievePlaybackAction(Context context, final String action) {
        final ComponentName serviceName = new ComponentName(context, MusicService.class);
        Intent intent = new Intent(action);
        intent.setComponent(serviceName);

        return PendingIntent.getService(context, 0, intent, 0);
    }

    private void stop(final boolean goToIdle) {
        switch (playbackLocation) {
            case PlaybackLocation.LOCAL: {
                if (player != null && player.isInitialized()) {
                    player.stop();
                }
                if (goToIdle) {
                    setIsSupposedToBePlaying(false, false);
                } else {
                    stopForegroundImpl(false, true);
                }
                break;
            }
            case PlaybackLocation.REMOTE: {
                try {
                    if (player != null && player.isInitialized()) {
                        player.seekTo(chromecastManager.castManager.getCurrentMediaPosition());
                        player.stop();
                    }
                    playbackState = STOPPED;
                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                }
                if (goToIdle) {
                    if (ShuttleUtils.isUpgraded() && chromecastManager.castManager != null) {
                        HttpServer.getInstance().stop();
                    }
                    setIsSupposedToBePlaying(false, false);
                } else {
                    stopForegroundImpl(false, true);
                }
                break;
            }
        }
    }

    /**
     * Stops playback.
     */
    public void stop() {
        stop(true);
    }

    /**
     * Pauses playback (call play() to resume)
     */
    public void pause() {
        synchronized (this) {

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
                        playbackState = PAUSED;
                        scheduleDelayedShutdown();
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
    }

    /**
     * Returns whether something is currently playing
     *
     * @return true if something is playing (or will be playing shortly, in case
     * we're currently transitioning between tracks), false if not.
     */
    public boolean isPlaying() {

        switch (playbackLocation) {
            case PlaybackLocation.LOCAL: {
                return isSupposedToBePlaying;
            }
            case PlaybackLocation.REMOTE: {
                return playbackState == PLAYING;
            }
        }

        return false;
    }

    /**
     * Helper function to wrap the logic around isSupposedToBePlaying for consistency
     *
     * @param value  to set isSupposedToBePlaying to
     * @param notify whether we want to fire PLAY_STATE_CHANGED event
     */
    void setIsSupposedToBePlaying(boolean value, boolean notify) {
        if (isSupposedToBePlaying != value) {
            isSupposedToBePlaying = value;

            // Update mLastPlayed time first and notify afterwards, as
            // the notification listener method needs the up-to-date value
            // for the recentlyPlayed() method to work
            if (!isSupposedToBePlaying) {
                scheduleDelayedShutdown();
                lastPlayedTime = System.currentTimeMillis();
            }

            if (notify) {
                notifyChange(InternalIntents.PLAY_STATE_CHANGED);
            }
        }
    }

    /**
     * @return true if is playing or has played within the last IDLE_DELAY time
     */
    private boolean recentlyPlayed() {
        return isPlaying() || System.currentTimeMillis() - lastPlayedTime < IDLE_DELAY;
    }

    public void prev() {
        playerHandler.sendEmptyMessage(PlayerHandler.GO_TO_PREV);
    }

    /*
        Desired behavior for prev/next/shuffle:

        - NEXT will move to the next track in the list when not shuffling, and to
          a track randomly picked from the not-yet-played tracks when shuffling.
          If all tracks have already been played, pick from the full set, but
          avoid picking the previously played track if possible.
        - when shuffling, PREV will go to the previously played track. Hitting PREV
          again will go to the track played before that, etc. When the start of the
          history has been reached, PREV is a no-op.
          When not shuffling, PREV will go to the sequentially previous track (the
          difference with the shuffle-case is mainly that when not shuffling, the
          user can back up to tracks that are not in the history).

          Example:
          When playing an album with 10 tracks from the start, and enabling shuffle
          while playing track 5, the remaining tracks (6-10) will be shuffled, e.g.
          the final play order might be 1-2-3-4-5-8-10-6-9-7.
          When hitting 'prev' 8 times while playing track 7 in this example, the
          user will go to tracks 9-6-10-8-5-4-3-2. If the user then hits 'next',
          a random track will be picked again. If at any time user disables shuffling
          the next/previous track will be picked in sequential order again.
       */
    public void previous() {
        synchronized (this) {
            queueManager.previous();
            stop(false);
            openCurrent();
            play();
            notifyChange(InternalIntents.META_CHANGED);
        }
    }

    /**
     * @param force True to force the player onto the track next, false
     *              otherwise.
     * @return The next position to play.
     */
    private int getNextPosition(final boolean force) {
        return queueManager.getNextPosition(force);
    }

    public void next() {
        playerHandler.sendEmptyMessage(PlayerHandler.GO_TO_NEXT);
    }

    /**
     * Changes from the current track to the next track
     */
    public void gotoNext(final boolean force) {
        synchronized (this) {

            notifyChange(InternalIntents.TRACK_ENDING);

            if (queueManager.getCurrentPlaylist().size() == 0) {
                scheduleDelayedShutdown();
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
    }

    private void saveBookmarkIfNeeded() {
        try {
            if (queueManager.getCurrentSong() != null) {
                if (queueManager.getCurrentSong().isPodcast) {
                    long pos = getPosition();
                    long bookmark = queueManager.getCurrentSong().bookMark;
                    long duration = queueManager.getCurrentSong().duration;
                    if ((pos < bookmark && (pos + 10000) > bookmark) || (pos > bookmark && (pos - 10000) < bookmark)) {
                        // The existing bookmark is close to the current
                        // position, so don't update it.
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
                        getContentResolver().update(uri, values, null, null);
                    }
                }
            }
        } catch (SQLiteException ignored) {
        }
    }

    public void playAutoShuffleList() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            DataManager.getInstance().getSongsRelay()
                    .firstOrError()
                    .subscribeOn(Schedulers.io())
                    .subscribe(songs -> {
                        queueManager.playlist = songs;
                        queueManager.queuePosition = -1;
                        queueManager.makeShuffleList();
                        setShuffleMode(QueueManager.ShuffleMode.ON);
                        notifyChange(InternalIntents.QUEUE_CHANGED);
                        queueManager.queuePosition = 0;
                        openCurrentAndNext();
                        play();
                        notifyChange(InternalIntents.META_CHANGED);
                        saveState(false);
                    }, error -> LogUtils.logException(TAG, "Error playing auto shuffle list", error));

        } else {
            queueManager.shuffleMode = QueueManager.ShuffleMode.OFF;
            saveState(false);
        }
    }


    public void toggleFavorite() {
        if (queueManager.getCurrentSong() != null) {
            PlaylistUtils.toggleFavorite(queueManager.getCurrentSong(), isFavorite -> {
                if (isFavorite) {
                    Toast.makeText(MusicService.this, getString(R.string.song_to_favourites, queueManager.getCurrentSong().name), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MusicService.this, getString(R.string.song_removed_from_favourites, queueManager.getCurrentSong().name), Toast.LENGTH_SHORT).show();
                }
                notifyChange(InternalIntents.FAVORITE_CHANGED);
            });
        }
    }

    public int getShuffleMode() {
        return queueManager.shuffleMode;
    }

    public void setShuffleMode(int shufflemode) {
        synchronized (this) {
            queueManager.setShuffleMode(shufflemode);
        }
    }

    public int getRepeatMode() {
        return queueManager.repeatMode;
    }

    public void setRepeatMode(int repeatMode) {
        synchronized (this) {
            queueManager.setRepeatMode(repeatMode);
            setNextTrack();
        }
    }

    @Nullable
    public Song getSong() {
        return queueManager.getCurrentSong();
    }

    /**
     * Returns the current playback position in milliseconds
     */
    public long getPosition() {
        synchronized (this) {
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
    }

    /**
     * Seeks to the position specified.
     *
     * @param position The position to seek to, in milliseconds
     */
    public void seekTo(long position) {
        synchronized (this) {
            if (player != null && player.isInitialized()) {
                if (position < 0) {
                    position = 0;
                } else if (position > player.getDuration()) {
                    position = player.getDuration();
                }

                player.seekTo(position);

                if (playbackLocation == PlaybackLocation.REMOTE) {
                    try {
                        chromecastManager.castManager.seek((int) position);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                notifyChange(InternalIntents.POSITION_CHANGED);
            }
        }
    }

    /**
     * Returns the audio session ID.
     *
     * @return int
     */
    public int getAudioSessionId() {
        synchronized (this) {
            if (player != null) {
                return player.getAudioSessionId();
            } else {
                return 0;
            }
        }
    }

    public void toggleShuffleMode() {
        switch (getShuffleMode()) {
            case QueueManager.ShuffleMode.ON:
                setShuffleMode(QueueManager.ShuffleMode.ON);
                notifyChange(InternalIntents.SHUFFLE_CHANGED);
                queueManager.makeShuffleList();
                notifyChange(InternalIntents.QUEUE_CHANGED);
                if (getRepeatMode() == QueueManager.RepeatMode.ONE) {
                    setRepeatMode(QueueManager.RepeatMode.ALL);
                }
                showToast(R.string.shuffle_on_notif);
                break;
            case QueueManager.ShuffleMode.OFF:
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

    public void closeEqualizerSessions(boolean internal, int audioSessionId) {
        equalizer.closeEqualizerSessions(internal, audioSessionId);
    }

    public void openEqualizerSession(boolean internal, int audioSessionId) {
        equalizer.openEqualizerSession(internal, audioSessionId);
    }

    public void updateEqualizer() {
        equalizer.update();
    }

    private void showToast(int resId) {
        Toast.makeText(getBaseContext(), resId, Toast.LENGTH_SHORT).show();
    }

    private void scheduleDelayedShutdown() {
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + IDLE_DELAY, shutdownIntent);
        shutdownScheduled = true;
    }

    private void cancelShutdown() {
        if (shutdownScheduled) {
            alarmManager.cancel(shutdownIntent);
            shutdownScheduled = false;
        }
    }

    private long getMediaSessionActions() {
        return PlaybackStateCompat.ACTION_PLAY
                | PlaybackStateCompat.ACTION_PAUSE
                | PlaybackStateCompat.ACTION_PLAY_PAUSE
                | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                | PlaybackStateCompat.ACTION_STOP
                | PlaybackStateCompat.ACTION_SEEK_TO;
    }

    /**
     * Starts the foreground notification, and cancels any stop messages
     */
    private void startForegroundImpl() {
        try {
            notificationStateHandler.sendEmptyMessage(NotificationStateHandler.START_FOREGROUND);
            Song song = queueManager.getCurrentSong();
            if (song != null) {
                notificationHelper.startForeground(this, queueManager.getCurrentSong(), isPlaying(), mediaSession);
            }
        } catch (NullPointerException | ConcurrentModificationException e) {
            Crashlytics.log("startForegroundImpl error: " + e.getMessage());
        }
    }

    /**
     * Stops the foreground notification
     *
     * @param removeNotification true to remove the notification as well as stop the service running in the foreground
     * @param withDelay          true to delay the stop call by 1.5 seconds, allowing subsequent start calls to cancel this call
     */
    void stopForegroundImpl(boolean removeNotification, boolean withDelay) {
        if (withDelay) {
            notificationStateHandler.sendEmptyMessageDelayed(NotificationStateHandler.STOP_FOREGROUND, 1500);
        } else {
            stopForeground(removeNotification);
        }
    }


    // Queue related methods

    /**
     * Opens a list for playback
     *
     * @param songs    The list of tracks to open
     * @param position The position to start playback at
     */
    public void open(List<Song> songs, final int position) {
        synchronized (this) {
            queueManager.open(songs, position, this::openCurrentAndNext);
        }
    }

    public void clearQueue() {
        stop(true);
        queueManager.clearQueue();
    }

    void reloadQueue() {
        if (ContextCompat.checkSelfPermission(MusicService.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        queueManager.reloadQueue(
                () -> {
                    if (playOnQueueLoad) {
                        play();
                        playOnQueueLoad = false;
                    }
                },
                new UnsafeAction() {
                    @Override
                    public void run() {
                        synchronized (this) {
                            openFailedCounter = 20;
                            openCurrentAndNext();
                        }
                    }
                },
                seekPos -> {
                    if (queueManager.getCurrentSong() != null) {
                        seekTo(seekPos < queueManager.getCurrentSong().duration ? seekPos : 0);
                    }
                });
    }

    public List<Song> getQueue() {
        synchronized (this) {
            return queueManager.getCurrentPlaylist();
        }
    }

    /**
     * Returns the position in the queue
     *
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
     * @param pos The position in the queue of the track that will be played.
     */
    public void setQueuePosition(int pos) {
        synchronized (this) {
            stop(false);
            queueManager.queuePosition = pos;
            openCurrentAndNext();
            play();
            notifyChange(InternalIntents.META_CHANGED);
        }
    }

    UnsafeAction stop = () -> stop(true);

    UnsafeAction moveToNextTrack = () -> {
        final boolean wasPlaying = isPlaying();
        stop(false);
        openCurrentAndNext();
        if (wasPlaying) {
            play();
        }
    };

    public void removeSongs(List<Song> songs) {
        queueManager.removeSongs(songs, stop, moveToNextTrack);
    }

    public void removeSong(int position) {
        queueManager.removeSong(position, stop, moveToNextTrack);
    }

    public void moveQueueItem(int from, int to) {
        queueManager.moveQueueItem(from, to);
    }

    public interface Callbacks {
        void notifyChange(String what);

        void pause();

        void play();

        void seekTo(long position);

        long getPosition();

        @PlaybackLocation
        int getPlaybackLocation();

        void setPlaybackLocation(@PlaybackLocation int location);

        void setPlaybackState(int playbackState);

        void setIsSupposedToBePlaying(boolean supposedToBePlaying, boolean notify);

        boolean getIsSupposedToBePlaying();

        void notifyTrackEnded();
    }

    class MusicServiceCallbacks implements Callbacks {

        @Override
        public void notifyChange(String what) {
            MusicService.this.notifyChange(what);
        }

        @Override
        public void pause() {
            MusicService.this.pause();
        }

        @Override
        public void play() {
            MusicService.this.play();
        }

        @Override
        public void seekTo(long position) {
            MusicService.this.seekTo(position);
        }

        @Override
        public long getPosition() {
            return MusicService.this.getPosition();
        }

        @Override
        public int getPlaybackLocation() {
            return MusicService.this.playbackLocation;
        }

        @Override
        public void setPlaybackLocation(@PlaybackLocation int location) {
            MusicService.this.playbackLocation = location;
        }

        @Override
        public void setPlaybackState(int playbackState) {
            MusicService.this.playbackState = playbackState;
        }

        @Override
        public void setIsSupposedToBePlaying(boolean supposedToBePlaying, boolean notify) {
            MusicService.this.setIsSupposedToBePlaying(supposedToBePlaying, notify);
        }

        @Override
        public boolean getIsSupposedToBePlaying() {
            return MusicService.this.isSupposedToBePlaying;
        }

        @Override
        public void notifyTrackEnded() {
            playerHandler.sendEmptyMessage(PlayerHandler.TRACK_ENDED);
        }
    }


}