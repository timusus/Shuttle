package com.simplecity.amp_library.ui.screens.queue

import android.content.Context
import android.view.MenuItem
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.ui.screens.tagger.TaggerDialog
import com.simplecity.amp_library.ui.dialog.DeleteDialog
import com.simplecity.amp_library.ui.screens.queue.menu.QueueMenuContract

interface QueueContract {

    interface View : QueueMenuContract.View {

        fun setData(queueItems: List<QueueItem>, position: Int)

        fun updateQueuePosition(queuePosition: Int)

        fun showToast(message: String, duration: Int)

        fun showTaggerDialog(taggerDialog: TaggerDialog)

        fun showDeleteDialog(deleteDialog: DeleteDialog)

        fun onRemovedFromQueue(queueItem: QueueItem)

        fun onRemovedFromQueue(queueItems: List<QueueItem>)

        fun showUpgradeDialog()

        fun setQueueSwipeLocked(locked: Boolean)

        fun showCreatePlaylistDialog(songs: List<Song>)
    }

    interface Presenter {

        fun saveQueue(context: Context)

        fun saveQueue(context: Context, item: MenuItem)

        fun clearQueue()

        fun moveQueueItem(from: Int, to: Int)

        fun loadData()

        fun play(queueItem: QueueItem)

        fun setQueueSwipeLocked(locked: Boolean)
    }
}