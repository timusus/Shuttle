package com.simplecity.amp_library.playback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.support.annotation.CallSuper
import android.util.Log
import com.simplecity.amp_library.playback.Playback.Callbacks
import com.simplecity.amp_library.playback.constants.MediaButtonCommand
import com.simplecity.amp_library.playback.constants.ServiceCommand

/**
 * A base class for local playback engines, which manages requesting/cancelling audio focus, and pausing, resuming or ducking
 * the audio in response to incoming calls/notifications etc.
 */
abstract class LocalPlayback(context: Context) : Playback {

    object Volume {
        /**
         * The volume we set the media player to when we lose audio focus, but are
         * allowed to reduce the volume instead of stopping playback.
         */
        const val DUCK = 0.2f

        /** The volume we set the media player when we have audio focus.  */
        const val NORMAL = 1.0f
    }

    object AudioFocus {
        /** We don't have audio focus, and can't duck */
        const val NO_FOCUS_NO_DUCK = "no_focus_no_duck"

        /** We don't have focus, but can duck */
        const val NO_FOCUS_CAN_DUCK = "no_focus_can_duck"

        /** We have full audio focus  */
        const val FOCUSED = "focused"
    }

    internal var context: Context = context.applicationContext

    private val audioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var playOnFocusGain: Boolean = false

    private var audioNoisyReceiverRegistered: Boolean = false

    private var currentAudioFocusState = AudioFocus.NO_FOCUS_NO_DUCK

    private val audioNoisyIntentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)

    override var callbacks: Callbacks? = null

    private val audioNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY == intent.action) {
                Log.d(TAG, "Headphones disconnected.")
                if (isPlaying) {
                    val intent = Intent(context, MusicService::class.java)
                    intent.action = ServiceCommand.COMMAND
                    intent.putExtra(MediaButtonCommand.CMD_NAME, ServiceCommand.PAUSE)
                    context.startService(intent)
                }
            }
        }
    }

    private val onAudioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        Log.d(TAG, String.format("onAudioFocusChange. focusChange: %s", focusChange))
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> currentAudioFocusState = AudioFocus.FOCUSED
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK ->
                // Audio focus was lost, but it's possible to duck (i.e.: play quietly)
                currentAudioFocusState = AudioFocus.NO_FOCUS_CAN_DUCK
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Lost audio focus, but will gain it back (shortly), so note whether
                // playback should resume
                currentAudioFocusState = AudioFocus.NO_FOCUS_NO_DUCK
                playOnFocusGain = isPlaying
            }
            AudioManager.AUDIOFOCUS_LOSS ->
                // Lost audio focus, probably "permanently"
                currentAudioFocusState = AudioFocus.NO_FOCUS_NO_DUCK
        }

        // Update the player state based on the change
        configurePlayerState()
    }

    override fun willResumePlayback(): Boolean {
        // Fixme: This returns true even after manually pausing playback. This should not be the case.
        return playOnFocusGain
    }

    @CallSuper
    override fun pause(fade: Boolean) {
        unregisterAudioNoisyReceiver()
    }

    @CallSuper
    override fun stop() {
        playOnFocusGain = false
        giveUpAudioFocus()
        unregisterAudioNoisyReceiver()
    }

    @CallSuper
    override fun start() {
        playOnFocusGain = true
        tryToGetAudioFocus()
        registerAudioNoisyReceiver()
    }

    private fun tryToGetAudioFocus() {
        Log.d(TAG, "tryToGetAudioFocus")
        val result = audioManager.requestAudioFocus(onAudioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            currentAudioFocusState = AudioFocus.FOCUSED
        } else {
            currentAudioFocusState = AudioFocus.NO_FOCUS_NO_DUCK
        }
    }

    private fun giveUpAudioFocus() {
        Log.d(TAG, "giveUpAudioFocus")
        if (audioManager.abandonAudioFocus(onAudioFocusChangeListener) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            currentAudioFocusState = AudioFocus.NO_FOCUS_NO_DUCK
        }
    }

    private fun registerAudioNoisyReceiver() {
        if (!audioNoisyReceiverRegistered) {
            context.registerReceiver(audioNoisyReceiver, audioNoisyIntentFilter)
            audioNoisyReceiverRegistered = true
        }
    }

    private fun unregisterAudioNoisyReceiver() {
        if (audioNoisyReceiverRegistered) {
            context.unregisterReceiver(audioNoisyReceiver)
            audioNoisyReceiverRegistered = false
        }
    }

    private fun configurePlayerState() {
        Log.d(TAG, String.format("configurePlayerState() called. currentAudioFocusState: %s", currentAudioFocusState))
        if (currentAudioFocusState == AudioFocus.NO_FOCUS_NO_DUCK) {
            // We don't have audio focus and can't duck, so we have to pause
            pause(false)
        } else {
            registerAudioNoisyReceiver()

            if (currentAudioFocusState == AudioFocus.NO_FOCUS_CAN_DUCK) {
                // We're permitted to play, but only if we 'duck', ie: play softly
                Log.d(TAG, "Adjusting volume: DUCK")
                setVolume(Volume.DUCK)
            } else {
                Log.d(TAG, "Adjusting volume: Normal")
                setVolume(Volume.NORMAL)
            }

            // If we were playing when we lost focus, we need to resume playing.
            if (playOnFocusGain) {
                start()
                playOnFocusGain = false
            }
        }
    }

    companion object {
        const val TAG = "LocalPlayback"
    }
}