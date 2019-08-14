package com.simplecity.amp_library.playback

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.PowerManager
import android.text.TextUtils
import android.util.Log
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.utils.LogUtils
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import java.io.File

internal class MediaPlayerPlayback(context: Context) : LocalPlayback(context), MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {

    private var currentMediaPlayer: MediaPlayer? = createMediaPlayer(context)
    private var nextMediaPlayer: MediaPlayer? = null

    override var isInitialized: Boolean = false

    private var isFadingDown: Boolean = false
    private var isFadingUp: Boolean = false
    private var fadeAnimator: ValueAnimator? = null

    override val isPlaying: Boolean
        get() = synchronized(this) {
            if (!isInitialized || isFadingDown) {
                return false
            } else {
                return currentMediaPlayer?.isPlaying ?: false || isFadingUp
            }
        }

    override val duration: Long
        get() = synchronized(this) {
            if (isInitialized) {
                try {
                    return currentMediaPlayer?.duration?.toLong() ?: 0
                } catch (e: IllegalStateException) {
                    Log.e(TAG, "Error in getDuration() of MediaPlayerPlayback: " + e.localizedMessage)
                }

            }
            return 0
        }

    override val position: Long
        get() = synchronized(this) {
            if (isInitialized) {
                try {
                    return currentMediaPlayer?.currentPosition?.toLong() ?: 0
                } catch (e: IllegalStateException) {
                    Log.e(TAG, "Error in getPosition() of MediaPlayerPlayback: " + e.localizedMessage)
                }

            }
            return 0
        }

    override val audioSessionId: Int
        get() = synchronized(this) {
            var sessionId = 0
            if (isInitialized) {
                try {
                    sessionId = currentMediaPlayer?.audioSessionId ?: 0
                } catch (e: IllegalStateException) {
                    Log.e(TAG, "Error in getAudioSessionId() of MediaPlayerPlayback: " + e.localizedMessage)
                }

            }
            return sessionId
        }

    override fun load(song: Song, playWhenReady: Boolean, seekPosition: Long, completion: ((Boolean) -> Unit)?) {
        synchronized(this) {
            fadeAnimator?.cancel()
            currentMediaPlayer?.let { currentMediaPlayer ->
                setDataSourceImpl(currentMediaPlayer, song.path) { success ->
                    isInitialized = success

                    if (isInitialized) {
                        // Invalidate any old 'next data source', will be re-set via external call to setNextDataSource().
                        setNextDataSource(null)

                        if (seekPosition != 0L) {
                            seekTo(seekPosition)
                        }

                        if (playWhenReady) {
                            start()
                        }
                    }
                    completion?.invoke(isInitialized)
                }
            }
        }
    }

    private fun setDataSourceImpl(mediaPlayer: MediaPlayer, path: String, completion: (Boolean) -> Unit) {
        synchronized(this) {

            if (TextUtils.isEmpty(path)) {
                completion(false)
            }
            try {
                mediaPlayer.reset()
                if (path.startsWith("content://")) {
                    val uri = Uri.parse(path)
                    mediaPlayer.setDataSource(context, uri)
                } else {
                    mediaPlayer.setDataSource(Uri.fromFile(File(path)).toString())
                }

                mediaPlayer.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )

                mediaPlayer.setOnPreparedListener {
                    mediaPlayer.setOnPreparedListener(null)
                    completion(true)
                }
                mediaPlayer.prepareAsync()
            } catch (e: Exception) {
                LogUtils.logException(TAG, "setDataSourceImpl failed. Path: [$path]", e)
                completion(false)
            }

            mediaPlayer.setOnCompletionListener(this)
            mediaPlayer.setOnErrorListener(this)
        }
    }

    override fun setNextDataSource(path: String?) {
        synchronized(this) {

            try {
                currentMediaPlayer?.setNextMediaPlayer(null)
            } catch (ignored: IllegalArgumentException) {
                // Nothing to do
            }

            releaseNextMediaPlayer()

            if (TextUtils.isEmpty(path)) {
                return
            }

            nextMediaPlayer = createMediaPlayer(context)
            nextMediaPlayer!!.audioSessionId = audioSessionId

            setDataSourceImpl(nextMediaPlayer!!, path!!) { success ->
                if (success) {
                    try {
                        currentMediaPlayer?.setNextMediaPlayer(nextMediaPlayer)
                    } catch (e: Exception) {
                        LogUtils.logException(TAG, "setNextDataSource failed - failed to call setNextMediaPlayer on currentMediaPlayer", e)
                        releaseNextMediaPlayer()
                    }
                } else {
                    LogUtils.logException(TAG, "setDataSourceImpl failed for path: [$path]. Setting next media player to null", null)
                    releaseNextMediaPlayer()
                }
            }
        }
    }

    private fun releaseNextMediaPlayer() {
        nextMediaPlayer?.release()
        nextMediaPlayer = null
    }

    override fun start() {
        synchronized(this) {
            super.start()

            fadeIn()

            try {
                currentMediaPlayer?.start()
            } catch (e: RuntimeException) {
                LogUtils.logException(TAG, "start() failed", e)
            }

            callbacks?.onPlayStateChanged(this)
        }
    }

    override fun stop() {
        synchronized(this) {
            super.stop()
            if (isInitialized) {
                try {
                    currentMediaPlayer?.reset()
                } catch (e: IllegalStateException) {
                    LogUtils.logException(TAG, "stop() failed", e)
                }

                isInitialized = false
            }

            callbacks?.onPlayStateChanged(this)
        }
    }

    /**
     * You cannot use this player anymore after calling release()
     */
    override fun release() {
        synchronized(this) {
            stop()
            currentMediaPlayer?.release()
        }
    }

    override fun pause(fade: Boolean) {
        synchronized(this) {
            if (fade) {
                fadeOut()
            } else {
                if (isInitialized) {
                    super.pause(fade)
                    try {
                        currentMediaPlayer?.pause()
                    } catch (e: IllegalStateException) {
                        Log.e(TAG, "Error pausing MediaPlayerPlayback: " + e.localizedMessage)
                    }
                    callbacks?.onPlayStateChanged(this)
                }
            }
        }
    }

    override fun seekTo(position: Long) {
        synchronized(this) {
            if (isInitialized) {
                try {
                    currentMediaPlayer?.seekTo(position.toInt())
                } catch (e: IllegalStateException) {
                    Log.e(TAG, "Error seeking MediaPlayerPlayback: " + e.localizedMessage)
                }

            }
        }
    }

    override fun setVolume(volume: Float) {
        synchronized(this) {
            if (isInitialized) {
                try {
                    currentMediaPlayer?.setVolume(volume, volume)
                } catch (e: IllegalStateException) {
                    Log.e(TAG, "Error setting MediaPlayerPlayback volume: " + e.localizedMessage)
                }

            }
        }
    }

    override val resumeWhenSwitched: Boolean = false

    override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean {
        when (what) {
            MediaPlayer.MEDIA_ERROR_SERVER_DIED -> {
                isInitialized = false
                currentMediaPlayer?.release()
                currentMediaPlayer = createMediaPlayer(context)
                callbacks?.onError(this, "Server died")
                return true
            }
            else -> {
            }
        }

        callbacks?.onError(this, "Unknown error")
        return false
    }

    override fun onCompletion(mediaPlayer: MediaPlayer) {
        if (mediaPlayer === currentMediaPlayer && nextMediaPlayer != null) {
            currentMediaPlayer?.release()
            currentMediaPlayer = nextMediaPlayer
            nextMediaPlayer = null
            callbacks?.onTrackEnded(this, true)
        } else {
            callbacks?.onTrackEnded(this, false)
        }
    }

    override fun updateLastKnownStreamPosition() {

    }

    private fun createMediaPlayer(context: Context): MediaPlayer {
        val mediaPlayer = MediaPlayer()
        mediaPlayer.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
        return mediaPlayer
    }

    private fun fadeIn() {
        // Animator needs to run on thread with a looper.
        isFadingUp = true
        Observable.fromCallable {
            val currentVolume = fadeAnimator?.animatedValue as? Float ?: 0f
            fadeAnimator?.cancel()

            setVolume(currentVolume)

            fadeAnimator = ValueAnimator.ofFloat(currentVolume, 1f)
            fadeAnimator!!.duration = 250
            fadeAnimator!!.interpolator = FadeInterpolator(2)
            fadeAnimator!!.addUpdateListener { animation -> setVolume(animation.animatedValue as Float) }
            fadeAnimator!!.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    isFadingUp = false
                }

                override fun onAnimationCancel(animation: Animator?) {
                    super.onAnimationCancel(animation)
                    fadeAnimator!!.removeAllListeners()
                    isFadingUp = false
                }
            })
            fadeAnimator!!.start()
        }
            .subscribeOn(AndroidSchedulers.mainThread())
            .subscribe()
    }

    private fun fadeOut() {
        // Animator needs to run on thread with a looper.
        isFadingDown = true
        Observable.fromCallable {

            val currentVolume = fadeAnimator?.animatedValue as? Float ?: 1f
            fadeAnimator?.cancel()

            fadeAnimator = ValueAnimator.ofFloat(currentVolume, 0f)
            fadeAnimator!!.duration = 150
            fadeAnimator!!.interpolator = FadeInterpolator(1)
            fadeAnimator!!.addUpdateListener { animation -> setVolume(animation.animatedValue as Float) }
            fadeAnimator!!.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    isFadingDown = false
                    pause(false)
                }

                override fun onAnimationCancel(animation: Animator?) {
                    super.onAnimationCancel(animation)
                    fadeAnimator!!.removeAllListeners()
                    isFadingDown = false
                }
            })
            fadeAnimator!!.start()
        }
            .subscribeOn(AndroidSchedulers.mainThread())
            .subscribe()
    }

    /**
     * @param multiplier exaggerates the logarithmic curve.
     * Higher numbers mean the majority of change occurs over the mid-section of the curve.
     * Numbers < 1.0 approximate a 'linear' curve.
     */
    private class FadeInterpolator(private val multiplier: Int) : TimeInterpolator {
        override fun getInterpolation(input: Float): Float {
            return (Math.exp((input * multiplier).toDouble()) * input / Math.exp(multiplier.toDouble())).toFloat()
        }
    }

    companion object {
        private const val TAG = "MediaPlayerPlayback"
    }
}