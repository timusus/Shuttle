package com.simplecity.amp_library.ui.screens.drawer

import com.simplecity.amp_library.model.Playlist
import com.simplecity.amp_library.ui.screens.playlist.menu.PlaylistMenuContract
import com.simplecity.amp_library.ui.views.PurchaseView

interface DrawerView :
    PurchaseView,
    PlaylistMenuContract.View {

    fun setPlaylistItems(playlists: List<Playlist>)

    fun closeDrawer()

    fun setDrawerItemSelected(@DrawerParent.Type type: Int)
}