package com.simplecity.amp_library.playback;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothHeadset;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
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
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.annimon.stream.function.Predicate;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;
import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.common.images.WebImage;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.VideoCastConsumerImpl;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.CastException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.NoConnectionException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.TransientNetworkDisconnectionException;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.glide.utils.GlideUtils;
import com.simplecity.amp_library.http.HttpServer;
import com.simplecity.amp_library.model.Album;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.notifications.MusicNotificationHelper;
import com.simplecity.amp_library.playback.constants.ExternalIntents;
import com.simplecity.amp_library.playback.constants.InternalIntents;
import com.simplecity.amp_library.playback.constants.MediaButtonCommand;
import com.simplecity.amp_library.playback.constants.PlayerHandler;
import com.simplecity.amp_library.playback.constants.ServiceCommand;
import com.simplecity.amp_library.playback.constants.ShortcutCommands;
import com.simplecity.amp_library.rx.UnsafeAction;
import com.simplecity.amp_library.services.Equalizer;
import com.simplecity.amp_library.ui.widgets.WidgetProviderExtraLarge;
import com.simplecity.amp_library.ui.widgets.WidgetProviderLarge;
import com.simplecity.amp_library.ui.widgets.WidgetProviderMedium;
import com.simplecity.amp_library.ui.widgets.WidgetProviderSmall;
import com.simplecity.amp_library.utils.DataManager;
import com.simplecity.amp_library.utils.LogUtils;
import com.simplecity.amp_library.utils.MediaButtonIntentReceiver;
import com.simplecity.amp_library.utils.PlaceholderProvider;
import com.simplecity.amp_library.utils.PlaylistUtils;
import com.simplecity.amp_library.utils.SettingsManager;
import com.simplecity.amp_library.utils.ShuttleUtils;
import com.simplecity.amp_library.utils.SleepTimer;

import java.io.ByteArrayOutputStream;
import java.util.ConcurrentModificationException;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Action;
import io.reactivex.schedulers.Schedulers;

@SuppressLint("InlinedApi")
public class MusicService extends Service {

    private static final String TAG = "MusicService";

    public static final String FROM_USER = "from_user";

    interface Status {
        int START = 0;
        int RESUME = 1;
        int PAUSE = 2;
        int COMPLETE = 3;
    }

    boolean playOnQueueLoad;

    QueueManager queueManager = new QueueManager(new QueueManagerCallbacks());

    private Equalizer equalizer;

    /**
     * Idle time before stopping the foreground notification (5 minutes)
     */
    private static final int IDLE_DELAY = 5 * 60 * 1000;

    VideoCastManager castManager;

    private VideoCastConsumerImpl castConsumer;

    int playbackLocation;

    int playbackState;

    public static final int REMOTE = 0;
    public static final int LOCAL = 1;

    public static final int PLAYING = 0;
    public static final int PAUSED = 1;
    public static final int STOPPED = 2;

    private final IBinder binder = new LocalBinder(this);

    @Nullable
    MultiPlayer player;

    private BroadcastReceiver headsetReceiver;
    private BroadcastReceiver bluetoothReceiver;
    private BroadcastReceiver unmountReceiver = null;
    private BroadcastReceiver a2dpReceiver = null;

    private boolean headsetReceiverIsRegistered;
    private boolean bluetoothReceiverIsRegistered;

    MediaSessionCompat mediaSession;

    private ComponentName mediaButtonReceiverComponent;

    int castMediaStatus = -1;

    //Todo:
    // Don't make this public. The MultiPlayer uses it. Just attach a listener to the MultiPlayer
    // to listen for onCompletion, and acquire the wakelock there.
    public WakeLock wakeLock;

    private int serviceStartId = -1;

    private boolean serviceInUse = false;

    int openFailedCounter = 0;

    boolean isSupposedToBePlaying = false;

    /**
     * Gets the last played time to determine whether we still want notifications or not
     */
    private long lastPlayedTime;

    private MusicNotificationHelper notificationHelper;

    private static final int NOTIFY_MODE_NONE = 0;
    private static final int NOTIFY_MODE_FOREGROUND = 1;
    private static final int NOTIFY_MODE_BACKGROUND = 2;

    SharedPreferences prefs;
    SharedPreferences servicePrefs;

    MediaPlayerHandler playerHandler;

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

    void updatePlaybackLocation(int location) {

        //If the location has changed and it's no longer ChromeCast
        if (location == LOCAL && location != playbackLocation) {
            try {
                if (castManager != null && castManager.isConnected()) {
                    if (player != null && player.isInitialized()) {
                        player.seekTo(castManager.getCurrentMediaPosition());
                    }
                    castManager.stop();
                }
            } catch (CastException | NoConnectionException | TransientNetworkDisconnectionException | IllegalStateException e) {
                Log.e(TAG, "updatePlaybackLocation error: " + e);
            }
        }

        playbackLocation = location;
    }

    void loadRemoteMedia(MediaInfo selectedMedia, int position, boolean autoPlay, final Bitmap bitmap, final Drawable errorDrawable) {
        Completable.fromAction(() -> {

            HttpServer.getInstance().serveAudio(queueManager.currentSong.path);

            ByteArrayOutputStream stream = new ByteArrayOutputStream();

            if (bitmap == null) {
                GlideUtils.drawableToBitmap(errorDrawable).compress(Bitmap.CompressFormat.JPEG, 80, stream);
            } else {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream);
            }

            HttpServer.getInstance().serveImage(stream.toByteArray());
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                    try {
                        castManager.loadMedia(selectedMedia, autoPlay, position);
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to load media. " + e.toString());
                    }
                }, throwable -> LogUtils.logException(TAG, "Error loading remote media", throwable));
    }

    void prepareChromeCastLoad(int position, boolean autoPlay) {

        if (queueManager.currentSong == null) {
            return;
        }

        if (TextUtils.isEmpty(queueManager.currentSong.path)) {
            return;
        }

        MediaMetadata metadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK);
        metadata.putString(MediaMetadata.KEY_ALBUM_ARTIST, getAlbumArtistName());
        metadata.putString(MediaMetadata.KEY_ALBUM_TITLE, getAlbumName());
        metadata.putString(MediaMetadata.KEY_TITLE, getSongName());
        metadata.addImage(new WebImage(Uri.parse("http://" + ShuttleUtils.getIpAddr() + ":5000" + "/image/" + getSongId())));

        MediaInfo selectedMedia = new MediaInfo.Builder("http://" + ShuttleUtils.getIpAddr() + ":5000" + "/audio/" + getSongId())
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setContentType("audio/*")
                .setMetadata(metadata)
                .build();

        if (ShuttleUtils.isUpgraded() && castManager != null) {
            doOnMainThread(() -> Glide.with(MusicService.this)
                    .load(getSong())
                    .asBitmap()
                    .override(1024, 1024)
                    .placeholder(PlaceholderProvider.getInstance().getPlaceHolderDrawable(getSong().name, true))
                    .into(new SimpleTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                            loadRemoteMedia(selectedMedia, position, autoPlay, resource, null);
                        }

                        @Override
                        public void onLoadFailed(Exception e, Drawable errorDrawable) {
                            super.onLoadFailed(e, errorDrawable);
                            loadRemoteMedia(selectedMedia, position, autoPlay, null, errorDrawable);
                        }
                    }));
        }
    }

    private void setupCastListener() {

        castConsumer = new VideoCastConsumerImpl() {

            @Override
            public void onApplicationConnected(ApplicationMetadata appMetadata, String sessionId, boolean wasLaunched) {

                Log.d(TAG, "onApplicationLaunched()");

                HttpServer.getInstance().start();

                boolean wasPlaying = isSupposedToBePlaying;

                //If music is playing on the phone, pause it
                if (playbackLocation == LOCAL && isSupposedToBePlaying) {
                    pause();
                }

                //Try to play from the same position, but on the ChromeCast
                prepareChromeCastLoad((int) getPosition(), wasPlaying);
                if (wasPlaying) {
                    playbackState = PLAYING;
                } else {
                    playbackState = PAUSED;
                }

                updatePlaybackLocation(REMOTE);
            }

            @Override
            public void onApplicationDisconnected(int errorCode) {
                Log.d(TAG, "onApplicationDisconnected() is reached with errorCode: " + errorCode);
                setIsSupposedToBePlaying(false, true);
                playbackState = STOPPED;
                updatePlaybackLocation(LOCAL);

                HttpServer.getInstance().stop();
            }

            @Override
            public void onDisconnected() {
                Log.d(TAG, "onDisconnected() is reached");
                setIsSupposedToBePlaying(false, true);
                playbackState = STOPPED;
                updatePlaybackLocation(LOCAL);

                HttpServer.getInstance().stop();
            }

            @Override
            public void onRemoteMediaPlayerStatusUpdated() {
                //Only send a track finished message if the state has changed..
                if (castManager.getPlaybackStatus() != castMediaStatus) {
                    if (castManager.getPlaybackStatus() == MediaStatus.PLAYER_STATE_IDLE
                            && castManager.getIdleReason() == MediaStatus.IDLE_REASON_FINISHED) {
                        playerHandler.sendEmptyMessage(PlayerHandler.TRACK_ENDED);
                    }
                }

                castMediaStatus = castManager.getPlaybackStatus();
            }
        };
    }

    final WidgetProviderMedium mWidgetProviderMedium = WidgetProviderMedium.getInstance();
    final WidgetProviderSmall mWidgetProviderSmall = WidgetProviderSmall.getInstance();
    final WidgetProviderLarge mWidgetProviderLarge = WidgetProviderLarge.getInstance();
    final WidgetProviderExtraLarge mWidgetProviderExtraLarge = WidgetProviderExtraLarge.getInstance();

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
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
                    if (WidgetProviderSmall.CMDAPPWIDGETUPDATE.equals(cmd)) {
                        // Someone asked us to refresh a set of specific widgets,
                        // probably because they were just added.
                        int[] appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
                        mWidgetProviderSmall.update(MusicService.this, appWidgetIds, true);
                    } else if (WidgetProviderMedium.CMDAPPWIDGETUPDATE.equals(cmd)) {
                        // Someone asked us to refresh a set of specific widgets,
                        // probably because they were just added.
                        int[] appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
                        mWidgetProviderMedium.update(MusicService.this, appWidgetIds, true);
                    } else if (WidgetProviderLarge.CMDAPPWIDGETUPDATE.equals(cmd)) {
                        // Someone asked us to refresh a set of specific widgets,
                        // probably because they were just added.
                        int[] appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
                        mWidgetProviderLarge.update(MusicService.this, appWidgetIds, true);
                    } else if (WidgetProviderExtraLarge.CMDAPPWIDGETUPDATE.equals(cmd)) {
                        // Someone asked us to refresh a set of specific widgets,
                        // probably because they were just added.
                        int[] appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
                        mWidgetProviderExtraLarge.update(MusicService.this, appWidgetIds, true);
                    }
                }
            }
        }
    };

    @SuppressLint("InlinedApi")
    @Override
    public void onCreate() {
        super.onCreate();

        notificationHelper = new MusicNotificationHelper(this);

        servicePrefs = getSharedPreferences("Service", 0);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Start up the thread running the service. Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block. We also make it
        // background priority so CPU-intensive work will not disrupt the UI.
        handlerThread = new HandlerThread("MusicPlayerHandler", android.os.Process.THREAD_PRIORITY_BACKGROUND);
        handlerThread.start();

        mainHandler = new Handler(Looper.getMainLooper());

        // Initialize the handlers
        playerHandler = new MediaPlayerHandler(this, handlerThread.getLooper());
        notificationStateHandler = new NotificationStateHandler(this);

        registerHeadsetPlugReceiver();
        registerBluetoothReceiver();

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        mediaButtonReceiverComponent = new ComponentName(getPackageName(),
                MediaButtonIntentReceiver.class.getName());

        setupMediaSession();

        playbackLocation = LOCAL;

        if (ShuttleUtils.isUpgraded()) {
            castManager = VideoCastManager.getInstance();
            setupCastListener();
            castManager.addVideoCastConsumer(castConsumer);
        }

        if (castManager != null && castManager.isConnected()) {
            updatePlaybackLocation(REMOTE);
        } else {
            updatePlaybackLocation(LOCAL);
        }

        playbackState = STOPPED;

        registerExternalStorageListener();
        registerA2dpServiceListener();

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
        registerReceiver(mIntentReceiver, intentFilter);

        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
        wakeLock.setReferenceCounted(false);

        // Initialize the delayed shutdown intent
        final Intent shutdownIntent = new Intent(this, MusicService.class);
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

    private void registerHeadsetPlugReceiver() {

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_HEADSET_PLUG);

        headsetReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {

                if (isInitialStickyBroadcast()) {
                    return;
                }

                if (intent.hasExtra("state")) {
                    if (intent.getIntExtra("state", 0) == 0) {
                        if (prefs.getBoolean("pref_headset_disconnect", true)) {
                            pause();
                        }
                    } else if (intent.getIntExtra("state", 0) == 1) {
                        if (prefs.getBoolean("pref_headset_connect", false)) {
                            play();
                        }
                    }
                }
            }
        };

        registerReceiver(headsetReceiver, filter);
        headsetReceiverIsRegistered = true;
    }

    private void registerBluetoothReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED);

        bluetoothReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                String action = intent.getAction();
                if (action != null) {
                    Bundle extras = intent.getExtras();
                    if (SettingsManager.getInstance().getBluetoothPauseDisconnect()) {
                        switch (action) {
                            case BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED:
                                if (extras != null) {
                                    int state = extras.getInt(BluetoothA2dp.EXTRA_STATE);
                                    int previousState = extras.getInt(BluetoothA2dp.EXTRA_PREVIOUS_STATE);
                                    if ((state == BluetoothA2dp.STATE_DISCONNECTED || state == BluetoothA2dp.STATE_DISCONNECTING) && previousState == BluetoothA2dp.STATE_CONNECTED) {
                                        pause();
                                    }
                                }
                                break;
                            case BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED:
                                if (extras != null) {
                                    int state = extras.getInt(BluetoothHeadset.EXTRA_STATE);
                                    int previousState = extras.getInt(BluetoothHeadset.EXTRA_PREVIOUS_STATE);
                                    if (state == BluetoothHeadset.STATE_AUDIO_DISCONNECTED && previousState == BluetoothHeadset.STATE_AUDIO_CONNECTED) {
                                        pause();
                                    }
                                }
                                break;
                        }
                    }

                    if (SettingsManager.getInstance().getBluetoothResumeConnect()) {
                        switch (action) {
                            case BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED:
                                if (extras != null) {
                                    int state = extras.getInt(BluetoothA2dp.EXTRA_STATE);
                                    if (state == BluetoothA2dp.STATE_CONNECTED) {
                                        play();
                                    }
                                }
                                break;
                            case BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED:
                                if (extras != null) {
                                    int state = extras.getInt(BluetoothHeadset.EXTRA_STATE);
                                    if (state == BluetoothHeadset.STATE_AUDIO_CONNECTED) {
                                        play();
                                    }
                                }
                                break;
                        }
                    }
                }
            }
        };
        registerReceiver(bluetoothReceiver, filter);
        bluetoothReceiverIsRegistered = true;
    }

    private void unregisterBluetoothReceiver() {
        if (bluetoothReceiverIsRegistered) {
            unregisterReceiver(bluetoothReceiver);
            bluetoothReceiverIsRegistered = false;
        }
    }

    private void unregisterHeadsetPlugReceiver() {

        if (headsetReceiverIsRegistered) {
            unregisterReceiver(headsetReceiver);
            headsetReceiverIsRegistered = false;
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

        if (playbackState == PLAYING) {
            try {
                castManager.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (castManager != null) {
            castManager.removeVideoCastConsumer(castConsumer);
        }

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

        unregisterHeadsetPlugReceiver();
        unregisterBluetoothReceiver();

        unregisterReceiver(mIntentReceiver);
        unregisterReceiver(a2dpReceiver);
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
                    if (action.equals(Intent.ACTION_MEDIA_EJECT)) {
                        saveState(true);
                        queueManager.queueIsSaveable = false;
                        closeExternalStorageFiles();
                    } else if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
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

    public void notifyChange(final String what) {
        notifyChange(what, false);
    }

    private Single<Bundle> getExtras(boolean fromUser) {
        return isFavorite()
                .flatMap(isFavorite -> {
                    Bundle extras = new Bundle();
                    extras.putLong("id", getSongId());
                    extras.putString("artist", getArtistName());
                    extras.putString("album", getAlbumName());
                    extras.putString("track", getSongName());
                    extras.putInt("shuffleMode", getShuffleMode());
                    extras.putInt("repeatMode", getRepeatMode());
                    extras.putBoolean("playing", isPlaying());
                    extras.putBoolean("isfavorite", isFavorite);
                    extras.putLong("duration", getDuration());
                    extras.putLong("position", getPosition());
                    extras.putLong("ListSize", queueManager.getCurrentPlaylist().size());
                    extras.putBoolean(FROM_USER, fromUser);
                    return Single.just(extras);
                });
    }

    private void notifyChange(String what, boolean fromUser) {
        if (what.equals(InternalIntents.TRACK_ENDING)) {
            //We're just about to change tracks, so 'current song' is the song that just finished
            Song finishedSong = queueManager.currentSong;
            if (finishedSong != null) {
                if (finishedSong.hasPlayed()) {
                    Completable.fromAction(() -> ShuttleUtils.incrementPlayCount(this, finishedSong)).subscribeOn(Schedulers.io())
                            .subscribe(() -> {
                                // Nothing to do
                            }, error -> LogUtils.logException(TAG, "Error incrementing play count", error));
                }
                scrobbleBroadcast(Status.COMPLETE, finishedSong);
            }
            return;
        }

        if (what.equals(InternalIntents.FAVORITE_CHANGED)) {
            updateNotification();
            Intent intent = new Intent(what);
            sendBroadcast(intent);
            return;
        }

        updateMediaSession(what);

        if (what.equals(InternalIntents.POSITION_CHANGED)) {
            return;
        }

        getExtras(fromUser)
                .subscribeOn(Schedulers.io())
                .subscribe(extras -> {
                    final Intent intent = new Intent(what);
                    intent.putExtras(extras);
                    sendBroadcast(intent);
                }, error -> LogUtils.logException(TAG, "Error sending broadcast", error));

        //Tasker intent
        Intent taskerIntent = new Intent(ExternalIntents.TASKER);

        //Pebble intent
        Intent pebbleIntent = new Intent(ExternalIntents.PEBBLE);
        pebbleIntent.putExtra("artist", getArtistName());
        pebbleIntent.putExtra("album", getAlbumName());
        pebbleIntent.putExtra("track", getSongName());

        if (what.equals(InternalIntents.PLAY_STATE_CHANGED)) {

            updateNotification();

            // Bluetooth intent
            getExtras(fromUser)
                    .subscribeOn(Schedulers.io())
                    .subscribe(extras -> {
                        final Intent intent = new Intent(ExternalIntents.AVRCP_PLAY_STATE_CHANGED);
                        intent.putExtras(extras);
                        sendBroadcast(intent);
                    }, error -> LogUtils.logException(TAG, "Error sending bluetooth intent", error));

            if (isPlaying()) {
                if (queueManager.currentSong != null) {
                    queueManager.currentSong.setResumed();
                }
                //Last.fm scrobbler intent
                scrobbleBroadcast(Status.RESUME, queueManager.currentSong);
                //Tasker intent
                taskerIntent.putExtra("%MTRACK", getSongName());
                //Pebble intent
                sendBroadcast(pebbleIntent);

            } else {
                if (queueManager.currentSong != null) {
                    queueManager.currentSong.setPaused();
                }
                //Last.fm scrobbler intent
                scrobbleBroadcast(Status.PAUSE, queueManager.currentSong);
                //Tasker intent
                taskerIntent.putExtra("%MTRACK", "");
            }

            sendBroadcast(taskerIntent);

        } else if (what.equals(InternalIntents.META_CHANGED)) {

            if (queueManager.currentSong != null) {
                queueManager.currentSong.setStartTime();
            }

            //Tasker intent
            taskerIntent.putExtra("%MTRACK", getSongName());
            sendBroadcast(taskerIntent);

            //Bluetooth intent
            getExtras(fromUser)
                    .subscribeOn(Schedulers.io())
                    .subscribe(extras -> {
                        final Intent intent = new Intent(ExternalIntents.AVRCP_META_CHANGED);
                        intent.putExtras(extras);
                        sendBroadcast(intent);
                    }, error -> LogUtils.logException(TAG, "Error AVRCP meta changed event", error));

            //Pebble intent
            sendBroadcast(pebbleIntent);

            //Last.fm scrobbler intent
            scrobbleBroadcast(Status.START, queueManager.currentSong);
        }

        if (!what.equals(InternalIntents.QUEUE_CHANGED)) {
            saveState(false);
        }

        mWidgetProviderLarge.notifyChange(MusicService.this, what);
        mWidgetProviderMedium.notifyChange(MusicService.this, what);
        mWidgetProviderSmall.notifyChange(MusicService.this, what);
        mWidgetProviderExtraLarge.notifyChange(MusicService.this, what);
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

            queueManager.currentSong = queueManager.getCurrentPlaylist().get(queueManager.queuePosition);

            while (true) {
                if (open(queueManager.currentSong)) {
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

                    queueManager.currentSong = queueManager.getCurrentPlaylist().get(queueManager.queuePosition);
                } else {
                    openFailedCounter = 0;
                    shutdown = true;
                    break;
                }
            }
            // Go to bookmark if needed
            if (isPodcast()) {
                long bookmark = getBookmark();
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

    public boolean open(Song song) {
        synchronized (this) {

            queueManager.currentSong = song;

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

            DataManager.getInstance().getSongsObservable(predicate)
                    .firstOrError()
                    .subscribe(songs -> {
                        if (!songs.isEmpty()) {
                            queueManager.currentSong = songs.get(0);
                            open(queueManager.currentSong);
                            if (completion != null) {
                                completion.run();
                            }
                        }
                    }, error -> LogUtils.logException(TAG, "Error opening file", error));
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

        if (playbackLocation == LOCAL) {
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
            case LOCAL: {
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
            case REMOTE: {
                // if we are at the end of the song, go to the next song first
                final long duration = player.getDuration();
                if (queueManager.repeatMode != QueueManager.RepeatMode.ONE && duration > 2000
                        && player.getPosition() >= duration - 2000) {
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
                            castManager.checkConnectivity();
                            prepareChromeCastLoad(0, true);
                            playbackState = PLAYING;
                            updateNotification();
                        } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
                            Log.e(TAG, "Play() called & failed. State: Stopped " + e.toString());
                            playbackState = STOPPED;
                            updateNotification();
                        }
                        break;
                    }
                    case PAUSED: {
                        try {
                            castManager.checkConnectivity();
                            castManager.play();
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

    private void updateMediaSession(final String what) {

        int playState = isSupposedToBePlaying
                ? PlaybackStateCompat.STATE_PLAYING
                : PlaybackStateCompat.STATE_PAUSED;

        long playbackActions = getMediaSessionActions();

        if (what.equals(InternalIntents.PLAY_STATE_CHANGED) || what.equals(InternalIntents.POSITION_CHANGED)) {
            //noinspection WrongConstant
            mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                    .setActions(playbackActions)
                    .setState(playState, getPosition(), 1.0f)
                    .build());
        } else if (what.equals(InternalIntents.META_CHANGED) || what.equals(InternalIntents.QUEUE_CHANGED)) {

            MediaMetadataCompat.Builder metaData = new MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, getArtistName())
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, getAlbumArtistName())
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, getAlbumName())
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, getSongName())
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, getDuration())
                    .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, (long) (getQueuePosition() + 1))
                    //Getting the genre is expensive.. let's not bother for now.
                    //.putString(MediaMetadataCompat.METADATA_KEY_GENRE, getGenreName())
                    .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, null)
                    .putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, (long) (getQueue().size()));

            if (SettingsManager.getInstance().showLockscreenArtwork()) {
                //Glide has to be called from the main thread.
                doOnMainThread(() -> Glide.with(MusicService.this)
                        .load(getAlbum())
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
                    notificationHelper.notify(this, queueManager.currentSong, isPlaying(), mediaSession);
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
            case LOCAL: {
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
            case REMOTE: {
                try {
                    if (player != null && player.isInitialized()) {
                        player.seekTo(castManager.getCurrentMediaPosition());
                        player.stop();
                    }
                    playbackState = STOPPED;
                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                }
                if (goToIdle) {
                    if (ShuttleUtils.isUpgraded() && castManager != null) {
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
                case LOCAL: {
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

                case REMOTE: {

                    try {
                        if (player != null) {
                            player.seekTo(castManager.getCurrentMediaPosition());
                        }
                        castManager.pause();
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
            case LOCAL: {
                return isSupposedToBePlaying;
            }
            case REMOTE: {
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
            if (isPodcast()) {
                long pos = getPosition();
                long bookmark = getBookmark();
                long duration = getDuration();
                if ((pos < bookmark && (pos + 10000) > bookmark)
                        || (pos > bookmark && (pos - 10000) < bookmark)) {
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
                Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, queueManager.currentSong.id);
                if (uri != null) {
                    getContentResolver().update(uri, values, null, null);
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
        if (queueManager.currentSong != null) {
            PlaylistUtils.toggleFavorite(queueManager.currentSong, isFavorite -> {
                if (isFavorite) {
                    Toast.makeText(MusicService.this, getString(R.string.song_to_favourites, queueManager.currentSong.name), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MusicService.this, getString(R.string.song_removed_from_favourites, queueManager.currentSong.name), Toast.LENGTH_SHORT).show();
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

    /**
     * Returns the path of the currently playing file, or null if no file is
     * currently playing.
     */
    public String getPath() {
        synchronized (this) {
            if (queueManager.currentSong != null) {
                return queueManager.currentSong.path;
            }
            return null;
        }
    }

    /**
     * Returns the id of the currently playing file, or -1 if no file is currently playing.
     */
    public long getSongId() {
        synchronized (this) {
            if (player == null || !player.isInitialized()) {
                return -1;
            }

            if (queueManager.currentSong != null) {
                return queueManager.currentSong.id;
            }
        }
        return -1;
    }

    public String getArtistName() {
        synchronized (this) {
            if (queueManager.currentSong == null) {
                return null;
            }
            return queueManager.currentSong.artistName;
        }
    }

    public String getAlbumArtistName() {
        synchronized (this) {
            if (queueManager.currentSong == null) {
                return null;
            }
            return queueManager.currentSong.albumArtistName;
        }
    }

    public long getDuration() {
        synchronized (this) {
            if (queueManager.currentSong == null) {
                return 0;
            }
            return queueManager.currentSong.duration;
        }
    }

    public String getAlbumName() {
        synchronized (this) {
            if (queueManager.currentSong == null) {
                return null;
            }
            return queueManager.currentSong.albumName;
        }
    }

    public String getSongName() {
        synchronized (this) {
            if (queueManager.currentSong == null) {
                return null;
            }
            return queueManager.currentSong.name;
        }
    }

    public Album getAlbum() {
        if (queueManager.currentSong != null) {
            return queueManager.currentSong.getAlbum();
        }
        return null;
    }

    @Nullable
    public Song getSong() {
        return queueManager.currentSong;
    }

    private boolean isPodcast() {
        synchronized (this) {
            return queueManager.currentSong != null && queueManager.currentSong.isPodcast;
        }
    }

    private long getBookmark() {
        synchronized (this) {
            if (queueManager.currentSong == null) {
                return 0;
            }
            if (queueManager.currentSong.isPodcast) {
                return queueManager.currentSong.bookMark;
            }
            return 0;
        }
    }

    /**
     * Returns the current playback position in milliseconds
     */
    public long getPosition() {
        synchronized (this) {
            switch (playbackLocation) {
                case LOCAL: {
                    if (player != null) {
                        return player.getPosition();
                    }
                    break;
                }
                case REMOTE: {
                    try {
                        return (int) castManager.getCurrentMediaPosition();
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

                if (playbackLocation == REMOTE) {
                    try {
                        castManager.seek((int) position);
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

    public Single<Boolean> isFavorite() {
        return PlaylistUtils.isFavorite(queueManager.currentSong).first(false);
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

    private void scrobbleBroadcast(int state, Song song) {

        if (song == null) {
            Log.e(TAG, "Failed to scrobble.. song null");
            return;
        }

        boolean scrobbleSimple = prefs.getBoolean("pref_simple_lastfm_scrobbler", false);

        //Check that state is a valid state
        if (state != Status.START
                && state != Status.RESUME
                && state != Status.PAUSE
                && state != Status.COMPLETE) {
            return;
        }

        if (scrobbleSimple) {
            Intent intent = new Intent(ExternalIntents.SCROBBLER);
            intent.putExtra("state", state);
            intent.putExtra("app-name", getString(R.string.app_name));
            intent.putExtra("app-package", getPackageName());
            intent.putExtra("artist", song.artistName);
            intent.putExtra("album", song.albumName);
            intent.putExtra("track", song.name);
            intent.putExtra("duration", song.duration / 1000);
            sendBroadcast(intent);
        }
    }

    public void registerA2dpServiceListener() {
        a2dpReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action != null && action.equals(ExternalIntents.PLAY_STATUS_REQUEST)) {
                    notifyChange(ExternalIntents.PLAY_STATUS_RESPONSE);
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ExternalIntents.PLAY_STATUS_REQUEST);
        registerReceiver(a2dpReceiver, intentFilter);
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
            notificationHelper.startForeground(this, queueManager.currentSong, isPlaying(), mediaSession);
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
                seekPos -> seekTo(seekPos < getDuration() ? seekPos : 0));
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

    class QueueManagerCallbacks implements QueueManager.Callbacks {
        @Override
        public void onQueueChanged() {
            if (isPlaying()) {
                setNextTrack();
            }
            notifyChange(InternalIntents.QUEUE_CHANGED);
        }

        @Override
        public void onShuffleChanged() {
            notifyChange(InternalIntents.SHUFFLE_CHANGED);
        }

        @Override
        public void onMetaChanged() {
            notifyChange(InternalIntents.META_CHANGED);
        }
    }
}