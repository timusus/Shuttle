package com.simplecity.amp_library.playback;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
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
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.util.Log;
import android.widget.Toast;
import com.crashlytics.android.Crashlytics;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.androidauto.MediaIdHelper;
import com.simplecity.amp_library.androidauto.PackageValidator;
import com.simplecity.amp_library.cast.CastManager;
import com.simplecity.amp_library.data.Repository;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.notifications.MusicNotificationHelper;
import com.simplecity.amp_library.playback.constants.ExternalIntents;
import com.simplecity.amp_library.playback.constants.InternalIntents;
import com.simplecity.amp_library.playback.constants.MediaButtonCommand;
import com.simplecity.amp_library.playback.constants.ServiceCommand;
import com.simplecity.amp_library.playback.constants.ShortcutCommands;
import com.simplecity.amp_library.playback.constants.WidgetManager;
import com.simplecity.amp_library.services.Equalizer;
import com.simplecity.amp_library.ui.screens.queue.QueueItem;
import com.simplecity.amp_library.utils.AnalyticsManager;
import com.simplecity.amp_library.utils.LogUtils;
import com.simplecity.amp_library.utils.SettingsManager;
import com.simplecity.amp_library.utils.ShuttleUtils;
import com.simplecity.amp_library.utils.playlists.FavoritesPlaylistManager;
import dagger.android.AndroidInjection;
import io.reactivex.Completable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import javax.inject.Inject;
import kotlin.Unit;

@SuppressLint("InlinedApi")
public class MusicService extends MediaBrowserServiceCompat {

    @interface NotifyMode {
        int NONE = 0;
        int FOREGROUND = 1;
        int BACKGROUND = 2;
    }

    private static final String TAG = "MusicService";

    private MusicServiceCallbacks musicServiceCallbacks = new MusicServiceCallbacks();

    private QueueManager queueManager;

    @Nullable
    private CastManager castManager;

    private BluetoothManager bluetoothManager;

    private HeadsetManager headsetManager;

    @Inject
    WidgetManager widgetManager;

    private ScrobbleManager scrobbleManager;

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

    private PackageValidator mPackageValidator;

    private PlaybackManager playbackManager;

    private DummyNotificationHelper dummyNotificationHelper = new DummyNotificationHelper();

    @Inject
    Repository.SongsRepository songsRepository;

    @Inject
    Repository.AlbumsRepository albumsRepository;

    @Inject
    Repository.AlbumArtistsRepository albumArtistsRepository;

    @Inject
    Repository.PlaylistsRepository playlistsRepository;

    @Inject
    Repository.GenresRepository genresRepository;

    @Inject
    PlaybackSettingsManager playbackSettingsManager;

    @Inject
    SettingsManager settingsManager;

    @Inject
    AnalyticsManager analyticsManager;

    @Inject
    FavoritesPlaylistManager favoritesPlaylistManager;

    @SuppressLint("InlinedApi")
    @Override
    public void onCreate() {
        AndroidInjection.inject(this);
        super.onCreate();

        queueManager = new QueueManager(
                musicServiceCallbacks,
                songsRepository,
                playbackSettingsManager,
                settingsManager
        );

        playbackManager = new PlaybackManager(
                this,
                queueManager,
                playbackSettingsManager,
                songsRepository,
                albumsRepository,
                albumArtistsRepository,
                genresRepository,
                playlistsRepository,
                musicServiceCallbacks,
                settingsManager
        );

        scrobbleManager = new ScrobbleManager(playbackSettingsManager);

        mPackageValidator = new PackageValidator(this);

        setSessionToken(playbackManager.getMediaSessionToken());

        if (CastManager.isCastAvailable(this, settingsManager)) {
            castManager = new CastManager(this, playbackManager);
        }

        bluetoothManager = new BluetoothManager(playbackManager, analyticsManager, musicServiceCallbacks, settingsManager);

        headsetManager = new HeadsetManager(playbackManager, playbackSettingsManager);

        notificationHelper = new MusicNotificationHelper(this, analyticsManager);

        notificationStateHandler = new NotificationStateHandler(this);

        headsetManager.registerHeadsetPlugReceiver(this);
        bluetoothManager.registerBluetoothReceiver(this);
        bluetoothManager.registerA2dpServiceListener(this);

        registerExternalStorageListener();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ServiceCommand.COMMAND);
        intentFilter.addAction(ServiceCommand.TOGGLE_PLAYBACK);
        intentFilter.addAction(ServiceCommand.PAUSE);
        intentFilter.addAction(ServiceCommand.NEXT);
        intentFilter.addAction(ServiceCommand.PREV);
        intentFilter.addAction(ServiceCommand.STOP);
        intentFilter.addAction(ServiceCommand.SHUFFLE);
        intentFilter.addAction(ServiceCommand.REPEAT);
        intentFilter.addAction(ExternalIntents.PLAY_STATUS_REQUEST);
        registerReceiver(intentReceiver, intentFilter);

        // Initialize the delayed shutdown intent
        Intent shutdownIntent = new Intent(this, MusicService.class);
        shutdownIntent.setAction(ServiceCommand.SHUTDOWN);

        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        this.shutdownIntent = PendingIntent.getService(this, 0, shutdownIntent, 0);

        analyticsManager.dropBreadcrumb(TAG, "onCreate(), scheduling delayed shutdown");
        scheduleDelayedShutdown();

        playbackManager.reloadQueue();
    }

    @Override
    public IBinder onBind(final Intent intent) {

        analyticsManager.dropBreadcrumb(TAG, "onBind().. cancelShutdown()");
        cancelShutdown();
        serviceInUse = true;

        // For Android auto, need to call super, or onGetRoot won't be called.
        if (intent != null && "android.media.browse.MediaBrowserService".equals(intent.getAction())) {
            return super.onBind(intent);
        }

        return binder;
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        // To ensure you are not allowing any arbitrary app to browse your app's contents, you
        // need to check the origin:
        if (!mPackageValidator.isCallerAllowed(this, clientPackageName, clientUid)) {
            // If the request comes from an untrusted package, return an empty browser root.
            // If you return null, then the media browser will not be able to connect and
            // no further calls will be made to other media browsing methods.
            Log.i(TAG, String.format("OnGetRoot: Browsing NOT ALLOWED for unknown caller. Returning empty browser root so all apps can use MediaController.%s", clientPackageName));
            return new MediaBrowserServiceCompat.BrowserRoot("EMPTY_ROOT", null);
        }
        return new BrowserRoot("media:/root/", null);
    }

    @Override
    public void onLoadChildren(@NonNull String parentMediaId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        if ("EMPTY_ROOT".equals(parentMediaId)) {
            result.sendResult(new ArrayList<>());
        } else {
            result.detach();
            // if music library is ready, return immediately
            new MediaIdHelper(
                    (ShuttleApplication) getApplication(),
                    songsRepository,
                    albumsRepository,
                    albumArtistsRepository,
                    genresRepository,
                    playlistsRepository
            ).getChildren(parentMediaId, mediaItems -> {
                result.sendResult(mediaItems);
                return Unit.INSTANCE;
            });
        }
    }

    @Override
    public void onRebind(Intent intent) {
        analyticsManager.dropBreadcrumb(TAG, "onRebind().. cancelShutdown()");
        cancelShutdown();
        serviceInUse = true;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        analyticsManager.dropBreadcrumb(TAG, "onUnbind()");

        serviceInUse = false;
        saveState(true);

        if (playbackManager.isPlaying() || playbackManager.willResumePlayback()) {
            // Something is currently playing, or will be playing once an in-progress action requesting audio focus ends, so don't stop the service now.
            return true;

            // If there is a playlist but playback is paused, then wait a while before stopping the service, so that pause/resume isn't slow.
            // Also delay stopping the service if we're transitioning between tracks.
        } else if (!queueManager.getCurrentPlaylist().isEmpty()) {
            analyticsManager.dropBreadcrumb(TAG, "onUnbind() scheduling delayed shutdown.");
            scheduleDelayedShutdown();
            return true;
        }

        analyticsManager.dropBreadcrumb(TAG, "stopSelf() called");
        stopSelf(serviceStartId);

        return true;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        analyticsManager.dropBreadcrumb(TAG, "onTaskRemoved()");

        // Fixme:
        //  playbackManager.willResumePlayback() returns true even after we've manually paused.
        //  This means we don't call stopSelf(), which in turn causes the service to act as if it has crashed, and will recreate itself unnecessarily.

        if (!isPlaying() && !playbackManager.willResumePlayback()) {
            analyticsManager.dropBreadcrumb(TAG, "stopSelf() called");
            stopSelf();
        }

        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        analyticsManager.dropBreadcrumb(TAG, "onDestroy()");

        saveState(true);

        //Shutdown the EQ
        Intent shutdownEqualizer = new Intent(MusicService.this, Equalizer.class);
        stopService(shutdownEqualizer);

        alarmManager.cancel(shutdownIntent);

        // Remove any callbacks from the handlers
        notificationStateHandler.removeCallbacksAndMessages(null);

        if (castManager != null) {
            castManager.destroy();
        }

        headsetManager.unregisterHeadsetPlugReceiver(this);
        bluetoothManager.unregisterBluetoothReceiver(this);
        bluetoothManager.unregisterA2dpServiceListener(this);

        unregisterReceiver(intentReceiver);
        if (unmountReceiver != null) {
            unregisterReceiver(unmountReceiver);
            unmountReceiver = null;
        }

        playbackManager.destroy();

        dummyNotificationHelper.teardown(this);
        notificationHelper.tearDown();

        disposables.clear();

        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        serviceStartId = startId;

        if (intent != null) {
            String action = intent.getAction();
            String command = intent.getStringExtra(MediaButtonCommand.CMD_NAME);
            analyticsManager.dropBreadcrumb(TAG, String.format("onStartCommand() Action: %s, Command: %s", action, command));
            if (command != null) {
                action = commandToAction(command);
            }

            if (action != null) {
                switch (action) {
                    case ServiceCommand.NEXT:
                        dummyNotificationHelper.showDummyNotification(this);

                        // If the service is not already started:
                        // - With queue: Force to next song, start playback, update notification, no ANR
                        // - No queue: ANR

                        // Possible solution: (A) Show the Shuttle notification, despite the fact that music isn't playing. Need to customise notification to allow for an empty queue (no current song)
                        // We could try to generate a queue of random songs as well, but there's no guarantee the user has music on their device.

                        gotoNext(true);

                        break;
                    case ServiceCommand.PREV:
                        dummyNotificationHelper.showDummyNotification(this);

                        // If the service is not already started:
                        // - With queue: ANR
                        // - No queue: ANR

                        // Possible solution: (A) Show the Shuttle notification, despite the fact that music isn't playing. Need to customise notification to allow for an empty queue (no current song)

                        previous(false);

                        break;
                    case ServiceCommand.PAUSE:
                        dummyNotificationHelper.showDummyNotification(this);

                        // If the service is not already started:
                        // - With queue: ANR
                        // - No queue: ANR

                        // Possible solution: (A) Show the Shuttle notification, despite the fact that music isn't playing. Need to customise notification to allow for an empty queue (no current song)

                        pause(true);
                        break;
                    case ServiceCommand.PLAY:
                    case ShortcutCommands.PLAY:
                        dummyNotificationHelper.showDummyNotification(this);

                        // If the service is not already started:
                        // - No queue: ANR
                        // - With queue: No ANR

                        // Possible solution: (A) Show the Shuttle notification, despite the fact that music isn't playing. Need to customise notification to allow for an empty queue (no current song)

                        play();
                        break;
                    case ServiceCommand.TOGGLE_PLAYBACK:
                        dummyNotificationHelper.showDummyNotification(this);

                        if (isPlaying()) {

                            // It's not possible to be playing and the service not be started. No ANR

                            pause(intent.getBooleanExtra(MediaButtonCommand.FORCE_PREVIOUS, false));
                        } else {

                            // Same as ServiceCommand.PLAY

                            play();
                        }
                        break;
                    case ServiceCommand.STOP:
                        dummyNotificationHelper.showDummyNotification(this);

                        // If the service is already started & we're not playing: ANR
                        // If the service is not already started: ANR

                        pause(false);
                        releaseServiceUiAndStop();
                        notificationStateHandler.removeCallbacksAndMessages(null);
                        //For some reason, the notification will only go away if this call is delayed.
                        new Handler().postDelayed(() -> stopForegroundImpl(true, false), 150);
                        break;
                    case ServiceCommand.SHUFFLE:
                        dummyNotificationHelper.showDummyNotification(this);

                        // If the service is not already started: ANR

                        toggleShuffleMode();
                        break;
                    case ServiceCommand.REPEAT:
                        dummyNotificationHelper.showDummyNotification(this);

                        // If the service is not already started: ANR

                        toggleRepeat();
                        break;
                    case ServiceCommand.TOGGLE_FAVORITE:
                        dummyNotificationHelper.showDummyNotification(this);

                        // If the service is not already started: ANR

                        toggleFavorite();
                        break;
                    case ExternalIntents.PLAY_STATUS_REQUEST:
                        dummyNotificationHelper.showDummyNotification(this);

                        // If the service is not already started: ANR

                        notifyChange(ExternalIntents.PLAY_STATUS_RESPONSE);
                        break;
                    case ServiceCommand.SHUTDOWN:
                        dummyNotificationHelper.showDummyNotification(this);

                        // If the service is not already started: ANR

                        shutdownScheduled = false;
                        releaseServiceUiAndStop();
                        return START_NOT_STICKY;
                    case ShortcutCommands.SHUFFLE_ALL:
                        dummyNotificationHelper.showDummyNotification(this);

                        // If service is not already started: ANR

                        queueManager.makeShuffleList();
                        playAutoShuffleList();
                        break;
                }
            }
        }

        // Make sure the service will shut down on its own if it was just started but not bound to and nothing is playing
        analyticsManager.dropBreadcrumb(TAG, "onStartCommand() scheduling delayed shutdown");
        scheduleDelayedShutdown();

        return START_STICKY;
    }

    private final BroadcastReceiver intentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {

            String action = intent.getAction();

            String command = intent.getStringExtra(MediaButtonCommand.CMD_NAME);
            if (command != null) {
                action = commandToAction(command);
                widgetManager.processCommand(MusicService.this, intent, command);
            }

            if (action != null) {
                analyticsManager.dropBreadcrumb(TAG, String.format("onReceive() Action: %s, Command: %s", action, command));
                switch (action) {
                    case ServiceCommand.NEXT:
                        gotoNext(true);
                        break;
                    case ServiceCommand.PREV:
                        previous(false);
                        break;
                    case ServiceCommand.TOGGLE_PLAYBACK:
                        if (isPlaying()) {
                            pause(true);
                        } else {
                            play();
                        }
                        break;
                    case ServiceCommand.PAUSE:
                        pause(true);
                        break;
                    case ServiceCommand.PLAY:
                        play();
                        break;
                    case ServiceCommand.STOP:
                        pause(false);
                        releaseServiceUiAndStop();
                        break;
                    case ServiceCommand.TOGGLE_FAVORITE:
                        toggleFavorite();
                        break;
                }
            }
        }
    };

    @Nullable
    public String commandToAction(@NonNull String command) {
        switch (command) {
            case MediaButtonCommand.NEXT:
                return ServiceCommand.NEXT;
            case MediaButtonCommand.PREVIOUS:
                return ServiceCommand.PREV;
            case MediaButtonCommand.TOGGLE_PAUSE:
                return ServiceCommand.TOGGLE_PLAYBACK;
            case MediaButtonCommand.PAUSE:
                return ServiceCommand.PAUSE;
            case MediaButtonCommand.PLAY:
                return ServiceCommand.PLAY;
            case MediaButtonCommand.STOP:
                return ServiceCommand.STOP;
            case MediaButtonCommand.TOGGLE_FAVORITE:
                return ServiceCommand.TOGGLE_FAVORITE;
        }
        return null;
    }


    /**
     * Release resources and destroy the service.
     */
    void releaseServiceUiAndStop() {

        // If we're currently playing, or we're going to be playing in the near future, don't shutdown.
        if (isPlaying() || playbackManager.willResumePlayback()) {
            return;
        }

        analyticsManager.dropBreadcrumb(TAG, "releaseServiceUiAndStop()");

        playbackManager.release();

        cancelNotification();

        if (!serviceInUse) {
            saveState(true);

            //Shutdown the EQ
            Intent shutdownEqualizer = new Intent(MusicService.this, Equalizer.class);
            stopService(shutdownEqualizer);

            analyticsManager.dropBreadcrumb(TAG, "stopSelf() called");
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
     * media is going to be ejected, so scs can clean up any files they have open.
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
                        playbackManager.reloadQueue();
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
        playbackManager.enqueue(songs, action);
    }

    public void moveToNext(QueueItem queueItem) {
        List<QueueItem> playlist = queueManager.getCurrentPlaylist();
        int fromIndex = playlist.indexOf(queueItem);

        QueueItem currentQueueItem = queueManager.getCurrentQueueItem();
        int toIndex = playlist.indexOf(currentQueueItem) + 1;

        if (fromIndex != toIndex) {
            playbackManager.moveQueueItem(fromIndex, toIndex);
        }
    }

    /**
     * Sets the track to be played
     */
    protected void setNextTrack() {
        playbackManager.setNextTrack();
    }

    /**
     * Opens a list of songs for playback
     *
     * @param songs The list of songs to open
     * @param position The position to start playback at
     */
    public void open(@NonNull List<Song> songs, int position, Boolean playWhenReady) {
        playbackManager.load(songs, position, playWhenReady, 0);
    }

    /**
     * Opens a file and prepares it for playback
     *
     * @param path The path of the file to open
     */
    public void openFile(String path, Boolean playWhenReady) {
        playbackManager.loadFile(path, playWhenReady);
    }

    /**
     * Starts playback of a previously opened file
     */
    public void play() {
        playbackManager.play();
    }

    public void togglePlayback() {
        playbackManager.togglePlayback();
    }

    /**
     * Stops playback
     */
    public void stop() {
        analyticsManager.dropBreadcrumb(TAG, "stop()");
        playbackManager.stop(true);
    }

    /**
     * Pauses playback
     *
     * @param canFade whether we are allowed to fade out before pausing.
     */
    public void pause(boolean canFade) {
        analyticsManager.dropBreadcrumb(TAG, "pause()");
        playbackManager.pause(canFade);
    }

    /**
     * Returns whether something is currently playing
     *
     * @return true if something is playing (or will be playing shortly, in case we're currently transitioning between tracks), false if not
     */
    public boolean isPlaying() {
        return playbackManager.isPlaying();
    }

    /**
     * @return true if is playing or has played recently
     */
    private boolean recentlyPlayed() {
        return playbackManager.recentlyPlayed();
    }

    /**
     * Changes from the current track to the previous track
     */
    public void previous(boolean force) {
        playbackManager.previous(force);
    }

    /**
     * Changes from the current track to the next track
     *
     * @param force true to move to the next song regardless of repeat mode.
     */
    public void gotoNext(boolean force) {
        playbackManager.next(force);
    }

    /**
     * Returns the current playback position in milliseconds
     */
    public long getSeekPosition() {
        return playbackManager.getSeekPosition();
    }

    /**
     * Seeks to the position specified.
     *
     * @param position The position to seek to, in milliseconds
     */
    public void seekTo(long position) {
        playbackManager.seekTo(position);
    }

    /**
     * @return int the audio session ID.
     */
    public int getAudioSessionId() {
        return playbackManager.getAudioSessionId();
    }

    /**
     * Creates a shuffled list of all songs and begins playback
     */
    public void playAutoShuffleList() {
        playbackManager.playAutoShuffleList();
    }

    @QueueManager.ShuffleMode
    public int getShuffleMode() {
        return queueManager.shuffleMode;
    }

    public void setShuffleMode(@QueueManager.ShuffleMode int shufflemode) {
        queueManager.setShuffleMode(shufflemode);
    }

    @QueueManager.RepeatMode
    public int getRepeatMode() {
        return queueManager.repeatMode;
    }

    public void setRepeatMode(@QueueManager.RepeatMode int repeatMode) {
        queueManager.setRepeatMode(repeatMode);
        setNextTrack();
    }

    @Nullable
    public Song getSong() {
        return queueManager.getCurrentSong();
    }

    public void toggleFavorite() {
        Song song = queueManager.getCurrentSong();
        if (song != null) {
            favoritesPlaylistManager.toggleFavorite(song, isFavorite -> {
                if (isFavorite) {
                    Toast.makeText(MusicService.this, getString(R.string.song_to_favourites, song.name), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MusicService.this, getString(R.string.song_removed_from_favourites, song.name), Toast.LENGTH_SHORT).show();
                }
                notifyChange(InternalIntents.FAVORITE_CHANGED);
                return Unit.INSTANCE;
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
        playbackManager.clearQueue();
    }

    public List<QueueItem> getQueue() {
        return queueManager.getCurrentPlaylist();
    }

    /**
     * @return the position in the queue
     */
    public int getQueuePosition() {
        return queueManager.queuePosition;
    }

    public boolean getQueueReloading() {
        synchronized (this) {
            return queueManager.queueReloading;
        }
    }

    /**
     * Starts playing the track at the given position in the queue.
     *
     * @param position The position in the queue of the track that will be played.
     */
    public void setQueuePosition(int position) {
        playbackManager.setQueuePosition(position);
    }

    public void removeQueueItems(List<QueueItem> queueItems) {
        playbackManager.removeQueueItems(queueItems);
    }

    public void removeSongs(List<Song> songs) {
        playbackManager.removeSongs(songs);
    }

    public void removeQueueItem(QueueItem queueItem) {
        playbackManager.removeQueueItem(queueItem);
    }

    public void moveQueueItem(int from, int to) {
        playbackManager.moveQueueItem(from, to);
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
        if (isPlaying() || serviceInUse || playbackManager.willResumePlayback()) {
            analyticsManager.dropBreadcrumb(TAG,
                    String.format("scheduleDelayedShutdown called.. returning early. isPlaying: %s service in use: %s will resume playback: %s",
                            isPlaying(), serviceInUse, playbackManager.willResumePlayback()));
            return;
        }

        analyticsManager.dropBreadcrumb(TAG, "scheduleDelayedShutdown for 5 mins from now");
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 5 * 60 * 1000 /* 5 mins */, shutdownIntent);
        shutdownScheduled = true;
    }

    private void cancelShutdown() {
        analyticsManager.dropBreadcrumb(TAG, "cancelShutdown() called. Shutdown scheduled: " + shutdownScheduled);
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
                        notificationHelper.notify(this, playlistsRepository, songsRepository, queueManager.getCurrentSong(), isPlaying(), playbackManager.getMediaSessionToken(), settingsManager,
                                favoritesPlaylistManager);
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
                Log.i(TAG, "startForeground called");
                if (notificationHelper.startForeground(
                        this,
                        playlistsRepository,
                        songsRepository,
                        queueManager.getCurrentSong(),
                        isPlaying(),
                        playbackManager.getMediaSessionToken(),
                        settingsManager,
                        favoritesPlaylistManager
                )) {
                    dummyNotificationHelper.setForegroundedByApp(true);
                }
            } else {
                Log.e(TAG, "startForeground should have been called, but song is null");
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
            dummyNotificationHelper.setForegroundedByApp(false);
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
        public void stopForegroundImpl(boolean removeNotification, boolean withDelay) {
            MusicService.this.stopForegroundImpl(removeNotification, withDelay);
        }
    }
}