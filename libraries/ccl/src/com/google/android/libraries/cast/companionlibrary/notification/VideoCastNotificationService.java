/*
 * Copyright (C) 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.libraries.cast.companionlibrary.notification;

import static com.google.android.libraries.cast.companionlibrary.utils.LogUtils.LOGD;
import static com.google.android.libraries.cast.companionlibrary.utils.LogUtils.LOGE;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaQueueItem;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.libraries.cast.companionlibrary.R;
import com.google.android.libraries.cast.companionlibrary.cast.CastConfiguration;
import com.google.android.libraries.cast.companionlibrary.cast.MediaQueue;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.VideoCastConsumerImpl;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.CastException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.NoConnectionException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions
        .TransientNetworkDisconnectionException;
import com.google.android.libraries.cast.companionlibrary.remotecontrol.VideoIntentReceiver;
import com.google.android.libraries.cast.companionlibrary.utils.FetchBitmapTask;
import com.google.android.libraries.cast.companionlibrary.utils.LogUtils;
import com.google.android.libraries.cast.companionlibrary.utils.Utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.NotificationCompat;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A service to provide status bar Notifications when we are casting. For JB+ versions,
 * notification area supports a number of actions such as play/pause toggle or an "x" button to
 * disconnect but that for GB, these actions are not supported that due to the framework
 * limitations.
 */
public class VideoCastNotificationService extends Service {

    private static final String TAG = LogUtils.makeLogTag(VideoCastNotificationService.class);

    public static final String ACTION_FORWARD =
            "com.google.android.libraries.cast.companionlibrary.action.forward";
    public static final String ACTION_REWIND =
            "com.google.android.libraries.cast.companionlibrary.action.rewind";
    public static final String ACTION_TOGGLE_PLAYBACK =
            "com.google.android.libraries.cast.companionlibrary.action.toggleplayback";
    public static final String ACTION_PLAY_NEXT =
            "com.google.android.libraries.cast.companionlibrary.action.playnext";
    public static final String ACTION_PLAY_PREV =
            "com.google.android.libraries.cast.companionlibrary.action.playprev";
    public static final String ACTION_STOP =
            "com.google.android.libraries.cast.companionlibrary.action.stop";
    public static final String ACTION_VISIBILITY =
            "com.google.android.libraries.cast.companionlibrary.action.notificationvisibility";
    public static final String EXTRA_FORWARD_STEP_MS = "ccl_extra_forward_step_ms";
    protected static final int NOTIFICATION_ID = 1;
    public static final String NOTIFICATION_VISIBILITY = "visible";

    private static final long TEN_SECONDS_MILLIS = TimeUnit.SECONDS.toMillis(10);
    private static final long THIRTY_SECONDS_MILLIS = TimeUnit.SECONDS.toMillis(30);

    private Bitmap mVideoArtBitmap;
    private boolean mIsPlaying;
    private Class<?> mTargetActivity;
    private int mOldStatus = -1;
    protected Notification mNotification;
    private boolean mVisible;
    protected VideoCastManager mCastManager;
    private VideoCastConsumerImpl mConsumer;
    private FetchBitmapTask mBitmapDecoderTask;
    private int mDimensionInPixels;
    private boolean mHasNext;
    private boolean mHasPrev;
    private List<Integer> mNotificationActions;
    private int[] mNotificationCompactActionsArray;
    private long mForwardTimeInMillis;

    @Override
    public void onCreate() {
        super.onCreate();
        mDimensionInPixels = Utils.convertDpToPixel(VideoCastNotificationService.this,
                getResources().getDimension(R.dimen.ccl_notification_image_size));
        mCastManager = VideoCastManager.getInstance();
        readPersistedData();
        if (!mCastManager.isConnected() && !mCastManager.isConnecting()) {
            mCastManager.reconnectSessionIfPossible();
        }
        MediaQueue mediaQueue = mCastManager.getMediaQueue();
        if (mediaQueue != null) {
            int position = mediaQueue.getCurrentItemPosition();
            int size = mediaQueue.getCount();
            mHasNext = position < (size - 1);
            mHasPrev = position > 0;
        }
        mConsumer = new VideoCastConsumerImpl() {
            @Override
            public void onApplicationDisconnected(int errorCode) {
                LOGD(TAG, "onApplicationDisconnected() was reached, stopping the notification"
                        + " service");
                stopSelf();
            }

            @Override
            public void onDisconnected() {
                stopSelf();
            }

            @Override
            public void onRemoteMediaPlayerStatusUpdated() {
                int mediaStatus = mCastManager.getPlaybackStatus();
                VideoCastNotificationService.this.onRemoteMediaPlayerStatusUpdated(mediaStatus);
            }

            @Override
            public void onUiVisibilityChanged(boolean visible) {
                mVisible = !visible;

                if (mNotification == null) {
                    try {
                        setUpNotification(mCastManager.getRemoteMediaInformation());
                    } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
                        LOGE(TAG, "onStartCommand() failed to get media", e);
                    }
                }
                if (mVisible && (mNotification != null)) {
                    startForeground(NOTIFICATION_ID, mNotification);
                } else {
                    stopForeground(true);
                }
            }

            @Override
            public void onMediaQueueUpdated(List<MediaQueueItem> queueItems, MediaQueueItem item,
                    int repeatMode, boolean shuffle) {
                int size = 0;
                int position = 0;
                if (queueItems != null) {
                    size = queueItems.size();
                    position = queueItems.indexOf(item);
                }
                mHasNext = position < (size - 1);
                mHasPrev = position > 0;
            }
        };
        mCastManager.addVideoCastConsumer(mConsumer);
        mNotificationActions = mCastManager.getCastConfiguration().getNotificationActions();
        List<Integer> notificationCompactActions = mCastManager.getCastConfiguration()
                .getNotificationCompactActions();
        if (notificationCompactActions != null) {
            mNotificationCompactActionsArray = new int[notificationCompactActions.size()];
            for (int i = 0; i < notificationCompactActions.size(); i++) {
                mNotificationCompactActionsArray[i] = notificationCompactActions.get(i);
            }
        }
        mForwardTimeInMillis = TimeUnit.SECONDS
                .toMillis(mCastManager.getCastConfiguration().getForwardStep());
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LOGD(TAG, "onStartCommand");
        if (intent != null) {

            String action = intent.getAction();
            if (ACTION_VISIBILITY.equals(action)) {
                mVisible = intent.getBooleanExtra(NOTIFICATION_VISIBILITY, false);
                LOGD(TAG, "onStartCommand(): Action: ACTION_VISIBILITY " + mVisible);
                onRemoteMediaPlayerStatusUpdated(mCastManager.getPlaybackStatus());
                if (mNotification == null) {
                    try {
                        setUpNotification(mCastManager.getRemoteMediaInformation());
                    } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
                        LOGE(TAG, "onStartCommand() failed to get media", e);
                    }
                }
                if (mVisible && mNotification != null) {
                    startForeground(NOTIFICATION_ID, mNotification);
                } else {
                    stopForeground(true);
                }
            }
        }

        return Service.START_STICKY;
    }

    private void setUpNotification(final MediaInfo info)
            throws TransientNetworkDisconnectionException, NoConnectionException {
        if (info == null) {
            return;
        }
        if (mBitmapDecoderTask != null) {
            mBitmapDecoderTask.cancel(false);
        }
        Uri imgUri = null;
        try {
            if (!info.getMetadata().hasImages()) {
                build(info, null, mIsPlaying);
                return;
            } else {
                imgUri = info.getMetadata().getImages().get(0).getUrl();
            }
        } catch (CastException e) {
            LOGE(TAG, "Failed to build notification", e);
        }
        mBitmapDecoderTask = new FetchBitmapTask() {
            @Override
            protected void onPostExecute(Bitmap bitmap) {
                try {
                    mVideoArtBitmap = Utils.scaleAndCenterCropBitmap(bitmap, mDimensionInPixels,
                            mDimensionInPixels);
                    build(info, mVideoArtBitmap, mIsPlaying);
                } catch (CastException | NoConnectionException
                        | TransientNetworkDisconnectionException e) {
                    LOGE(TAG, "Failed to set notification for " + info.toString(), e);
                }
                if (mVisible && (mNotification != null)) {
                    startForeground(NOTIFICATION_ID, mNotification);
                }
                if (this == mBitmapDecoderTask) {
                    mBitmapDecoderTask = null;
                }
            }
        };
        mBitmapDecoderTask.execute(imgUri);
    }

    /**
     * Removes the existing notification.
     */
    private void removeNotification() {
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).
                cancel(NOTIFICATION_ID);
    }

    protected void onRemoteMediaPlayerStatusUpdated(int mediaStatus) {
        if (mOldStatus == mediaStatus) {
            // not need to make any updates here
            return;
        }
        mOldStatus = mediaStatus;
        LOGD(TAG, "onRemoteMediaPlayerStatusUpdated() reached with status: " + mediaStatus);
        try {
            switch (mediaStatus) {
                case MediaStatus.PLAYER_STATE_BUFFERING: // (== 4)
                    mIsPlaying = false;
                    setUpNotification(mCastManager.getRemoteMediaInformation());
                    break;
                case MediaStatus.PLAYER_STATE_PLAYING: // (== 2)
                    mIsPlaying = true;
                    setUpNotification(mCastManager.getRemoteMediaInformation());
                    break;
                case MediaStatus.PLAYER_STATE_PAUSED: // (== 3)
                    mIsPlaying = false;
                    setUpNotification(mCastManager.getRemoteMediaInformation());
                    break;
                case MediaStatus.PLAYER_STATE_IDLE: // (== 1)
                    mIsPlaying = false;
                    if (!mCastManager.shouldRemoteUiBeVisible(mediaStatus,
                            mCastManager.getIdleReason())) {
                        stopForeground(true);
                    } else {
                        setUpNotification(mCastManager.getRemoteMediaInformation());
                    }
                    break;
                case MediaStatus.PLAYER_STATE_UNKNOWN: // (== 0)
                    mIsPlaying = false;
                    stopForeground(true);
                    break;
                default:
                    break;
            }
        } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
            LOGE(TAG, "Failed to update the playback status due to network issues", e);
        }
    }

    /*
     * (non-Javadoc)
     * @see android.app.Service#onDestroy()
     */
    @Override
    public void onDestroy() {
        if (mBitmapDecoderTask != null) {
            mBitmapDecoderTask.cancel(false);
        }
        removeNotification();
        if (mCastManager != null && mConsumer != null) {
            mCastManager.removeVideoCastConsumer(mConsumer);
            mCastManager = null;
        }
    }

    /**
     * Build the MediaStyle notification. The action that are added to this notification are
     * selected by the client application from a pre-defined set of actions
     *
     * @see CastConfiguration.Builder#addNotificationAction(int, boolean)
     **/
    protected void build(MediaInfo info, Bitmap bitmap, boolean isPlaying)
            throws CastException, TransientNetworkDisconnectionException, NoConnectionException {

        // Media metadata
        MediaMetadata metadata = info.getMetadata();
        String castingTo = getResources().getString(R.string.ccl_casting_to_device,
                mCastManager.getDeviceName());

        NotificationCompat.Builder builder
                = (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_action_notification)
                .setContentTitle(metadata.getString(MediaMetadata.KEY_TITLE))
                .setContentText(castingTo)
                .setContentIntent(getContentIntent(info))
                .setLargeIcon(bitmap)
                .setStyle(new NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(mNotificationCompactActionsArray)
                        .setMediaSession(mCastManager.getMediaSessionCompatToken()))
                .setOngoing(true)
                .setShowWhen(false)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        for (Integer notificationType : mNotificationActions) {
            switch (notificationType) {
                case CastConfiguration.NOTIFICATION_ACTION_DISCONNECT:
                    builder.addAction(getDisconnectAction());
                    break;
                case CastConfiguration.NOTIFICATION_ACTION_PLAY_PAUSE:
                    builder.addAction(getPlayPauseAction(info, isPlaying));
                    break;
                case CastConfiguration.NOTIFICATION_ACTION_SKIP_NEXT:
                    builder.addAction(getSkipNextAction());
                    break;
                case CastConfiguration.NOTIFICATION_ACTION_SKIP_PREVIOUS:
                    builder.addAction(getSkipPreviousAction());
                    break;
                case CastConfiguration.NOTIFICATION_ACTION_FORWARD:
                    builder.addAction(getForwardAction(mForwardTimeInMillis));
                    break;
                case CastConfiguration.NOTIFICATION_ACTION_REWIND:
                    builder.addAction(getRewindAction(mForwardTimeInMillis));
                    break;
            }
        }

        mNotification = builder.build();

    }

    /**
     * Returns the {@link NotificationCompat.Action} for forwarding the current media by
     * {@code millis} milliseconds.
     */
    protected NotificationCompat.Action getForwardAction(long millis) {
        Intent intent = new Intent(this, VideoIntentReceiver.class);
        intent.setAction(ACTION_FORWARD);
        intent.setPackage(getPackageName());
        intent.putExtra(EXTRA_FORWARD_STEP_MS, (int) millis);
        PendingIntent pendingIntent = PendingIntent
                .getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        int iconResourceId = R.drawable.ic_notification_forward_48dp;
        if (millis == TEN_SECONDS_MILLIS) {
            iconResourceId = R.drawable.ic_notification_forward10_48dp;
        } else if (millis == THIRTY_SECONDS_MILLIS) {
            iconResourceId = R.drawable.ic_notification_forward30_48dp;
        }

        return new NotificationCompat.Action.Builder(iconResourceId,
                getString(R.string.ccl_forward), pendingIntent).build();
    }

    /**
     * Returns the {@link NotificationCompat.Action} for rewinding the current media by
     * {@code millis} milliseconds.
     */
    protected NotificationCompat.Action getRewindAction(long millis) {
        Intent intent = new Intent(this, VideoIntentReceiver.class);
        intent.setAction(ACTION_REWIND);
        intent.setPackage(getPackageName());
        intent.putExtra(EXTRA_FORWARD_STEP_MS, (int)-millis);
        PendingIntent pendingIntent = PendingIntent
                .getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        int iconResourceId = R.drawable.ic_notification_rewind_48dp;
        if (millis == TEN_SECONDS_MILLIS) {
            iconResourceId = R.drawable.ic_notification_rewind10_48dp;
        } else if (millis == THIRTY_SECONDS_MILLIS) {
            iconResourceId = R.drawable.ic_notification_rewind30_48dp;
        }
        return new NotificationCompat.Action.Builder(iconResourceId,
                getString(R.string.ccl_rewind), pendingIntent).build();
    }

    /**
     * Returns the {@link NotificationCompat.Action} for skipping to the next item in the queue. If
     * we are already at the end of the queue, we show a dimmed version of the icon for this action
     * and won't send any {@link PendingIntent}
     */
    protected NotificationCompat.Action getSkipNextAction() {
        PendingIntent pendingIntent = null;
        int iconResourceId = R.drawable.ic_notification_skip_next_semi_48dp;
        if (mHasNext) {
            Intent intent = new Intent(this, VideoIntentReceiver.class);
            intent.setAction(ACTION_PLAY_NEXT);
            intent.setPackage(getPackageName());
            pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
            iconResourceId = R.drawable.ic_notification_skip_next_48dp;
        }

        return new NotificationCompat.Action.Builder(iconResourceId,
                getString(R.string.ccl_skip_next), pendingIntent).build();
    }

    /**
     * Returns the {@link NotificationCompat.Action} for skipping to the previous item in the queue.
     * If we are already at the beginning of the queue, we show a dimmed version of the icon for
     * this action and won't send any {@link PendingIntent}
     */
    protected NotificationCompat.Action getSkipPreviousAction() {
        PendingIntent pendingIntent = null;
        int iconResourceId = R.drawable.ic_notification_skip_prev_semi_48dp;
        if (mHasPrev) {
            Intent intent = new Intent(this, VideoIntentReceiver.class);
            intent.setAction(ACTION_PLAY_PREV);
            intent.setPackage(getPackageName());
            pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
            iconResourceId = R.drawable.ic_notification_skip_prev_48dp;
        }

        return new NotificationCompat.Action.Builder(iconResourceId,
                getString(R.string.ccl_skip_previous), pendingIntent).build();
    }

    /**
     * Returns the {@link NotificationCompat.Action} for toggling play/pause/stop of the currently
     * playing item.
     */
    protected NotificationCompat.Action getPlayPauseAction(MediaInfo info, boolean isPlaying) {
        int pauseOrStopResourceId;
        if (info.getStreamType() == MediaInfo.STREAM_TYPE_LIVE) {
            pauseOrStopResourceId = R.drawable.ic_notification_stop_48dp;
        } else {
            pauseOrStopResourceId = R.drawable.ic_notification_pause_48dp;
        }
        int pauseOrPlayTextResourceId = isPlaying ? R.string.ccl_pause : R.string.ccl_play;
        int pauseOrPlayResourceId = isPlaying ? pauseOrStopResourceId
                : R.drawable.ic_notification_play_48dp;
        Intent intent = new Intent(this, VideoIntentReceiver.class);
        intent.setAction(ACTION_TOGGLE_PLAYBACK);
        intent.setPackage(getPackageName());
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        return new NotificationCompat.Action.Builder(pauseOrPlayResourceId,
                getString(pauseOrPlayTextResourceId), pendingIntent).build();
    }

    /**
     * Returns the {@link NotificationCompat.Action} for disconnecting this app from the cast
     * device.
     */
    protected NotificationCompat.Action getDisconnectAction() {
        Intent intent = new Intent(this, VideoIntentReceiver.class);
        intent.setAction(ACTION_STOP);
        intent.setPackage(getPackageName());
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        return new NotificationCompat.Action.Builder(R.drawable.ic_notification_disconnect_24dp,
                getString(R.string.ccl_disconnect), pendingIntent).build();
    }

    /**
     * Returns the {@link PendingIntent} for showing the full screen cast controller page. We also
     * build an appropriate "back stack" so that when user is sent to that full screen controller,
     * clicking on the Back button would allow navigation into the app.
     */
    protected PendingIntent getContentIntent(MediaInfo mediaInfo) {
        Bundle mediaWrapper = Utils.mediaInfoToBundle(mediaInfo);
        Intent contentIntent = new Intent(this, mTargetActivity);
        contentIntent.putExtra(VideoCastManager.EXTRA_MEDIA, mediaWrapper);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(mTargetActivity);
        stackBuilder.addNextIntent(contentIntent);
        if (stackBuilder.getIntentCount() > 1) {
            stackBuilder.editIntentAt(1).putExtra(VideoCastManager.EXTRA_MEDIA, mediaWrapper);
        }
        return stackBuilder.getPendingIntent(NOTIFICATION_ID, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /*
     * Reads application ID and target activity from preference storage.
     */
    private void readPersistedData() {
        mTargetActivity = mCastManager.getCastConfiguration().getTargetActivity();
        if (mTargetActivity == null) {
            mTargetActivity = VideoCastManager.DEFAULT_TARGET_ACTIVITY;
        }
    }
}
