package com.simplecity.amp_library.ui.screens.queue.menu

import android.content.Context
import com.simplecity.amp_library.data.Repository.AlbumArtistsRepository
import com.simplecity.amp_library.data.Repository.AlbumsRepository
import com.simplecity.amp_library.data.Repository.BlacklistRepository
import com.simplecity.amp_library.playback.MediaManager
import com.simplecity.amp_library.ui.screens.drawer.NavigationEventRelay
import com.simplecity.amp_library.ui.screens.queue.QueueItem
import com.simplecity.amp_library.ui.screens.songs.menu.SongMenuPresenter
import com.simplecity.amp_library.utils.RingtoneManager
import com.simplecity.amp_library.utils.playlists.PlaylistManager
import javax.inject.Inject

class QueueMenuPresenter @Inject constructor(
    context: Context,
    private val mediaManager: MediaManager,
    playlistManager: PlaylistManager,
    blacklistRepository: BlacklistRepository,
    ringtoneManager: RingtoneManager,
    albumArtistsRepository: AlbumArtistsRepository,
    albumsRepository: AlbumsRepository,
    navigationEventRelay: NavigationEventRelay
) : SongMenuPresenter(
    context,
    mediaManager,
    playlistManager,
    blacklistRepository,
    ringtoneManager,
    albumArtistsRepository,
    albumsRepository,
    navigationEventRelay
), QueueMenuContract.Presenter {

    override fun moveToNext(queueItem: QueueItem) {
        mediaManager.moveToNext(queueItem)
    }

    override fun removeQueueItems(queueItems: List<QueueItem>) {
        mediaManager.removeFromQueue(queueItems)
    }

    companion object {
        const val TAG = "QueueMenuPresenter"
    }
}