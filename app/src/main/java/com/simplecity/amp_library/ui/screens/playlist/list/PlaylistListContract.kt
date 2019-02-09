package com.simplecity.amp_library.ui.screens.playlist.list

import com.simplecity.amp_library.model.Playlist
import com.simplecity.amp_library.ui.screens.playlist.menu.PlaylistMenuContract

interface PlaylistListContract {

    interface View : PlaylistMenuContract.View {

        fun setData(playlists: List<Playlist>)
    }

    interface Presenter {

        fun loadData()
    }

}