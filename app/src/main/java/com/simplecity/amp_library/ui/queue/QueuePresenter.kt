package com.simplecity.amp_library.ui.queue

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.MenuItem
import com.cantrowitz.rxbroadcast.RxBroadcast
import com.simplecity.amp_library.ShuttleApplication
import com.simplecity.amp_library.model.Playlist
import com.simplecity.amp_library.playback.MediaManager
import com.simplecity.amp_library.playback.constants.InternalIntents
import com.simplecity.amp_library.ui.presenters.Presenter
import com.simplecity.amp_library.ui.queue.QueueContract.View
import com.simplecity.amp_library.utils.PlaylistUtils
import io.reactivex.BackpressureStrategy
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class QueuePresenter @Inject
constructor(internal var mediaManager: MediaManager) : Presenter<View>(), QueueContract.Presenter {

    override fun bindView(view: View) {
        super.bindView(view)

        var filter = IntentFilter()
        filter.addAction(InternalIntents.META_CHANGED)
        addDisposable(RxBroadcast.fromBroadcast(ShuttleApplication.getInstance(), filter)
            .startWith(Intent(InternalIntents.QUEUE_CHANGED))
            .toFlowable(BackpressureStrategy.LATEST)
            .debounce(150, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { intent ->
                val queueView = getView()
                queueView?.updateQueuePosition(mediaManager.getQueuePosition())
            })

        filter = IntentFilter()
        filter.addAction(InternalIntents.REPEAT_CHANGED)
        filter.addAction(InternalIntents.SHUFFLE_CHANGED)
        filter.addAction(InternalIntents.QUEUE_CHANGED)
        filter.addAction(InternalIntents.SERVICE_CONNECTED)
        addDisposable(RxBroadcast.fromBroadcast(ShuttleApplication.getInstance(), filter)
            .startWith(Intent(InternalIntents.QUEUE_CHANGED))
            .toFlowable(BackpressureStrategy.LATEST)
            .debounce(150, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { intent ->
                loadData()
            })
    }

    override fun saveQueue(context: Context) {
        PlaylistUtils.createPlaylistDialog(context, mediaManager.getQueue().toSongs(), null)
    }

    override fun saveQueue(context: Context, item: MenuItem) {
        val playlist = item.intent.getSerializableExtra(PlaylistUtils.ARG_PLAYLIST) as Playlist
        PlaylistUtils.addToPlaylist(context, playlist, mediaManager.getQueue().toSongs(), null)
    }

    override fun clearQueue() {
        mediaManager.clearQueue()
    }

    override fun removeFromQueue(queueItem: QueueItem) {
        mediaManager.removeFromQueue(queueItem)
        view?.onRemovedFromQueue(queueItem)
    }

    override fun removeFromQueue(queueItems: Single<List<QueueItem>>) {
        addDisposable(queueItems.subscribe { queueItems, error ->
            mediaManager.removeFromQueue(queueItems)
            view?.onRemovedFromQueue(queueItems)
        })
    }

    override fun loadData() {
        view?.setData(mediaManager.getQueue(), mediaManager.getQueuePosition())
    }

    override fun onQueueItemClick(queueItem: QueueItem) {
        val index = mediaManager.getQueue().indexOf(queueItem)
        if (index >= 0) {
            mediaManager.setQueuePosition(index)
            view?.updateQueuePosition(index)
        }
    }

    companion object {
        const val TAG = "QueuePresenter"
    }

}
