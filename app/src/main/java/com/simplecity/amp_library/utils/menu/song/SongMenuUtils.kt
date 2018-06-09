package com.simplecity.amp_library.utils.menu.song

import android.support.v7.widget.PopupMenu
import android.support.v7.widget.Toolbar
import com.simplecity.amp_library.R
import com.simplecity.amp_library.model.Playlist
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.playback.MediaManager
import com.simplecity.amp_library.ui.queue.QueueItem
import com.simplecity.amp_library.utils.PlaylistUtils
import com.simplecity.amp_library.utils.menu.MenuUtils
import io.reactivex.Single

object SongMenuUtils {

    interface SongCallbacks {

        fun playNext(song: Song)

        fun moveToNext(queueItem: QueueItem)

        fun newPlaylist(song: Song)

        fun playlistSelected(playlist: Playlist, song: Song)

        fun onPlaylistItemsInserted(songs: Single<List<Song>>)

        fun onPlaylistItemsInserted(songs: List<Song>)

        fun addToQueue(song: Song)

        fun onQueueItemInserted(message: String)

        fun showBiographyDialog(song: Song)

        fun showTagEditor(song: Song)

        fun showToast(message: String)

        fun shareSong(song: Song)

        fun setRingtone(song: Song)

        fun removeQueueItem(queueItem: QueueItem)

        fun removeSong(song: Song)

        fun showDeleteDialog(song: Song)
    }

    interface SongListCallbacks {

        fun playNext(songs: Single<List<Song>>)

        fun newPlaylist(songs: Single<List<Song>>)

        fun playlistSelected(playlist: Playlist, songsSingle: Single<List<Song>>)

        fun onPlaylistItemsInserted(songs: Single<List<Song>>)

        fun onPlaylistItemsInserted(songs: List<Song>)

        fun addToQueue(songs: Single<List<Song>>)

        fun blacklist(songs: Single<List<Song>>)

        fun onQueueItemInserted(message: String)

        fun showToast(message: String)

        fun removeQueueItems(queueItems: Single<List<QueueItem>>)

        fun removeQueueItem(queueItem: QueueItem)

        fun showDeleteDialog(songs: Single<List<Song>>)
    }

    fun setupSongMenu(menu: PopupMenu, showRemoveButton: Boolean) {
        menu.inflate(R.menu.menu_song)

        if (!showRemoveButton) {
            menu.menu.findItem(R.id.remove).isVisible = false
        }

        // Add playlist menu
        val sub = menu.menu.findItem(R.id.addToPlaylist).subMenu
        PlaylistUtils.createPlaylistMenu(sub)
    }

    fun getSongMenuClickListener(songsSingle: Single<List<Song>>, callbacks: SongListCallbacks): Toolbar.OnMenuItemClickListener {
        return Toolbar.OnMenuItemClickListener { item ->
            when (item.itemId) {
                MediaManager.NEW_PLAYLIST -> {
                    callbacks.newPlaylist(songsSingle)
                    return@OnMenuItemClickListener true
                }
                MediaManager.PLAYLIST_SELECTED -> {
                    callbacks.playlistSelected(item.intent.getSerializableExtra(PlaylistUtils.ARG_PLAYLIST) as Playlist, songsSingle)
                    return@OnMenuItemClickListener true
                }
                R.id.playNext -> {
                    callbacks.playNext(songsSingle)
                    return@OnMenuItemClickListener true
                }
                R.id.addToQueue -> {
                    callbacks.addToQueue(songsSingle)
                    return@OnMenuItemClickListener true
                }
                R.id.blacklist -> {
                    callbacks.blacklist(songsSingle)
                    return@OnMenuItemClickListener true
                }
                R.id.delete -> {
                    callbacks.showDeleteDialog(songsSingle)
                    return@OnMenuItemClickListener true
                }
            }
            false
        }
    }

    fun getSongMenuClickListener(song: Song, callbacks: SongCallbacks): PopupMenu.OnMenuItemClickListener {
        return PopupMenu.OnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.playNext -> {
                    callbacks.playNext(song)
                    return@OnMenuItemClickListener true
                }
                MediaManager.NEW_PLAYLIST -> {
                    callbacks.newPlaylist(song)
                    return@OnMenuItemClickListener true
                }
                MediaManager.PLAYLIST_SELECTED -> {
                    callbacks.playlistSelected(item.intent.getSerializableExtra(PlaylistUtils.ARG_PLAYLIST) as Playlist, song)
                    return@OnMenuItemClickListener true
                }
                R.id.addToQueue -> {
                    callbacks.addToQueue(song)
                    return@OnMenuItemClickListener true
                }
                R.id.editTags -> {
                    callbacks.showTagEditor(song)
                    return@OnMenuItemClickListener true
                }
                R.id.share -> {
                    callbacks.shareSong(song)
                    return@OnMenuItemClickListener true
                }
                R.id.ringtone -> {
                    callbacks.setRingtone(song)
                    return@OnMenuItemClickListener true
                }
                R.id.songInfo -> {
                    callbacks.showBiographyDialog(song)
                    return@OnMenuItemClickListener true
                }
                R.id.blacklist -> {
                    MenuUtils.blacklist(song)
                    return@OnMenuItemClickListener true
                }
                R.id.delete -> {
                    callbacks.showDeleteDialog(song)
                    return@OnMenuItemClickListener true
                }
                R.id.remove -> {
                    callbacks.removeSong(song)
                    return@OnMenuItemClickListener true
                }
            }
            false
        }
    }
}