package com.simplecity.amp_library.ui.screens.album.list

import com.simplecity.amp_library.model.Album
import com.simplecity.amp_library.ui.screens.album.menu.AlbumMenuContract

interface AlbumListContract {

    interface View : AlbumMenuContract.View {

        fun setData(albums: List<Album>, scrollToTop: Boolean = false)

        fun invalidateOptionsMenu()
    }

    interface Presenter {

        fun loadAlbums(scrollToTop: Boolean)

        fun setAlbumsSortOrder(order: Int)

        fun setAlbumsAscending(ascending: Boolean)
    }
}