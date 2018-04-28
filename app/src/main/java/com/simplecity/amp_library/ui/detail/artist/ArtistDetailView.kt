package com.simplecity.amp_library.ui.detail.artist

import android.support.v4.util.Pair

import com.simplecity.amp_library.model.Album
import com.simplecity.amp_library.model.Song

interface ArtistDetailView {

    fun setData(data: Pair<MutableList<Album>, MutableList<Song>>)

    fun showToast(message: String)

    fun showTaggerDialog()

    fun showDeleteDialog()

    fun showArtworkDialog()

    fun showBioDialog()

    fun showUpgradeDialog()

    @JvmSuppressWildcards
    fun showCreatePlaylistDialog(songs: List<Song>)

    fun closeContextualToolbar()
}
