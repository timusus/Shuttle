package com.simplecity.amp_library.utils.extensions

import com.simplecity.amp_library.model.Genre
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.utils.ComparisonUtils
import io.reactivex.Single
import java.util.Comparator

fun Genre.getSongs(): Single<List<Song>> {
    return songsObservable
        .map { songs ->
            songs.sortWith(Comparator { a, b -> ComparisonUtils.compareInt(b.year, a.year) })
            songs.sortWith(Comparator { a, b -> ComparisonUtils.compareInt(a.track, b.track) })
            songs.sortWith(Comparator { a, b -> ComparisonUtils.compareInt(a.discNumber, b.discNumber) })
            songs.sortWith(Comparator { a, b -> ComparisonUtils.compare(a.albumName, b.albumName) })
            songs.sortWith(Comparator { a, b -> ComparisonUtils.compare(a.albumArtistName, b.albumArtistName) })
            songs
        }
}