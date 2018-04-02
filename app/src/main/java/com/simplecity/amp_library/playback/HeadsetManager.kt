package com.simplecity.amp_library.playback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

class HeadsetManager(private val callbacks: MusicService.Callbacks) {

    private var headsetReceiver: BroadcastReceiver? = null

    private var headsetReceiverIsRegistered: Boolean = false

    fun registerHeadsetPlugReceiver(context: Context) {

        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_HEADSET_PLUG)

        headsetReceiver = object : BroadcastReceiver() {

            override fun onReceive(context: Context, intent: Intent) {

                if (isInitialStickyBroadcast) {
                    return
                }

                if (intent.hasExtra("state")) {
                    if (intent.getIntExtra("state", 0) == 0) {
                        if (PlaybackSettingsManager.pauseOnHeadsetDisconnect) {
                            callbacks!!.pause()
                        }
                    } else if (intent.getIntExtra("state", 0) == 1) {
                        if (PlaybackSettingsManager.playOnHeadsetConnect) {
                            callbacks!!.play()
                        }
                    }
                }
            }
        }

        context.registerReceiver(headsetReceiver, filter)
    }

    fun unregisterHeadsetPlugReceiver(context: Context) {

        if (headsetReceiverIsRegistered) {
            context.unregisterReceiver(headsetReceiver)
            headsetReceiverIsRegistered = false
        }
    }
}
