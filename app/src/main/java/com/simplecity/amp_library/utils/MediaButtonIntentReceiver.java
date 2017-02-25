package com.simplecity.amp_library.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.view.KeyEvent;

import com.simplecity.amp_library.playback.MusicService;
import com.simplecity.amp_library.ui.activities.MainActivity;

/**
 * This class is used to control headset playback. Single press: pause/resume
 * Double press: next track Long press: voice search
 */
public class MediaButtonIntentReceiver extends WakefulBroadcastReceiver {

    private static final int MSG_LONGPRESS_TIMEOUT = 1;
    private static final int MSG_HEADSET_DOUBLE_CLICK_TIMEOUT = 2;

    private static final int LONG_PRESS_DELAY = 1000;
    private static final int DOUBLE_CLICK = 800;

    private static PowerManager.WakeLock mWakeLock = null;
    static int mClickCounter = 0;
    static long mLastClickTime = 0;
    static boolean mDown = false;
    static boolean mLaunched = false;

    public static class MediaButtonReceiverHelper {
        public static void onReceive(Context context, Intent intent) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

            final String intentAction = intent.getAction();

            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intentAction) && preferences.getBoolean("pref_headset_disconnect", true)) {
                startService(context, MusicService.MediaButtonCommand.PAUSE);

            } else if (Intent.ACTION_MEDIA_BUTTON.equals(intentAction)) {
                final KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                if (event == null) {
                    return;
                }

                final int keyCode = event.getKeyCode();
                final int action = event.getAction();
                final long eventTime = event.getEventTime();

                String command = null;
                switch (keyCode) {
                    case KeyEvent.KEYCODE_MEDIA_STOP:
                        command = MusicService.MediaButtonCommand.STOP;
                        break;
                    case KeyEvent.KEYCODE_HEADSETHOOK:
                    case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                        command = MusicService.MediaButtonCommand.TOGGLE_PAUSE;
                        break;
                    case KeyEvent.KEYCODE_MEDIA_NEXT:
                        command = MusicService.MediaButtonCommand.NEXT;
                        break;
                    case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                        command = MusicService.MediaButtonCommand.PREVIOUS;
                        break;
                    case KeyEvent.KEYCODE_MEDIA_PAUSE:
                        command = MusicService.MediaButtonCommand.PAUSE;
                        break;
                    case KeyEvent.KEYCODE_MEDIA_PLAY:
                        command = MusicService.MediaButtonCommand.PLAY;
                        break;
                }

                if (command != null) {
                    if (action == KeyEvent.ACTION_DOWN) {
                        if (mDown) {
                            if ((MusicService.MediaButtonCommand.TOGGLE_PAUSE.equals(command) ||
                                    MusicService.MediaButtonCommand.PLAY.equals(command))) {
                                if (mLastClickTime != 0 && eventTime - mLastClickTime > LONG_PRESS_DELAY) {
                                    acquireWakeLockAndSendMessage(context,
                                            mHandler.obtainMessage(MSG_LONGPRESS_TIMEOUT, context), 0);
                                }
                            }
                        } else if (event.getRepeatCount() == 0) {
                            // Only consider the first event in a sequence, not the repeat events,
                            // so that we don't trigger in cases where the first event went to a
                            // different app (e.g. when the user ends a phone call by long pressing
                            // the headset button)

                            // The service may or may not be running, but we need to send it a command
                            if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK) {
                                if (eventTime - mLastClickTime >= DOUBLE_CLICK) {
                                    mClickCounter = 0;
                                }

                                mClickCounter++;

                                mHandler.removeMessages(MSG_HEADSET_DOUBLE_CLICK_TIMEOUT);

                                Message msg = mHandler.obtainMessage(MSG_HEADSET_DOUBLE_CLICK_TIMEOUT, mClickCounter, 0, context);

                                long delay = mClickCounter < 3 ? DOUBLE_CLICK : 0;
                                if (mClickCounter >= 3) {
                                    mClickCounter = 0;
                                }
                                mLastClickTime = eventTime;
                                acquireWakeLockAndSendMessage(context, msg, delay);
                            } else {
                                startService(context, command);
                            }
                            mLaunched = false;
                            mDown = true;
                        }
                    } else {
                        mHandler.removeMessages(MSG_LONGPRESS_TIMEOUT);
                        mDown = false;
                    }

                    releaseWakeLockIfHandlerIdle();
                }
            }
        }
    }

    static Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_LONGPRESS_TIMEOUT:
                    if (!mLaunched) {
                        final Context context = (Context) msg.obj;
                        final Intent intent = new Intent();
                        intent.setClass(context, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        context.startActivity(intent);
                        mLaunched = true;
                    }
                    break;

                case MSG_HEADSET_DOUBLE_CLICK_TIMEOUT:
                    final int clickCount = msg.arg1;
                    final String command;

                    switch (clickCount) {
                        case 1:
                            command = MusicService.MediaButtonCommand.TOGGLE_PAUSE;
                            break;
                        case 2:
                            command = MusicService.MediaButtonCommand.NEXT;
                            break;
                        case 3:
                            command = MusicService.MediaButtonCommand.PREVIOUS;
                            break;
                        default:
                            command = null;
                            break;
                    }

                    if (command != null) {
                        final Context context = (Context) msg.obj;
                        startService(context, command);
                    }
                    break;
            }
            releaseWakeLockIfHandlerIdle();
        }
    };

    @Override
    public void onReceive(Context context, Intent intent) {
        MediaButtonReceiverHelper.onReceive(context, intent);
        if (isOrderedBroadcast()) {
            abortBroadcast();
        }
    }

    static void startService(final Context context, final String command) {
        final Intent intent = new Intent(context, MusicService.class);
        intent.setAction(MusicService.ServiceCommand.SERVICE_COMMAND);
        intent.putExtra(MusicService.MediaButtonCommand.CMD_NAME, command);
        intent.putExtra(MusicService.MediaButtonCommand.FROM_MEDIA_BUTTON, true);
        startWakefulService(context, intent);
    }

    static void acquireWakeLockAndSendMessage(Context context, Message msg, long delay) {
        if (mWakeLock == null) {
            Context appContext = context.getApplicationContext();
            PowerManager pm = (PowerManager) appContext.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Headset button");
            mWakeLock.setReferenceCounted(false);
        }

        // Make sure we don't indefinitely hold the wake lock under any circumstances
        mWakeLock.acquire(10000);

        mHandler.sendMessageDelayed(msg, delay);
    }

    static void releaseWakeLockIfHandlerIdle() {
        if (mHandler.hasMessages(MSG_LONGPRESS_TIMEOUT) || mHandler.hasMessages(MSG_HEADSET_DOUBLE_CLICK_TIMEOUT)) {
            return;
        }

        if (mWakeLock != null) {
            mWakeLock.release();
            mWakeLock = null;
        }
    }
}
