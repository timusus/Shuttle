package com.simplecity.amp_library.utils.extensions

import com.simplecity.amp_library.data.Repository
import com.simplecity.amp_library.data.Repository.SongsRepository
import com.simplecity.amp_library.model.Album
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.utils.ComparisonUtils
import io.reactivex.Single

fun Album.getSongsSingle(songsRepository: Repository.SongsRepository): Single<List<Song>> {
    return songsRepository.getSongs(this)
        .first(emptyList())
        .map { songs ->
            songs
                .sortedWith(Comparator { a, b -> ComparisonUtils.compareInt(b.year, a.year) })
                .sortedWith(Comparator { a, b -> ComparisonUtils.compareInt(a.track, b.track) })
                .sortedWith(Comparator { a, b -> ComparisonUtils.compareInt(a.discNumber, b.discNumber) })
        }
}

fun List<Album>.getSongs(songsRepository: SongsRepository): Single<List<Song>> {
    return Single.concat(
        map { album -> album.getSongsSingle(songsRepository) })
        .reduce(emptyList()) { a, b -> a + b }

}

fun Single<List<Album>>.getSongsSingle(songsRepository: SongsRepository): Single<List<Song>> {
    return flatMap { album -> album.getSongs(songsRepository) }
}