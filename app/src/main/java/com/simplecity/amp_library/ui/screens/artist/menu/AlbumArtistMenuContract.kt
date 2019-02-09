package com.simplecity.amp_library.ui.screens.album.menu

import com.simplecity.amp_library.model.AlbumArtist
import com.simplecity.amp_library.model.Playlist
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.utils.menu.albumartist.AlbumArtistMenuCallbacks

interface AlbumArtistMenuContract {

    interface View {

        fun presentCreatePlaylistDialog(songs: List<Song>)

        fun onSongsAddedToPlaylist(playlist: Playlist, numSongs: Int)

        fun onSongsAddedToQueue(numSongs: Int)

        fun onPlaybackFailed()

        fun presentTagEditorDialog(albumArtist: AlbumArtist)

        fun presentArtistDeleteDialog(albumArtists: List<AlbumArtist>)

        fun presentAlbumArtistInfoDialog(albumArtist: AlbumArtist)

        fun presentArtworkEditorDialog(albumArtist: AlbumArtist)
    }

    interface Presenter : AlbumArtistMenuCallbacks

}