package com.simplecity.amp_library.playback;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothDevice;
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
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.RemoteControlClient;
import android.net.Uri;
import android.os.Build;
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
import android.widget.RemoteViews;
import android.widget.Toast;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.annimon.stream.function.Predicate;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
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
import com.simplecity.amp_library.BuildConfig;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.glide.utils.GlideUtils;
import com.simplecity.amp_library.http.HttpServer;
import com.simplecity.amp_library.model.Album;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.services.EqualizerService;
import com.simplecity.amp_library.ui.widgets.WidgetProviderExtraLarge;
import com.simplecity.amp_library.ui.widgets.WidgetProviderLarge;
import com.simplecity.amp_library.ui.widgets.WidgetProviderMedium;
import com.simplecity.amp_library.ui.widgets.WidgetProviderSmall;
import com.simplecity.amp_library.utils.DataManager;
import com.simplecity.amp_library.utils.DrawableUtils;
import com.simplecity.amp_library.utils.MediaButtonIntentReceiver;
import com.simplecity.amp_library.utils.PlaylistUtils;
import com.simplecity.amp_library.utils.SettingsManager;
import com.simplecity.amp_library.utils.ShuttleUtils;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

@SuppressLint("InlinedApi")
public class MusicService extends Service {

    private static final String TAG = "MusicService";

    private static final int NOTIFICATION_ID = 150;

    public interface ShuffleMode {
        int OFF = 0;
        int ON = 1;
    }

    public interface RepeatMode {
        int OFF = 0;
        int ONE = 1;
        int ALL = 2;
    }

    public interface EnqueueAction {
        int NOW = 1;
        int NEXT = 2;
        int LAST = 3;
    }

    public static final String INTERNAL_INTENT_PREFIX = "com.simplecity.shuttle";

    public interface InternalIntents {
        String PLAY_STATE_CHANGED = INTERNAL_INTENT_PREFIX + ".playstatechanged";
        String POSITION_CHANGED = INTERNAL_INTENT_PREFIX + ".positionchanged";
        String TRACK_ENDING = INTERNAL_INTENT_PREFIX + ".trackending";
        String META_CHANGED = INTERNAL_INTENT_PREFIX + ".metachanged";
        String QUEUE_CHANGED = INTERNAL_INTENT_PREFIX + ".queuechanged";
        String SHUFFLE_CHANGED = INTERNAL_INTENT_PREFIX + ".shufflechanged";
        String REPEAT_CHANGED = INTERNAL_INTENT_PREFIX + ".repeatchanged";
        String FAVORITE_CHANGED = INTERNAL_INTENT_PREFIX + ".favoritechanged";
    }

    public interface MediaButtonCommand {
        String CMD_NAME = "command";
        String TOGGLE_PAUSE = "togglepause";
        String STOP = "stop";
        String PAUSE = "pause";
        String PLAY = "play";
        String PREVIOUS = "previous";
        String NEXT = "next";
        String TOGGLE_FAVORITE = "togglefavorite";
        String FROM_MEDIA_BUTTON = "frommediabutton";
    }

    public static final String SERVICE_COMMAND_PREFIX = "com.simplecity.shuttle.music_service_command";

    public interface ServiceCommand {
        String SERVICE_COMMAND = SERVICE_COMMAND_PREFIX;
        String TOGGLE_PAUSE_ACTION = SERVICE_COMMAND_PREFIX + ".togglepause";
        String PAUSE_ACTION = SERVICE_COMMAND_PREFIX + ".pause";
        String PREV_ACTION = SERVICE_COMMAND_PREFIX + ".prev";
        String NEXT_ACTION = SERVICE_COMMAND_PREFIX + ".next";
        String STOP_ACTION = SERVICE_COMMAND_PREFIX + ".stop";
        String SHUFFLE_ACTION = SERVICE_COMMAND_PREFIX + ".shuffle";
        String REPEAT_ACTION = SERVICE_COMMAND_PREFIX + ".repeat";
        String SHUTDOWN = SERVICE_COMMAND_PREFIX + ".shutdown";
    }

    public interface ExternalIntents {
        String PLAY_STATUS_REQUEST = "com.android.music.playstatusrequest";
        String PLAY_STATUS_RESPONSE = "com.android.music.playstatusresponse";

        String AVRCP_PLAY_STATE_CHANGED = "com.android.music.playstatechanged";
        String AVRCP_META_CHANGED = "com.android.music.metachanged";

        String TASKER = "net.dinglisch.android.tasker.extras.VARIABLE_REPLACE_KEYS";

        String SCROBBLER = "com.adam.aslfms.notify.playstatechanged";

        String PEBBLE = "com.getpebble.action.NOW_PLAYING";
    }

    public interface ShortcutCommands {
        String PLAY = "com.simplecity.amp_library.shortcuts.PLAY";
        String SHUFFLE_ALL = "com.simplecity.amp_library.shortcuts.SHUFFLE";
        String FOLDERS = "com.simplecity.amp_library.shortcuts.FOLDERS";
        String PLAYLIST = "com.simplecity.amp_library.shortcuts.PLAYLIST";
    }

    public static final String FROM_USER = "from_user";

    interface Status {
        int START = 0;
        int RESUME = 1;
        int PAUSE = 2;
        int COMPLETE = 3;
    }

    interface PlayerHandler {
        int TRACK_ENDED = 1;
        int RELEASE_WAKELOCK = 2;
        int SERVER_DIED = 3;
        int FOCUS_CHANGE = 4;
        int FADE_DOWN = 5;
        int FADE_UP = 6;
        int TRACK_WENT_TO_NEXT = 7;
        int SLEEP = 8;
        int FADE_DOWN_STOP = 9;
        int GO_TO_NEXT = 10;
        int GO_TO_PREV = 11;
        int SHUFFLE_ALL = 12;
    }

    private static final Random mShuffler = new Random();

    /**
     * Idle time before stopping the foreground notification (5 minutes)
     */
    private static final int IDLE_DELAY = 5 * 60 * 1000;

    VideoCastManager mCastManager;

    private VideoCastConsumerImpl mCastConsumer;

    int mPlaybackLocation;

    int playbackState;

    public static final int REMOTE = 0;
    public static final int LOCAL = 1;

    public static final int PLAYING = 0;
    public static final int PAUSED = 1;
    public static final int STOPPED = 2;

    private final IBinder mBinder = new LocalBinder(this);

    MultiPlayer player;

    int shuffleMode = ShuffleMode.OFF;
    int repeatMode = RepeatMode.OFF;

    List<Song> playlist = new ArrayList<>();
    List<Song> shuffleList = new ArrayList<>();

    Song currentSong;

    int playPos = -1;
    int nextPlayPos = -1;

    private BroadcastReceiver mHeadsetReceiver;
    private BroadcastReceiver mBluetoothReceiver;
    private BroadcastReceiver mUnmountReceiver = null;
    private BroadcastReceiver mA2dpReceiver = null;

    private boolean mHeadsetReceiverIsRegistered;
    private boolean mBluetoothReceiverIsRegistered;

    MediaSessionCompat mSession;

    private ComponentName mMediaButtonReceiverComponent;

    private int castMediaStatus = -1;

    //Todo:
    // Don't make this public. The MultiPlayer uses it. Just attach a listener to the MultiPlayer
    //to listen for onCompletion, and acquire the wakelock there.
    public WakeLock mWakeLock;

    private int mServiceStartId = -1;

    private boolean mServiceInUse = false;

    int mOpenFailedCounter = 0;

    boolean isSupposedToBePlaying = false;

    /**
     * Gets the last played time to determine whether we still want notifications or not
     */
    private long mLastPlayedTime;

    NotificationManager mNotificationManager;
    Notification mNotification;

    private static final int NOTIFY_MODE_NONE = 0;
    private static final int NOTIFY_MODE_FOREGROUND = 1;
    private static final int NOTIFY_MODE_BACKGROUND = 2;

    private Toast mToast;

    boolean queueIsSaveable = true;

    SharedPreferences mPrefs;
    SharedPreferences servicePrefs;

    MediaPlayerHandler playerHandler;

    private HandlerThread mHandlerThread;

    private Handler mainHandler;

    private static NotificationStateHandler mNotificationStateHandler;

    SleepHandler sleepHandler;

    private long sleepTime;

    // Used to track what type of audio focus loss caused the playback to pause
    boolean pausedByTransientLossOfFocus = false;

    private AudioManager mAudioManager;

    private OnAudioFocusChangeListener mAudioFocusListener = new OnAudioFocusChangeListener() {
        public void onAudioFocusChange(final int focusChange) {
            playerHandler.obtainMessage(PlayerHandler.FOCUS_CHANGE, focusChange, 0).sendToTarget();
        }
    };

    private AlarmManager mAlarmManager;

    private PendingIntent mShutdownIntent;

    private boolean mShutdownScheduled;

    private boolean queueReloading;
    private boolean playOnQueueLoad;

    void updatePlaybackLocation(int location) {

        //If the location has changed and it's no longer ChromeCast
        if (location == LOCAL && location != mPlaybackLocation) {
            try {
                if (mCastManager != null && mCastManager.isConnected()) {
                    if (player != null && player.isInitialized()) {
                        player.seekTo(mCastManager.getCurrentMediaPosition());
                    }
                    mCastManager.stop();
                }
            } catch (CastException | NoConnectionException | TransientNetworkDisconnectionException | IllegalStateException e) {
                Log.e(TAG, "updatePlaybackLocation error: " + e);
            }
        }

        mPlaybackLocation = location;
    }

    void loadRemoteMedia(MediaInfo selectedMedia, int position, boolean autoPlay, final Bitmap bitmap, final Drawable errorDrawable) {

        Observable.fromCallable(() -> {

            HttpServer.getInstance().serveAudio(currentSong.path);

            ByteArrayOutputStream stream = new ByteArrayOutputStream();

            if (bitmap == null) {
                GlideUtils.drawableToBitmap(errorDrawable).compress(Bitmap.CompressFormat.JPEG, 80, stream);
            } else {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream);
            }

            HttpServer.getInstance().serveImage(stream.toByteArray());

            return null;
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(o -> {
                    try {
                        mCastManager.loadMedia(selectedMedia, autoPlay, position);
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to load media. " + e.toString());
                    }
                });
    }

    void prepareChromeCastLoad(int position, boolean autoPlay) {

        if (currentSong == null) {
            return;
        }

        if (TextUtils.isEmpty(currentSong.path)) {
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


        if (ShuttleUtils.isUpgraded() && mCastManager != null) {
            doOnMainThread(() -> {
                Glide.with(MusicService.this)
                        .load(getSong())
                        .asBitmap()
                        .override(1024, 1024)
                        .placeholder(GlideUtils.getLargePlaceHolderResId())
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
                        });
            });
        }
    }

    private void setupCastListener() {

        mCastConsumer = new VideoCastConsumerImpl() {

            @Override
            public void onApplicationConnected(ApplicationMetadata appMetadata, String sessionId, boolean wasLaunched) {

                Log.d(TAG, "onApplicationLaunched()");

                HttpServer.getInstance().start();

                boolean wasPlaying = isSupposedToBePlaying;

                //If music is playing on the phone, pause it
                if (mPlaybackLocation == LOCAL && isSupposedToBePlaying) {
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

                Log.i(TAG, "onRemoteMediaPlayerStatusUpdated.. Status: " + mCastManager.getPlaybackStatus());

                //Only send a track finished message if the state has changed..
                if (mCastManager.getPlaybackStatus() != castMediaStatus) {
                    if (mCastManager.getPlaybackStatus() == MediaStatus.PLAYER_STATE_IDLE
                            && mCastManager.getIdleReason() == MediaStatus.IDLE_REASON_FINISHED) {
                        playerHandler.sendEmptyMessage(PlayerHandler.TRACK_ENDED);
                    }
                }

                castMediaStatus = mCastManager.getPlaybackStatus();
            }
        };
    }

    final WidgetProviderMedium mWidgetProviderMedium = WidgetProviderMedium.getInstance();
    final WidgetProviderSmall mWidgetProviderSmall = WidgetProviderSmall.getInstance();
    final WidgetProviderLarge mWidgetProviderLarge = WidgetProviderLarge.getInstance();
    final WidgetProviderExtraLarge mWidgetProviderExtraLarge = WidgetProviderExtraLarge.getInstance();

    private final char hexDigits[] = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

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
                        PlaylistUtils.toggleFavorite(MusicService.this);
                        notifyChange(InternalIntents.FAVORITE_CHANGED);
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

        servicePrefs = getSharedPreferences("Service", 0);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Start up the thread running the service. Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block. We also make it
        // background priority so CPU-intensive work will not disrupt the UI.
        mHandlerThread = new HandlerThread("MusicPlayerHandler", android.os.Process.THREAD_PRIORITY_BACKGROUND);
        mHandlerThread.start();

        mainHandler = new Handler(Looper.getMainLooper());

        // Initialize the handlers
        playerHandler = new MediaPlayerHandler(this, mHandlerThread.getLooper());
        sleepHandler = new SleepHandler(this);
        mNotificationStateHandler = new NotificationStateHandler(this);

        registerHeadsetPlugReceiver();
        registerBluetoothReceiver();

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        mMediaButtonReceiverComponent = new ComponentName(getPackageName(),
                MediaButtonIntentReceiver.class.getName());

        setupMediaSession();

        mPlaybackLocation = LOCAL;

        if (ShuttleUtils.isUpgraded()) {
            mCastManager = VideoCastManager.getInstance();
            setupCastListener();
            mCastManager.addVideoCastConsumer(mCastConsumer);
        }

        if (mCastManager != null && mCastManager.isConnected()) {
            updatePlaybackLocation(REMOTE);
        } else {
            updatePlaybackLocation(LOCAL);
        }

        playbackState = STOPPED;

        registerExternalStorageListener();
        registerA2dpServiceListener();

        player = new MultiPlayer(this);
        player.setHandler(playerHandler);

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
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
        mWakeLock.setReferenceCounted(false);

        // Initialize the delayed shutdown intent
        final Intent shutdownIntent = new Intent(this, MusicService.class);
        shutdownIntent.setAction(ServiceCommand.SHUTDOWN);

        mAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        mShutdownIntent = PendingIntent.getService(this, 0, shutdownIntent, 0);

        // Listen for the idle state
        scheduleDelayedShutdown();

        reloadQueue();
    }

    List<Song> getCurrentPlaylist() {
        if (shuffleMode == ShuffleMode.OFF) {
            return playlist;
        } else {
            return shuffleList;
        }
    }

    private void setupMediaSession() {
        mSession = new MediaSessionCompat(this, "Shuttle", mMediaButtonReceiverComponent, null);
        mSession.setCallback(new MediaSessionCompat.Callback() {
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
        mSession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS | MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS);

        //For some reason, MediaSessionCompat doesn't seem to pass all of the available 'actions' on as
        //transport control flags for the RCC, so we do that manually
        RemoteControlClient remoteControlClient = (RemoteControlClient) mSession.getRemoteControlClient();
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

        mHeadsetReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {

                if (isInitialStickyBroadcast()) {
                    return;
                }

                if (intent.hasExtra("state")) {
                    if (intent.getIntExtra("state", 0) == 0) {
                        if (mPrefs.getBoolean("pref_headset_disconnect", true)) {
                            pause();
                        }
                    } else if (intent.getIntExtra("state", 0) == 1) {
                        if (mPrefs.getBoolean("pref_headset_connect", false)) {
                            play();
                        }
                    }
                }
            }
        };

        registerReceiver(mHeadsetReceiver, filter);
        mHeadsetReceiverIsRegistered = true;
    }

    private void registerBluetoothReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);

        mBluetoothReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                if (SettingsManager.getInstance().getBluetoothPauseDisconnect()) {
                    if (intent.getAction().equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
                        pause();
                    }
                    if (intent.getAction().equals(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)) {
                        int state = intent.getExtras().getInt(BluetoothA2dp.EXTRA_STATE);
                        if (state == BluetoothA2dp.STATE_DISCONNECTED) {
                            pause();
                        }
                    }
                    if (intent.getAction().equals(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)) {
                        int state = intent.getExtras().getInt(BluetoothHeadset.EXTRA_STATE);
                        if (state == BluetoothHeadset.STATE_DISCONNECTED || state == BluetoothHeadset.STATE_AUDIO_DISCONNECTED) {
                            pause();
                        }
                    }
                }

                if (SettingsManager.getInstance().getBluetoothResumeConnect()) {
                    if (intent.getAction().equals(BluetoothDevice.ACTION_ACL_CONNECTED)) {
                        play();
                    }
                    if (intent.getAction().equals(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)) {
                        int state = intent.getExtras().getInt(BluetoothA2dp.EXTRA_STATE);
                        if (state == BluetoothA2dp.STATE_CONNECTED) {
                            play();
                        }
                    }
                    if (intent.getAction().equals(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)) {
                        int state = intent.getExtras().getInt(BluetoothHeadset.EXTRA_STATE);
                        if (state == BluetoothHeadset.STATE_CONNECTED || state == BluetoothHeadset.STATE_AUDIO_CONNECTED) {
                            play();
                        }
                    }
                }
            }
        };
        registerReceiver(mBluetoothReceiver, filter);
        mBluetoothReceiverIsRegistered = true;
    }

    private void unregisterBluetoothReceiver() {
        if (mBluetoothReceiverIsRegistered) {
            unregisterReceiver(mBluetoothReceiver);
            mBluetoothReceiverIsRegistered = false;
        }
    }

    private void unregisterHeadsetPlugReceiver() {

        if (mHeadsetReceiverIsRegistered) {
            unregisterReceiver(mHeadsetReceiver);
            mHeadsetReceiverIsRegistered = false;
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
                mCastManager.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (mCastManager != null) {
            mCastManager.removeVideoCastConsumer(mCastConsumer);
        }

        EqualizerService.closeEqualizerSessions(this, true, getAudioSessionId());

        //Shutdown the EQ
        Intent shutdownEqualizer = new Intent(MusicService.this, EqualizerService.class);
        stopService(shutdownEqualizer);

        mAlarmManager.cancel(mShutdownIntent);

        // Remove any callbacks from the handlers
        playerHandler.removeCallbacksAndMessages(null);
        sleepHandler.removeCallbacksAndMessages(null);
        mNotificationStateHandler.removeCallbacksAndMessages(null);

        // quit the thread so that anything that gets posted won't run
        if (ShuttleUtils.hasJellyBeanMR2()) {
            mHandlerThread.quitSafely();
        }

        mainHandler.removeCallbacksAndMessages(null);

        // release all MediaPlayer resources, including the native player and
        // wakelocks
        player.release();
        player = null;

        // Remove the audio focus listener and lock screen controls
        mAudioManager.abandonAudioFocus(mAudioFocusListener);
        mSession.release();

        unregisterHeadsetPlugReceiver();
        unregisterBluetoothReceiver();

        unregisterReceiver(mIntentReceiver);
        unregisterReceiver(mA2dpReceiver);
        if (mUnmountReceiver != null) {
            unregisterReceiver(mUnmountReceiver);
            mUnmountReceiver = null;
        }

        mWakeLock.release();

        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mServiceStartId = startId;

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
                mNotificationStateHandler.removeCallbacksAndMessages(null);
                //For some reason, the notification will only fuck off if this call is delayed.
                new Handler().postDelayed(() -> stopForegroundImpl(true, false), 150);
            } else if (ServiceCommand.SHUFFLE_ACTION.equals(action)) {
                toggleShuffleMode();
            } else if (ServiceCommand.REPEAT_ACTION.equals(action)) {
                toggleRepeat();
            } else if (MediaButtonCommand.TOGGLE_FAVORITE.equals(action)) {
                PlaylistUtils.toggleFavorite(this);
                notifyChange(InternalIntents.FAVORITE_CHANGED);
            } else if (ExternalIntents.PLAY_STATUS_REQUEST.equals(action)) {
                notifyChange(ExternalIntents.PLAY_STATUS_RESPONSE);

            } else if (ServiceCommand.SHUTDOWN.equals(action)) {
                mShutdownScheduled = false;
                releaseServiceUiAndStop();
                return START_NOT_STICKY;
            }

            if (action != null) {
                switch (action) {
                    case ShortcutCommands.PLAY:
                        play();
                        break;
                    case ShortcutCommands.SHUFFLE_ALL:
                        makeShuffleList();
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
        mAudioManager.abandonAudioFocus(mAudioFocusListener);

        mSession.setActive(false);

        if (!mServiceInUse) {
            saveQueue(true);

            //Shutdown the EQ
            Intent shutdownEqualizer = new Intent(MusicService.this, EqualizerService.class);
            stopService(shutdownEqualizer);

            stopSelf(mServiceStartId);
        }
    }

    /**
     * Method saveQueue.
     *
     * @param full boolean
     */
    void saveQueue(final boolean full) {

        if (!queueIsSaveable) {
            return;
        }

        final SharedPreferences.Editor editor = servicePrefs.edit();

        if (full) {
            final StringBuilder q = new StringBuilder();

            // The current playlist is saved as a list of "reverse hexadecimal"
            // numbers, which we can generate faster than normal decimal or
            // hexadecimal numbers, which in turn allows us to save the playlist
            // more often without worrying too much about performance.
            int len = playlist.size();
            for (int i = 0; i < len; i++) {
                long n = playlist.get(i).id;
                if (n >= 0) {
                    if (n == 0) {
                        q.append("0;");
                    } else {
                        while (n != 0) {
                            final int digit = (int) (n & 0xf);
                            n >>>= 4;
                            q.append(hexDigits[digit]);
                        }
                        q.append(";");
                    }
                }
            }

            editor.putString("queue", q.toString());

            //Now save shuffle queue
            if (shuffleMode != ShuffleMode.OFF) {
                len = shuffleList.size();
                q.setLength(0);
                for (int i = 0; i < len; i++) {
                    long n = shuffleList.get(i).id;
                    if (n >= 0) {
                        if (n == 0) {
                            q.append("0;");
                        } else {
                            while (n != 0) {
                                final int digit = (int) (n & 0xf);
                                n >>>= 4;
                                q.append(hexDigits[digit]);
                            }
                            q.append(";");
                        }
                    }
                }
                editor.putString("shuffleList", q.toString());
            }
        }
        editor.putInt("curpos", playPos);
        if (player != null && player.isInitialized()) {
            editor.putLong("seekpos", player.getPosition());
        }
        editor.putInt("repeatmode", repeatMode);
        editor.putInt("shufflemode", shuffleMode);

        editor.apply();
    }

    synchronized void reloadQueue() {

        queueReloading = true;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        shuffleMode = servicePrefs.getInt("shufflemode", ShuffleMode.OFF);
        repeatMode = servicePrefs.getInt("repeatmode", RepeatMode.OFF);

        DataManager.getInstance().getSongsRelay()
                .first()
                .subscribe(new Action1<List<Song>>() {
                    @Override
                    public void call(List<Song> songs) {
                        String q = servicePrefs.getString("queue", "");

                        List<Long> ids = new ArrayList<>();

                        int len = q.length();
                        if (len > 1) {
                            int n = 0;
                            int shift = 0;
                            for (int i = 0; i < len; i++) {
                                char c = q.charAt(i);
                                if (c == ';') {
                                    ids.add((long) n);
                                    n = 0;
                                    shift = 0;
                                } else {
                                    if (c >= '0' && c <= '9') {
                                        n += ((c - '0') << shift);
                                    } else if (c >= 'a' && c <= 'f') {
                                        n += ((10 + c - 'a') << shift);
                                    } else {
                                        // bogus playlist data
                                        playlist.clear();
                                        break;
                                    }
                                    shift += 4;
                                }
                            }

                            Map<Integer, Song> map = new TreeMap<>();

                            for (Song song : songs) {
                                int index = ids.indexOf(song.id);
                                if (index != -1) {
                                    map.put(index, song);
                                }
                            }

                            playlist = new ArrayList<>(map.values());

                            final int pos = servicePrefs.getInt("curpos", 0);
                            if (pos < 0 || pos >= playlist.size()) {
                                // The saved playlist is bogus, discard it
                                playlist.clear();
                                queueReloadComplete();
                                return;
                            }

                            ids.clear();

                            playPos = pos;

                            if (repeatMode != RepeatMode.ALL && repeatMode != RepeatMode.ONE) {
                                repeatMode = RepeatMode.OFF;
                            }
                            if (shuffleMode != ShuffleMode.ON) {
                                shuffleMode = ShuffleMode.OFF;
                            }
                            if (shuffleMode == ShuffleMode.ON) {
                                q = servicePrefs.getString("shuffleList", "");
                                len = q.length();
                                if (len > 1) {
                                    n = 0;
                                    shift = 0;
                                    for (int i = 0; i < len; i++) {
                                        char c = q.charAt(i);
                                        if (c == ';') {
                                            ids.add((long) n);
                                            n = 0;
                                            shift = 0;
                                        } else {
                                            if (c >= '0' && c <= '9') {
                                                n += ((c - '0') << shift);
                                            } else if (c >= 'a' && c <= 'f') {
                                                n += ((10 + c - 'a') << shift);
                                            } else {
                                                // bogus playlist data
                                                break;
                                            }
                                            shift += 4;
                                        }
                                    }

                                    map.clear();

                                    for (Song song : songs) {
                                        int index = ids.indexOf(song.id);
                                        if (index != -1) {
                                            map.put(index, song);
                                        }
                                    }

                                    shuffleList = new ArrayList<>(map.values());

                                    if (pos < 0 || pos >= shuffleList.size()) {
                                        // The saved playlist is bogus, discard it
                                        shuffleList.clear();
                                        queueReloadComplete();
                                        return;
                                    }
                                }
                            }

                            if (playPos >= 0 && playPos < getCurrentPlaylist().size()) {
                                currentSong = getCurrentPlaylist().get(playPos);
                            } else {
                                playPos = 0;
                            }

                            synchronized (this) {
                                mOpenFailedCounter = 20;
                                openCurrentAndNext();
                            }

                            if (player == null || !player.isInitialized()) {
                                // couldn't restore the saved state
                                queueReloadComplete();
                                return;
                            }

                            final long seekPos = servicePrefs.getLong("seekpos", 0);
                            seekTo(seekPos >= 0 && seekPos < getDuration() ? seekPos : 0);
                        }

                        queueReloadComplete();
                    }
                });
    }

    void queueReloadComplete() {

        notifyChange(InternalIntents.QUEUE_CHANGED);
        notifyChange(InternalIntents.META_CHANGED);

        queueReloading = false;
        if (playOnQueueLoad) {
            play();
            playOnQueueLoad = false;
        }
    }

    @Override
    public IBinder onBind(final Intent intent) {
        cancelShutdown();
        mServiceInUse = true;
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        cancelShutdown();
        mServiceInUse = true;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mServiceInUse = false;
        saveQueue(true);

        if (isSupposedToBePlaying || pausedByTransientLossOfFocus) {
            // Something is currently playing, or will be playing once
            // an in-progress action requesting audio focus ends, so don't stop
            // the service now.
            return true;

            // If there is a playlist but playback is paused, then wait a while
            // before stopping the service, so that pause/resume isn't slow.
            // Also delay stopping the service if we're transitioning between
            // tracks.
        } else if (playlist.size() > 0 || shuffleList.size() > 0 || playerHandler.hasMessages(PlayerHandler.TRACK_ENDED)) {
            scheduleDelayedShutdown();
            return true;
        }
        stopSelf(mServiceStartId);
        //Shutdown the EQ
        Intent shutdownEqualizer = new Intent(MusicService.this, EqualizerService.class);
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
        if (mUnmountReceiver == null) {
            mUnmountReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    final String action = intent.getAction();
                    if (action.equals(Intent.ACTION_MEDIA_EJECT)) {
                        saveQueue(true);
                        queueIsSaveable = false;
                        closeExternalStorageFiles();
                    } else if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
                        queueIsSaveable = true;
                        reloadQueue();
                    }
                }
            };
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Intent.ACTION_MEDIA_EJECT);
            intentFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
            intentFilter.addDataScheme("file");
            registerReceiver(mUnmountReceiver, intentFilter);
        }
    }

    public void notifyChange(final String what) {
        notifyChange(what, false);
    }

    private Observable<Bundle> getExtras(boolean fromUser) {
        return isFavorite().flatMap(isFavorite -> {
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
            extras.putLong("ListSize", getCurrentPlaylist().size());
            extras.putBoolean(FROM_USER, fromUser);
            return Observable.just(extras);
        });
    }

    private void notifyChange(String what, boolean fromUser) {
        if (what.equals(InternalIntents.TRACK_ENDING)) {
            //We're just about to change tracks, so 'current song' is the song that just finished
            Song finishedSong = currentSong;
            if (finishedSong != null) {
                if (finishedSong.hasPlayed()) {
                    Observable.fromCallable(() -> {
                        ShuttleUtils.incrementPlayCount(this, finishedSong);
                        return null;
                    }).subscribeOn(Schedulers.io())
                            .subscribe();
                }
                scrobbleBroadcast(Status.COMPLETE, finishedSong);
            }
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
                });

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
                    });

            if (isPlaying()) {
                if (currentSong != null) {
                    currentSong.setResumed();
                }
                //Last.fm scrobbler intent
                scrobbleBroadcast(Status.RESUME, currentSong);
                //Tasker intent
                taskerIntent.putExtra("%MTRACK", getSongName());
                //Pebble intent
                sendBroadcast(pebbleIntent);

            } else {
                if (currentSong != null) {
                    currentSong.setPaused();
                }
                //Last.fm scrobbler intent
                scrobbleBroadcast(Status.PAUSE, currentSong);
                //Tasker intent
                taskerIntent.putExtra("%MTRACK", "");
            }

            sendBroadcast(taskerIntent);

        } else if (what.equals(InternalIntents.META_CHANGED)) {

            if (currentSong != null) {
                currentSong.setStartTime();
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
                    });

            //Pebble intent
            sendBroadcast(pebbleIntent);

            //Last.fm scrobbler intent
            scrobbleBroadcast(Status.START, currentSong);
        }

        if (what.equals(InternalIntents.QUEUE_CHANGED)) {
            saveQueue(true);
            if (isPlaying()) {
                setNextTrack();
            }

        } else {
            saveQueue(false);
        }

        mWidgetProviderLarge.notifyChange(MusicService.this, what);
        mWidgetProviderMedium.notifyChange(MusicService.this, what);
        mWidgetProviderSmall.notifyChange(MusicService.this, what);
        mWidgetProviderExtraLarge.notifyChange(MusicService.this, what);
    }

    /**
     * Queues a new list for playback
     *
     * @param songs  The list to queue
     * @param action The action to take
     */
    public void enqueue(List<Song> songs, final int action) {
        synchronized (this) {
            if (action == EnqueueAction.NEXT && playPos + 1 < getCurrentPlaylist().size()) {
                getCurrentPlaylist().addAll(playPos + 1, songs);
                setNextTrack();
                notifyChange(InternalIntents.QUEUE_CHANGED);
            } else {
                getCurrentPlaylist().addAll(songs);
                notifyChange(InternalIntents.QUEUE_CHANGED);
                if (action == EnqueueAction.NOW) {
                    playPos = getCurrentPlaylist().size() - songs.size();
                    openCurrentAndNext();
                    play();
                    notifyChange(InternalIntents.META_CHANGED);
                    return;
                }
            }
            if (playPos < 0) {
                playPos = 0;
                openCurrentAndNext();
                play();
                notifyChange(InternalIntents.META_CHANGED);
            }
        }
    }


    /**
     * Opens a list for playback
     *
     * @param songs    The list of tracks to open
     * @param position The position to start playback at
     */
    public void open(List<Song> songs, final int position) {
        synchronized (this) {

            final long oldId = getSongId();
            boolean newList = false;

            if (!playlist.equals(songs)) {
                newList = true;
            }

            if (newList) {
                playlist.clear();
                shuffleList.clear();
                playlist.addAll(songs);
                notifyChange(InternalIntents.QUEUE_CHANGED);
            }

            if (position >= 0) {
                playPos = position;
            } else {
                playPos = mShuffler.nextInt(playlist.size());
            }

            if (shuffleMode == ShuffleMode.ON) {
                makeShuffleList();
                notifyChange(InternalIntents.QUEUE_CHANGED);
                notifyChange(InternalIntents.META_CHANGED);
            }

            openCurrentAndNext();
            if (oldId != getSongId()) {
                notifyChange(InternalIntents.META_CHANGED);
            }
        }
    }

    /**
     * Moves the item at index1 to index2.
     *
     * @param from
     * @param to
     */
    public void moveQueueItem(int from, int to) {
        synchronized (this) {

            if (from >= getCurrentPlaylist().size()) {
                from = getCurrentPlaylist().size() - 1;
            }
            if (to >= getCurrentPlaylist().size()) {
                to = getCurrentPlaylist().size() - 1;
            }

            getCurrentPlaylist().add(to, getCurrentPlaylist().remove(from));

            if (from < to) {
                if (playPos == from) {
                    playPos = to;
                } else if (playPos >= from && playPos <= to) {
                    playPos--;
                }
            } else if (to < from) {
                if (playPos == from) {
                    playPos = to;
                } else if (playPos >= to && playPos <= from) {
                    playPos++;
                }
            }
            notifyChange(InternalIntents.QUEUE_CHANGED, true);
        }
    }

    public List<Song> getQueue() {
        synchronized (this) {
            return getCurrentPlaylist();
        }
    }

    public void makeShuffleList() {
        synchronized (this) {

            if (playlist == null || playlist.isEmpty()) {
                return;
            }

            shuffleList = new ArrayList<>(playlist);
            Song currentSong = null;
            if (playPos >= 0 && playPos < shuffleList.size()) {
                currentSong = shuffleList.remove(playPos);
            }

            Collections.shuffle(shuffleList);

            if (currentSong != null) {
                shuffleList.add(0, currentSong);
            }
            playPos = 0;
        }
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
            if (getCurrentPlaylist() == null || getCurrentPlaylist().isEmpty() || playPos < 0 || playPos >= getCurrentPlaylist().size()) {
                return;
            }
            stop(false);

            boolean shutdown = false;

            currentSong = getCurrentPlaylist().get(playPos);

            while (true) {
                if (open(currentSong)) {
                    break;
                }
                // If we get here then opening the file failed.
                if (mOpenFailedCounter++ < 10 && getCurrentPlaylist().size() > 1) {
                    final int pos = getNextPosition(false);
                    if (pos < 0) {
                        scheduleDelayedShutdown();
                        if (isSupposedToBePlaying) {
                            isSupposedToBePlaying = false;
                            notifyChange(InternalIntents.PLAY_STATE_CHANGED);
                        }
                        return;
                    }
                    playPos = pos;
                    stop(false);
                    playPos = pos;

                    currentSong = getCurrentPlaylist().get(playPos);
                } else {
                    mOpenFailedCounter = 0;
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
        nextPlayPos = getNextPosition(false);
        if (nextPlayPos >= 0
                && getCurrentPlaylist() != null
                && !getCurrentPlaylist().isEmpty()
                && nextPlayPos < getCurrentPlaylist().size()) {
            final Song nextSong = getCurrentPlaylist().get(nextPlayPos);
            try {
                player.setNextDataSource(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI + "/" + nextSong.id);
            } catch (Exception e) {
                Log.e(TAG, "Error: " + e.getMessage());
                CrashlyticsCore.getInstance().log("setNextTrack() with id failed. error: " + e.getLocalizedMessage());
            }
        } else {
            try {
                player.setNextDataSource(null);
            } catch (Exception e) {
                Log.e(TAG, "Error: " + e.getMessage());
                CrashlyticsCore.getInstance().log("setNextTrack() failed with null id. error: " + e.getLocalizedMessage());
            }
        }
    }

    public boolean open(Song song) {
        synchronized (this) {

            currentSong = song;

            if (player != null) {
                player.setDataSource(song.path);
                if (player != null && player.isInitialized()) {
                    mOpenFailedCounter = 0;
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
    public void openFile(String path, @Nullable Action0 completion) {
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

            DataManager.getInstance().getSongsRelay()
                    .first()
                    .map(songs -> Stream.of(songs)
                            .filter(predicate)
                            .collect(Collectors.toList()))
                    .subscribe(songs -> {
                        if (!songs.isEmpty()) {
                            currentSong = songs.get(0);
                            open(currentSong);
                            if (completion != null) {
                                completion.call();
                            }
                        }
                    });
        }
    }

    /**
     * Starts playback of a previously opened file.
     */
    public void play() {

        int status = mAudioManager.requestAudioFocus(mAudioFocusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        if (status != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            return;
        }

        if (mPlaybackLocation == LOCAL) {
            if (SettingsManager.getInstance().getEqualizerEnabled()) {
                //Shutdown any existing external audio sessions
                EqualizerService.closeEqualizerSessions(this, false, getAudioSessionId());

                //Start internal equalizer session (will only turn on if enabled)
                EqualizerService.openEqualizerSession(this, true, getAudioSessionId());
            } else {
                EqualizerService.openEqualizerSession(this, false, getAudioSessionId());
            }
        }

        if (mSession != null && !mSession.isActive()) {
            try {
                mSession.setActive(true);
            } catch (Exception e) {
                Log.e(TAG, "mSession.setActive() failed");
            }
        }

        switch (mPlaybackLocation) {
            case LOCAL: {
                if (player != null && player.isInitialized()) {
                    // if we are at the end of the song, go to the next song first
                    final long duration = player.getDuration();
                    if (repeatMode != RepeatMode.ONE && duration > 2000
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

                } else if (getCurrentPlaylist().size() == 0) {
                    // This is mostly so that if you press 'play' on a bluetooth headset
                    // without ever having played anything before, it will still play
                    // something.

                    if (queueReloading) {
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
                if (repeatMode != RepeatMode.ONE && duration > 2000
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
                            mCastManager.checkConnectivity();
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
                            mCastManager.checkConnectivity();
                            mCastManager.play();
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

                if (getCurrentPlaylist().size() == 0) {
                    // This is mostly so that if you press 'play' on a bluetooth headset
                    // without every having played anything before, it will still play
                    // something.

                    if (queueReloading) {
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

        if (what.equals(InternalIntents.PLAY_STATE_CHANGED) || what.equals(InternalIntents.POSITION_CHANGED)) {
            //noinspection WrongConstant
            mSession.setPlaybackState(new PlaybackStateCompat.Builder()
                    .setActions(getMediaSessionActions())
                    .setState(playState, getPosition(), 1.0f).build());
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
                    .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, null);

            // MetadataEditor does not support NUM_TRACKS at all, so we don't attempt to set it on <API21 devices
            if(ShuttleUtils.hasLollipop()) {
                metaData.putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, (long) (getQueue().size()));
            }


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
                                    mSession.setMetadata(metaData.build());
                                } catch (NullPointerException e) {
                                    metaData.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, null);
                                    mSession.setMetadata(metaData.build());
                                }
                            }

                            @Override
                            public void onLoadFailed(Exception e, Drawable errorDrawable) {
                                super.onLoadFailed(e, errorDrawable);
                                mSession.setMetadata(metaData.build());
                            }
                        }));
            } else {
                mSession.setMetadata(metaData.build());
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
                startForegroundImpl(NOTIFICATION_ID, buildNotification());
                break;
            case NOTIFY_MODE_BACKGROUND:
                mNotificationManager.notify(NOTIFICATION_ID, buildNotification());
                stopForegroundImpl(false, false);
                break;
            case NOTIFY_MODE_NONE:
                stopForegroundImpl(false, false);
                mNotificationManager.cancel(NOTIFICATION_ID);
                break;
        }
    }

    private void cancelNotification() {
        stopForegroundImpl(true, true);
        mNotificationManager.cancel(NOTIFICATION_ID);
    }

    private Notification buildNotification() {

        final boolean isPlaying = isPlaying();

        if (mNotification == null) {

            Intent intent = new Intent(BuildConfig.APPLICATION_ID + (ShuttleUtils.isTablet() ? ".TABLET_PLAYBACK_VIEWER" : ".PLAYBACK_VIEWER"));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent contentIntent = PendingIntent.getActivity(MusicService.this, 0, intent, 0);

            Notification.Builder builder = new Notification.Builder(this);

            builder.setSmallIcon(R.drawable.ic_stat_notification)
                    .setContentIntent(contentIntent)
                    .setPriority(Notification.PRIORITY_MAX);

            if (ShuttleUtils.hasJellyBeanMR1()) {
                builder.setShowWhen(false);
            }

            if (ShuttleUtils.hasLollipop()) {
                builder.setVisibility(Notification.VISIBILITY_PUBLIC);
            }

            if (ShuttleUtils.hasNougat()) {
                builder.setStyle(new Notification.DecoratedCustomViewStyle());
            }

            mNotification = builder.build();
        }

        boolean invertIconsAndText = SettingsManager.getInstance().invertNotificationIcons();

        int baseLayoutResId = invertIconsAndText ? R.layout.notification_template_base_inverse : R.layout.notification_template_base;
        int baseBigLayoutResId = invertIconsAndText ? R.layout.notification_template_big_base_inverse : R.layout.notification_template_big_base;

        if (ShuttleUtils.hasNougat()) {
            baseLayoutResId = invertIconsAndText ? R.layout.notification_template_base_v24_inverse : R.layout.notification_template_base_v24;
            baseBigLayoutResId = invertIconsAndText ? R.layout.notification_template_big_base_v24_inverse : R.layout.notification_template_big_base_v24;
        }

        RemoteViews contentView = new RemoteViews(getPackageName(), baseLayoutResId);
        RemoteViews bigContentView = new RemoteViews(getPackageName(), baseBigLayoutResId);

        String artistName = getArtistName();
        String albumName = getAlbumName();
        String trackName = getSongName();
        if (artistName != null && albumName != null) {
            contentView.setTextViewText(R.id.text, String.format("%s | %s", artistName, albumName));
            bigContentView.setTextViewText(R.id.text, String.format("%s | %s", artistName, albumName));
        }
        if (trackName != null) {
            contentView.setTextViewText(R.id.title, trackName);
            bigContentView.setTextViewText(R.id.title, trackName);
        }

        contentView.setImageViewBitmap(R.id.pause, DrawableUtils.getTintedNotificationDrawable(this, R.drawable.ic_pause_white));
        contentView.setImageViewBitmap(R.id.next, DrawableUtils.getTintedNotificationDrawable(this, R.drawable.ic_skip_white));
        contentView.setImageViewBitmap(R.id.prev, DrawableUtils.getTintedNotificationDrawable(this, R.drawable.ic_prev_white));

        bigContentView.setImageViewBitmap(R.id.pause, DrawableUtils.getTintedNotificationDrawable(this, R.drawable.ic_pause_white));
        bigContentView.setImageViewBitmap(R.id.next, DrawableUtils.getTintedNotificationDrawable(this, R.drawable.ic_skip_white));
        bigContentView.setImageViewBitmap(R.id.prev, DrawableUtils.getTintedNotificationDrawable(this, R.drawable.ic_prev_white));

        PendingIntent pendingIntent = retrievePlaybackAction(ServiceCommand.PREV_ACTION);
        bigContentView.setOnClickPendingIntent(R.id.prev, pendingIntent);

        pendingIntent = retrievePlaybackAction(ServiceCommand.TOGGLE_PAUSE_ACTION);
        contentView.setOnClickPendingIntent(R.id.pause, pendingIntent);
        bigContentView.setOnClickPendingIntent(R.id.pause, pendingIntent);

        pendingIntent = retrievePlaybackAction(ServiceCommand.NEXT_ACTION);
        contentView.setOnClickPendingIntent(R.id.next, pendingIntent);
        bigContentView.setOnClickPendingIntent(R.id.next, pendingIntent);

        pendingIntent = retrievePlaybackAction(ServiceCommand.PREV_ACTION);
        contentView.setOnClickPendingIntent(R.id.prev, pendingIntent);
        bigContentView.setOnClickPendingIntent(R.id.prev, pendingIntent);

        pendingIntent = retrievePlaybackAction(ServiceCommand.STOP_ACTION);
        contentView.setOnClickPendingIntent(R.id.close, pendingIntent);
        bigContentView.setOnClickPendingIntent(R.id.close, pendingIntent);

        doOnMainThread(() -> Glide.with(MusicService.this)
                .load(getAlbum())
                .asBitmap()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .override(600, 600)
                .placeholder(GlideUtils.getPlaceHolderDrawable(albumName, false))
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                        if (resource != null) {
                            contentView.setImageViewBitmap(R.id.icon, resource);
                            bigContentView.setImageViewBitmap(R.id.icon, resource);
                        }
                        mNotificationManager.notify(NOTIFICATION_ID, mNotification);
                    }

                    @Override
                    public void onLoadFailed(Exception e, Drawable errorDrawable) {
                        super.onLoadFailed(e, errorDrawable);
                        contentView.setImageViewBitmap(R.id.icon, GlideUtils.drawableToBitmap(errorDrawable));
                        bigContentView.setImageViewBitmap(R.id.icon, GlideUtils.drawableToBitmap(errorDrawable));
                        mNotificationManager.notify(NOTIFICATION_ID, mNotification);
                    }
                }));

        mNotification.contentView = contentView;

        try {
            mNotification.bigContentView = bigContentView;
        } catch (NoSuchFieldError ignored) {

        }

        if (ShuttleUtils.hasAndroidLPreview()) {
            mNotification.contentView.setImageViewBitmap(R.id.pause, !isPlaying ? DrawableUtils.getTintedNotificationDrawable(this, R.drawable.ic_play_white) : DrawableUtils.getTintedNotificationDrawable(this, R.drawable.ic_pause_white));
        } else {
            mNotification.contentView.setImageViewResource(R.id.pause, !isPlaying ? R.drawable.ic_play_white : R.drawable.ic_pause_white);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            try {
                if (mNotification.bigContentView != null) {
                    if (ShuttleUtils.hasAndroidLPreview()) {
                        mNotification.bigContentView.setImageViewBitmap(R.id.pause, !isPlaying ? DrawableUtils.getTintedNotificationDrawable(this, R.drawable.ic_play_white) : DrawableUtils.getTintedNotificationDrawable(this, R.drawable.ic_pause_white));
                    } else {
                        mNotification.bigContentView.setImageViewResource(R.id.pause, !isPlaying ? R.drawable.ic_play_white : R.drawable.ic_pause_white
                        );
                    }
                }
            } catch (NoSuchFieldError ignored) {
            }
        }

        return mNotification;
    }

    private void doOnMainThread(Action0 action) {
        mainHandler.post(action::call);
    }

    private PendingIntent retrievePlaybackAction(final String action) {
        final ComponentName serviceName = new ComponentName(this, MusicService.class);
        Intent intent = new Intent(action);
        intent.setComponent(serviceName);

        return PendingIntent.getService(this, 0, intent, 0);
    }

    private void stop(final boolean goToIdle) {

        switch (mPlaybackLocation) {
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
                        player.seekTo(mCastManager.getCurrentMediaPosition());
                        player.stop();
                    }
                    playbackState = STOPPED;
                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                }
                if (goToIdle) {
                    if (ShuttleUtils.isUpgraded() && mCastManager != null) {
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

            switch (mPlaybackLocation) {
                case LOCAL: {
                    playerHandler.removeMessages(PlayerHandler.FADE_UP);
                    if (isSupposedToBePlaying) {
                        EqualizerService.closeEqualizerSessions(this, false, getAudioSessionId());
                        player.pause();
                        setIsSupposedToBePlaying(false, true);
                        notifyChange(InternalIntents.PLAY_STATE_CHANGED);
                        saveBookmarkIfNeeded();
                    }
                    break;
                }

                case REMOTE: {

                    try {
                        player.seekTo(mCastManager.getCurrentMediaPosition());
                        mCastManager.pause();
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

        switch (mPlaybackLocation) {
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
                mLastPlayedTime = System.currentTimeMillis();
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
        return isPlaying() || System.currentTimeMillis() - mLastPlayedTime < IDLE_DELAY;
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
            if (playPos > 0) {
                playPos--;
            } else {
                playPos = getCurrentPlaylist().size() - 1;
            }
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
        if (!force && repeatMode == RepeatMode.ONE) {
            if (playPos < 0) {
                return 0;
            }
            return playPos;
        } else if (playPos >= getCurrentPlaylist().size() - 1) {
            if (repeatMode == RepeatMode.OFF && !force) {
                return -1;
            } else if (repeatMode == RepeatMode.ALL || force) {
                return 0;
            }
            return -1;
        } else {
            return playPos + 1;
        }
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

            if (getCurrentPlaylist().size() == 0) {
                scheduleDelayedShutdown();
                return;
            }

            final int pos = getNextPosition(force);
            if (pos < 0) {
                setIsSupposedToBePlaying(false, true);
                return;
            }

            playPos = pos;
            saveBookmarkIfNeeded();
            stop(false);
            playPos = pos;
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
                Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, currentSong.id);
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
                    .first()
                    .subscribeOn(Schedulers.io())
                    .subscribe(songs -> {
                        playlist = songs;
                        playPos = -1;
                        makeShuffleList();
                        setShuffleMode(ShuffleMode.ON);
                        notifyChange(InternalIntents.QUEUE_CHANGED);
                        playPos = 0;
                        openCurrentAndNext();
                        play();
                        notifyChange(InternalIntents.META_CHANGED);
                        saveQueue(false);
                    });

        } else {
            shuffleMode = ShuffleMode.OFF;
            saveQueue(false);
        }
    }

    public void clearQueue() {
        playlist.clear();
        shuffleList.clear();
        setShuffleMode(ShuffleMode.OFF);
        stop(true);
        playPos = -1;
    }

    /**
     * Removes all instances of the track with the given id from the playlist.
     *
     * @param song   The Song to be removed
     * @param notify true to broadcast a QUEUE_CHANGE event
     */
    public void removeTrack(final Song song, boolean notify) {
        synchronized (this) {
            List<Song> songs = new ArrayList<>();
            songs.add(song);
            removeTracks(songs, notify);
        }
    }

    /**
     * Removes the range of tracks specified from the play list. If a file
     * within the range is the file currently being played, playback will move
     * to the next file after the range.
     *
     * @param songs  the Songs to remove
     * @param notify true to broadcast a QUEUE_CHANGE event
     */
    public void removeTracks(List<Song> songs, boolean notify) {
        synchronized (this) {

            if (songs.isEmpty()) {
                return;
            }

            int first = Collections.indexOfSubList(getCurrentPlaylist(), songs);

            playlist.removeAll(songs);
            shuffleList.removeAll(songs);

            boolean gotoNext = false;

            //If we're removing the current song from the queue, we need to adjust the playback position
            //accordingly.
            if (songs.contains(currentSong)) {
                playPos = first;
                gotoNext = true;
            } else {
                playPos = getCurrentPlaylist().indexOf(currentSong);
            }

            if (gotoNext) {
                if (getCurrentPlaylist().isEmpty()) {
                    stop(true);
                    playPos = -1;
                } else {
                    if (playPos >= getCurrentPlaylist().size()) {
                        playPos = 0;
                    }
                    final boolean wasPlaying = isPlaying();
                    stop(false);
                    openCurrentAndNext();
                    if (wasPlaying) {
                        play();
                    }
                }
                notifyChange(InternalIntents.META_CHANGED);
            }

            if (notify) {
                notifyChange(InternalIntents.QUEUE_CHANGED);
            }
        }
    }

    public int getShuffleMode() {
        return shuffleMode;
    }

    public void setShuffleMode(int shufflemode) {
        synchronized (this) {
            if (shuffleMode == shufflemode && !getCurrentPlaylist().isEmpty()) {
                return;
            }
            shuffleMode = shufflemode;
            notifyChange(InternalIntents.SHUFFLE_CHANGED);
            saveQueue(false);
        }
    }


    public int getRepeatMode() {
        return repeatMode;
    }

    public void setRepeatMode(int repeatMode) {
        synchronized (this) {
            this.repeatMode = repeatMode;
            setNextTrack();
            saveQueue(false);
        }
    }

    /**
     * Returns the path of the currently playing file, or null if no file is
     * currently playing.
     */
    public String getPath() {
        synchronized (this) {
            if (currentSong != null) {
                return currentSong.path;
            }
            return null;
        }
    }

    /**
     * Returns the rowid of the currently playing file, or -1 if no file is
     * currently playing.
     */
    public long getSongId() {
        synchronized (this) {
            if (player == null) {
                return -1;
            }
            if (getCurrentPlaylist() != null
                    && !getCurrentPlaylist().isEmpty()
                    && playPos >= 0
                    && player.isInitialized()
                    && playPos < getCurrentPlaylist().size()) {
                return getCurrentPlaylist().get(playPos).id;
            }
        }
        return -1;
    }

    /**
     * Returns the position in the queue
     *
     * @return the position in the queue
     */
    public int getQueuePosition() {
        synchronized (this) {
            return playPos;
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
            playPos = pos;
            openCurrentAndNext();
            play();
            notifyChange(InternalIntents.META_CHANGED);
        }
    }

    public String getArtistName() {
        synchronized (this) {
            if (currentSong == null) {
                return null;
            }
            return currentSong.artistName;
        }
    }

    public String getAlbumArtistName() {
        synchronized (this) {
            if (currentSong == null) {
                return null;
            }
            return currentSong.albumArtistName;
        }
    }

    public long getDuration() {
        synchronized (this) {
            if (currentSong == null) {
                return 0;
            }
            return currentSong.duration;
        }
    }

    public long getArtistId() {
        synchronized (this) {
            if (currentSong == null) {
                return -1;
            }
            return currentSong.artistId;
        }
    }

    public String getAlbumName() {
        synchronized (this) {
            if (currentSong == null) {
                return null;
            }
            return currentSong.albumName;
        }
    }

    public int getPlaybackLocation() {
        synchronized (this) {
            return mPlaybackLocation;
        }
    }

    public long getAlbumId() {
        synchronized (this) {
            if (currentSong == null) {
                return -1;
            }
            return currentSong.albumId;
        }
    }

    public String getSongName() {
        synchronized (this) {
            if (currentSong == null) {
                return null;
            }
            return currentSong.name;
        }
    }

    public Album getAlbum() {
        if (currentSong != null) {
            return currentSong.getAlbum();
        }
        return null;
    }

    public Song getSong() {
        return currentSong;
    }

    private boolean isPodcast() {
        synchronized (this) {
            return currentSong != null && currentSong.isPodcast;
        }
    }

    private long getBookmark() {
        synchronized (this) {
            if (currentSong == null) {
                return 0;
            }
            if (currentSong.isPodcast) {
                return currentSong.bookMark;
            }
            return 0;
        }
    }

    /**
     * Returns the current playback position in milliseconds
     */
    public long getPosition() {
        synchronized (this) {
            switch (mPlaybackLocation) {
                case LOCAL: {
                    if (player != null) {
                        return player.getPosition();
                    }
                    break;
                }
                case REMOTE: {
                    try {
                        return (int) mCastManager.getCurrentMediaPosition();
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
            if (player.isInitialized()) {
                if (position < 0) {
                    position = 0;
                } else if (position > player.getDuration()) {
                    position = player.getDuration();
                }

                player.seekTo(position);

                if (mPlaybackLocation == REMOTE) {
                    try {
                        mCastManager.seek((int) position);
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

    public void sleep(int sleepNumber) {
        sleepHandler.removeCallbacksAndMessages(null);
        sleepTime = 0;
        sleepHandler.sendMessageDelayed(sleepHandler.obtainMessage(PlayerHandler.SLEEP), sleepNumber);
        sleepTime = System.currentTimeMillis() + sleepNumber;

        final int minutes = (sleepNumber / (1000 * 60)) % 60;
        final int hours = (sleepNumber / (1000 * 60 * 60)) % 24;

        final String hourSeq = (hours == 0) ? "" : (hours == 1) ? hours + getString(R.string.hour) : hours + getString(R.string.hours);

        final String minSeq = (minutes == 0) ? "" : (minutes == 1) ? "" : minutes + getString(R.string.minutes);

        doOnMainThread(() -> {
            String toastText = getString(R.string.sleep_message) + hourSeq;
            if (hours != 0 && minutes != 0) {
                toastText = toastText + getString(R.string.and) + minSeq;
            } else if (hours == 0 && minutes != 0) {
                toastText = toastText + minSeq;
            }
            Toast.makeText(MusicService.this, toastText, Toast.LENGTH_LONG).show();
        });

    }

    public Observable<Boolean> isFavorite() {
        return PlaylistUtils.isFavorite(this, getSong());
    }

    public boolean isTimerActive() {
        return sleepHandler.hasMessages(PlayerHandler.SLEEP);
    }

    public void stopTimer() {
        sleepHandler.removeCallbacksAndMessages(null);
        sleepTime = 0;
    }

    public long timeRemaining() {
        return sleepTime;
    }

    public void toggleShuffleMode() {
        int shuffle = getShuffleMode();
        if (shuffle == ShuffleMode.OFF) {
            setShuffleMode(ShuffleMode.ON);
            notifyChange(InternalIntents.SHUFFLE_CHANGED);
            makeShuffleList();
            notifyChange(InternalIntents.QUEUE_CHANGED);
            if (getRepeatMode() == RepeatMode.ONE) {
                setRepeatMode(RepeatMode.ALL);
            }
            showToast(R.string.shuffle_on_notif);
        } else if (shuffle == ShuffleMode.ON) {
            setShuffleMode(ShuffleMode.OFF);
            notifyChange(InternalIntents.SHUFFLE_CHANGED);
            if (this.playPos >= 0 && this.playPos < shuffleList.size()) {
                int playPos = playlist.indexOf(shuffleList.get(this.playPos));
                if (playPos != -1) {
                    this.playPos = playPos;
                }
            }
            notifyChange(InternalIntents.QUEUE_CHANGED);
            showToast(R.string.shuffle_off_notif);
        }
    }

    public void toggleRepeat() {
        int mode = getRepeatMode();
        if (mode == RepeatMode.OFF) {
            setRepeatMode(RepeatMode.ALL);
            showToast(R.string.repeat_all_notif);
        } else if (mode == RepeatMode.ALL) {
            setRepeatMode(RepeatMode.ONE);
            showToast(R.string.repeat_current_notif);
        } else {
            setRepeatMode(RepeatMode.OFF);
            showToast(R.string.repeat_off_notif);
        }
        notifyChange(InternalIntents.REPEAT_CHANGED);
    }

    private void showToast(int resid) {
        if (mToast == null) {
            mToast = Toast.makeText(getBaseContext(), "", Toast.LENGTH_SHORT);
        }
        mToast.setText(resid);
        mToast.show();
    }

    private void scrobbleBroadcast(int state, Song song) {

        if (song == null) {
            Log.e(TAG, "Failed to scrobble.. song null");
            return;
        }

        boolean scrobbleSimple = mPrefs.getBoolean("pref_simple_lastfm_scrobbler", false);

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
        mA2dpReceiver = new BroadcastReceiver() {
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
        registerReceiver(mA2dpReceiver, intentFilter);
    }

    private void scheduleDelayedShutdown() {
        mAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + IDLE_DELAY, mShutdownIntent);
        mShutdownScheduled = true;
    }

    private void cancelShutdown() {
        if (mShutdownScheduled) {
            mAlarmManager.cancel(mShutdownIntent);
            mShutdownScheduled = false;
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
     * Starts the foreground notification, and cancels any stop messaged
     *
     * @param id           hte notification id
     * @param notification the notification to start
     */
    private void startForegroundImpl(int id, Notification notification) {
        try {
            mNotificationStateHandler.sendEmptyMessage(NotificationStateHandler.START_FOREGROUND);
            startForeground(id, notification);
        } catch (NullPointerException e) {
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
            mNotificationStateHandler.sendEmptyMessageDelayed(NotificationStateHandler.STOP_FOREGROUND, 1500);
        } else {
            stopForeground(removeNotification);
        }
    }
}
