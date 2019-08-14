package com.simplecity.amp_library.playback

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.transcode.BitmapBytesTranscoder
import com.bumptech.glide.request.animation.GlideAnimation
import com.bumptech.glide.request.target.SimpleTarget
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadOptions
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaStatus
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.google.android.gms.common.images.WebImage
import com.simplecity.amp_library.R
import com.simplecity.amp_library.glide.utils.GlideUtils
import com.simplecity.amp_library.http.HttpServer
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.playback.Playback.Callbacks
import com.simplecity.amp_library.utils.LogUtils
import com.simplecity.amp_library.utils.ShuttleUtils
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.json.JSONException
import java.io.ByteArrayOutputStream

class CastPlayback(context: Context, castSession: CastSession) : Playback {

    private val applicationContext = context.applicationContext

    private val remoteMediaClient: RemoteMediaClient = castSession.remoteMediaClient
    private val remoteMediaClientCallback: RemoteMediaClient.Callback

    private var currentPosition = 0L

    private var currentSong: Song? = null

    private var playerState: Int? = MediaStatus.PLAYER_STATE_UNKNOWN;

    // remoteMediaClient.isPlaying() returns true momentarily after it is paused, so we use this to track whether
    // it really is playing, based on calls to play(), pause(), stop() and load()
    private var isMeantToBePlaying = false

    init {
        remoteMediaClientCallback = CastMediaClientCallback()
    }

    override var isInitialized: Boolean = false

    override val isPlaying: Boolean
        get() {
            return remoteMediaClient.isPlaying || isMeantToBePlaying
        }

    override val position: Long
        get() {
            if (remoteMediaClient.approximateStreamPosition == 0L) {
                return if (currentPosition <= duration) currentPosition else 0L
            }
            return remoteMediaClient.approximateStreamPosition
        }

    override val audioSessionId: Int
        get() = 0

    override val duration: Long
        get() {
            return remoteMediaClient.streamDuration
        }

    override var callbacks: Callbacks? = null

    override fun setVolume(volume: Float) {
        // Nothing to do
    }

    override fun load(song: Song, playWhenReady: Boolean, seekPosition: Long, completion: ((Boolean) -> Unit)?) {

        HttpServer.getInstance().start()
        HttpServer.getInstance().serveAudio(song.path)
        HttpServer.getInstance().clearImage()

        val metadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK)
        metadata.putString(MediaMetadata.KEY_ALBUM_ARTIST, song.albumArtistName)
        metadata.putString(MediaMetadata.KEY_ALBUM_TITLE, song.albumName)
        metadata.putString(MediaMetadata.KEY_TITLE, song.name)
        metadata.addImage(WebImage(Uri.parse("http://" + ShuttleUtils.getIpAddr(applicationContext) + ":5000" + "/image/" + song.id)))

        val mediaInfo = MediaInfo.Builder("http://" + ShuttleUtils.getIpAddr(applicationContext) + ":5000" + "/audio/" + song.id)
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType("audio/*")
            .setMetadata(metadata)
            .build()

        currentSong = song
        currentPosition = seekPosition

        fun performLoad() {
            remoteMediaClient.registerCallback(remoteMediaClientCallback)

            remoteMediaClient.load(
                mediaInfo, MediaLoadOptions.Builder()
                    .setPlayPosition(seekPosition)
                    .setAutoplay(playWhenReady)
                    .build()
            )

            if (playWhenReady) {
                isMeantToBePlaying = true
            }

            isInitialized = true

            completion?.invoke(true)
        }

        Glide.with(applicationContext).load(song)
            .asBitmap()
            .transcode(BitmapBytesTranscoder(), ByteArray::class.java)
            .placeholder(R.drawable.ic_placeholder_dark_large)
            .into(object : SimpleTarget<ByteArray>() {
                override fun onResourceReady(resource: ByteArray, glideAnimation: GlideAnimation<in ByteArray>?) {
                    HttpServer.getInstance().serveImage(resource)
                    performLoad()
                }

                @SuppressLint("CheckResult")
                override fun onLoadFailed(e: Exception?, errorDrawable: Drawable?) {
                    super.onLoadFailed(e, errorDrawable)

                    Single.fromCallable {
                        errorDrawable?.let {
                            val outputStream = ByteArrayOutputStream()
                            val bitmap = GlideUtils.drawableToBitmap(errorDrawable)
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                            HttpServer.getInstance().serveImage(outputStream.toByteArray())
                        }
                    }
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({
                            performLoad()
                        }, { error -> LogUtils.logException(TAG, "Failed to load error drawable", error) })
                }
            })
    }

    override fun willResumePlayback(): Boolean {
        return false
    }

    override fun setNextDataSource(path: String?) {
        // Nothing to do
    }

    override fun release() {
        HttpServer.getInstance().stop()
    }

    override fun seekTo(position: Long) {
        currentPosition = position
        try {
            if (remoteMediaClient.hasMediaSession()) {
                remoteMediaClient.seek(position)
            } else {
                currentSong?.let { currentSong ->
                    load(currentSong, true, position, null)
                } ?: Log.e(TAG, "Seek failed, no remote media session")
            }
        } catch (e: JSONException) {
            LogUtils.logException(TAG, "Exception pausing cast playback", e)
            if (callbacks != null) {
                callbacks?.onError(this, e.message ?: "Unspecified error")
            }
        }
    }

    override fun pause(fade: Boolean) {
        isMeantToBePlaying = false
        try {
            if (remoteMediaClient.hasMediaSession()) {
                currentPosition = remoteMediaClient.approximateStreamPosition
                remoteMediaClient.pause()
            } else {
                Log.e(TAG, "Pause failed, no remote media session")
            }
        } catch (e: JSONException) {
            LogUtils.logException(TAG, "Exception pausing cast playback", e)
            callbacks?.onError(this, e.message ?: "Unspecified error")
        }
    }

    override fun stop() {
        isMeantToBePlaying = false

        if (remoteMediaClient.hasMediaSession()) {
            currentPosition = remoteMediaClient.approximateStreamPosition
            remoteMediaClient.stop()
        }

        remoteMediaClient.unregisterCallback(remoteMediaClientCallback)

        release()
    }

    override fun start() {
        isMeantToBePlaying = true

        if (remoteMediaClient.hasMediaSession() && !remoteMediaClient.isPlaying) {
            currentPosition = remoteMediaClient.approximateStreamPosition
            remoteMediaClient.play()
        } else {
            Log.e(TAG, "start() failed.. hasMediaSession " + remoteMediaClient.hasMediaSession())
        }
    }

    override fun updateLastKnownStreamPosition() {
        currentPosition = position
    }

    override val resumeWhenSwitched: Boolean = true

    private fun updatePlaybackState() {
        val playerState = remoteMediaClient.playerState
        if (playerState != this.playerState) {
            // Convert the remote playback states to media playback states.
            when (playerState) {
                MediaStatus.PLAYER_STATE_IDLE -> {
                    val idleReason = remoteMediaClient.idleReason
                    Log.d(TAG, "onRemoteMediaPlayerStatusUpdated... IDLE, reason: $idleReason")
                    if (idleReason == MediaStatus.IDLE_REASON_FINISHED) {
                        currentPosition = 0L
                        Log.i(TAG, "Calling onTrackEnded")
                        callbacks?.onTrackEnded(this, false)
                    }
                }
                MediaStatus.PLAYER_STATE_PLAYING -> {
                    Log.d(TAG, "onRemoteMediaPlayerStatusUpdated.. PLAYING")
                    callbacks?.onPlayStateChanged(this)
                }
                MediaStatus.PLAYER_STATE_PAUSED -> {
                    Log.d(TAG, "onRemoteMediaPlayerStatusUpdated.. PAUSED")
                    callbacks?.onPlayStateChanged(this)
                }
                else -> {
                    Log.d(TAG, "State default : $playerState")
                }
            }
        }
        this.playerState = playerState
    }

    companion object {
        const val TAG = "CastPlayback"
    }

    private inner class CastMediaClientCallback : RemoteMediaClient.Callback() {

        override fun onMetadataUpdated() {
            Log.d(TAG, "RemoteMediaClient.onMetadataUpdated")
        }

        override fun onStatusUpdated() {
            Log.d(TAG, "RemoteMediaClient.onStatusUpdated")
            updatePlaybackState()
        }
    }
}