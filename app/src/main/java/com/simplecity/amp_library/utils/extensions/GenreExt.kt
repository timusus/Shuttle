package com.simplecity.amp_library.utils.extensions

import android.content.Context
import android.provider.MediaStore
import com.simplecity.amp_library.model.Genre
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.sql.sqlbrite.SqlBriteUtils
import com.simplecity.amp_library.utils.ComparisonUtils
import io.reactivex.Single
import java.util.Comparator

fun Genre.getSongsObservable(context: Context): Single<List<Song>> {
    val query = Song.getQuery()
    query.uri = MediaStore.Audio.Genres.Members.getContentUri("external", id)

    return SqlBriteUtils.createSingleList(context, { Song(it) }, query)
}

fun Genre.getSongs(context: Context): Single<List<Song>> {
    return getSongsObservable(context)
        .map { songs ->
            songs
                .sortedWith(Comparator { a, b -> ComparisonUtils.compareInt(b.year, a.year) })
                .sortedWith(Comparator { a, b -> ComparisonUtils.compareInt(a.track, b.track) })
                .sortedWith(Comparator { a, b -> ComparisonUtils.compareInt(a.discNumber, b.discNumber) })
                .sortedWith(Comparator { a, b -> ComparisonUtils.compare(a.albumName, b.albumName) })
                .sortedWith(Comparator { a, b -> ComparisonUtils.compare(a.albumArtistName, b.albumArtistName) })
        }
}