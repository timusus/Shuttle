package com.simplecity.amp_library.ui.screens.songs.menu

import com.simplecity.amp_library.model.Playlist
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.utils.menu.song.SongsMenuCallbacks

interface SongMenuContract {

    interface View {

        fun presentCreatePlaylistDialog(songs: List<Song>)

        fun presentSongInfoDialog(song: Song)

        fun onSongsAddedToPlaylist(playlist: Playlist, numSongs: Int)

        fun onSongsAddedToQueue(numSongs: Int)

        fun presentTagEditorDialog(song: Song)

        fun presentDeleteDialog(songs: List<Song>)

        fun presentRingtonePermissionDialog()

        fun showRingtoneSetMessage()

        fun shareSong(song: Song)
    }

    interface Presenter : SongsMenuCallbacks
}