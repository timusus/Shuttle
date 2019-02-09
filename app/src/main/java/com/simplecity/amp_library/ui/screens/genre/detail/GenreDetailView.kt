package com.simplecity.amp_library.ui.screens.genre.detail

import com.simplecity.amp_library.model.Album
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.ui.screens.album.menu.AlbumMenuContract
import com.simplecity.amp_library.ui.screens.genre.menu.GenreMenuContract
import com.simplecity.amp_library.ui.screens.songs.menu.SongMenuContract

interface GenreDetailView :
    GenreMenuContract.View,
    SongMenuContract.View,
    AlbumMenuContract.View {

    fun setData(albums: List<Album>, songs: List<Song>)

    fun closeContextualToolbar()

    fun fadeInSlideShowAlbum(previousAlbum: Album?, newAlbum: Album)
}