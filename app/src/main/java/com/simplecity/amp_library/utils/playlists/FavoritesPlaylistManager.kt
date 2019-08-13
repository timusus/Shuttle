package com.simplecity.amp_library.utils.playlists

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import android.support.v4.util.Pair
import com.simplecity.amp_library.R
import com.simplecity.amp_library.data.PlaylistsRepository
import com.simplecity.amp_library.data.SongsRepository
import com.simplecity.amp_library.model.Playlist
import com.simplecity.amp_library.model.Playlist.Type
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.utils.LogUtils
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import java.util.Collections
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class FavoritesPlaylistManager @Inject constructor(
    private val applicationContext: Context,
    private val playlistManager: PlaylistManager,
    private val playlistsRepository: PlaylistsRepository,
    private val songsRepository: SongsRepository
) {

    fun getFavoritesPlaylist(): Single<Playlist?> {
        return playlistsRepository.getPlaylists()
            .first(Collections.emptyList())
            .flatMapObservable { playlists -> Observable.fromIterable(playlists) }
            .filter { playlist -> playlist.type == Type.FAVORITES }
            .switchIfEmpty(Maybe.fromCallable { createFavoritePlaylist() }.toObservable())
            .firstOrError()
            .doOnError { throwable -> LogUtils.logException(TAG, "getFavoritesPlaylist failed", throwable) }
    }

    fun isFavorite(song: Song?): Observable<Boolean> {
        return if (song == null) {
            Observable.just(false)
        } else getFavoritesPlaylist().flatMapObservable { playlist -> songsRepository.getSongs(playlist) }
            .map { songs -> songs.contains(song) }

    }

    fun createFavoritePlaylist(): Playlist? {
        val playlist = playlistManager.createPlaylist(applicationContext.getString(R.string.fav_title))
        if (playlist != null) {
            playlist.canDelete = false
            playlist.canRename = false
            playlist.type = Playlist.Type.FAVORITES
        }
        return playlist
    }

    fun clearFavorites(): Disposable {
        return getFavoritesPlaylist()
            .flatMapCompletable { playlist ->
                Completable.fromAction {
                    val uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlist.id)
                    applicationContext.contentResolver.delete(uri, null, null)
                }
            }
            .subscribeOn(Schedulers.io())
            .subscribe(
                { },
                { throwable -> LogUtils.logException(TAG, "clearFavorites error", throwable) }
            )
    }

    fun toggleFavorite(song: Song, isFavorite: (Boolean) -> Unit): Disposable {
        return isFavorite(song)
            .first(false)
            .subscribeOn(Schedulers.io())
            .subscribe(
                { favorite ->
                    if (!favorite) {
                        addToFavorites(song) { success ->
                            if (success) {
                                isFavorite.invoke(true)
                            }
                        }
                    } else {
                        removeFromFavorites(song) { success ->
                            if (success) {
                                isFavorite.invoke(false)
                            }
                        }
                    }
                },
                { error -> LogUtils.logException(TAG, "PlaylistManager: Error toggling favorites", error) }
            )
    }

    fun addToFavorites(song: Song, success: (Boolean) -> Unit): Disposable {
        return Single.zip<Playlist, Int, Pair<Playlist, Int>>(
            getFavoritesPlaylist(),
            getFavoritesPlaylist().flatMapObservable<List<Song>> { songsRepository.getSongs(it) }
                .first(emptyList())
                .map { it.size },
            BiFunction { first, second -> Pair(first, second) })
            .map { pair ->
                val uri = MediaStore.Audio.Playlists.Members.getContentUri("external", pair.first!!.id)
                val values = ContentValues()
                values.put(MediaStore.Audio.Playlists.Members.AUDIO_ID, song.id)
                values.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, pair.second!! + 1)
                val newUri = applicationContext.contentResolver.insert(uri, values)
                applicationContext.contentResolver.notifyChange(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, null)
                newUri != null
            }
            .delay(150, TimeUnit.MILLISECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { success.invoke(it) },
                { throwable -> LogUtils.logException(TAG, "Error adding to playlist", throwable) }
            )
    }

    fun removeFromFavorites(song: Song, callback: (Boolean) -> Unit): Disposable {
        return getFavoritesPlaylist()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { playlist -> playlist?.let { playlistManager.removeFromPlaylist(it, song, callback) } },
                { error -> LogUtils.logException(TAG, "PlaylistManager: Error Removing from favorites", error) }
            )
    }

    companion object {

        private val TAG = "FavoritesPlaylistManage"
    }
}