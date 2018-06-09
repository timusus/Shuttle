package com.simplecity.amp_library.utils.menu.playlist

import android.support.annotation.StringRes
import android.support.v7.widget.PopupMenu
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import com.simplecity.amp_library.R
import com.simplecity.amp_library.model.Playlist
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.playback.MediaManager
import com.simplecity.amp_library.utils.PlaylistUtils
import com.simplecity.amp_library.utils.PlaylistUtils.PlaylistIds
import com.simplecity.amp_library.utils.menu.MenuUtils

object PlaylistMenuUtils {

    interface Callbacks {

        fun showToast(message: String)

        fun showToast(@StringRes messageResId: Int)

        fun showWeekSelectorDialog()

        fun showRenamePlaylistDialog(playlist: Playlist)

        fun showCreateM3uPlaylistDialog(playlist: Playlist)

        fun playNext(playlist: Playlist)

        fun showDeleteConfirmationDialog(playlist: Playlist, onDelete: () -> Unit)

        fun onPlaylistDeleted()
    }

    fun setupPlaylistMenu(menu: PopupMenu, playlist: Playlist) {
        menu.inflate(R.menu.menu_playlist)

        if (!playlist.canDelete) {
            menu.menu.findItem(R.id.deletePlaylist).isVisible = false
        }

        if (!playlist.canClear) {
            menu.menu.findItem(R.id.clearPlaylist).isVisible = false
        }

        if (playlist.id != PlaylistUtils.PlaylistIds.RECENTLY_ADDED_PLAYLIST) {
            menu.menu.findItem(R.id.editPlaylist).isVisible = false
        }

        if (!playlist.canRename) {
            menu.menu.findItem(R.id.renamePlaylist).isVisible = false
        }

        if (playlist.id == PlaylistUtils.PlaylistIds.MOST_PLAYED_PLAYLIST) {
            menu.menu.findItem(R.id.exportPlaylist).isVisible = false
        }
    }

    fun setupPlaylistMenu(toolbar: Toolbar, playlist: Playlist) {
        toolbar.inflateMenu(R.menu.menu_playlist)

        if (!playlist.canDelete) {
            toolbar.menu.findItem(R.id.deletePlaylist).isVisible = false
        }

        if (!playlist.canClear) {
            toolbar.menu.findItem(R.id.clearPlaylist).isVisible = false
        }

        if (playlist.id != PlaylistUtils.PlaylistIds.RECENTLY_ADDED_PLAYLIST) {
            toolbar.menu.findItem(R.id.editPlaylist).isVisible = false
        }

        if (!playlist.canRename) {
            toolbar.menu.findItem(R.id.renamePlaylist).isVisible = false
        }

        if (playlist.id == PlaylistUtils.PlaylistIds.MOST_PLAYED_PLAYLIST) {
            toolbar.menu.findItem(R.id.exportPlaylist).isVisible = false
        }
    }

    fun getPlaylistPopupMenuClickListener(mediaManager: MediaManager, playlist: Playlist, callbacks: Callbacks): PopupMenu.OnMenuItemClickListener {
        return PopupMenu.OnMenuItemClickListener { item -> handleMenuItemClicks(item, mediaManager, playlist, callbacks) }
    }

    fun handleMenuItemClicks(menuItem: MenuItem, mediaManager: MediaManager, playlist: Playlist, callbacks: Callbacks): Boolean {
        when (menuItem.itemId) {
            R.id.playPlaylist -> {
                MenuUtils.play(mediaManager, playlist.songsObservable.first(emptyList<Song>()), { callbacks.showToast(it) })
                return true
            }
            R.id.playNext -> {
                callbacks.playNext(playlist)
                return true
            }
            R.id.deletePlaylist -> {
                callbacks.showDeleteConfirmationDialog(playlist, {
                    playlist.delete()
                    callbacks.onPlaylistDeleted()
                })
                return true
            }
            R.id.editPlaylist -> {
                if (playlist.id == PlaylistIds.RECENTLY_ADDED_PLAYLIST) {
                    callbacks.showWeekSelectorDialog()
                }
                return true
            }
            R.id.renamePlaylist -> {
                callbacks.showRenamePlaylistDialog(playlist)
                return true
            }
            R.id.exportPlaylist -> {
                callbacks.showCreateM3uPlaylistDialog(playlist)
                return true
            }
            R.id.clearPlaylist -> {
                playlist.clear()
                return true
            }
        }
        return false
    }
}
