/*
 * Copyright (C) 2014 Google Inc. All Rights Reserved.
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

package com.google.android.libraries.cast.companionlibrary.cast.player;

import static com.google.android.libraries.cast.companionlibrary.utils.LogUtils.LOGD;
import static com.google.android.libraries.cast.companionlibrary.utils.LogUtils.LOGE;

import com.google.android.gms.cast.CastStatusCodes;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaQueueItem;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.MediaTrack;
import com.google.android.gms.cast.RemoteMediaPlayer;
import com.google.android.libraries.cast.companionlibrary.R;
import com.google.android.libraries.cast.companionlibrary.cast.CastConfiguration;
import com.google.android.libraries.cast.companionlibrary.cast.MediaQueue;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.VideoCastConsumerImpl;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.CastException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.NoConnectionException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.TransientNetworkDisconnectionException;
import com.google.android.libraries.cast.companionlibrary.utils.FetchBitmapTask;
import com.google.android.libraries.cast.companionlibrary.utils.LogUtils;
import com.google.android.libraries.cast.companionlibrary.utils.Utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.View;
import android.widget.SeekBar;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A fragment that provides a mechanism to retain the state and other needed objects for
 * {@link VideoCastControllerActivity} (or more generally, for any class implementing
 * {@link VideoCastController} interface). This can come very handy when set up of that activity
 * allows for a configuration changes. Most of the logic required for
 * {@link VideoCastControllerActivity} is maintained in this fragment to enable application
 * developers provide a different implementation, if desired.
 * <p/>
 * This fragment also provides an implementation of {@link MediaAuthListener} which can be useful
 * if a pre-authorization is required for playback of a media.
 */
public class VideoCastControllerFragment extends Fragment implements
        OnVideoCastControllerListener, MediaAuthListener {

    private static final String EXTRAS = "extras";
    private static final String TAG = LogUtils.makeLogTag(VideoCastControllerFragment.class);
    private MediaInfo mSelectedMedia;
    private VideoCastManager mCastManager;
    private MediaAuthService mMediaAuthService;
    private Thread mAuthThread;
    private Timer mMediaAuthTimer;
    private Handler mHandler;
    protected boolean mAuthSuccess = true;
    private VideoCastController mCastController;
    private FetchBitmapTask mImageAsyncTask;
    private Timer mSeekbarTimer;
    private int mPlaybackState;
    private MyCastConsumer mCastConsumer;
    private OverallState mOverallState = OverallState.UNKNOWN;
    private UrlAndBitmap mUrlAndBitmap;
    private static boolean sDialogCanceled = false;
    private boolean mIsFresh = true;
    private MediaStatus mMediaStatus;

    private enum OverallState {
        AUTHORIZING, PLAYBACK, UNKNOWN
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        sDialogCanceled = false;
        mCastController = (VideoCastController) activity;
        mHandler = new Handler();
        mCastManager = VideoCastManager.getInstance();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mCastConsumer = new MyCastConsumer();
        Bundle bundle = getArguments();
        if (bundle == null) {
            return;
        }
        Bundle extras = bundle.getBundle(EXTRAS);
        Bundle mediaWrapper = extras.getBundle(VideoCastManager.EXTRA_MEDIA);

        // Retain this fragment across configuration changes.
        setRetainInstance(true);
        mCastManager.addTracksSelectedListener(this);
        boolean explicitStartActivity = mCastManager.getPreferenceAccessor()
                .getBooleanFromPreference(VideoCastManager.PREFS_KEY_START_ACTIVITY, false);
        if (explicitStartActivity) {
            mIsFresh = true;
        }
        mCastManager.getPreferenceAccessor().saveBooleanToPreference(
                VideoCastManager.PREFS_KEY_START_ACTIVITY, false);
        mCastController.setNextPreviousVisibilityPolicy(
                mCastManager.getCastConfiguration().getNextPrevVisibilityPolicy());
        if (extras.getBoolean(VideoCastManager.EXTRA_HAS_AUTH)) {
            if (mIsFresh) {
                mOverallState = OverallState.AUTHORIZING;
                mMediaAuthService = mCastManager.getMediaAuthService();
                handleMediaAuthTask(mMediaAuthService);
                showImage(Utils.getImageUri(mMediaAuthService.getMediaInfo(), 1));
            }
        } else if (mediaWrapper != null) {
            mOverallState = OverallState.PLAYBACK;
            boolean shouldStartPlayback = extras.getBoolean(VideoCastManager.EXTRA_SHOULD_START);
            String customDataStr = extras.getString(VideoCastManager.EXTRA_CUSTOM_DATA);
            JSONObject customData = null;
            if (!TextUtils.isEmpty(customDataStr)) {
                try {
                    customData = new JSONObject(customDataStr);
                } catch (JSONException e) {
                    LOGE(TAG, "Failed to unmarshalize custom data string: customData="
                            + customDataStr, e);
                }
            }
            MediaInfo info = Utils.bundleToMediaInfo(mediaWrapper);
            int startPoint = extras.getInt(VideoCastManager.EXTRA_START_POINT, 0);
            onReady(info, shouldStartPlayback && explicitStartActivity, startPoint, customData);
        }
    }

    /*
     * Starts a background thread for starting the Auth Service
     */
    private void handleMediaAuthTask(final MediaAuthService authService) {
        mCastController.showLoading(true);
        if (authService == null) {
            return;
        }
        mCastController.setSubTitle(authService.getPendingMessage() != null
                ? authService.getPendingMessage() : "");
        mAuthThread = new Thread(new Runnable() {

            @Override
            public void run() {
                authService.setMediaAuthListener(VideoCastControllerFragment.this);
                authService.startAuthorization();
            }
        });
        mAuthThread.start();

        // start a timeout timer; we don't want authorization process to take too long
        mMediaAuthTimer = new Timer();
        mMediaAuthTimer.schedule(new MediaAuthServiceTimerTask(mAuthThread),
                authService.getTimeout());
    }

    /*
     * A TimerTask that will be called when the auth timer expires
     */
    class MediaAuthServiceTimerTask extends TimerTask {

        private final Thread mThread;

        public MediaAuthServiceTimerTask(Thread thread) {
            this.mThread = thread;
        }

        @Override
        public void run() {
            if (mThread != null) {
                LOGD(TAG, "Timer is expired, going to interrupt the thread");
                mThread.interrupt();
                mHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        mCastController.showLoading(false);
                        showErrorDialog(getString(R.string.ccl_failed_authorization_timeout));
                        mAuthSuccess = false;
                        if ((mMediaAuthService != null)
                                && (mMediaAuthService.getStatus() == MediaAuthStatus.PENDING)) {
                            mMediaAuthService.abortAuthorization(MediaAuthStatus.TIMED_OUT);
                        }
                    }
                });

            }
        }

    }

    private class MyCastConsumer extends VideoCastConsumerImpl {

        @Override
        public void onDisconnected() {
            mCastController.closeActivity();
        }

        @Override
        public void onApplicationDisconnected(int errorCode) {
            mCastController.closeActivity();
        }

        @Override
        public void onRemoteMediaPlayerMetadataUpdated() {
            try {
                mSelectedMedia = mCastManager.getRemoteMediaInformation();
                updateClosedCaptionState();
                updateMetadata();
            } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
                LOGE(TAG, "Failed to update the metadata due to network issues", e);
            }
        }

        @Override
        public void onMediaLoadResult(int statusCode) {
            if (CastStatusCodes.SUCCESS != statusCode) {
                LOGD(TAG, "onMediaLoadResult(): Failed to load media with status code: "
                        + statusCode);
                Utils.showToast(getActivity(), R.string.ccl_failed_to_load_media);
                mCastController.closeActivity();
            }
        }

        @Override
        public void onFailed(int resourceId, int statusCode) {
            LOGD(TAG, "onFailed(): " + getString(resourceId) + ", status code: " + statusCode);
            if (statusCode == RemoteMediaPlayer.STATUS_FAILED
                    || statusCode == RemoteMediaPlayer.STATUS_TIMED_OUT) {
                Utils.showToast(getActivity(), resourceId);
                mCastController.closeActivity();
            }
        }

        @Override
        public void onRemoteMediaPlayerStatusUpdated() {
            updatePlayerStatus();
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
            mCastController.onQueueItemsUpdated(size, position);
        }

        @Override
        public void onConnectionSuspended(int cause) {
            mCastController.updateControllersStatus(false);
        }

        @Override
        public void onConnectivityRecovered() {
            mCastController.updateControllersStatus(true);
        }

    }

    private class UpdateSeekbarTask extends TimerTask {

        @Override
        public void run() {
            mHandler.post(new Runnable() {

                @Override
                public void run() {
                    int currentPos;
                    if (mPlaybackState == MediaStatus.PLAYER_STATE_BUFFERING) {
                        return;
                    }
                    if (!mCastManager.isConnected()) {
                        return;
                    }
                    try {
                        int duration = (int) mCastManager.getMediaDuration();
                        if (duration > 0) {
                            try {
                                currentPos = (int) mCastManager.getCurrentMediaPosition();
                                mCastController.updateSeekbar(currentPos, duration);
                            } catch (Exception e) {
                                LOGE(TAG, "Failed to get current media position", e);
                            }
                        }
                    } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
                        LOGE(TAG, "Failed to update the progress bar due to network issues", e);
                    }

                }
            });
        }
    }

    /**
     * Loads the media on the cast device.
     *
     * @param mediaInfo The media to be loaded
     * @param shouldStartPlayback If {@code true}, playback starts after load automatically
     * @param startPoint The position to start the play back
     * @param customData An optional custom data to be sent along the load api; it can be
     * {@code null}
     */
    private void onReady(MediaInfo mediaInfo, boolean shouldStartPlayback, int startPoint,
            JSONObject customData) {
        mSelectedMedia = mediaInfo;
        updateClosedCaptionState();
        try {
            mCastController.setStreamType(mSelectedMedia.getStreamType());
            if (shouldStartPlayback) {
                // need to start remote playback
                mPlaybackState = MediaStatus.PLAYER_STATE_BUFFERING;
                mCastController.setPlaybackStatus(mPlaybackState);
                mCastManager.loadMedia(mSelectedMedia, true, startPoint, customData);
            } else {
                // we don't change the status of remote playback
                if (mCastManager.isRemoteMediaPlaying()) {
                    mPlaybackState = MediaStatus.PLAYER_STATE_PLAYING;
                } else {
                    mPlaybackState = MediaStatus.PLAYER_STATE_PAUSED;
                }
                mCastController.setPlaybackStatus(mPlaybackState);
            }
        } catch (Exception e) {
            LOGE(TAG, "Failed to get playback and media information", e);
            mCastController.closeActivity();
        }
        MediaQueue mediaQueue = mCastManager.getMediaQueue();
        int size = 0;
        int position = 0;
        if (mediaQueue != null) {
            size = mediaQueue.getCount();
            position = mediaQueue.getCurrentItemPosition();
        }
        mCastController.onQueueItemsUpdated(size, position);
        updateMetadata();
        restartTrickplayTimer();
    }

    private void updateClosedCaptionState() {
        int state = VideoCastController.CC_HIDDEN;
        if (mCastManager.isFeatureEnabled(CastConfiguration.FEATURE_CAPTIONS_PREFERENCE)
                && mSelectedMedia != null
                && mCastManager.getTracksPreferenceManager().isCaptionEnabled()) {
            List<MediaTrack> tracks = mSelectedMedia.getMediaTracks();
            state = Utils.hasAudioOrTextTrack(tracks) ? VideoCastController.CC_ENABLED
                    : VideoCastController.CC_DISABLED;
        }
        mCastController.setClosedCaptionState(state);
    }

    private void stopTrickplayTimer() {
        LOGD(TAG, "Stopped TrickPlay Timer");
        if (mSeekbarTimer != null) {
            mSeekbarTimer.cancel();
        }
    }

    private void restartTrickplayTimer() {
        stopTrickplayTimer();
        mSeekbarTimer = new Timer();
        mSeekbarTimer.scheduleAtFixedRate(new UpdateSeekbarTask(), 100, 1000);
        LOGD(TAG, "Restarted TrickPlay Timer");
    }

    private void updateOverallState() {
        MediaAuthService authService;
        switch (mOverallState) {
            case AUTHORIZING:
                authService = mCastManager.getMediaAuthService();
                if (authService != null) {
                    mCastController.setSubTitle(authService.getPendingMessage() != null
                            ? authService.getPendingMessage() : "");
                    mCastController.showLoading(true);
                }
                break;
            case PLAYBACK:
                // nothing yet, may be needed in future
                break;
            default:
                break;
        }
    }

    private void updateMetadata() {
        Uri imageUrl = null;
        if (mSelectedMedia == null) {
            if (mMediaAuthService != null) {
                imageUrl = Utils.getImageUri(mMediaAuthService.getMediaInfo(), 1);
            }
        } else {
            imageUrl = Utils.getImageUri(mSelectedMedia, 1);
        }
        showImage(imageUrl);
        if (mSelectedMedia == null) {
            return;
        }
        MediaMetadata mm = mSelectedMedia.getMetadata();
        mCastController.setTitle(mm.getString(MediaMetadata.KEY_TITLE) != null
                ? mm.getString(MediaMetadata.KEY_TITLE) : "");
        boolean isLive = mSelectedMedia.getStreamType() == MediaInfo.STREAM_TYPE_LIVE;
        mCastController.adjustControllersForLiveStream(isLive);
    }

    private void updatePlayerStatus() {
        int mediaStatus = mCastManager.getPlaybackStatus();
        mMediaStatus = mCastManager.getMediaStatus();
        LOGD(TAG, "updatePlayerStatus(), state: " + mediaStatus);
        if (mSelectedMedia == null) {
            return;
        }
        mCastController.setStreamType(mSelectedMedia.getStreamType());
        if (mediaStatus == MediaStatus.PLAYER_STATE_BUFFERING) {
            mCastController.setSubTitle(getString(R.string.ccl_loading));
        } else {
            mCastController.setSubTitle(getString(R.string.ccl_casting_to_device,
                    mCastManager.getDeviceName()));
        }
        switch (mediaStatus) {
            case MediaStatus.PLAYER_STATE_PLAYING:
                mIsFresh = false;
                if (mPlaybackState != MediaStatus.PLAYER_STATE_PLAYING) {
                    mPlaybackState = MediaStatus.PLAYER_STATE_PLAYING;
                    mCastController.setPlaybackStatus(mPlaybackState);
                }
                break;
            case MediaStatus.PLAYER_STATE_PAUSED:
                mIsFresh = false;
                if (mPlaybackState != MediaStatus.PLAYER_STATE_PAUSED) {
                    mPlaybackState = MediaStatus.PLAYER_STATE_PAUSED;
                    mCastController.setPlaybackStatus(mPlaybackState);
                }
                break;
            case MediaStatus.PLAYER_STATE_BUFFERING:
                mIsFresh = false;
                if (mPlaybackState != MediaStatus.PLAYER_STATE_BUFFERING) {
                    mPlaybackState = MediaStatus.PLAYER_STATE_BUFFERING;
                    mCastController.setPlaybackStatus(mPlaybackState);
                }
                break;
            case MediaStatus.PLAYER_STATE_IDLE:
                LOGD(TAG, "Idle Reason: " + (mCastManager.getIdleReason()));
                switch (mCastManager.getIdleReason()) {
                    case MediaStatus.IDLE_REASON_FINISHED:
                        if (!mIsFresh && (mMediaStatus == null || mMediaStatus.getLoadingItemId()
                                == MediaQueueItem.INVALID_ITEM_ID)) {
                            mCastController.closeActivity();
                        }
                        break;
                    case MediaStatus.IDLE_REASON_CANCELED:
                        try {
                            if (mCastManager.isRemoteStreamLive()) {
                                if (mPlaybackState != MediaStatus.PLAYER_STATE_IDLE) {
                                    mPlaybackState = MediaStatus.PLAYER_STATE_IDLE;
                                    mCastController.setPlaybackStatus(mPlaybackState);
                                }
                            } else {
                                mCastController.closeActivity();
                            }
                        } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
                            LOGD(TAG, "Failed to determine if stream is live", e);
                        }
                        break;
                    case MediaStatus.IDLE_REASON_INTERRUPTED:
                        mPlaybackState = MediaStatus.PLAYER_STATE_IDLE;
                        mCastController.setPlaybackStatus(mPlaybackState);
                        break;
                    default:
                        break;
                }
                break;

            default:
                break;
        }
    }

    @Override
    public void onDestroy() {
        LOGD(TAG, "onDestroy()");
        stopTrickplayTimer();
        cleanup();
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            if (mCastManager.isRemoteMediaPaused() || mCastManager.isRemoteMediaPlaying()) {
                if (mCastManager.getRemoteMediaInformation() != null
                        && TextUtils.equals(mSelectedMedia.getContentId(),
                        mCastManager.getRemoteMediaInformation().getContentId())) {
                    mIsFresh = false;
                }
            }
            if (!mCastManager.isConnecting()) {
                boolean shouldFinish = !mCastManager.isConnected()
                        || (mCastManager.getPlaybackStatus() == MediaStatus.PLAYER_STATE_IDLE
                        && mCastManager.getIdleReason() == MediaStatus.IDLE_REASON_FINISHED);
                if (shouldFinish && !mIsFresh) {
                    mCastController.closeActivity();
                    return;
                }
            }
            mMediaStatus = mCastManager.getMediaStatus();
            mCastManager.addVideoCastConsumer(mCastConsumer);
            if (!mIsFresh) {
                updatePlayerStatus();
                // updating metadata in case another client has changed it and we are resuming the
                // activity
                mSelectedMedia = mCastManager.getRemoteMediaInformation();
                updateClosedCaptionState();
                updateMetadata();
            }
        } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
            LOGE(TAG, "Failed to get media information or status of media playback", e);
            if (e instanceof  NoConnectionException) {
                mCastController.closeActivity();
            }
        } finally {
            mCastManager.incrementUiCounter();
        }
    }

    @Override
    public void onPause() {
        mCastManager.removeVideoCastConsumer(mCastConsumer);
        mCastManager.decrementUiCounter();
        mIsFresh = false;
        super.onPause();
    }

    /**
     * Call this static method to create an instance of this fragment.
     */
    public static VideoCastControllerFragment newInstance(Bundle extras) {
        VideoCastControllerFragment f = new VideoCastControllerFragment();
        Bundle b = new Bundle();
        b.putBundle(EXTRAS, extras);
        f.setArguments(b);
        return f;
    }

    /*
     * Gets the image at the given url and populates the image view with that. It tries to cache the
     * image to avoid unnecessary network calls.
     */
    private void showImage(final Uri uri) {
        if (uri == null) {
            mCastController.setImage(BitmapFactory.decodeResource(getActivity().getResources(),
                    R.drawable.album_art_placeholder_large));
            return;
        }
        if (mUrlAndBitmap != null && mUrlAndBitmap.isMatch(uri)) {
            // we can reuse mBitmap
            mCastController.setImage(mUrlAndBitmap.mBitmap);
            return;
        }
        mUrlAndBitmap = null;
        if (mImageAsyncTask != null) {
            mImageAsyncTask.cancel(true);
        }
        Point screenSize = Utils.getDisplaySize(getActivity());
        mImageAsyncTask = new FetchBitmapTask(screenSize.x, screenSize.y, false) {
            @Override
            protected void onPostExecute(Bitmap bitmap) {
                if (bitmap != null) {
                    mUrlAndBitmap = new UrlAndBitmap();
                    mUrlAndBitmap.mBitmap = bitmap;
                    mUrlAndBitmap.mUrl = uri;
                    if (!isCancelled()) {
                        mCastController.setImage(bitmap);
                    }
                }
                if (this == mImageAsyncTask) {
                    mImageAsyncTask = null;
                }
            }
        };
        mImageAsyncTask.execute(uri);
    }

    /**
     * A modal dialog with an OK button, where upon clicking on it, will finish the activity. We
     * use a DialogFragment so during configuration changes, system manages the dialog for us.
     */
    public static class ErrorDialogFragment extends DialogFragment {

        private VideoCastController mController;
        private static final String MESSAGE = "message";

        public static ErrorDialogFragment newInstance(String message) {
            ErrorDialogFragment frag = new ErrorDialogFragment();
            Bundle args = new Bundle();
            args.putString(MESSAGE, message);
            frag.setArguments(args);
            return frag;
        }

        @Override
        public void onAttach(Activity activity) {
            mController = (VideoCastController) activity;
            super.onAttach(activity);
            setCancelable(false);
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            String message = getArguments().getString(MESSAGE);
            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.ccl_error)
                    .setMessage(message)
                    .setPositiveButton(R.string.ccl_ok, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            sDialogCanceled = true;
                            mController.closeActivity();
                        }
                    })
                    .create();
        }
    }

    /*
     * Shows an error dialog
     */
    private void showErrorDialog(String message) {
        ErrorDialogFragment.newInstance(message).show(getFragmentManager(), "dlg");
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mImageAsyncTask != null) {
            mImageAsyncTask.cancel(true);
            mImageAsyncTask = null;
        }
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        try {
            if (mPlaybackState == MediaStatus.PLAYER_STATE_PLAYING) {
                mPlaybackState = MediaStatus.PLAYER_STATE_BUFFERING;
                mCastController.setPlaybackStatus(mPlaybackState);
                mCastManager.play(seekBar.getProgress());
            } else if (mPlaybackState == MediaStatus.PLAYER_STATE_PAUSED) {
                mCastManager.seek(seekBar.getProgress());
            }
            restartTrickplayTimer();
        } catch (Exception e) {
            LOGE(TAG, "Failed to complete seek", e);
            mCastController.closeActivity();
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        stopTrickplayTimer();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
    }

    @Override
    public void onPlayPauseClicked(View v) throws CastException,
            TransientNetworkDisconnectionException, NoConnectionException {
        LOGD(TAG, "isConnected returning: " + mCastManager.isConnected());
        togglePlayback();
    }

    private void togglePlayback() throws CastException, TransientNetworkDisconnectionException,
            NoConnectionException {
        switch (mPlaybackState) {
            case MediaStatus.PLAYER_STATE_PAUSED:
                mCastManager.play();
                mPlaybackState = MediaStatus.PLAYER_STATE_BUFFERING;
                restartTrickplayTimer();
                break;
            case MediaStatus.PLAYER_STATE_PLAYING:
                mCastManager.pause();
                mPlaybackState = MediaStatus.PLAYER_STATE_BUFFERING;
                break;
            case MediaStatus.PLAYER_STATE_IDLE:
                if ((mSelectedMedia.getStreamType() == MediaInfo.STREAM_TYPE_LIVE)
                        && (mCastManager.getIdleReason() == MediaStatus.IDLE_REASON_CANCELED)) {
                    mCastManager.play();
                } else {
                    mCastManager.loadMedia(mSelectedMedia, true, 0);
                }
                mPlaybackState = MediaStatus.PLAYER_STATE_BUFFERING;
                restartTrickplayTimer();
                break;
            default:
                break;
        }
        mCastController.setPlaybackStatus(mPlaybackState);
    }

    @Override
    public void onConfigurationChanged() {
        updateOverallState();
        if (mSelectedMedia == null) {
            if (mMediaAuthService != null) {
                showImage(Utils.getImageUri(mMediaAuthService.getMediaInfo(), 1));
            }
        } else {
            updateMetadata();
            updatePlayerStatus();
            mCastController.updateControllersStatus(mCastManager.isConnected());

        }
    }

    @Override
    public void onAuthResult(MediaAuthStatus status, final MediaInfo info, final String message,
            final int startPoint, final JSONObject customData) {
        if (status == MediaAuthStatus.AUTHORIZED && mAuthSuccess) {
            // successful authorization
            mMediaAuthService = null;
            if (mMediaAuthTimer != null) {
                mMediaAuthTimer.cancel();
            }
            mSelectedMedia = info;
            updateClosedCaptionState();
            mHandler.post(new Runnable() {

                @Override
                public void run() {
                    mOverallState = OverallState.PLAYBACK;
                    onReady(info, true, startPoint, customData);
                }
            });
        } else {
            if (mMediaAuthTimer != null) {
                mMediaAuthTimer.cancel();
            }
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mOverallState = OverallState.UNKNOWN;
                    showErrorDialog(message);
                }
            });

        }
    }

    @Override
    public void onAuthFailure(final String failureMessage) {
        if (mMediaAuthTimer != null) {
            mMediaAuthTimer.cancel();
        }
        mHandler.post(new Runnable() {

            @Override
            public void run() {
                mOverallState = OverallState.UNKNOWN;
                showErrorDialog(failureMessage);
            }
        });

    }

    @Override
    public void onTracksSelected(List<MediaTrack> tracks) {
        mCastManager.setActiveTracks(tracks);
    }

    /*
     * A simple class that holds a URL and a bitmap, mainly used to cache the fetched image
     */
    private class UrlAndBitmap {

        private Bitmap mBitmap;
        private Uri mUrl;

        private boolean isMatch(Uri url) {
            return url != null && mBitmap != null && url.equals(mUrl);
        }
    }

    /*
     * Cleanup of threads and timers and bitmap and ...
     */
    private void cleanup() {
        MediaAuthService authService = mCastManager.getMediaAuthService();
        if (mMediaAuthTimer != null) {
            mMediaAuthTimer.cancel();
        }
        if (mAuthThread != null) {
            mAuthThread = null;
        }
        if (mCastManager.getMediaAuthService() != null) {
            authService.setMediaAuthListener(null);
            mCastManager.removeMediaAuthService();
        }
        if (mCastManager != null) {
            mCastManager.removeVideoCastConsumer(mCastConsumer);
        }
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }
        if (mUrlAndBitmap != null) {
            mUrlAndBitmap.mBitmap = null;
        }
        if (!sDialogCanceled && mMediaAuthService != null) {
            mMediaAuthService.abortAuthorization(MediaAuthStatus.CANCELED_BY_USER);
        }

        mCastManager.removeTracksSelectedListener(this);
    }

    @Override
    public void onSkipNextClicked(View view)
            throws TransientNetworkDisconnectionException, NoConnectionException {
        mCastController.showLoading(true);
        mCastManager.queueNext(null);
    }

    @Override
    public void onSkipPreviousClicked(View view)
            throws TransientNetworkDisconnectionException, NoConnectionException {
        mCastController.showLoading(true);
        mCastManager.queuePrev(null);
    }

}
