package com.simplecity.amp_library.ui.screens.search

import android.view.View
import com.simplecity.amp_library.model.Album
import com.simplecity.amp_library.model.AlbumArtist
import com.simplecity.amp_library.ui.screens.album.menu.AlbumArtistMenuContract
import com.simplecity.amp_library.ui.screens.album.menu.AlbumMenuContract
import com.simplecity.amp_library.ui.screens.songs.menu.SongMenuContract

interface SearchView : SongMenuContract.View, AlbumMenuContract.View, AlbumArtistMenuContract.View {

    fun setLoading(loading: Boolean)

    fun setData(searchResult: SearchResult)

    fun setFilterFuzzyChecked(checked: Boolean)

    fun setFilterArtistsChecked(checked: Boolean)

    fun setFilterAlbumsChecked(checked: Boolean)

    fun showPlaybackError()

    fun goToArtist(albumArtist: AlbumArtist, transitionView: View)

    fun goToAlbum(album: Album, transitionView: View)

    fun showUpgradeDialog()
}