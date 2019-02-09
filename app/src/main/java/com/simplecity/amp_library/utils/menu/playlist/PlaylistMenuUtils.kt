package com.simplecity.amp_library.utils.menu.playlist

import android.support.v7.widget.PopupMenu
import android.support.v7.widget.Toolbar
import android.view.Menu
import com.simplecity.amp_library.R
import com.simplecity.amp_library.model.Playlist
import com.simplecity.amp_library.utils.playlists.PlaylistManager

object PlaylistMenuUtils {

    fun setupPlaylistMenu(menu: PopupMenu, playlist: Playlist) {
        menu.inflate(R.menu.menu_playlist)
        updateMenuItemVisibility(menu.menu, playlist)
    }

    fun setupPlaylistMenu(toolbar: Toolbar, playlist: Playlist) {
        toolbar.inflateMenu(R.menu.menu_playlist)
        updateMenuItemVisibility(toolbar.menu, playlist)
    }

    fun updateMenuItemVisibility(menu: Menu, playlist: Playlist) {
        if (!playlist.canDelete) {
            menu.findItem(R.id.deletePlaylist).isVisible = false
        }

        if (!playlist.canClear) {
            menu.findItem(R.id.clearPlaylist).isVisible = false
        }

        if (playlist.id != PlaylistManager.PlaylistIds.RECENTLY_ADDED_PLAYLIST) {
            menu.findItem(R.id.editPlaylist).isVisible = false
        }

        if (!playlist.canRename) {
            menu.findItem(R.id.renamePlaylist).isVisible = false
        }

        if (playlist.id == PlaylistManager.PlaylistIds.MOST_PLAYED_PLAYLIST) {
            menu.findItem(R.id.exportPlaylist).isVisible = false
        }
    }

    fun getPlaylistPopupMenuClickListener(playlist: Playlist, callbacks: PlaylistMenuCallbacks): PopupMenu.OnMenuItemClickListener {
        return PopupMenu.OnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.playPlaylist -> {
                    callbacks.play(playlist)
                    return@OnMenuItemClickListener true
                }
                R.id.playNext -> {
                    callbacks.playNext(playlist)
                    return@OnMenuItemClickListener true
                }
                R.id.deletePlaylist -> {
                    callbacks.delete(playlist)
                    return@OnMenuItemClickListener true
                }
                R.id.editPlaylist -> {
                    callbacks.edit(playlist)
                    return@OnMenuItemClickListener true
                }
                R.id.renamePlaylist -> {
                    callbacks.rename(playlist)
                    return@OnMenuItemClickListener true
                }
                R.id.exportPlaylist -> {
                    callbacks.createM3uPlaylist(playlist)
                    return@OnMenuItemClickListener true
                }
                R.id.clearPlaylist -> {
                    callbacks.clear(playlist)
                    return@OnMenuItemClickListener true
                }
            }
            false
        }
    }
}
