package com.simplecity.amp_library.ui.screens.album.detail

import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.ui.screens.album.menu.AlbumMenuContract
import com.simplecity.amp_library.ui.screens.songs.menu.SongMenuContract

interface AlbumDetailView :
    SongMenuContract.View,
    AlbumMenuContract.View {
    fun setData(data: MutableList<Song>)

    fun closeContextualToolbar()
}