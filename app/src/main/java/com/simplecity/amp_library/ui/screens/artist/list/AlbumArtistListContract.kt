package com.simplecity.amp_library.ui.screens.artist.list

import com.simplecity.amp_library.model.AlbumArtist
import com.simplecity.amp_library.ui.screens.album.menu.AlbumArtistMenuContract

interface AlbumArtistListContract {

    interface View : AlbumArtistMenuContract.View {

        fun setData(albumArtists: List<AlbumArtist>, scrollToTop: Boolean = false)

        fun invalidateOptionsMenu()
    }

    interface Presenter {

        fun loadAlbumArtists(scrollToTop: Boolean)

        fun setAlbumArtistsSortOrder(order: Int)

        fun setAlbumArtistsAscending(ascending: Boolean)
    }

}