package com.simplecity.amp_library.utils.extensions

import com.simplecity.amp_library.model.AlbumArtist
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.utils.ComparisonUtils
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.ArrayList
import java.util.Comparator

fun AlbumArtist.getSongs(): Single<List<Song>> {
    return songsSingle
        .map { songs ->
            songs.sortWith(Comparator { a, b -> ComparisonUtils.compareInt(b.year, a.year) })
            songs.sortWith(Comparator { a, b -> ComparisonUtils.compareInt(a.track, b.track) })
            songs.sortWith(Comparator { a, b -> ComparisonUtils.compareInt(a.discNumber, b.discNumber) })
            songs.sortWith(Comparator { a, b -> ComparisonUtils.compare(a.albumName, b.albumName) })
            songs
        }
}

fun Single<List<AlbumArtist>>.getSongs(): Single<List<Song>> {
    return this.flatMapObservable { list -> Observable.fromIterable(list) }
        .concatMap { artist: AlbumArtist -> artist.getSongs().toObservable() }
        .reduce(emptyList(),
            { songs: List<Song>, songs2: List<Song> ->
                val allSongs = ArrayList<Song>(songs)
                allSongs.addAll(songs2)
                allSongs
            })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
}