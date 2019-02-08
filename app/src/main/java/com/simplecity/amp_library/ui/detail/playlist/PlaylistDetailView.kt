package com.simplecity.amp_library.ui.detail.playlist

import com.simplecity.amp_library.model.Album
import com.simplecity.amp_library.model.Song

interface PlaylistDetailView {

    fun setData(data: MutableList<Song>)

    fun showToast(message: String)

    @JvmSuppressWildcards
    fun showCreatePlaylistDialog(songs: List<Song>)

    fun closeContextualToolbar()

    fun fadeInSlideShowAlbum(previousAlbum: Album?, newAlbum: Album)
}