package com.simplecity.amp_library.utils.menu.albumartist

import com.simplecity.amp_library.model.AlbumArtist
import com.simplecity.amp_library.model.Playlist
import io.reactivex.Single

interface AlbumArtistMenuCallbacks {

    fun createArtistsPlaylist(albumArtists: List<AlbumArtist>)

    fun addArtistsToPlaylist(playlist: Playlist, albumArtists: List<AlbumArtist>)

    fun addArtistsToQueue(albumArtists: List<AlbumArtist>)

    fun playArtistsNext(albumArtists: List<AlbumArtist>)

    fun play(albumArtist: AlbumArtist)

    fun editTags(albumArtist: AlbumArtist)

    fun albumArtistInfo(albumArtist: AlbumArtist)

    fun editArtwork(albumArtist: AlbumArtist)

    fun blacklistArtists(albumArtists: List<AlbumArtist>)

    fun deleteArtists(albumArtists: List<AlbumArtist>)

    fun goToArtist(albumArtist: AlbumArtist)

    fun albumShuffle(albumArtist: AlbumArtist)

    fun <T> transform(src: Single<List<T>>, dst: (List<T>) -> Unit)
}

fun AlbumArtistMenuCallbacks.createArtistsPlaylist(albumArtists: Single<List<AlbumArtist>>) {
    transform(albumArtists) { albumArtists -> createArtistsPlaylist(albumArtists) }
}

fun AlbumArtistMenuCallbacks.addArtistsToPlaylist(playlist: Playlist, albumArtists: Single<List<AlbumArtist>>) {
    transform(albumArtists) { albumArtists -> addArtistsToPlaylist(playlist, albumArtists) }
}

fun AlbumArtistMenuCallbacks.playArtistsNext(albumArtists: Single<List<AlbumArtist>>) {
    transform(albumArtists) { albumArtists -> playArtistsNext(albumArtists) }
}

fun AlbumArtistMenuCallbacks.addArtistsToQueue(albumArtists: Single<List<AlbumArtist>>) {
    transform(albumArtists) { albumArtists -> addArtistsToQueue(albumArtists) }
}

fun AlbumArtistMenuCallbacks.deleteArtists(albumArtists: Single<List<AlbumArtist>>) {
    transform(albumArtists) { albumArtists -> deleteArtists(albumArtists) }
}

fun AlbumArtistMenuCallbacks.playArtistsNext(albumArtist: AlbumArtist) {
    playArtistsNext(listOf(albumArtist))
}

fun AlbumArtistMenuCallbacks.createArtistsPlaylist(albumArtist: AlbumArtist) {
    createArtistsPlaylist(listOf(albumArtist))
}

fun AlbumArtistMenuCallbacks.addArtistsToPlaylist(playlist: Playlist, albumArtist: AlbumArtist) {
    addArtistsToPlaylist(playlist, listOf(albumArtist))
}

fun AlbumArtistMenuCallbacks.addArtistsToQueue(albumArtist: AlbumArtist) {
    addArtistsToQueue(listOf(albumArtist))
}

fun AlbumArtistMenuCallbacks.blacklistArtists(albumArtist: AlbumArtist) {
    blacklistArtists(listOf(albumArtist))
}

fun AlbumArtistMenuCallbacks.deleteArtists(albumArtist: AlbumArtist) {
    deleteArtists(listOf(albumArtist))
}

fun AlbumArtistMenuCallbacks.albumShuffle(albumArtist: AlbumArtist) {
    albumShuffle(albumArtist)
}