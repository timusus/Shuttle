package com.simplecity.amp_library.ui.queue

import android.content.Context
import android.view.MenuItem
import com.simplecity.amp_library.tagger.TaggerDialog
import com.simplecity.amp_library.ui.dialog.DeleteDialog
import io.reactivex.Single

interface QueueContract {

    interface View {

        fun setData(queueItems: List<QueueItem>, position: Int)

        fun updateQueuePosition(queuePosition: Int)

        fun showToast(message: String, duration: Int)

        fun showTaggerDialog(taggerDialog: TaggerDialog)

        fun showDeleteDialog(deleteDialog: DeleteDialog)

        fun onRemovedFromQueue(queueItem: QueueItem)

        fun onRemovedFromQueue(queueItems: List<QueueItem>)

        fun showUpgradeDialog()
    }

    interface Presenter {

        fun saveQueue(context: Context)

        fun saveQueue(context: Context, item: MenuItem)

        fun clearQueue()

        fun removeFromQueue(queueItem: QueueItem)

        fun removeFromQueue(queueItems: Single<List<QueueItem>>)

        fun loadData()

        fun onQueueItemClick(queueItem: QueueItem)
    }
}