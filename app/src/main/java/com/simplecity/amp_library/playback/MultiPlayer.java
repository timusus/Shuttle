package com.simplecity.amp_library.playback;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;

import com.crashlytics.android.core.CrashlyticsCore;

import java.lang.ref.WeakReference;

/**
 * Provides a unified interface for dealing with midi files and other media
 * files.
 */
class MultiPlayer implements
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener {

    private static final String TAG = "MultiPlayer";

    private final WeakReference<MusicService> mService;
    private MediaPlayer mCurrentMediaPlayer = new MediaPlayer();
    private MediaPlayer mNextMediaPlayer;
    private Handler mHandler;
    private boolean mIsInitialized = false;

    MultiPlayer(final MusicService service) {
        mService = new WeakReference<>(service);
        mCurrentMediaPlayer.setWakeMode(mService.get(), PowerManager.PARTIAL_WAKE_LOCK);
    }

    void setDataSource(final String path) {
        mIsInitialized = setDataSourceImpl(mCurrentMediaPlayer, path);
        if (mIsInitialized) {
            setNextDataSource(null);
        }
    }

    private boolean setDataSourceImpl(final MediaPlayer mediaPlayer, final String path) {
        if (TextUtils.isEmpty(path) || mediaPlayer == null) {
            return false;
        }
        try {
            mediaPlayer.reset();
            mediaPlayer.setOnPreparedListener(null);
            if (path.startsWith("content://")) {
                Uri uri = Uri.parse(path);
                mediaPlayer.setDataSource(mService.get(), uri);
            } else {
                mediaPlayer.setDataSource(path);
            }
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.prepare();
        } catch (final Exception e) {
            Log.e(TAG, "setDataSource failed: " + e.getLocalizedMessage());
            CrashlyticsCore.getInstance().log("setDataSourceImpl failed. Path: [" + path + "] error: " + e.getLocalizedMessage());
            return false;
        }
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);

        return true;
    }

    void setNextDataSource(final String path) {
        try {
            mCurrentMediaPlayer.setNextMediaPlayer(null);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Next media player is current one, continuing");
        } catch (IllegalStateException e) {
            Log.e(TAG, "Media player not initialized!");
            CrashlyticsCore.getInstance().log("setNextDataSource failed for. Media player not intitialized.");
            return;
        }
        if (mNextMediaPlayer != null) {
            mNextMediaPlayer.release();
            mNextMediaPlayer = null;
        }
        if (TextUtils.isEmpty(path)) {
            return;
        }
        mNextMediaPlayer = new MediaPlayer();
        mNextMediaPlayer.setWakeMode(mService.get(), PowerManager.PARTIAL_WAKE_LOCK);
        mNextMediaPlayer.setAudioSessionId(getAudioSessionId());
        if (setDataSourceImpl(mNextMediaPlayer, path)) {
            try {
                mCurrentMediaPlayer.setNextMediaPlayer(mNextMediaPlayer);
            } catch (Exception e) {
                Log.e(TAG, "setNextDataSource failed - failed to call setNextMediaPlayer on mCurrentMediaPlayer. Error: " + e.getLocalizedMessage());
                CrashlyticsCore.getInstance().log("setNextDataSource failed - failed to call setNextMediaPlayer on mCurrentMediaPlayer. Error: " + e.getLocalizedMessage());
                if (mNextMediaPlayer != null) {
                    mNextMediaPlayer.release();
                    mNextMediaPlayer = null;
                }
            }
        } else {
            Log.e(TAG, "setDataSourceImpl failed for path: [" + path + "]. Setting next media player to null");
            CrashlyticsCore.getInstance().log("setDataSourceImpl failed for path: [" + path + "]. Setting next media player to null");
            if (mNextMediaPlayer != null) {
                mNextMediaPlayer.release();
                mNextMediaPlayer = null;
            }
        }
    }

    boolean isInitialized() {
        return mIsInitialized;
    }

    public void start() {
        try {
            mCurrentMediaPlayer.start();
        } catch (RuntimeException e) {
            CrashlyticsCore.getInstance().log("MusicService.start() failed. Exception: " + e.toString());
        }
    }

    public void stop() {
        try {
            mCurrentMediaPlayer.reset();
        } catch (IllegalStateException e) {
            Log.e(TAG, "Error stopping MultiPlayer: " + e.getLocalizedMessage());
            CrashlyticsCore.getInstance().log("stop() failed. Error: " + e.getLocalizedMessage());
        }
        mIsInitialized = false;
    }

    /**
     * You CANNOT use this player anymore after calling release()
     */
    public void release() {
        stop();
        mCurrentMediaPlayer.release();
    }

    public void pause() {
        try {
            mCurrentMediaPlayer.pause();
        } catch (IllegalStateException e) {
            Log.e(TAG, "Error pausing MultiPlayer: " + e.getLocalizedMessage());
        }
    }

    public void setHandler(Handler handler) {
        mHandler = handler;
    }

    public long getDuration() {
        try {
            return mCurrentMediaPlayer.getDuration();
        } catch (IllegalStateException ignored) {
            return 0;
        }
    }

    public long getPosition() {
        try {
            return mCurrentMediaPlayer.getCurrentPosition();
        } catch (IllegalStateException ignored) {
            return 0;
        }
    }

    long seekTo(long whereto) {
        try {
            mCurrentMediaPlayer.seekTo((int) whereto);
        } catch (IllegalStateException e) {
            Log.e(TAG, "Error seeking MultiPlayer: " + e.getLocalizedMessage());
        }
        return whereto;
    }

    void setVolume(float vol) {
        try {
            mCurrentMediaPlayer.setVolume(vol, vol);
        } catch (IllegalStateException e) {
            Log.e(TAG, "Error setting MultiPlayer volume: " + e.getLocalizedMessage());
        }
    }

    int getAudioSessionId() {
        if (Build.VERSION.SDK_INT > 8) {
            int sessionId = 0;
            try {
                sessionId = mCurrentMediaPlayer.getAudioSessionId();
            } catch (IllegalStateException ignored) {
                //Nothing to do
            }
            return sessionId;
        } else {
            return 0;
        }
    }

    @Override
    public boolean onError(final MediaPlayer mp, final int what, final int extra) {
        switch (what) {
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                mIsInitialized = false;
                mCurrentMediaPlayer.release();
                mCurrentMediaPlayer = new MediaPlayer();
                mCurrentMediaPlayer.setWakeMode(mService.get(), PowerManager.PARTIAL_WAKE_LOCK);
                mHandler.sendMessageDelayed(mHandler.obtainMessage(MusicService.PlayerHandler.SERVER_DIED), 2000);
                return true;
            default:
                break;
        }
        return false;
    }

    @Override
    public void onCompletion(final MediaPlayer mp) {
        if (mp == mCurrentMediaPlayer && mNextMediaPlayer != null) {
            mCurrentMediaPlayer.release();
            mCurrentMediaPlayer = mNextMediaPlayer;
            mNextMediaPlayer = null;
            mHandler.sendEmptyMessage(MusicService.PlayerHandler.TRACK_WENT_TO_NEXT);
        } else {
            mService.get().mWakeLock.acquire(30000);
            mHandler.sendEmptyMessage(MusicService.PlayerHandler.TRACK_ENDED);
            mHandler.sendEmptyMessage(MusicService.PlayerHandler.RELEASE_WAKELOCK);
        }
    }
}
