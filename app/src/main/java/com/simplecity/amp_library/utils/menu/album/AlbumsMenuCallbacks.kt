package com.simplecity.amp_library.utils.menu.album

import com.simplecity.amp_library.model.Album
import com.simplecity.amp_library.model.Playlist
import io.reactivex.Single

interface AlbumsMenuCallbacks {

    fun createPlaylistFromAlbums(albums: List<Album>)

    fun addAlbumsToPlaylist(playlist: Playlist, albums: List<Album>)

    fun addAlbumsToQueue(albums: List<Album>)

    fun playAlbumsNext(albums: List<Album>)

    fun play(album: Album)

    fun editTags(album: Album)

    fun albumInfo(album: Album)

    fun editArtwork(album: Album)

    fun blacklistAlbums(albums: List<Album>)

    fun deleteAlbums(albums: List<Album>)

    fun goToArtist(album: Album)

    fun <T> transform(src: Single<List<T>>, dst: (List<T>) -> Unit)
}

fun AlbumsMenuCallbacks.createPlaylistFromAlbums(albums: Single<List<Album>>) {
    transform(albums) { albums -> createPlaylistFromAlbums(albums) }
}

fun AlbumsMenuCallbacks.addAlbumsToPlaylist(playlist: Playlist, albums: Single<List<Album>>) {
    transform(albums) { albums -> addAlbumsToPlaylist(playlist, albums) }
}

fun AlbumsMenuCallbacks.playAlbumsNext(albums: Single<List<Album>>) {
    transform(albums) { albums -> playAlbumsNext(albums) }
}

fun AlbumsMenuCallbacks.addAlbumsToQueue(albums: Single<List<Album>>) {
    transform(albums) { albums -> addAlbumsToQueue(albums) }
}

fun AlbumsMenuCallbacks.deleteAlbums(albums: Single<List<Album>>) {
    transform(albums) { albums -> deleteAlbums(albums) }
}

fun AlbumsMenuCallbacks.playAlbumsNext(album: Album) {
    playAlbumsNext(listOf(album))
}

fun AlbumsMenuCallbacks.createPlaylistFromAlbums(album: Album) {
    createPlaylistFromAlbums(listOf(album))
}

fun AlbumsMenuCallbacks.addAlbumsToPlaylist(playlist: Playlist, album: Album) {
    addAlbumsToPlaylist(playlist, listOf(album))
}

fun AlbumsMenuCallbacks.addAlbumsToQueue(album: Album) {
    addAlbumsToQueue(listOf(album))
}

fun AlbumsMenuCallbacks.blacklistAlbums(album: Album) {
    blacklistAlbums(listOf(album))
}

fun AlbumsMenuCallbacks.deleteAlbums(album: Album) {
    deleteAlbums(listOf(album))
}