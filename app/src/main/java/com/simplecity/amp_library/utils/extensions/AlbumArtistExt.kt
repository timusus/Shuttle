package com.simplecity.amp_library.utils.extensions

import com.simplecity.amp_library.data.Repository.SongsRepository
import com.simplecity.amp_library.model.AlbumArtist
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.utils.ComparisonUtils
import io.reactivex.Single
import java.util.Comparator

fun AlbumArtist.getSongs(songsRepository: SongsRepository): Single<List<Song>> {
    return songsRepository.getSongs(this)
        .first(emptyList())
        .map { songs ->
            songs.sortedWith(Comparator { a, b -> ComparisonUtils.compareInt(b.year, a.year) })
                .sortedWith(Comparator { a, b -> ComparisonUtils.compareInt(a.track, b.track) })
                .sortedWith(Comparator { a, b -> ComparisonUtils.compareInt(a.discNumber, b.discNumber) })
                .sortedWith(Comparator { a, b -> ComparisonUtils.compare(a.albumName, b.albumName) })
        }
}

fun List<AlbumArtist>.getSongs(songsRepository: SongsRepository): Single<List<Song>> {
    return Single.concat(
        map { albumArtist -> albumArtist.getSongsSingle(songsRepository) })
        .reduce(emptyList()) { a, b -> a + b }
}

fun Single<List<AlbumArtist>>.getSongs(songsRepository: SongsRepository): Single<List<Song>> {
    return flatMap { albumArtist -> albumArtist.getSongs(songsRepository) }
}