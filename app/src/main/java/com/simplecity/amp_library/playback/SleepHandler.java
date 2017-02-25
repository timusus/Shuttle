package com.simplecity.amp_library.playback;

import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;

import java.lang.ref.WeakReference;

final class SleepHandler extends Handler {

    private final WeakReference<MusicService> mService;

    private boolean handled = false;

    SleepHandler(final MusicService service) {
        mService = new WeakReference<>(service);

    }

    @Override
    public void handleMessage(final Message msg) {
        final MusicService service = mService.get();
        if (service == null) {
            return;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(service);

        boolean waitTilEnd = prefs.getBoolean("sleep_timer_wait_til_end", true);

        if (waitTilEnd && !handled) {

            //If we're waiting til the end of the currently playing song,
            //resend a message with the time remaining until sleep
            long duration = service.getDuration();
            long position = service.getPosition();
            handled = true;
            service.sleepHandler.sendMessageDelayed(
                    service.sleepHandler.obtainMessage(
                            MusicService.PlayerHandler.SLEEP
                    ), duration - position - 350
            );
        } else if (waitTilEnd) {
            //IThe currently playing song has just finished, pause with no fade
            service.pause();
        } else {
            //We don't care about where the current song is. Fade down and pause.
            service.playerHandler.sendEmptyMessage(MusicService.PlayerHandler.FADE_DOWN_STOP);
            handled = false;
        }
    }
}
