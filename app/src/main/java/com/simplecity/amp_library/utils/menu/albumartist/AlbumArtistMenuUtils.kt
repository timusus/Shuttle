package com.simplecity.amp_library.utils.menu.albumartist

import android.content.Context
import android.support.v7.widget.PopupMenu
import android.support.v7.widget.Toolbar
import com.simplecity.amp_library.R
import com.simplecity.amp_library.model.AlbumArtist
import com.simplecity.amp_library.model.Playlist
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.playback.MediaManager
import com.simplecity.amp_library.utils.Operators
import com.simplecity.amp_library.utils.PlaylistUtils
import com.simplecity.amp_library.utils.ShuttleUtils
import com.simplecity.amp_library.utils.extensions.getSongs
import com.simplecity.amp_library.utils.menu.MenuUtils
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers

object AlbumArtistMenuUtils {

    interface Callbacks {

        fun onPlaylistItemsInserted()

        fun onQueueItemsInserted(message: String)

        fun playNext(songsSingle: Single<List<Song>>)

        fun showTagEditor(albumArtist: AlbumArtist)

        fun showDeleteDialog(albumArtist: AlbumArtist)

        fun showDeleteDialog(albumArtists: List<AlbumArtist>)

        fun showDeleteDialog(albumArtists: Single<List<AlbumArtist>>)

        fun showAlbumArtistInfo(albumArtist: AlbumArtist)

        fun showArtworkChooser(albumArtist: AlbumArtist)

        fun showUpgradeDialog()

        fun showToast(message: String)
    }

    fun getAlbumArtistMenuClickListener(context: Context, mediaManager: MediaManager, selectedAlbumArtists: Single<List<AlbumArtist>>, callbacks: Callbacks): Toolbar.OnMenuItemClickListener {
        return Toolbar.OnMenuItemClickListener { item ->
            when (item.itemId) {
                MediaManager.NEW_PLAYLIST -> {
                    MenuUtils.newPlaylist(context, selectedAlbumArtists.getSongs(), { callbacks.onPlaylistItemsInserted() })
                    return@OnMenuItemClickListener true
                }
                MediaManager.PLAYLIST_SELECTED -> {
                    MenuUtils.addToPlaylist(context, item.intent.getSerializableExtra(PlaylistUtils.ARG_PLAYLIST) as Playlist, selectedAlbumArtists.getSongs(), { callbacks.onPlaylistItemsInserted() })
                    return@OnMenuItemClickListener true
                }
                R.id.playNext -> {
                    callbacks.playNext(selectedAlbumArtists.getSongs())
                    return@OnMenuItemClickListener true
                }
                R.id.addToQueue -> {
                    MenuUtils.addToQueue(mediaManager, selectedAlbumArtists.getSongs(), { callbacks.onQueueItemsInserted(it) })
                    return@OnMenuItemClickListener true
                }
                R.id.delete -> {
                    callbacks.showDeleteDialog(selectedAlbumArtists)
                    return@OnMenuItemClickListener true
                }
            }
            false
        }
    }

    fun getAlbumArtistClickListener(context: Context, mediaManager: MediaManager, albumArtist: AlbumArtist, callbacks: Callbacks): PopupMenu.OnMenuItemClickListener {
        return PopupMenu.OnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.play -> {
                    MenuUtils.play(mediaManager, albumArtist.songsSingle, { callbacks.showToast(it) })
                    return@OnMenuItemClickListener true
                }
                R.id.playNext -> {
                    callbacks.playNext(albumArtist.songsSingle)
                    albumArtist.songsSingle
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ songs -> MenuUtils.playNext(mediaManager, songs, { callbacks.showToast(it) }) })
                    return@OnMenuItemClickListener true
                }
                R.id.albumShuffle -> {
                    MenuUtils.play(
                        mediaManager,
                        albumArtist.songsSingle.map({ Operators.albumShuffleSongs(it) }),
                        { callbacks.showToast(it) })
                    return@OnMenuItemClickListener true
                }
                MediaManager.NEW_PLAYLIST -> {
                    MenuUtils.newPlaylist(context, albumArtist.songsSingle, { callbacks.onPlaylistItemsInserted() })
                    return@OnMenuItemClickListener true
                }
                MediaManager.PLAYLIST_SELECTED -> {
                    MenuUtils.addToPlaylist(context, item.intent.getSerializableExtra(PlaylistUtils.ARG_PLAYLIST) as Playlist, albumArtist.songsSingle, { callbacks.onPlaylistItemsInserted() })
                    return@OnMenuItemClickListener true
                }
                R.id.addToQueue -> {
                    MenuUtils.addToQueue(mediaManager, albumArtist.songsSingle, { callbacks.onQueueItemsInserted(it) })
                    return@OnMenuItemClickListener true
                }
                R.id.editTags -> {
                    if (ShuttleUtils.isUpgraded()) {
                        callbacks.showTagEditor(albumArtist)
                    } else {
                        callbacks.showUpgradeDialog()
                    }
                    return@OnMenuItemClickListener true
                }
                R.id.info -> {
                    callbacks.showAlbumArtistInfo(albumArtist)
                    return@OnMenuItemClickListener true
                }
                R.id.artwork -> {
                    callbacks.showArtworkChooser(albumArtist)
                    return@OnMenuItemClickListener true
                }
                R.id.blacklist -> {
                    MenuUtils.blacklist(albumArtist.songsSingle)
                    return@OnMenuItemClickListener true
                }
                R.id.delete -> {
                    callbacks.showDeleteDialog(albumArtist)
                    return@OnMenuItemClickListener true
                }
            }
            false
        }
    }
}
