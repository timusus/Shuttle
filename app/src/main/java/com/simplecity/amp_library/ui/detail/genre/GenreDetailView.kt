package com.simplecity.amp_library.ui.detail.genre

import androidx.core.util.Pair

import com.simplecity.amp_library.model.Album
import com.simplecity.amp_library.model.Song

interface GenreDetailView {

    fun setData(data: Pair<MutableList<Album>, MutableList<Song>>)

    fun showToast(message: String)

    @JvmSuppressWildcards
    fun showCreatePlaylistDialog(songs: List<Song>)

    fun closeContextualToolbar()

    fun fadeInSlideShowAlbum(previousAlbum: Album?, newAlbum: Album)

}