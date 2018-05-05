package com.simplecity.amp_library.playback;

import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import com.simplecity.amp_library.playback.constants.InternalIntents;
import com.simplecity.amp_library.playback.constants.PlayerHandler;
import java.lang.ref.WeakReference;

final class MediaPlayerHandler extends Handler {

    private static final String TAG = "MediaPlayerHandler";

    private final WeakReference<PlaybackManager> playbackManagerWeakReference;
    private final WeakReference<QueueManager> queueManagerWeakReference;

    private float currentVolume = 1.0f;

    MediaPlayerHandler(PlaybackManager playbackManager, QueueManager queueManager, Looper looper) {
        super(looper);
        this.playbackManagerWeakReference = new WeakReference<>(playbackManager);
        this.queueManagerWeakReference = new WeakReference<>(queueManager);
    }

    @Override
    public void handleMessage(Message msg) {
        PlaybackManager playbackManager = playbackManagerWeakReference.get();
        QueueManager queueManager = queueManagerWeakReference.get();
        if (playbackManager == null || queueManager == null) {
            return;
        }

        switch (msg.what) {
            case PlayerHandler.FADE_DOWN:
                currentVolume -= .05f;
                if (currentVolume > .2f) {
                    sendEmptyMessageDelayed(PlayerHandler.FADE_DOWN, 10);
                } else {
                    currentVolume = .2f;
                }
                playbackManager.setVolume(currentVolume);
                break;
            case PlayerHandler.FADE_UP:
                currentVolume += .01f;
                if (currentVolume < 1.0f) {
                    sendEmptyMessageDelayed(PlayerHandler.FADE_UP, 10);
                } else {
                    currentVolume = 1.0f;
                }
                playbackManager.setVolume(currentVolume);
                break;
            case PlayerHandler.SERVER_DIED:
                if (playbackManager.isPlaying()) {
                    playbackManager.next(true);
                } else {
                    playbackManager.openCurrentAndNext();
                }
                break;
            case PlayerHandler.TRACK_WENT_TO_NEXT:
                playbackManager.notifyChange(InternalIntents.TRACK_ENDING);
                queueManager.queuePosition = queueManager.nextPlayPos;
                playbackManager.notifyChange(InternalIntents.META_CHANGED);
                playbackManager.setNextTrack();

                if (playbackManager.pauseOnTrackFinish) {
                    playbackManager.pause();
                    playbackManager.pauseOnTrackFinish = false;
                }
                break;
            case PlayerHandler.TRACK_ENDED:
                playbackManager.notifyChange(InternalIntents.TRACK_ENDING);
                if (queueManager.repeatMode == QueueManager.RepeatMode.ONE) {
                    playbackManager.seekTo(0);
                    playbackManager.play();
                } else {
                    playbackManager.next(false);
                }
                break;
            case PlayerHandler.RELEASE_WAKELOCK:
                playbackManager.releaseWakelock();
                break;

            case PlayerHandler.FOCUS_CHANGE:
                // This code is here so we can better synchronize it with
                // the code that handles fade-in
                switch (msg.arg1) {
                    case AudioManager.AUDIOFOCUS_LOSS:
                        if (playbackManager.isPlaying()) {
                            playbackManager.pausedByTransientLossOfFocus = false;
                        }
                        playbackManager.pause();
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                        removeMessages(PlayerHandler.FADE_UP);
                        sendEmptyMessage(PlayerHandler.FADE_DOWN);
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                        if (playbackManager.isPlaying()) {
                            playbackManager.pausedByTransientLossOfFocus = true;
                        }
                        playbackManager.pause();
                        break;
                    case AudioManager.AUDIOFOCUS_GAIN:
                        if (!playbackManager.isPlaying() && playbackManager.pausedByTransientLossOfFocus) {
                            playbackManager.pausedByTransientLossOfFocus = false;
                            currentVolume = 0f;
                            if (playbackManager.player != null) {
                                playbackManager.player.setVolume(currentVolume);
                            }
                            playbackManager.play(); // also queues a fade-in
                        } else {
                            removeMessages(PlayerHandler.FADE_DOWN);
                            sendEmptyMessage(PlayerHandler.FADE_UP);
                        }
                        break;
                    default:
                        Log.e(TAG, "Unknown audio focus change code");
                }
                break;

            case PlayerHandler.FADE_DOWN_STOP:
                currentVolume -= .05f;
                if (currentVolume > 0f) {
                    sendEmptyMessageDelayed(PlayerHandler.FADE_DOWN_STOP, 200);
                } else {
                    playbackManager.pause();
                }
                if (playbackManager.player != null) {
                    playbackManager.player.setVolume(currentVolume);
                }
                break;

            case PlayerHandler.GO_TO_NEXT:
                playbackManager.next(true);
                break;

            case PlayerHandler.GO_TO_PREV:
                playbackManager.previous();
                break;

            case PlayerHandler.SHUFFLE_ALL:
                playbackManager.playAutoShuffleList();
                break;
        }
    }
}