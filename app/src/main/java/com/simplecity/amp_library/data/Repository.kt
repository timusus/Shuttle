package com.simplecity.amp_library.data

import com.simplecity.amp_library.model.Album
import com.simplecity.amp_library.model.AlbumArtist
import com.simplecity.amp_library.model.Genre
import com.simplecity.amp_library.model.InclExclItem
import com.simplecity.amp_library.model.Playlist
import com.simplecity.amp_library.model.Song
import io.reactivex.Observable

interface Repository {

    interface SongsRepository {

        /**
         * Returns a continuous List of all [Song]s, no filtering is applied.
         */
        fun getAllSongs(): Observable<List<Song>>

        /**
         * Returns a continuous List of [Song]s, excluding those which are blacklisted, podcasts, or not-whitelisted.
         */
        fun getSongs(predicate: ((Song) -> Boolean)? = null): Observable<List<Song>>

        /**
         * Returns a continuous List of [Song]s belonging to the given [Playlist], excluding those which are blacklisted, podcasts, or not-whitelisted.
         */
        fun getSongs(playlist: Playlist): Observable<List<Song>>

        /**
         * Returns a continuous List of [Song]s belonging to the given [Album], excluding those which are blacklisted, podcasts, or not-whitelisted.
         */
        fun getSongs(album: Album): Observable<List<Song>>

        /**
         * Returns a continuous List of [Song]s belonging to the given [AlbumArtist], excluding those which are blacklisted, podcasts, or not-whitelisted.
         */
        fun getSongs(albumArtist: AlbumArtist): Observable<List<Song>>

        /**
         * Returns a continuous List of [Song]s belonging to the given [Genre], excluding those which are blacklisted, podcasts, or not-whitelisted.
         */
        fun getSongs(genre: Genre): Observable<List<Song>>
    }

    interface AlbumsRepository {

        /**
         * Returns a continuous List of [Album]s
         */
        fun getAlbums(): Observable<List<Album>>
    }

    interface AlbumArtistsRepository {

        /**
         * Returns a continuous list of [AlbumArtist]s
         */
        fun getAlbumArtists(): Observable<List<AlbumArtist>>
    }

    interface GenresRepository {

        /**
         * Returns a continuous List of [Genre]s
         */
        fun getGenres(): Observable<List<Genre>>
    }

    interface PlaylistsRepository {

        /**
         * Returns a continuous List of [Playlist]s
         */
        fun getPlaylists(): Observable<List<Playlist>>

        /**
         * Returns a continuous List of [Playlist]s, including user-created playlists. Empty playlists are no returned.
         */
        fun getAllPlaylists(songsRepository: SongsRepository): Observable<MutableList<Playlist>>

        fun deletePlaylist(playlist: Playlist)


        fun getPodcastPlaylist(): Playlist

        fun getRecentlyAddedPlaylist(): Playlist

        fun getMostPlayedPlaylist(): Playlist

        fun getRecentlyPlayedPlaylist(): Playlist
    }

    interface InclExclRepository {

        fun add(inclExclItem: InclExclItem)

        fun addAll(inclExclItems: List<InclExclItem>)

        fun addSong(song: Song)

        fun addAllSongs(songs: List<Song>)

        fun delete(inclExclItem: InclExclItem)

        fun deleteAll()
    }

    interface BlacklistRepository : InclExclRepository {

        fun getBlacklistItems(songsRepository: Repository.SongsRepository): Observable<List<InclExclItem>>
    }

    interface WhitelistRepository : InclExclRepository {

        fun getWhitelistItems(songsRepository: Repository.SongsRepository): Observable<List<InclExclItem>>
    }
}