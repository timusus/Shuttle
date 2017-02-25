package com.simplecity.amp_library.playback;

import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.lang.ref.WeakReference;

final class MediaPlayerHandler extends Handler {

    private static final String TAG = "MediaPlayerHandler";

    private final WeakReference<MusicService> mService;
    private float mCurrentVolume = 1.0f;

    MediaPlayerHandler(final MusicService service, final Looper looper) {
        super(looper);
        mService = new WeakReference<>(service);
    }

    @Override
    public void handleMessage(Message msg) {
        final MusicService service = mService.get();
        if (service == null) {
            return;
        }

        switch (msg.what) {
            case MusicService.PlayerHandler.FADE_DOWN:
                mCurrentVolume -= .05f;
                if (mCurrentVolume > .2f) {
                    sendEmptyMessageDelayed(MusicService.PlayerHandler.FADE_DOWN, 10);
                } else {
                    mCurrentVolume = .2f;
                }
                service.player.setVolume(mCurrentVolume);
                break;
            case MusicService.PlayerHandler.FADE_UP:
                mCurrentVolume += .01f;
                if (mCurrentVolume < 1.0f) {
                    sendEmptyMessageDelayed(MusicService.PlayerHandler.FADE_UP, 10);
                } else {
                    mCurrentVolume = 1.0f;
                }
                if (service.player != null) {
                    service.player.setVolume(mCurrentVolume);
                }
                break;
            case MusicService.PlayerHandler.SERVER_DIED:
                if (service.isPlaying()) {
                    service.gotoNext(true);
                } else {
                    service.openCurrentAndNext();
                }
                break;
            case MusicService.PlayerHandler.TRACK_WENT_TO_NEXT:
                service.notifyChange(MusicService.InternalIntents.TRACK_ENDING);
                service.playPos = service.nextPlayPos;
                if (service.playPos >= 0 && !service.getCurrentPlaylist().isEmpty()) {
                    service.currentSong = service.getCurrentPlaylist().get(service.playPos);
                }
                service.notifyChange(MusicService.InternalIntents.META_CHANGED);
                service.updateNotification();
                service.setNextTrack();
                break;
            case MusicService.PlayerHandler.TRACK_ENDED:
                service.notifyChange(MusicService.InternalIntents.TRACK_ENDING);
                if (service.repeatMode == MusicService.RepeatMode.ONE) {
                    service.seekTo(0);
                    service.play();
                } else {
                    service.gotoNext(false);
                }
                break;
            case MusicService.PlayerHandler.RELEASE_WAKELOCK:
                service.mWakeLock.release();
                break;

            case MusicService.PlayerHandler.FOCUS_CHANGE:
                // This code is here so we can better synchronize it with
                // the code that handles fade-in
                switch (msg.arg1) {
                    case AudioManager.AUDIOFOCUS_LOSS:
                        if (service.isPlaying()) {
                            service.pausedByTransientLossOfFocus = false;
                        }
                        service.pause();
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                        removeMessages(MusicService.PlayerHandler.FADE_UP);
                        sendEmptyMessage(MusicService.PlayerHandler.FADE_DOWN);
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                        if (service.isPlaying()) {
                            service.pausedByTransientLossOfFocus = true;
                        }
                        service.pause();
                        break;
                    case AudioManager.AUDIOFOCUS_GAIN:
                        if (!service.isPlaying()
                                && service.pausedByTransientLossOfFocus) {
                            service.pausedByTransientLossOfFocus = false;
                            mCurrentVolume = 0f;
                            service.player.setVolume(mCurrentVolume);
                            service.play(); // also queues a fade-in
                        } else {
                            removeMessages(MusicService.PlayerHandler.FADE_DOWN);
                            sendEmptyMessage(MusicService.PlayerHandler.FADE_UP);
                        }
                        break;
                    default:
                        Log.e(TAG, "Unknown audio focus change code");
                }
                break;

            case MusicService.PlayerHandler.FADE_DOWN_STOP:
                mCurrentVolume -= .05f;
                if (mCurrentVolume > 0f) {
                    sendEmptyMessageDelayed(MusicService.PlayerHandler.FADE_DOWN_STOP, 200);
                } else {
                    service.pause();
                }
                service.player.setVolume(mCurrentVolume);
                break;

            case MusicService.PlayerHandler.GO_TO_NEXT:
                service.gotoNext(true);
                break;

            case MusicService.PlayerHandler.GO_TO_PREV:
                service.previous();
                break;

            case MusicService.PlayerHandler.SHUFFLE_ALL:
                service.playAutoShuffleList();
                break;
        }
    }
}
