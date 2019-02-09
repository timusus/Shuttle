package com.simplecity.amp_library.ui.screens.playlist.menu

import com.simplecity.amp_library.model.Playlist
import com.simplecity.amp_library.utils.menu.playlist.PlaylistMenuCallbacks

interface PlaylistMenuContract {

    interface View {

        fun onPlaybackFailed()

        fun onSongsAddedToQueue(numSongs: Int)

        fun presentEditDialog(playlist: Playlist)

        fun presentRenameDialog(playlist: Playlist)

        fun presentM3uDialog(playlist: Playlist)

        fun presentDeletePlaylistDialog(playlist: Playlist)
    }

    interface Presenter : PlaylistMenuCallbacks

}