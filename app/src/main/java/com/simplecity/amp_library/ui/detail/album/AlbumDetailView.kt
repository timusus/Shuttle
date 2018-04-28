package com.simplecity.amp_library.ui.detail.album

import com.simplecity.amp_library.model.Song

interface AlbumDetailView {

    fun setData(data: MutableList<Song>)

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
