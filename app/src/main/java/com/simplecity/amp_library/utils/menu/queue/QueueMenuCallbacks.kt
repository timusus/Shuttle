package com.simplecity.amp_library.utils.menu.queue

import com.simplecity.amp_library.ui.screens.queue.QueueItem
import com.simplecity.amp_library.utils.menu.song.SongsMenuCallbacks
import io.reactivex.Single

interface QueueMenuCallbacks : SongsMenuCallbacks {

    fun moveToNext(queueItem: QueueItem)

    fun removeQueueItems(queueItems: List<QueueItem>)
}


fun QueueMenuCallbacks.removeQueueItem(queueItem: QueueItem) {
    removeQueueItems(listOf(queueItem))
}


fun QueueMenuCallbacks.removeQueueItems(queueItems: Single<List<QueueItem>>) {
    transform(queueItems) { queueItems -> removeQueueItems(queueItems) }
}