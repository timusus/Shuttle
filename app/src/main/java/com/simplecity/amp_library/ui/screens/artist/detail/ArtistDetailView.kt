package com.simplecity.amp_library.ui.screens.artist.detail

import com.simplecity.amp_library.model.Album
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.ui.screens.album.menu.AlbumArtistMenuContract
import com.simplecity.amp_library.ui.screens.album.menu.AlbumMenuContract
import com.simplecity.amp_library.ui.screens.songs.menu.SongMenuContract

interface ArtistDetailView :
    SongMenuContract.View,
    AlbumMenuContract.View,
    AlbumArtistMenuContract.View {

    fun setData(albums: List<Album>, songs: List<Song>)

    fun closeContextualToolbar()
}