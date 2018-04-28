package com.simplecity.amp_library.ui.detail

import android.support.v7.widget.Toolbar
import android.view.MenuItem
import com.simplecity.amp_library.R
import com.simplecity.amp_library.utils.SortManager

class DetailSortHelper {

    companion object {

        @JvmStatic
        fun updateSongSortMenuItems(toolbar: Toolbar, songsSortOrder: Int, songsAscending: Boolean) {
            when (songsSortOrder) {
                SortManager.SongSort.DETAIL_DEFAULT -> toolbar.menu.findItem(R.id.sort_song_default).isChecked = true
                SortManager.SongSort.NAME -> toolbar.menu.findItem(R.id.sort_song_name).isChecked = true
                SortManager.SongSort.TRACK_NUMBER -> toolbar.menu.findItem(R.id.sort_song_track_number).isChecked = true
                SortManager.SongSort.DURATION -> toolbar.menu.findItem(R.id.sort_song_duration).isChecked = true
                SortManager.SongSort.DATE -> toolbar.menu.findItem(R.id.sort_song_date).isChecked = true
                SortManager.SongSort.YEAR -> toolbar.menu.findItem(R.id.sort_song_year).isChecked = true
                SortManager.SongSort.ALBUM_NAME -> toolbar.menu.findItem(R.id.sort_song_album_name).isChecked = true
                SortManager.SongSort.ARTIST_NAME -> toolbar.menu.findItem(R.id.sort_song_artist_name).isChecked = true
            }

            toolbar.menu.findItem(R.id.sort_songs_ascending).isChecked = songsAscending
        }

        @JvmStatic
        fun updateAlbumSortMenuItems(toolbar: Toolbar, albumsSortOrder: Int, albumsAscending: Boolean) {
            when (albumsSortOrder) {
                SortManager.AlbumSort.DEFAULT -> toolbar.menu.findItem(R.id.sort_album_default).isChecked = true
                SortManager.AlbumSort.NAME -> toolbar.menu.findItem(R.id.sort_album_name).isChecked = true
                SortManager.AlbumSort.YEAR -> toolbar.menu.findItem(R.id.sort_album_year).isChecked = true
                SortManager.AlbumSort.ARTIST_NAME -> toolbar.menu.findItem(R.id.sort_album_artist_name).isChecked = true
            }

            toolbar.menu.findItem(R.id.sort_albums_ascending).isChecked = albumsAscending
        }

        @JvmStatic
        @SortManager.SongSort
        fun handleSongMenuSortOrderClicks(item: MenuItem): Int? {
            return when (item.itemId) {
                R.id.sort_song_default -> SortManager.SongSort.DETAIL_DEFAULT
                R.id.sort_song_name -> SortManager.SongSort.NAME
                R.id.sort_song_track_number -> SortManager.SongSort.TRACK_NUMBER
                R.id.sort_song_duration -> SortManager.SongSort.DURATION
                R.id.sort_song_year -> SortManager.SongSort.YEAR
                R.id.sort_song_date -> SortManager.SongSort.DATE
                R.id.sort_song_album_name -> SortManager.SongSort.ALBUM_NAME
                else -> null
            }
        }

        @JvmStatic
        fun handleSongMenuSortOrderAscClicks(item: MenuItem): Boolean? {
            return when (item.itemId) {
                R.id.sort_songs_ascending -> !item.isChecked
                else -> null
            }
        }

        @JvmStatic
        @SortManager.AlbumSort
        fun handleAlbumMenuSortOrderClicks(item: MenuItem): Int? {
            return when (item.itemId) {
                R.id.sort_album_default -> SortManager.AlbumSort.DEFAULT
                R.id.sort_album_name -> SortManager.AlbumSort.NAME
                R.id.sort_album_year -> SortManager.AlbumSort.YEAR
                else -> null
            }
        }

        @JvmStatic
        fun handleAlbumMenuSortOrderAscClicks(item: MenuItem): Boolean? {
            return when (item.itemId) {
                R.id.sort_albums_ascending -> !item.isChecked
                else -> null
            }
        }
    }
}