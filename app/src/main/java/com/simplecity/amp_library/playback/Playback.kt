package com.simplecity.amp_library.playback

import com.simplecity.amp_library.model.Song

interface Playback {

    var isInitialized: Boolean

    val isPlaying: Boolean

    val position: Long

    val audioSessionId: Int

    val duration: Long

    var callbacks: Callbacks?

    fun setVolume(volume: Float)

    fun load(song: Song, playWhenReady: Boolean, seekPosition: Long, completion: ((Boolean) -> Unit)?)

    fun willResumePlayback(): Boolean

    fun setNextDataSource(path: String?)

    fun release()

    fun seekTo(position: Long)

    fun pause(fade: Boolean)

    fun stop()

    fun start()

    fun updateLastKnownStreamPosition()

    val resumeWhenSwitched: Boolean

    interface Callbacks {

        /**
         * @param trackDidChange true if the underlying [Playback] already handled the transition to next track.
         */
        fun onTrackEnded(playback: Playback, trackDidChange: Boolean)

        fun onPlayStateChanged(playback: Playback)

        fun onError(playback: Playback, message: String)
    }
}
