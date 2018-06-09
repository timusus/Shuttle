package com.simplecity.amp_library.utils.menu.album

import android.content.Context
import android.support.v7.widget.PopupMenu
import android.support.v7.widget.Toolbar
import com.simplecity.amp_library.R
import com.simplecity.amp_library.model.Album
import com.simplecity.amp_library.model.Playlist
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.playback.MediaManager
import com.simplecity.amp_library.utils.PlaylistUtils
import com.simplecity.amp_library.utils.ShuttleUtils
import com.simplecity.amp_library.utils.extensions.getSongsSingle
import com.simplecity.amp_library.utils.menu.MenuUtils
import io.reactivex.Single

object AlbumMenuUtils {

    interface Callbacks {

        fun onPlaylistItemsInserted()

        fun onQueueItemsInserted(message: String)

        fun playNext(songsSingle: Single<List<Song>>)

        fun showTagEditor(album: Album)

        fun showDeleteDialog(album: Album)

        fun showDeleteDialog(albums: List<Album>)

        fun showDeleteDialog(albums: Single<List<Album>>)

        fun showAlbumInfo(album: Album)

        fun showArtworkChooser(album: Album)

        fun showUpgradeDialog()

        fun showToast(message: String)
    }

    fun setupAlbumMenu(menu: PopupMenu) {
        menu.inflate(R.menu.menu_album)

        // Add playlist menu
        val sub = menu.menu.findItem(R.id.addToPlaylist).subMenu
        PlaylistUtils.createPlaylistMenu(sub)
    }

    fun getAlbumMenuClickListener(context: Context, mediaManager: MediaManager, selectedAlbums: Single<List<Album>>, callbacks: Callbacks): Toolbar.OnMenuItemClickListener {
        return Toolbar.OnMenuItemClickListener { item ->
            when (item.itemId) {
                MediaManager.NEW_PLAYLIST -> {
                    MenuUtils.newPlaylist(context, selectedAlbums.getSongsSingle(), { callbacks.onPlaylistItemsInserted() })
                    return@OnMenuItemClickListener true
                }
                MediaManager.PLAYLIST_SELECTED -> {
                    MenuUtils.addToPlaylist(
                        context,
                        item.intent.getSerializableExtra(PlaylistUtils.ARG_PLAYLIST) as Playlist,
                        selectedAlbums.getSongsSingle(),
                        { callbacks.onPlaylistItemsInserted() })
                    return@OnMenuItemClickListener true
                }
                R.id.playNext -> {
                    callbacks.playNext(selectedAlbums.getSongsSingle())
                    return@OnMenuItemClickListener true
                }
                R.id.addToQueue -> {
                    MenuUtils.addToQueue(mediaManager, selectedAlbums.getSongsSingle(), { callbacks.onQueueItemsInserted(it) })
                    return@OnMenuItemClickListener true
                }
                R.id.delete -> {
                    callbacks.showDeleteDialog(selectedAlbums)
                    return@OnMenuItemClickListener true
                }
            }
            false
        }
    }

    fun getAlbumMenuClickListener(context: Context, mediaManager: MediaManager, album: Album, callbacks: Callbacks): PopupMenu.OnMenuItemClickListener {
        return PopupMenu.OnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.play -> {
                    MenuUtils.play(mediaManager, album.songsSingle, { callbacks.showToast(it) })
                    return@OnMenuItemClickListener true
                }
                R.id.playNext -> {
                    callbacks.playNext(album.songsSingle)
                    return@OnMenuItemClickListener true
                }
                MediaManager.NEW_PLAYLIST -> {
                    MenuUtils.newPlaylist(context, album.songsSingle, { callbacks.onPlaylistItemsInserted() })
                    return@OnMenuItemClickListener true
                }
                MediaManager.PLAYLIST_SELECTED -> {
                    MenuUtils.addToPlaylist(context, item.intent.getSerializableExtra(PlaylistUtils.ARG_PLAYLIST) as Playlist, album.songsSingle, { callbacks.onPlaylistItemsInserted() })
                    return@OnMenuItemClickListener true
                }
                R.id.addToQueue -> {
                    MenuUtils.addToQueue(mediaManager, album.songsSingle, { callbacks.onQueueItemsInserted(it) })
                    return@OnMenuItemClickListener true
                }
                R.id.editTags -> {
                    if (ShuttleUtils.isUpgraded()) {
                        callbacks.showTagEditor(album)
                    } else {
                        callbacks.showUpgradeDialog()
                    }
                    return@OnMenuItemClickListener true
                }
                R.id.info -> {
                    callbacks.showAlbumInfo(album)
                    return@OnMenuItemClickListener true
                }
                R.id.artwork -> {
                    callbacks.showArtworkChooser(album)
                    return@OnMenuItemClickListener true
                }
                R.id.blacklist -> {
                    MenuUtils.blacklist(album.songsSingle)
                    return@OnMenuItemClickListener true
                }
                R.id.delete -> {
                    callbacks.showDeleteDialog(album)
                    return@OnMenuItemClickListener true
                }
            }
            false
        }
    }
}