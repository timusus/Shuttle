package com.simplecity.amp_library.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AsyncPlayer;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.view.KeyEvent;

import com.simplecity.amp_library.R;
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

    private static PowerManager.WakeLock wakeLock = null;
    static int clickCounter = 0;
    static long lastClickTime = 0;
    static boolean down = false;
    static boolean launched = false;

    /**
     * Play a beep sound.
     */
    private static void beep(Context context) {
        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("pref_headset_beep", true)) {
            AsyncPlayer beepPlayer = new AsyncPlayer("BeepPlayer");
            Uri beepSoundUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" +
                    context.getResources().getResourcePackageName(R.raw.beep) + '/' +
                    context.getResources().getResourceTypeName(R.raw.beep) + '/' +
                    context.getResources().getResourceEntryName(R.raw.beep));

            if (ShuttleUtils.hasMarshmallow()) {
                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        // Could use AudioAttributes.ASSISTANCE_SONIFICATION here, since this represents a button press type action..
                        // However, that seems to play our audio a little too quietly (and the beep track is already adjusted to be relatively quiet).
                        // So let's just treat it as music, which will use the user's music stream's volume anyway.
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build();
                beepPlayer.play(context, beepSoundUri, false, audioAttributes);
            } else {
                beepPlayer.play(context, beepSoundUri, false, AudioManager.STREAM_MUSIC);
            }
        }
    }

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
                        if (down) {
                            if ((MusicService.MediaButtonCommand.TOGGLE_PAUSE.equals(command) ||
                                    MusicService.MediaButtonCommand.PLAY.equals(command))) {
                                if (lastClickTime != 0 && eventTime - lastClickTime > LONG_PRESS_DELAY) {
                                    acquireWakeLockAndSendMessage(context,
                                            handler.obtainMessage(MSG_LONGPRESS_TIMEOUT, context), 0);
                                }
                            }
                        } else if (event.getRepeatCount() == 0) {
                            // Only consider the first event in a sequence, not the repeat events,
                            // so that we don't trigger in cases where the first event went to a
                            // different app (e.g. when the user ends a phone call by long pressing
                            // the headset button)

                            // The service may or may not be running, but we need to send it a command
                            if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK) {
                                if (eventTime - lastClickTime >= DOUBLE_CLICK) {
                                    clickCounter = 0;
                                }

                                clickCounter++;

                                handler.removeMessages(MSG_HEADSET_DOUBLE_CLICK_TIMEOUT);

                                Message msg = handler.obtainMessage(MSG_HEADSET_DOUBLE_CLICK_TIMEOUT, clickCounter, 0, context);

                                long delay = clickCounter < 3 ? DOUBLE_CLICK : 0;
                                if (clickCounter >= 3) {
                                    clickCounter = 0;
                                }
                                lastClickTime = eventTime;
                                acquireWakeLockAndSendMessage(context, msg, delay);
                            } else {
                                startService(context, command);
                            }
                            launched = false;
                            down = true;
                        }
                    } else {
                        handler.removeMessages(MSG_LONGPRESS_TIMEOUT);
                        down = false;
                    }

                    releaseWakeLockIfHandlerIdle();
                }
            }
        }
    }

    static Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_LONGPRESS_TIMEOUT:
                    if (!launched) {
                        final Context context = (Context) msg.obj;
                        final Intent intent = new Intent();
                        intent.setClass(context, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        context.startActivity(intent);
                        launched = true;
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
                        if (MusicService.MediaButtonCommand.NEXT.equals((command))) {
                            beep(context);
                        }
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
        if (wakeLock == null) {
            Context appContext = context.getApplicationContext();
            PowerManager pm = (PowerManager) appContext.getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Headset button");
            wakeLock.setReferenceCounted(false);
        }

        // Make sure we don't indefinitely hold the wake lock under any circumstances
        wakeLock.acquire(10000);

        handler.sendMessageDelayed(msg, delay);
    }

    static void releaseWakeLockIfHandlerIdle() {
        if (handler.hasMessages(MSG_LONGPRESS_TIMEOUT) || handler.hasMessages(MSG_HEADSET_DOUBLE_CLICK_TIMEOUT)) {
            return;
        }

        if (wakeLock != null) {
            wakeLock.release();
            wakeLock = null;
        }
    }
}
