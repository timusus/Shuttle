package com.simplecity.amp_library.ui.screens.genre.menu

import com.simplecity.amp_library.model.Playlist
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.utils.menu.genre.GenreMenuCallbacks

interface GenreMenuContract {

    interface View {

        fun presentCreatePlaylistDialog(songs: List<Song>)

        fun onSongsAddedToPlaylist(playlist: Playlist, numSongs: Int)

        fun onSongsAddedToQueue(numSongs: Int)

        fun onPlaybackFailed()
    }

    interface Presenter : GenreMenuCallbacks

}