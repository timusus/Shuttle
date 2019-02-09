package com.simplecity.amp_library.utils.menu.album

import android.support.v7.widget.PopupMenu
import android.support.v7.widget.Toolbar
import com.simplecity.amp_library.R
import com.simplecity.amp_library.model.Album
import com.simplecity.amp_library.model.Playlist
import com.simplecity.amp_library.playback.MediaManager.Defs
import com.simplecity.amp_library.utils.playlists.PlaylistManager
import com.simplecity.amp_library.utils.playlists.PlaylistMenuHelper
import io.reactivex.Single

object AlbumMenuUtils {

    const val TAG = "AlbumMenuUtils"

    fun setupAlbumMenu(menu: PopupMenu, playlistMenuHelper: PlaylistMenuHelper, showGoToArtist: Boolean = true) {
        menu.inflate(R.menu.menu_album)

        if (!showGoToArtist) {
            menu.menu.findItem(R.id.go_to).isVisible = false
        }

        // Add playlist menu
        val subMenu = menu.menu.findItem(R.id.addToPlaylist).subMenu
        playlistMenuHelper.createPlaylistMenu(subMenu)
    }

    fun getAlbumMenuClickListener(selectedAlbums: Single<List<Album>>, callbacks: AlbumsMenuCallbacks): Toolbar.OnMenuItemClickListener {
        return Toolbar.OnMenuItemClickListener { item ->
            when (item.itemId) {
                Defs.NEW_PLAYLIST -> {
                    callbacks.createPlaylistFromAlbums(selectedAlbums)
                    return@OnMenuItemClickListener true
                }
                Defs.PLAYLIST_SELECTED -> {
                    callbacks.addAlbumsToPlaylist(item.intent.getSerializableExtra(PlaylistManager.ARG_PLAYLIST) as Playlist, selectedAlbums)
                    return@OnMenuItemClickListener true
                }
                R.id.playNext -> {
                    callbacks.playAlbumsNext(selectedAlbums)
                    return@OnMenuItemClickListener true
                }
                R.id.addToQueue -> {
                    callbacks.addAlbumsToQueue(selectedAlbums)
                    return@OnMenuItemClickListener true
                }
                R.id.delete -> {
                    callbacks.deleteAlbums(selectedAlbums)
                    return@OnMenuItemClickListener true
                }
            }
            false
        }
    }

    fun getAlbumMenuClickListener(album: Album, callbacks: AlbumsMenuCallbacks): PopupMenu.OnMenuItemClickListener {
        return PopupMenu.OnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.play -> {
                    callbacks.play(album)
                    return@OnMenuItemClickListener true
                }
                R.id.playNext -> {
                    callbacks.playAlbumsNext(album)
                    return@OnMenuItemClickListener true
                }
                Defs.NEW_PLAYLIST -> {
                    callbacks.createPlaylistFromAlbums(album)
                    return@OnMenuItemClickListener true
                }
                Defs.PLAYLIST_SELECTED -> {
                    callbacks.addAlbumsToPlaylist(item.intent.getSerializableExtra(PlaylistManager.ARG_PLAYLIST) as Playlist, album)
                    return@OnMenuItemClickListener true
                }
                R.id.addToQueue -> {
                    callbacks.addAlbumsToQueue(album)
                    return@OnMenuItemClickListener true
                }
                R.id.editTags -> {
                    callbacks.editTags(album)
                    return@OnMenuItemClickListener true
                }
                R.id.info -> {
                    callbacks.albumInfo(album)
                    return@OnMenuItemClickListener true
                }
                R.id.artwork -> {
                    callbacks.editArtwork(album)
                    return@OnMenuItemClickListener true
                }
                R.id.blacklist -> {
                    callbacks.blacklistAlbums(album)
                    return@OnMenuItemClickListener true
                }
                R.id.delete -> {
                    callbacks.deleteAlbums(album)
                    return@OnMenuItemClickListener true
                }
                R.id.goToArtist -> {
                    callbacks.goToArtist(album)
                    return@OnMenuItemClickListener true
                }
            }
            false
        }
    }
}