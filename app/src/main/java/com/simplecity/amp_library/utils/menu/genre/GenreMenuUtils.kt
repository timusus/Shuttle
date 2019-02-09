package com.simplecity.amp_library.utils.menu.genre

import android.support.v7.widget.PopupMenu
import com.simplecity.amp_library.R
import com.simplecity.amp_library.model.Genre
import com.simplecity.amp_library.model.Playlist
import com.simplecity.amp_library.playback.MediaManager.Defs
import com.simplecity.amp_library.utils.playlists.PlaylistManager

object GenreMenuUtils {

    fun getGenreClickListener(genre: Genre, callbacks: GenreMenuCallbacks): PopupMenu.OnMenuItemClickListener {
        return PopupMenu.OnMenuItemClickListener { item ->
            when (item.itemId) {
                Defs.NEW_PLAYLIST -> {
                    callbacks.createPlaylist(genre)
                    return@OnMenuItemClickListener true
                }
                Defs.PLAYLIST_SELECTED -> {
                    callbacks.addToPlaylist(item.intent.getSerializableExtra(PlaylistManager.ARG_PLAYLIST) as Playlist, genre)
                    return@OnMenuItemClickListener true
                }
                R.id.play -> {
                    callbacks.play(genre)
                    return@OnMenuItemClickListener true
                }
                R.id.playNext -> {
                    callbacks.playNext(genre)
                    return@OnMenuItemClickListener true
                }
                R.id.addToQueue -> {
                    callbacks.addToQueue(genre)
                    return@OnMenuItemClickListener true
                }
            }
            false
        }
    }
}