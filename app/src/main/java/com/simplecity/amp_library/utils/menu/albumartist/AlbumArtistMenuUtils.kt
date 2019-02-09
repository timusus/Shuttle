package com.simplecity.amp_library.utils.menu.albumartist

import android.support.v7.widget.PopupMenu
import android.support.v7.widget.Toolbar
import com.simplecity.amp_library.R
import com.simplecity.amp_library.model.AlbumArtist
import com.simplecity.amp_library.model.Playlist
import com.simplecity.amp_library.playback.MediaManager.Defs
import com.simplecity.amp_library.utils.playlists.PlaylistManager
import io.reactivex.Single

object AlbumArtistMenuUtils {

    fun getAlbumArtistMenuClickListener(selectedAlbumArtists: Single<List<AlbumArtist>>, callbacks: AlbumArtistMenuCallbacks): Toolbar.OnMenuItemClickListener {
        return Toolbar.OnMenuItemClickListener { item ->
            when (item.itemId) {
                Defs.NEW_PLAYLIST -> {
                    callbacks.createArtistsPlaylist(selectedAlbumArtists)
                    return@OnMenuItemClickListener true
                }
                Defs.PLAYLIST_SELECTED -> {
                    callbacks.addArtistsToPlaylist(item.intent.getSerializableExtra(PlaylistManager.ARG_PLAYLIST) as Playlist, selectedAlbumArtists)
                    return@OnMenuItemClickListener true
                }
                R.id.playNext -> {
                    callbacks.playArtistsNext(selectedAlbumArtists)
                    return@OnMenuItemClickListener true
                }
                R.id.addToQueue -> {
                    callbacks.addArtistsToQueue(selectedAlbumArtists)
                    return@OnMenuItemClickListener true
                }
                R.id.delete -> {
                    callbacks.deleteArtists(selectedAlbumArtists)
                    return@OnMenuItemClickListener true
                }
            }
            false
        }
    }

    fun getAlbumArtistClickListener(albumArtist: AlbumArtist, callbacks: AlbumArtistMenuCallbacks): PopupMenu.OnMenuItemClickListener {
        return PopupMenu.OnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.play -> {
                    callbacks.play(albumArtist)
                    return@OnMenuItemClickListener true
                }
                R.id.playNext -> {
                    callbacks.playArtistsNext(albumArtist)
                    return@OnMenuItemClickListener true
                }
                Defs.NEW_PLAYLIST -> {
                    callbacks.createArtistsPlaylist(albumArtist)
                    return@OnMenuItemClickListener true
                }
                Defs.PLAYLIST_SELECTED -> {
                    callbacks.addArtistsToPlaylist(item.intent.getSerializableExtra(PlaylistManager.ARG_PLAYLIST) as Playlist, albumArtist)
                    return@OnMenuItemClickListener true
                }
                R.id.addToQueue -> {
                    callbacks.addArtistsToQueue(albumArtist)
                    return@OnMenuItemClickListener true
                }
                R.id.editTags -> {
                    callbacks.editTags(albumArtist)
                    return@OnMenuItemClickListener true
                }
                R.id.info -> {
                    callbacks.albumArtistInfo(albumArtist)
                    return@OnMenuItemClickListener true
                }
                R.id.artwork -> {
                    callbacks.editArtwork(albumArtist)
                    return@OnMenuItemClickListener true
                }
                R.id.blacklist -> {
                    callbacks.blacklistArtists(albumArtist)
                    return@OnMenuItemClickListener true
                }
                R.id.delete -> {
                    callbacks.deleteArtists(albumArtist)
                    return@OnMenuItemClickListener true
                }
                R.id.albumShuffle -> {
                    callbacks.albumShuffle(albumArtist)
                    return@OnMenuItemClickListener true
                }
            }
            false
        }
    }
}
