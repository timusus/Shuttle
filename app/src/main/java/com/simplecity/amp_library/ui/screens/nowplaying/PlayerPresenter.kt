package com.simplecity.amp_library.ui.screens.nowplaying

import android.app.Activity
import android.content.Context
import android.content.IntentFilter
import com.cantrowitz.rxbroadcast.RxBroadcast
import com.simplecity.amp_library.ShuttleApplication
import com.simplecity.amp_library.playback.MediaManager
import com.simplecity.amp_library.playback.PlaybackMonitor
import com.simplecity.amp_library.playback.constants.InternalIntents
import com.simplecity.amp_library.ui.common.Presenter
import com.simplecity.amp_library.ui.screens.songs.menu.SongMenuPresenter
import com.simplecity.amp_library.utils.LogUtils
import com.simplecity.amp_library.utils.SettingsManager
import com.simplecity.amp_library.utils.ShuttleUtils
import com.simplecity.amp_library.utils.menu.song.SongsMenuCallbacks
import com.simplecity.amp_library.utils.playlists.FavoritesPlaylistManager
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class PlayerPresenter @Inject constructor(
    private val context: Context,
    private val mediaManager: MediaManager,
    private val playbackMonitor: PlaybackMonitor,
    private val settingsManager: SettingsManager,
    private val favoritesPlaylistManager: FavoritesPlaylistManager,
    private val songMenuPresenter: SongMenuPresenter
) : Presenter<PlayerView>(),
    SongsMenuCallbacks by songMenuPresenter {

    private var startSeekPos: Long = 0
    private var lastSeekEventTime: Long = 0

    private var currentPlaybackTime: Long = 0
    private var currentPlaybackTimeVisible: Boolean = false

    private var isFavoriteDisposable: Disposable? = null

    override fun bindView(view: PlayerView) {
        super.bindView(view)

        songMenuPresenter.bindView(view)

        updateTrackInfo()
        updateShuffleMode()
        updatePlaystate()
        updateRepeatMode()

        addDisposable(
            playbackMonitor.progressObservable
                //.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { progress -> view.setSeekProgress((progress!! * 1000).toInt()) },
                    { error -> LogUtils.logException(TAG, "PlayerPresenter: Error updating seek progress", error) })
        )

        addDisposable(
            playbackMonitor.currentTimeObservable
                //.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { pos -> refreshTimeText(pos!! / 1000) },
                    { error -> LogUtils.logException(TAG, "PlayerPresenter: Error refreshing time text", error) })
        )

        addDisposable(
            Flowable.interval(500, TimeUnit.MILLISECONDS)
                .onBackpressureDrop()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { setCurrentTimeVisibility(mediaManager.isPlaying || !currentPlaybackTimeVisible) },
                    { error -> LogUtils.logException(TAG, "PlayerPresenter: Error emitting current time", error) })
        )

        val filter = IntentFilter()
        filter.addAction(InternalIntents.META_CHANGED)
        filter.addAction(InternalIntents.QUEUE_CHANGED)
        filter.addAction(InternalIntents.PLAY_STATE_CHANGED)
        filter.addAction(InternalIntents.SHUFFLE_CHANGED)
        filter.addAction(InternalIntents.REPEAT_CHANGED)
        filter.addAction(InternalIntents.SERVICE_CONNECTED)

        addDisposable(
            RxBroadcast.fromBroadcast(context, filter)
                .toFlowable(BackpressureStrategy.LATEST)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { intent ->
                        when (intent.action) {
                            InternalIntents.META_CHANGED -> updateTrackInfo()
                            InternalIntents.QUEUE_CHANGED -> updateTrackInfo()
                            InternalIntents.PLAY_STATE_CHANGED -> {
                                updateTrackInfo()
                                updatePlaystate()
                            }
                            InternalIntents.SHUFFLE_CHANGED -> {
                                updateTrackInfo()
                                updateShuffleMode()
                            }
                            InternalIntents.REPEAT_CHANGED -> updateRepeatMode()
                            InternalIntents.SERVICE_CONNECTED -> {
                                updateTrackInfo()
                                updatePlaystate()
                                updateShuffleMode()
                                updateRepeatMode()
                            }
                        }
                    },
                    { error -> LogUtils.logException(TAG, "PlayerPresenter: Error sending broadcast", error) }
                )
        )
    }

    override fun unbindView(view: PlayerView) {
        super.unbindView(view)

        songMenuPresenter.unbindView(view)
    }

    private fun refreshTimeText(playbackTime: Long) {
        if (playbackTime != currentPlaybackTime) {
            view?.currentTimeChanged(playbackTime)
            if (settingsManager.displayRemainingTime()) {
                view?.totalTimeChanged(-(mediaManager.duration / 1000 - playbackTime))
            }
        }
        currentPlaybackTime = playbackTime
    }

    private fun setCurrentTimeVisibility(visible: Boolean) {
        if (visible != currentPlaybackTimeVisible) {
            view?.currentTimeVisibilityChanged(visible)
        }
        currentPlaybackTimeVisible = visible
    }

    private fun updateFavorite(isFavorite: Boolean) {
        view?.favoriteChanged(isFavorite)
    }

    fun updateTrackInfo() {
        view?.trackInfoChanged(mediaManager.song)
        view?.queueChanged(mediaManager.queuePosition + 1, mediaManager.queue.size)
        view?.currentTimeChanged(mediaManager.position / 1000)
        updateRemainingTime()

        isFavoriteDisposable?.dispose()
        isFavoriteDisposable = favoritesPlaylistManager.isFavorite(mediaManager.song)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { isFavorite -> updateFavorite(isFavorite) },
                { error -> LogUtils.logException(TAG, "updateTrackInfo error", error) }
            )

        addDisposable(isFavoriteDisposable!!)
    }

    private fun updatePlaystate() {
        view?.playbackChanged(mediaManager.isPlaying)
    }

    private fun updateShuffleMode() {
        view?.repeatChanged(mediaManager.repeatMode)
        view?.shuffleChanged(mediaManager.shuffleMode)
    }

    private fun updateRepeatMode() {
        view?.repeatChanged(mediaManager.repeatMode)
    }

    fun togglePlayback() {
        mediaManager.togglePlayback()
    }

    fun toggleFavorite() {
        mediaManager.toggleFavorite()
    }

    fun skip() {
        mediaManager.next()
    }

    fun prev(force: Boolean) {
        mediaManager.previous(force)
    }

    fun toggleShuffle() {
        mediaManager.toggleShuffleMode()
        updateShuffleMode()
    }

    fun toggleRepeat() {
        mediaManager.cycleRepeat()
        updateRepeatMode()
    }

    fun seekTo(progress: Int) {
        mediaManager.seekTo(mediaManager.duration * progress / 1000)
    }

    fun scanForward(repeatCount: Int, delta: Long) {
        var delta = delta
        if (repeatCount == 0) {
            startSeekPos = mediaManager.position
            lastSeekEventTime = 0
        } else {
            if (delta < 5000) {
                // seek at 10x speed for the first 5 seconds
                delta *= 10
            } else {
                // seek at 40x after that
                delta = 50000 + (delta - 5000) * 40
            }
            var newpos = startSeekPos + delta
            val duration = mediaManager.duration
            if (newpos >= duration) {
                // move to next track
                mediaManager.next()
                startSeekPos -= duration // is OK to go negative
                newpos -= duration
            }
            if (delta - lastSeekEventTime > 250 || repeatCount < 0) {
                mediaManager.seekTo(newpos)
                lastSeekEventTime = delta
            }
        }
    }

    fun scanBackward(repeatCount: Int, delta: Long) {
        var delta = delta
        if (repeatCount == 0) {
            startSeekPos = mediaManager.position
            lastSeekEventTime = 0
        } else {
            if (delta < 5000) {
                // seek at 10x speed for the first 5 seconds
                delta *= 10
            } else {
                // seek at 40x after that
                delta = 50000 + (delta - 5000) * 40
            }
            var newpos = startSeekPos - delta
            if (newpos < 0) {
                // move to previous track
                mediaManager.previous(true)
                val duration = mediaManager.duration
                startSeekPos += duration
                newpos += duration
            }
            if (delta - lastSeekEventTime > 250 || repeatCount < 0) {
                mediaManager.seekTo(newpos)
                lastSeekEventTime = delta
            }
        }
    }

    fun showLyrics() {
        view?.showLyricsDialog()
    }

    fun editTagsClicked(activity: Activity) {
        if (!ShuttleUtils.isUpgraded(activity.applicationContext as ShuttleApplication, settingsManager)) {
            view?.showUpgradeDialog()
        } else {
            view?.presentTagEditorDialog(mediaManager.song!!)
        }
    }

    fun songInfoClicked() {
        val song = mediaManager.song
        if (song != null) {
            view?.presentSongInfoDialog(song)
        }
    }

    fun updateRemainingTime() {
        if (settingsManager.displayRemainingTime()) {
            view?.totalTimeChanged(-((mediaManager.duration - mediaManager.position) / 1000))
        } else {
            view?.totalTimeChanged(mediaManager.duration / 1000)
        }
    }

    fun shareClicked() {
        view?.shareSong(mediaManager.song!!)
    }

    companion object {

        private const val TAG = "PlayerPresenter"
    }
}
