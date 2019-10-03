package com.simplecity.amp_library.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import android.util.Log
import com.jakewharton.rxrelay2.BehaviorRelay
import com.simplecity.amp_library.R
import com.simplecity.amp_library.data.Repository.SongsRepository
import com.simplecity.amp_library.model.Playlist
import com.simplecity.amp_library.model.Playlist.Type
import com.simplecity.amp_library.sql.sqlbrite.SqlBriteUtils
import com.simplecity.amp_library.utils.LogUtils
import com.simplecity.amp_library.utils.playlists.PlaylistManager
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistsRepository @Inject constructor(
    private val context: Context
) : Repository.PlaylistsRepository {

    private var playlistsSubscription: Disposable? = null
    private val playlistsRelay = BehaviorRelay.create<List<Playlist>>()

    override fun getPlaylists(): Observable<List<Playlist>> {
        if (playlistsSubscription == null || playlistsSubscription?.isDisposed == true) {
            playlistsSubscription = SqlBriteUtils.createObservableList(
                context,
                { cursor -> Playlist(context, cursor) },
                Playlist.getQuery()
            )
                .subscribe(
                    playlistsRelay,
                    Consumer { error -> LogUtils.logException(TAG, "Failed to get playlists", error) }
                )
        }
        return playlistsRelay.subscribeOn(Schedulers.io())
    }

    override fun getAllPlaylists(songsRepository: SongsRepository): Observable<MutableList<Playlist>> {
        val defaultPlaylistsObservable = Observable.fromCallable<List<Playlist>> {
            val playlists = mutableListOf<Playlist>()

            // Todo: Hide Podcasts if there are no songs
            playlists.add(getPodcastPlaylist())
            playlists.add(getRecentlyAddedPlaylist())
            playlists.add(getMostPlayedPlaylist())

            playlists
        }.subscribeOn(Schedulers.io())

        val playlistsObservable = getPlaylists()

        return Observable.combineLatest<List<Playlist>, List<Playlist>, MutableList<Playlist>>(
            defaultPlaylistsObservable, playlistsObservable, BiFunction { defaultPlaylists: List<Playlist>, playlists1: List<Playlist> ->
                val list = mutableListOf<Playlist>()
                list.addAll(defaultPlaylists)
                list.addAll(playlists1)
                list
            })
            .concatMap { playlists ->
                Observable.fromIterable<Playlist?>(playlists)
                    .concatMap<Playlist> { playlist ->
                        songsRepository.getSongs(playlist)
                            .first(emptyList())
                            .flatMapObservable { songs ->
                                if (playlist.type != Type.USER_CREATED && playlist.type != Type.FAVORITES && songs.isEmpty()
                                ) {
                                    Observable.empty()
                                } else {
                                    Observable.just(playlist)
                                }
                            }
                    }
                    .toList()
                    .toObservable()
            }

    }

    override fun deletePlaylist(playlist: Playlist) {
        if (!playlist.canDelete) {
            Log.e(TAG, "Playlist cannot be deleted")
            return
        }

        ContentUris.withAppendedId(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, playlist.id)?.let { uri ->
            context.contentResolver.delete(uri, null, null)
        }
    }

    override fun getPodcastPlaylist(): Playlist {
        return Playlist(
            Type.PODCAST,
            PlaylistManager.PlaylistIds.PODCASTS_PLAYLIST,
            context.getString(R.string.podcasts_title),
            false,
            false,
            false,
            false,
            false
        )
    }

    override fun getRecentlyAddedPlaylist(): Playlist {
        return Playlist(
            Type.RECENTLY_ADDED,
            PlaylistManager.PlaylistIds.RECENTLY_ADDED_PLAYLIST,
            context.getString(R.string.recentlyadded),
            false,
            false,
            false,
            false,
            false
        )
    }

    override fun getMostPlayedPlaylist(): Playlist {
        return Playlist(
            Type.MOST_PLAYED,
            PlaylistManager.PlaylistIds.MOST_PLAYED_PLAYLIST,
            context.getString(R.string.mostplayed),
            false,
            true,
            false,
            false,
            false
        )
    }

    override fun getRecentlyPlayedPlaylist(): Playlist {
        return Playlist(
            Type.RECENTLY_PLAYED,
            PlaylistManager.PlaylistIds.RECENTLY_PLAYED_PLAYLIST,
            context.getString(R.string.suggested_recent_title),
            false,
            false,
            false,
            false,
            false
        )
    }

    companion object {
        const val TAG = "PlaylistsRepository"
    }

}