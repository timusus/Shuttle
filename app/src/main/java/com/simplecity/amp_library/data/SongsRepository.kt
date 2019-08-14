
package com.simplecity.amp_library.data

import android.content.Context
import android.provider.MediaStore
import android.util.Pair
import com.jakewharton.rxrelay2.BehaviorRelay
import com.simplecity.amp_library.data.Repository.SongsRepository
import com.simplecity.amp_library.model.Album
import com.simplecity.amp_library.model.AlbumArtist
import com.simplecity.amp_library.model.Genre
import com.simplecity.amp_library.model.InclExclItem
import com.simplecity.amp_library.model.Playlist
import com.simplecity.amp_library.model.Query
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.sql.providers.PlayCountTable
import com.simplecity.amp_library.sql.sqlbrite.SqlBriteUtils
import com.simplecity.amp_library.utils.ComparisonUtils
import com.simplecity.amp_library.utils.LogUtils
import com.simplecity.amp_library.utils.SettingsManager
import com.simplecity.amp_library.utils.StringUtils
import com.simplecity.amp_library.utils.playlists.PlaylistManager
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import io.reactivex.functions.Function3
import io.reactivex.schedulers.Schedulers
import java.util.ArrayList
import java.util.Arrays
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class SongsRepository @Inject constructor(
    private val context: Context,
    private val blacklistRepository: Repository.BlacklistRepository,
    private val whitelistRepository: Repository.WhitelistRepository,
    private val settingsManager: SettingsManager
) : SongsRepository {

    private var songsSubscription: Disposable? = null
    private val songsRelay = BehaviorRelay.create<List<Song>>()

    private var allSongsSubscription: Disposable? = null
    private val allSongsRelay = BehaviorRelay.create<List<Song>>()

    override fun getAllSongs(): Observable<List<Song>> {
        if (allSongsSubscription == null || allSongsSubscription?.isDisposed == true) {
            allSongsSubscription = SqlBriteUtils.createObservableList<Song>(context, { Song(it) }, Song.getQuery())
                .subscribe(
                    allSongsRelay,
                    Consumer { error -> LogUtils.logException(PlaylistsRepository.TAG, "Failed to get all songs", error) }
                )
        }

        return allSongsRelay
            .subscribeOn(Schedulers.io())
    }

    override fun getSongs(predicate: ((Song) -> Boolean)?): Observable<List<Song>> {
        if (songsSubscription == null || songsSubscription?.isDisposed == true) {
            songsSubscription = getAllSongs()
                .compose(getInclExclTransformer())
                .map { songs ->
                    songs
                        .filterNot { song -> song.isPodcast }
                        .toList()
                }
                .subscribe(songsRelay)
        }

        return songsRelay
            .map { songs -> predicate?.let { predicate -> songs.filter(predicate) } ?: songs }
            .subscribeOn(Schedulers.io())
    }

    override fun getSongs(album: Album): Observable<List<Song>> {
        return getSongs { song -> song.albumId == album.id }
    }

    override fun getSongs(albumArtist: AlbumArtist): Observable<List<Song>> {
        return getSongs { song ->
            albumArtist.albums
                .map { album -> album.id }
                .any { albumId -> albumId == song.albumId }
        }
    }

    override fun getSongs(playlist: Playlist): Observable<List<Song>> {
        return when (playlist.id) {
            PlaylistManager.PlaylistIds.RECENTLY_ADDED_PLAYLIST -> {
                val numWeeks = settingsManager.numWeeks * 3600 * 24 * 7
                return getSongs { song -> song.dateAdded > System.currentTimeMillis() / 1000 - numWeeks }
                    .map { songs ->
                        songs
                            .sortedWith(Comparator { a, b -> ComparisonUtils.compare(a.albumArtistName, b.albumArtistName) })
                            .sortedWith(Comparator { a, b -> ComparisonUtils.compare(a.albumArtistName, b.albumArtistName) })
                            .sortedWith(Comparator { a, b -> ComparisonUtils.compareInt(b.year, a.year) })
                            .sortedWith(Comparator { a, b -> ComparisonUtils.compareInt(a.track, b.track) })
                            .sortedWith(Comparator { a, b -> ComparisonUtils.compareInt(a.discNumber, b.discNumber) })
                            .sortedWith(Comparator { a, b -> ComparisonUtils.compare(a.albumName, b.albumName) })
                            .sortedWith(Comparator { a, b -> ComparisonUtils.compareLong(b.dateAdded.toLong(), a.dateAdded.toLong()) })
                    }
            }

            PlaylistManager.PlaylistIds.PODCASTS_PLAYLIST -> {
                getAllSongs()
                    .compose(getInclExclTransformer())
                    .map { songs -> songs.filter { song -> song.isPodcast } }
                    .map { songs -> songs.sortedWith(Comparator { a, b -> ComparisonUtils.compareLong(a.playlistSongPlayOrder, b.playlistSongPlayOrder) }) }
            }

            PlaylistManager.PlaylistIds.MOST_PLAYED_PLAYLIST -> {
                val query = Query.Builder()
                    .uri(PlayCountTable.URI)
                    .projection(arrayOf(PlayCountTable.COLUMN_ID, PlayCountTable.COLUMN_PLAY_COUNT))
                    .sort(PlayCountTable.COLUMN_PLAY_COUNT + " DESC")
                    .build()

                SqlBriteUtils.createObservableList(context, { cursor ->
                    Pair(
                        cursor.getLong(cursor.getColumnIndexOrThrow(PlayCountTable.COLUMN_ID)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(PlayCountTable.COLUMN_PLAY_COUNT))
                    )
                }, query)
                    .flatMap { pairs ->
                        getSongs { song ->
                            pairs.firstOrNull { pair ->
                                song.playCount = pair.second
                                pair.first == song.id && pair.second >= 2
                            } != null
                        }.map { songs -> songs.sortedWith(Comparator { a, b -> ComparisonUtils.compareInt(b.playCount, a.playCount) }) }
                    }
            }

            PlaylistManager.PlaylistIds.RECENTLY_PLAYED_PLAYLIST -> {
                val query = Query.Builder()
                    .uri(PlayCountTable.URI)
                    .projection(arrayOf(PlayCountTable.COLUMN_ID, PlayCountTable.COLUMN_TIME_PLAYED))
                    .sort(PlayCountTable.COLUMN_TIME_PLAYED + " DESC")
                    .build()

                SqlBriteUtils.createObservableList(context, { cursor ->
                    Pair(
                        cursor.getLong(cursor.getColumnIndexOrThrow(PlayCountTable.COLUMN_ID)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(PlayCountTable.COLUMN_TIME_PLAYED))
                    )
                }, query)
                    .flatMap { pairs ->
                        getSongs { song ->
                            pairs.filter { pair ->
                                song.lastPlayed = pair.second
                                pair.first == song.id
                            }.firstOrNull() != null
                        }.map { songs -> songs.sortedWith(Comparator { a, b -> ComparisonUtils.compareLong(b.lastPlayed, a.lastPlayed) }) }
                    }
            }

            else -> {
                val query = Song.getQuery()
                query.uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlist.id)
                val projection = ArrayList(Arrays.asList(*Song.getProjection()))
                projection.add(MediaStore.Audio.Playlists.Members._ID)
                projection.add(MediaStore.Audio.Playlists.Members.AUDIO_ID)
                projection.add(MediaStore.Audio.Playlists.Members.PLAY_ORDER)
                query.projection = projection.toTypedArray()

                SqlBriteUtils.createObservableList<Song>(context, { Playlist.createSongFromPlaylistCursor(it) }, query)
                    .map { songs -> songs.sortedWith(Comparator { a, b -> ComparisonUtils.compareLong(a.playlistSongPlayOrder, b.playlistSongPlayOrder) }) }
            }
        }
    }

    override fun getSongs(genre: Genre): Observable<List<Song>> {
        return getSongs()
            .map { songs ->
                songs.sortedWith(Comparator { a, b -> ComparisonUtils.compareInt(b.year, a.year) })
                    .sortedWith(Comparator { a, b -> ComparisonUtils.compareInt(a.track, b.track) })
                    .sortedWith(Comparator { a, b -> ComparisonUtils.compareInt(a.discNumber, b.discNumber) })
                    .sortedWith(Comparator { a, b -> ComparisonUtils.compare(a.albumName, b.albumName) })
                    .sortedWith(Comparator { a, b -> ComparisonUtils.compare(a.albumArtistName, b.albumArtistName) })
            }
    }

    private fun getInclExclTransformer(): ObservableTransformer<List<Song>, List<Song>> {
        return ObservableTransformer { upstream ->
            Observable.combineLatest<List<Song>, List<InclExclItem>, List<InclExclItem>, List<Song>>(
                upstream,
                whitelistRepository.getWhitelistItems(this),
                blacklistRepository.getBlacklistItems(this),
                Function3 { songs: List<Song>, inclItems: List<InclExclItem>, exclItems: List<InclExclItem> ->
                    var result = songs

                    // Filter out excluded paths
                    if (!exclItems.isEmpty()) {
                        result = songs
                            .filterNot { song -> exclItems.any { exclItem -> StringUtils.containsIgnoreCase(song.path, exclItem.path) } }
                            .toList()
                    }

                    // Filter out non-included paths
                    if (!inclItems.isEmpty()) {
                        result = result
                            .filter { song -> inclItems.any { inclItem -> StringUtils.containsIgnoreCase(song.path, inclItem.path) } }
                            .toList()
                    }

                    result
                })
        }
    }

    companion object {
        const val TAG = "SongsRepository"
    }
}