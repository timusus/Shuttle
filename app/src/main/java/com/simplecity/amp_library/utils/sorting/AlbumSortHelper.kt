package com.simplecity.amp_library.utils.sorting

import android.view.Menu
import android.view.MenuItem
import com.simplecity.amp_library.R

object AlbumSortHelper {

    @JvmStatic
    fun updateAlbumSortMenuItems(menu: Menu, albumsSortOrder: Int, albumsAscending: Boolean) {
        when (albumsSortOrder) {
            SortManager.AlbumSort.DEFAULT -> menu.findItem(R.id.sort_album_default).isChecked = true
            SortManager.AlbumSort.NAME -> menu.findItem(R.id.sort_album_name).isChecked = true
            SortManager.AlbumSort.YEAR -> menu.findItem(R.id.sort_album_year).isChecked = true
            SortManager.AlbumSort.ARTIST_NAME -> menu.findItem(R.id.sort_album_artist_name).isChecked = true
        }

        menu.findItem(R.id.sort_albums_ascending).isChecked = albumsAscending
    }

    @JvmStatic
    @SortManager.AlbumSort
    fun handleAlbumDetailMenuSortOrderClicks(item: MenuItem): Int? {
        return when (item.itemId) {
            R.id.sort_album_default -> SortManager.AlbumSort.DEFAULT
            R.id.sort_album_name -> SortManager.AlbumSort.NAME
            R.id.sort_album_year -> SortManager.AlbumSort.YEAR
            else -> null
        }
    }

    @JvmStatic
    fun handleAlbumDetailMenuSortOrderAscClicks(item: MenuItem): Boolean? {
        return when (item.itemId) {
            R.id.sort_albums_ascending -> !item.isChecked
            else -> null
        }
    }
}