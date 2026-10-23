package com.simplecity.amp_library.playback

import android.content.BroadcastReceiver
import android.content.Context
import androidx.core.content.ContextCompat
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import com.simplecity.amp_library.utils.SettingsManager

class HeadsetManager(private val playbackManager: PlaybackManager) {

    private var headsetReceiver: BroadcastReceiver? = null
    private var noisyReceiver: BroadcastReceiver? = null

    fun registerHeadsetPlugReceiver(context: Context) {

        val filter = IntentFilter()
        filter.addAction(AudioManager.ACTION_HEADSET_PLUG)

        headsetReceiver = object : BroadcastReceiver() {

            override fun onReceive(context: Context, intent: Intent) {

                if (isInitialStickyBroadcast) {
                    return
                }

                if (intent.hasExtra("state")) {
                    if (intent.getIntExtra("state", 0) == 0) {
                        if (PlaybackSettingsManager.pauseOnHeadsetDisconnect) {
                            playbackManager.pause()
                        }
                    } else if (intent.getIntExtra("state", 0) == 1) {
                        if (PlaybackSettingsManager.playOnHeadsetConnect) {
                            playbackManager.play()
                        }
                    }
                }
            }
        }

        // System broadcast
        ContextCompat.registerReceiver(context, headsetReceiver, filter, ContextCompat.RECEIVER_EXPORTED)

        // The A2DP/headset broadcasts need BLUETOOTH_CONNECT at targetSdk 31+, and manifest receivers
        // no longer get this one, so listen at runtime. It doesn't say which device went away.
        noisyReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (PlaybackSettingsManager.pauseOnHeadsetDisconnect || SettingsManager.getInstance().bluetoothPauseDisconnect) {
                    playbackManager.pause()
                }
            }
        }
        ContextCompat.registerReceiver(context, noisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY), ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    fun unregisterHeadsetPlugReceiver(context: Context) {
        context.unregisterReceiver(headsetReceiver)
        context.unregisterReceiver(noisyReceiver)
    }
}
