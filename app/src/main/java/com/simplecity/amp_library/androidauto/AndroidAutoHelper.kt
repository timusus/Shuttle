package com.simplecity.amp_library.androidauto

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaDescriptionCompat.Builder
import android.util.Log
import com.simplecity.amp_library.R.string
import com.simplecity.amp_library.ShuttleApplication
import com.simplecity.amp_library.model.Album
import com.simplecity.amp_library.model.AlbumArtist
import com.simplecity.amp_library.model.Genre
import com.simplecity.amp_library.model.Playlist
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.utils.DataManager
import com.simplecity.amp_library.utils.StringUtils
import io.reactivex.Single

sealed class MediaIdWrapper {

    class RootDirectory : MediaIdWrapper()

    class ArtistDirectory : MediaIdWrapper()

    class AlbumDirectory(var artistHash: Int?) : MediaIdWrapper()

    class SongDirectory(var artistHash: String?, var albumId: Long?) : MediaIdWrapper()

    class PlaylistDirectory : MediaIdWrapper()

    class GenreDirectory : MediaIdWrapper()

    class Song(var artistHash: String?, var albumId: Long?, var songId: Long?) : MediaIdWrapper()

    class Genre(var genreId: Long) : MediaIdWrapper()

    class Playlist(var playlistId: Long) : MediaIdWrapper()
}

class MediaIdHelper {

    companion object {
        const val TAG = "MediaIdHelper"
    }

    @Throws(IllegalStateException::class)
    private fun parseMediaId(mediaId: String): MediaIdWrapper {

        val uri = Uri.parse(mediaId)

        return when (uri.pathSegments.firstOrNull()) {

            "root" -> {
                MediaIdWrapper.RootDirectory()
            }
            else -> {
                val artistHash = uri.pathSegments.getNextSegment("artists")
                val albumId = uri.pathSegments.getNextSegment("albums")?.toLongOrNull()
                val songId = uri.pathSegments.getNextSegment("songs")?.toLongOrNull()
                val playlistId = uri.pathSegments.getNextSegment("playlists")?.toLongOrNull()
                val genreId = uri.pathSegments.getNextSegment("genres")?.toLongOrNull()

                if (uri.toString().endsWith('/')) {
                    when {
                        uri.pathSegments.contains("songs") -> MediaIdWrapper.SongDirectory(artistHash, albumId)
                        uri.pathSegments.contains("albums") -> MediaIdWrapper.AlbumDirectory(artistHash?.toInt())
                        uri.pathSegments.contains("artists") -> MediaIdWrapper.ArtistDirectory()
                        uri.pathSegments.contains("playlists") -> MediaIdWrapper.PlaylistDirectory()
                        uri.pathSegments.contains("genres") -> MediaIdWrapper.GenreDirectory()
                        else -> {
                            throw IllegalStateException("Unknown MediaId '$mediaId' path")
                        }
                    }
                } else {
                    when {
                        playlistId != null -> MediaIdWrapper.Playlist(playlistId)
                        genreId != null -> MediaIdWrapper.Genre(genreId)
                        else -> MediaIdWrapper.Song(artistHash, albumId, songId)
                    }
                }
            }
        }
    }

    private fun List<String>.getNextSegment(segmentName: String): String? {
        val index = indexOf(segmentName)
        if (index >= 0 && size > index + 1) {
            return this[index + 1]
        }
        return null
    }

    fun getChildren(mediaId: String, result: (MutableList<MediaBrowserCompat.MediaItem>) -> Unit) {

        val mediaIdWrapper: MediaIdWrapper? = try {
            parseMediaId(mediaId)
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Failed to parse media id: ${e.localizedMessage}")
            null
        }

        when (mediaIdWrapper) {
            is MediaIdWrapper.RootDirectory -> result(
                mutableListOf(
                    MediaItem(
                        Builder()
                            .setTitle(ShuttleApplication.getInstance().getString(string.artists_title))
                            .setMediaId("media:/artists/")
                            .build(), MediaItem.FLAG_BROWSABLE
                    ),
                    MediaItem(
                        Builder()
                            .setTitle(ShuttleApplication.getInstance().getString(string.albums_title))
                            .setMediaId("media:/albums/")
                            .build(), MediaItem.FLAG_BROWSABLE
                    ), MediaItem(
                        Builder()
                            .setTitle(ShuttleApplication.getInstance().getString(string.playlists_title))
                            .setMediaId("media:/playlists/")
                            .build(), MediaItem.FLAG_BROWSABLE
                    ), MediaItem(
                        Builder()
                            .setTitle(ShuttleApplication.getInstance().getString(string.genres_title))
                            .setMediaId("media:/genres/")
                            .build(), MediaItem.FLAG_BROWSABLE
                    )
                )
            )
            is MediaIdWrapper.GenreDirectory -> listGenres(mediaId, result)
            is MediaIdWrapper.PlaylistDirectory -> listPlaylists(mediaId, result)
            is MediaIdWrapper.ArtistDirectory -> listArtists(mediaId, result)
            is MediaIdWrapper.AlbumDirectory -> listAlbums(mediaId, mediaIdWrapper.artistHash, result)
            is MediaIdWrapper.SongDirectory -> listSongs(mediaId, mediaIdWrapper.albumId, result)
            else -> result(mutableListOf())
        }
    }

    fun getSongListForMediaId(mediaId: String, completion: (List<Song>, position: Int) -> Unit) {
        val mediaWrapper = parseMediaId(mediaId)
        when (mediaWrapper) {
            is MediaIdWrapper.Song -> {
                getSongsForPredicate { if (mediaWrapper.albumId == null) true else it.albumId == mediaWrapper.albumId }
                    .map { songs ->
                        songs
                            .sortedBy { it.albumArtistName }
                            .sortedBy { it.albumName }
                            .sortedBy { it.track }
                    }
                    .subscribe(
                        { songs -> completion(songs, songs.indexOfFirst { it.id == mediaWrapper.songId }.or(0)) },
                        { completion(mutableListOf(), 0) }
                    )
            }
            is MediaIdWrapper.Playlist -> {
                getSongsForPlaylistId(mediaWrapper.playlistId)
                    .subscribe(
                        { songs -> completion(songs, 0) },
                        { completion(mutableListOf(), 0) }
                    )
            }
            is MediaIdWrapper.Genre -> {
                getSongsForGenreId(mediaWrapper.genreId)
                    .subscribe(
                        { songs -> completion(songs, 0) },
                        { completion(mutableListOf(), 0) }
                    )
            }
        }
    }

    fun handlePlayFromSearch(query: String, extras: Bundle): Single<List<Song>> {
        val mediaFocus = extras.getString(MediaStore.EXTRA_MEDIA_FOCUS)
        when (mediaFocus) {
            MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE -> {
                extras.getString(MediaStore.EXTRA_MEDIA_ARTIST)?.let { artist ->
                    return getSongsForPredicate { it.artistName.equals(artist, true) }
                        .map { songs ->
                            songs
                                .sortedBy { it.albumName }
                                .sortedBy { it.track }
                        }
                }
            }
            MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE -> {
                extras.getString(MediaStore.EXTRA_MEDIA_ALBUM)?.let { album ->
                    return getSongsForPredicate { it.albumName.equals(album, true) }
                        .map { songs ->
                            songs
                                .sortedBy { it.track }
                        }
                }
            }
            MediaStore.Audio.Genres.ENTRY_CONTENT_TYPE -> {
                extras.getString(MediaStore.EXTRA_MEDIA_GENRE)?.let { genre ->
                    return DataManager.getInstance().genresRelay
                        .first(emptyList())
                        .flatMap { Single.just(it.first { it.name == genre }) }
                        .flatMap { it.songsObservable }
                        .map { songs ->
                            songs
                                .sortedBy { it.playlistSongPlayOrder }
                                .toMutableList()
                        }
                }
            }
        }

        return getSongsForPredicate { it.name.contains(query, true) }
            .flatMap {
                if (it.isEmpty()) {
                    Single.just(emptyList<Song>())
                } else {
                    it.first().album.songsSingle
                }
            }
            .map { songs ->
                songs
                    .sortedBy { it.artistName }
                    .sortedBy { it.albumName }
                    .sortedBy { it.track }
                    .sortedBy { it.discNumber }
            }
    }

    // DataManager helpers

    @SuppressLint("CheckResult")
    private fun listArtists(mediaId: String, completion: (MutableList<MediaBrowserCompat.MediaItem>) -> Unit) {
        DataManager.getInstance().albumArtistsRelay.first(emptyList())
            .map { albumArtists ->
                albumArtists
                    .sortedBy { albumArtist -> StringUtils.keyFor(albumArtist.name) }
                    .map { albumArtist -> albumArtist.toMediaItem(mediaId) }
                    .toMutableList()
            }
            .subscribe({ mediaItems -> completion(mediaItems) }, {})
    }

    @SuppressLint("CheckResult")
    private fun listPlaylists(mediaId: String, completion: (MutableList<MediaBrowserCompat.MediaItem>) -> Unit) {
        DataManager.getInstance().playlistsRelay.first(emptyList())
            .map { playlists ->
                playlists
                    .sortedBy { playlist -> playlist.type }
                    .map { playlist -> playlist.toMediaItem(mediaId) }
                    .toMutableList()
            }
            .subscribe({ mediaItems -> completion(mediaItems) }, {})
    }

    @SuppressLint("CheckResult")
    private fun listGenres(mediaId: String, completion: (MutableList<MediaBrowserCompat.MediaItem>) -> Unit) {
        DataManager.getInstance().genresRelay.first(emptyList())
            .map { genres ->
                genres
                    .sortedBy { genre -> genre.name }
                    .map { genre -> genre.toMediaItem(mediaId) }
                    .toMutableList()
            }
            .subscribe({ mediaItems -> completion(mediaItems) }, {})
    }

    @SuppressLint("CheckResult")
    private fun listAlbums(mediaId: String, artistHash: Int?, completion: (MutableList<MediaItem>) -> Unit) {

        val albumsSingle = if (artistHash != null) {
            DataManager.getInstance().albumArtistsRelay.first(emptyList())
                .map { albumArtists -> albumArtists.first { it.hashCode() == artistHash } }
                .map { albumArtist -> albumArtist.albums }
        } else {
            DataManager.getInstance().albumsRelay.first(emptyList())
        }

        albumsSingle.map { albums ->
            albums
                .sortedBy { it.name }
                .map { album -> album.toMediaItem(mediaId) }
                .toMutableList()
        }
            .subscribe({ mediaItems -> completion(mediaItems) }, {})
    }

    @SuppressLint("CheckResult")
    private fun listSongs(mediaId: String, albumId: Long?, completion: (MutableList<MediaItem>) -> Unit) {
        getSongsForPredicate { if (albumId == null) true else it.albumId == albumId }
            .map { songs ->
                songs
                    .sortedBy { it.albumArtistName }
                    .sortedBy { it.albumName }
                    .sortedBy { it.track }
                    .map { song -> song.toMediaItem(mediaId) }
                    .toMutableList()
            }
            .subscribe({ mediaItems -> completion(mediaItems) }, {})
    }

    private fun getSongsForPredicate(predicate: (Song) -> Boolean): Single<List<Song>> {
        return DataManager.getInstance().getSongsObservable(predicate).first(emptyList())
    }

    private fun getSongsForPlaylistId(playlistId: Long?): Single<List<Song>> {
        return DataManager.getInstance().playlistsRelay
            .first(emptyList())
            .flatMap { Single.just(it.first { it.id == playlistId }) }
            .flatMap { it.songsObservable.first(emptyList()) }
            .map { songs ->
                songs
                    .sortedBy { it.playlistSongPlayOrder }
                    .toMutableList()
            }
    }

    private fun getSongsForGenreId(genreId: Long?): Single<MutableList<Song>> {
        return DataManager.getInstance().genresRelay
            .first(emptyList())
            .flatMap { Single.just(it.first { it.id == genreId }) }
            .flatMap { it.songsObservable }
            .map { songs ->
                songs.shuffled().toMutableList()
            }
    }

    // MediaItem helpers

    private fun Playlist.toMediaItem(parent: String): MediaItem {
        return MediaItem(
            Builder()
                .setTitle(name)
                .setMediaId("$parent$id/songs")
                .build(), MediaItem.FLAG_PLAYABLE
        )
    }

    private fun Genre.toMediaItem(parent: String): MediaItem {
        return MediaItem(
            Builder()
                .setTitle(name)
                .setMediaId("$parent$id/songs")
                .build(), MediaItem.FLAG_PLAYABLE
        )
    }

    private fun AlbumArtist.toMediaItem(parent: String): MediaItem {
        return MediaItem(
            Builder()
                .setTitle(name)
                .setMediaId("$parent${hashCode()}/albums/")
                .build(), MediaItem.FLAG_BROWSABLE
        )
    }

    private fun Album.toMediaItem(parent: String): MediaItem {
        return MediaItem(
            Builder()
                .setTitle(name)
                .setSubtitle(albumArtistName)
                .setMediaId("$parent$id/songs/")
                .build(), MediaItem.FLAG_BROWSABLE
        )
    }

    private fun Song.toMediaItem(parent: String): MediaItem {
        return MediaItem(
            Builder()
                .setTitle(name)
                .setSubtitle(albumArtistName)
                .setMediaUri(Uri.parse(path))
                .setMediaId("$parent$id")
                .build(), MediaItem.FLAG_PLAYABLE
        )
    }
}