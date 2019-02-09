package com.simplecity.amp_library.ui.screens.album.menu

import com.simplecity.amp_library.model.Album
import com.simplecity.amp_library.model.Playlist
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.utils.menu.album.AlbumsMenuCallbacks

interface AlbumMenuContract {

    interface View {

        fun presentCreatePlaylistDialog(songs: List<Song>)

        fun onSongsAddedToPlaylist(playlist: Playlist, numSongs: Int)

        fun onSongsAddedToQueue(numSongs: Int)

        fun onPlaybackFailed()

        fun presentTagEditorDialog(album: Album)

        fun presentDeleteAlbumsDialog(albums: List<Album>)

        fun presentAlbumInfoDialog(album: Album)

        fun presentArtworkEditorDialog(album: Album)
    }

    interface Presenter : AlbumsMenuCallbacks

}