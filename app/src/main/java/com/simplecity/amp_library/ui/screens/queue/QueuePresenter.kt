package com.simplecity.amp_library.ui.screens.queue

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.MenuItem
import com.cantrowitz.rxbroadcast.RxBroadcast
import com.simplecity.amp_library.ShuttleApplication
import com.simplecity.amp_library.model.Playlist
import com.simplecity.amp_library.playback.MediaManager
import com.simplecity.amp_library.playback.constants.InternalIntents
import com.simplecity.amp_library.ui.common.Presenter
import com.simplecity.amp_library.ui.screens.queue.QueueContract.View
import com.simplecity.amp_library.ui.screens.queue.menu.QueueMenuContract
import com.simplecity.amp_library.ui.screens.queue.menu.QueueMenuPresenter
import com.simplecity.amp_library.utils.SettingsManager
import com.simplecity.amp_library.utils.playlists.PlaylistManager
import io.reactivex.BackpressureStrategy
import io.reactivex.android.schedulers.AndroidSchedulers
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class QueuePresenter @Inject constructor(
    private val application: ShuttleApplication,
    private val mediaManager: MediaManager,
    private val settingsManager: SettingsManager,
    private val playlistManager: PlaylistManager,
    private val queueMenuPresenter: QueueMenuPresenter
) : Presenter<View>(),
    QueueContract.Presenter,
    QueueMenuContract.Presenter by queueMenuPresenter {

    override fun bindView(view: View) {
        super.bindView(view)

        queueMenuPresenter.bindView(view)

        var filter = IntentFilter()
        filter.addAction(InternalIntents.META_CHANGED)
        addDisposable(RxBroadcast.fromBroadcast(application, filter)
            .startWith(Intent(InternalIntents.QUEUE_CHANGED))
            .toFlowable(BackpressureStrategy.LATEST)
            .debounce(150, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { intent ->
                val queueView = getView()
                queueView?.updateQueuePosition(mediaManager.queuePosition)
            })

        filter = IntentFilter()
        filter.addAction(InternalIntents.REPEAT_CHANGED)
        filter.addAction(InternalIntents.SHUFFLE_CHANGED)
        filter.addAction(InternalIntents.QUEUE_CHANGED)
        filter.addAction(InternalIntents.SERVICE_CONNECTED)
        addDisposable(RxBroadcast.fromBroadcast(application, filter)
            .startWith(Intent(InternalIntents.QUEUE_CHANGED))
            .toFlowable(BackpressureStrategy.LATEST)
            .debounce(150, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { intent ->
                loadData()
            })
    }

    override fun unbindView(view: View) {
        super.unbindView(view)

        queueMenuPresenter.unbindView(view)
    }

    override fun loadData() {
        view?.setData(mediaManager.queue, mediaManager.queuePosition)
    }

    override fun play(queueItem: QueueItem) {
        val index = mediaManager.queue.indexOf(queueItem)
        if (index >= 0) {
            mediaManager.queuePosition = index
            view?.updateQueuePosition(index)
        }
    }

    override fun saveQueue(context: Context) {
        view?.showCreatePlaylistDialog(mediaManager.queue.toSongs())
    }

    override fun saveQueue(context: Context, item: MenuItem) {
        val playlist = item.intent.getSerializableExtra(PlaylistManager.ARG_PLAYLIST) as Playlist
        playlistManager.addToPlaylist(playlist, mediaManager.queue.toSongs(), null)
    }

    override fun clearQueue() {
        mediaManager.clearQueue()
    }

    override fun moveQueueItem(from: Int, to: Int) {
        mediaManager.moveQueueItem(from, to)
    }

    override fun setQueueSwipeLocked(locked: Boolean) {
        settingsManager.setQueueSwipeLocked(locked)
        view?.setQueueSwipeLocked(locked)
    }

    companion object {
        const val TAG = "QueuePresenter"
    }

}
