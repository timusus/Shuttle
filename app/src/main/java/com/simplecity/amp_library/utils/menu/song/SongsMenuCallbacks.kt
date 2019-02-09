package com.simplecity.amp_library.utils.menu.song

import com.simplecity.amp_library.model.Playlist
import com.simplecity.amp_library.model.Song
import io.reactivex.Single

interface SongsMenuCallbacks {

    fun createPlaylist(songs: List<Song>)

    fun addToPlaylist(playlist: Playlist, songs: List<Song>)

    fun addToQueue(songs: List<Song>)

    fun playNext(songs: List<Song>)

    fun blacklist(songs: List<Song>)

    fun delete(songs: List<Song>)

    fun songInfo(song: Song)

    fun setRingtone(song: Song)

    fun share(song: Song)

    fun editTags(song: Song)

    fun goToArtist(song: Song)

    fun goToAlbum(song: Song)

    fun goToGenre(song: Song)

    fun <T> transform(src: Single<List<T>>, dst: (List<T>) -> Unit)
}


fun SongsMenuCallbacks.createPlaylist(song: Song) {
    createPlaylist(listOf(song))
}

fun SongsMenuCallbacks.addToPlaylist(playlist: Playlist, song: Song) {
    addToPlaylist(playlist, listOf(song))
}

fun SongsMenuCallbacks.addToQueue(song: Song) {
    addToQueue(listOf(song))
}

fun SongsMenuCallbacks.playNext(song: Song) {
    playNext(listOf(song))
}

fun SongsMenuCallbacks.blacklist(song: Song) {
    blacklist(listOf(song))
}

fun SongsMenuCallbacks.delete(song: Song) {
    delete(listOf(song))
}


fun SongsMenuCallbacks.createPlaylist(songs: Single<List<Song>>) {
    transform(songs) { createPlaylist(songs) }
}

fun SongsMenuCallbacks.addToPlaylist(playlist: Playlist, songs: Single<List<Song>>) {
    transform(songs) { songs -> addToPlaylist(playlist, songs) }
}

fun SongsMenuCallbacks.addToQueue(songs: Single<List<Song>>) {
    transform(songs) { songs -> addToQueue(songs) }
}

fun SongsMenuCallbacks.playNext(songs: Single<List<Song>>) {
    transform(songs) { songs -> playNext(songs) }
}

fun SongsMenuCallbacks.blacklist(songs: Single<List<Song>>) {
    transform(songs) { songs -> blacklist(songs) }
}

fun SongsMenuCallbacks.delete(songs: Single<List<Song>>) {
    transform(songs) { songs -> delete(songs) }
}