package com.simplecity.amp_library.utils.sorting

import android.view.Menu
import android.view.MenuItem
import com.simplecity.amp_library.R

object SongSortHelper {

    @JvmStatic
    fun updateSongSortMenuItems(menu: Menu, songsSortOrder: Int, songsAscending: Boolean) {
        when (songsSortOrder) {
            SortManager.SongSort.DETAIL_DEFAULT -> menu.findItem(R.id.sort_song_detail_default).isChecked = true
            SortManager.SongSort.DEFAULT -> menu.findItem(R.id.sort_song_default).isChecked = true
            SortManager.SongSort.NAME -> menu.findItem(R.id.sort_song_name).isChecked = true
            SortManager.SongSort.TRACK_NUMBER -> menu.findItem(R.id.sort_song_track_number).isChecked = true
            SortManager.SongSort.DURATION -> menu.findItem(R.id.sort_song_duration).isChecked = true
            SortManager.SongSort.DATE -> menu.findItem(R.id.sort_song_date).isChecked = true
            SortManager.SongSort.YEAR -> menu.findItem(R.id.sort_song_year).isChecked = true
            SortManager.SongSort.ALBUM_NAME -> menu.findItem(R.id.sort_song_album_name).isChecked = true
            SortManager.SongSort.ARTIST_NAME -> menu.findItem(R.id.sort_song_artist_name).isChecked = true
        }

        menu.findItem(R.id.sort_song_ascending).isChecked = songsAscending
    }

    @JvmStatic
    @SortManager.SongSort
    fun handleSongMenuSortOrderClicks(item: MenuItem): Int? {
        return when (item.itemId) {
            R.id.sort_song_detail_default -> SortManager.SongSort.DETAIL_DEFAULT
            R.id.sort_song_default -> SortManager.SongSort.DEFAULT
            R.id.sort_song_name -> SortManager.SongSort.NAME
            R.id.sort_song_track_number -> SortManager.SongSort.TRACK_NUMBER
            R.id.sort_song_duration -> SortManager.SongSort.DURATION
            R.id.sort_song_year -> SortManager.SongSort.YEAR
            R.id.sort_song_date -> SortManager.SongSort.DATE
            R.id.sort_song_album_name -> SortManager.SongSort.ALBUM_NAME
            R.id.sort_song_artist_name -> SortManager.SongSort.ARTIST_NAME
            else -> null
        }
    }

    @JvmStatic
    fun handleSongDetailMenuSortOrderAscClicks(item: MenuItem): Boolean? {
        return when (item.itemId) {
            R.id.sort_song_ascending -> !item.isChecked
            else -> null
        }
    }
}