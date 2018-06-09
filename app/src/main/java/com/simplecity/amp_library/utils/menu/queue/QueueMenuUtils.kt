package com.simplecity.amp_library.utils.menu.queue

import android.support.v7.widget.PopupMenu
import android.support.v7.widget.Toolbar
import com.simplecity.amp_library.R
import com.simplecity.amp_library.model.Playlist
import com.simplecity.amp_library.playback.MediaManager
import com.simplecity.amp_library.ui.queue.QueueItem
import com.simplecity.amp_library.ui.queue.toSongs
import com.simplecity.amp_library.utils.PlaylistUtils
import com.simplecity.amp_library.utils.menu.MenuUtils
import com.simplecity.amp_library.utils.menu.song.SongMenuUtils
import io.reactivex.Single

object QueueMenuUtils {

    fun setupSongMenu(menu: PopupMenu) {
        menu.inflate(R.menu.menu_song)

        // Add playlist menu
        val sub = menu.menu.findItem(R.id.addToPlaylist).subMenu

        menu.menu.findItem(R.id.addToQueue)?.isVisible = false

        PlaylistUtils.createPlaylistMenu(sub)
    }

    fun getQueueMenuClickListener(queueItems: Single<List<QueueItem>>, callbacks: SongMenuUtils.SongListCallbacks, closeCab: () -> Unit): Toolbar.OnMenuItemClickListener {
        return Toolbar.OnMenuItemClickListener { item ->
            when (item.itemId) {
                MediaManager.NEW_PLAYLIST -> {
                    callbacks.newPlaylist(queueItems.map { it.toSongs() })
                    closeCab()
                    return@OnMenuItemClickListener true
                }
                MediaManager.PLAYLIST_SELECTED -> {
                    callbacks.playlistSelected(item.intent.getSerializableExtra(PlaylistUtils.ARG_PLAYLIST) as Playlist, queueItems.map { it.toSongs() })
                    closeCab()
                    return@OnMenuItemClickListener true
                }
                R.id.delete -> {
                    callbacks.showDeleteDialog(queueItems.map { it.toSongs() })
                    closeCab()
                    return@OnMenuItemClickListener true
                }
                R.id.queue_remove -> {
                    callbacks.removeQueueItems(queueItems)
                    closeCab()
                    return@OnMenuItemClickListener true
                }
            }
            false
        }
    }

    fun getQueueMenuClickListener(queueItem: QueueItem, callbacks: SongMenuUtils.SongCallbacks): PopupMenu.OnMenuItemClickListener {
        return PopupMenu.OnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.playNext -> {
                    callbacks.moveToNext(queueItem)
                    return@OnMenuItemClickListener true
                }
                MediaManager.NEW_PLAYLIST -> {
                    callbacks.newPlaylist(queueItem.song)
                    return@OnMenuItemClickListener true
                }
                MediaManager.PLAYLIST_SELECTED -> {
                    callbacks.playlistSelected(item.intent.getSerializableExtra(PlaylistUtils.ARG_PLAYLIST) as Playlist, queueItem.song)
                    return@OnMenuItemClickListener true
                }
                R.id.addToQueue -> {
                    callbacks.addToQueue(queueItem.song)
                    return@OnMenuItemClickListener true
                }
                R.id.editTags -> {
                    callbacks.showTagEditor(queueItem.song)
                    return@OnMenuItemClickListener true
                }
                R.id.share -> {
                    callbacks.shareSong(queueItem.song)
                    return@OnMenuItemClickListener true
                }
                R.id.ringtone -> {
                    callbacks.setRingtone(queueItem.song)
                    return@OnMenuItemClickListener true
                }
                R.id.songInfo -> {
                    callbacks.showBiographyDialog(queueItem.song)
                    return@OnMenuItemClickListener true
                }
                R.id.blacklist -> {
                    MenuUtils.blacklist(queueItem.song)
                    return@OnMenuItemClickListener true
                }
                R.id.delete -> {
                    callbacks.showDeleteDialog(queueItem.song)
                    return@OnMenuItemClickListener true
                }
                R.id.remove -> {
                    callbacks.removeQueueItem(queueItem)
                    return@OnMenuItemClickListener true
                }
            }
            false
        }
    }
}
