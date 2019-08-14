package com.simplecity.amp_library.data

import com.jakewharton.rxrelay2.BehaviorRelay
import com.simplecity.amp_library.data.Repository.AlbumsRepository
import com.simplecity.amp_library.model.Album
import com.simplecity.amp_library.utils.LogUtils
import com.simplecity.amp_library.utils.Operators
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlbumsRepository @Inject constructor(private val songsRepository: Repository.SongsRepository) : AlbumsRepository {

    private var albumsSubscription: Disposable? = null
    private val albumsRelay = BehaviorRelay.create<List<Album>>()

    override fun getAlbums(): Observable<List<Album>> {
        if (albumsSubscription == null || albumsSubscription?.isDisposed == true) {
            albumsSubscription = songsRepository.getSongs()
                .flatMap { songs -> Observable.just(Operators.songsToAlbums(songs)) }
                .subscribe(
                    albumsRelay,
                    Consumer { error -> LogUtils.logException(PlaylistsRepository.TAG, "Failed to get albums", error) }
                )
        }
        return albumsRelay.subscribeOn(Schedulers.io())
    }

    companion object {
        const val TAG = "AlbumsRepository"
    }
}