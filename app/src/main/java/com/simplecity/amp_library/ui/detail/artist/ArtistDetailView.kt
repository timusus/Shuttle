package com.simplecity.amp_library.ui.detail.artist

import androidx.core.util.Pair

import com.simplecity.amp_library.model.Album
import com.simplecity.amp_library.model.Song

interface ArtistDetailView {

    fun setData(data: Pair<MutableList<Album>, MutableList<Song>>)

    fun showToast(message: String)

    fun showTaggerDialog()

    fun showDeleteDialog()

    fun showArtworkDialog()

    fun showBioDialog()

    @JvmSuppressWildcards
    fun showCreatePlaylistDialog(songs: List<Song>)

    fun closeContextualToolbar()
}
