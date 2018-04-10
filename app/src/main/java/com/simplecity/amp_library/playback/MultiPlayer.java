package com.simplecity.amp_library.playback;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;
import com.crashlytics.android.core.CrashlyticsCore;
import com.simplecity.amp_library.playback.constants.PlayerHandler;
import com.simplecity.amp_library.utils.ShuttleUtils;

class MultiPlayer implements MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {

    private static final String TAG = "MultiPlayer";

    private Context context;

    private MediaPlayer currentMediaPlayer = new MediaPlayer();

    private MediaPlayer nextMediaPlayer;

    private Handler handler;

    private boolean isInitialized = false;

    private PowerManager.WakeLock wakeLock;

    MultiPlayer(Context context) {
        this.context = context.getApplicationContext();

        currentMediaPlayer.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK);

        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
        }
        wakeLock.setReferenceCounted(false);
    }

    void setDataSource(final String path) {
        isInitialized = setDataSourceImpl(currentMediaPlayer, path);
        if (isInitialized) {
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
                mediaPlayer.setDataSource(context, uri);
            } else {
                mediaPlayer.setDataSource(path);
            }
            if (ShuttleUtils.hasOreo()) {
                mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build());
            } else {
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            }
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
            currentMediaPlayer.setNextMediaPlayer(null);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Next media player is current one, continuing");
        } catch (IllegalStateException e) {
            Log.e(TAG, "Media player not initialized!");
            CrashlyticsCore.getInstance().log("setNextDataSource failed for. Media player not intitialized.");
            return;
        }
        if (nextMediaPlayer != null) {
            nextMediaPlayer.release();
            nextMediaPlayer = null;
        }
        if (TextUtils.isEmpty(path)) {
            return;
        }
        nextMediaPlayer = new MediaPlayer();
        nextMediaPlayer.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK);
        nextMediaPlayer.setAudioSessionId(getAudioSessionId());
        if (setDataSourceImpl(nextMediaPlayer, path)) {
            try {
                currentMediaPlayer.setNextMediaPlayer(nextMediaPlayer);
            } catch (Exception e) {
                Log.e(TAG, "setNextDataSource failed - failed to call setNextMediaPlayer on currentMediaPlayer. Error: " + e.getLocalizedMessage());
                CrashlyticsCore.getInstance().log("setNextDataSource failed - failed to call setNextMediaPlayer on currentMediaPlayer. Error: " + e.getLocalizedMessage());
                if (nextMediaPlayer != null) {
                    nextMediaPlayer.release();
                    nextMediaPlayer = null;
                }
            }
        } else {
            Log.e(TAG, "setDataSourceImpl failed for path: [" + path + "]. Setting next media player to null");
            CrashlyticsCore.getInstance().log("setDataSourceImpl failed for path: [" + path + "]. Setting next media player to null");
            if (nextMediaPlayer != null) {
                nextMediaPlayer.release();
                nextMediaPlayer = null;
            }
        }
    }

    boolean isInitialized() {
        return isInitialized;
    }

    public void start() {
        try {
            currentMediaPlayer.start();
        } catch (RuntimeException e) {
            CrashlyticsCore.getInstance().log("MusicService.start() failed. Exception: " + e.toString());
        }
    }

    public void stop() {
        try {
            currentMediaPlayer.reset();
        } catch (IllegalStateException e) {
            Log.e(TAG, "Error stopping MultiPlayer: " + e.getLocalizedMessage());
            CrashlyticsCore.getInstance().log("stop() failed. Error: " + e.getLocalizedMessage());
        }
        isInitialized = false;
    }

    /**
     * You CANNOT use this player anymore after calling release()
     */
    public void release() {
        stop();
        currentMediaPlayer.release();
    }

    public void pause() {
        try {
            currentMediaPlayer.pause();
        } catch (IllegalStateException e) {
            Log.e(TAG, "Error pausing MultiPlayer: " + e.getLocalizedMessage());
        }
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    public long getDuration() {
        try {
            return currentMediaPlayer.getDuration();
        } catch (IllegalStateException ignored) {
            return 0;
        }
    }

    public long getPosition() {
        try {
            return currentMediaPlayer.getCurrentPosition();
        } catch (IllegalStateException ignored) {
            return 0;
        }
    }

    void seekTo(long whereto) {
        try {
            currentMediaPlayer.seekTo((int) whereto);
        } catch (IllegalStateException e) {
            Log.e(TAG, "Error seeking MultiPlayer: " + e.getLocalizedMessage());
        }
    }

    void setVolume(float vol) {
        try {
            currentMediaPlayer.setVolume(vol, vol);
        } catch (IllegalStateException e) {
            Log.e(TAG, "Error setting MultiPlayer volume: " + e.getLocalizedMessage());
        }
    }

    int getAudioSessionId() {
        int sessionId = 0;
        try {
            sessionId = currentMediaPlayer.getAudioSessionId();
        } catch (IllegalStateException ignored) {
            //Nothing to do
        }
        return sessionId;
    }

    @Override
    public boolean onError(final MediaPlayer mp, final int what, final int extra) {
        switch (what) {
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                isInitialized = false;
                currentMediaPlayer.release();
                currentMediaPlayer = new MediaPlayer();
                currentMediaPlayer.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK);
                handler.sendMessageDelayed(handler.obtainMessage(PlayerHandler.SERVER_DIED), 2000);
                return true;
            default:
                break;
        }
        return false;
    }

    @Override
    public void onCompletion(final MediaPlayer mp) {
        if (mp == currentMediaPlayer && nextMediaPlayer != null) {
            currentMediaPlayer.release();
            currentMediaPlayer = nextMediaPlayer;
            nextMediaPlayer = null;
            handler.sendEmptyMessage(PlayerHandler.TRACK_WENT_TO_NEXT);
        } else {
            wakeLock.acquire(30000);
            handler.sendEmptyMessage(PlayerHandler.TRACK_ENDED);
            handler.sendEmptyMessage(PlayerHandler.RELEASE_WAKELOCK);
        }
    }

    public void releaseWakelock() {
        wakeLock.release();
    }
}
