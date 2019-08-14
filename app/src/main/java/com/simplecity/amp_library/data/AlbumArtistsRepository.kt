package com.simplecity.amp_library.data

import com.jakewharton.rxrelay2.BehaviorRelay
import com.simplecity.amp_library.data.Repository.AlbumArtistsRepository
import com.simplecity.amp_library.data.Repository.AlbumsRepository
import com.simplecity.amp_library.model.AlbumArtist
import com.simplecity.amp_library.utils.LogUtils
import com.simplecity.amp_library.utils.Operators
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlbumArtistsRepository @Inject constructor(private val albumsRepository: AlbumsRepository) : AlbumArtistsRepository {

    private var albumArtistsSubscription: Disposable? = null
    private val albumArtistsRelay = BehaviorRelay.create<List<AlbumArtist>>()

    override fun getAlbumArtists(): Observable<List<AlbumArtist>> {
        if (albumArtistsSubscription == null || albumArtistsSubscription?.isDisposed == true) {
            albumArtistsSubscription = albumsRepository.getAlbums()
                .flatMap { albums -> Observable.just(Operators.albumsToAlbumArtists(albums)) }
                .subscribe(
                    albumArtistsRelay,
                    Consumer { error -> LogUtils.logException(PlaylistsRepository.TAG, "Failed to get album artists", error) }
                )
        }
        return albumArtistsRelay.subscribeOn(Schedulers.io())
    }

    companion object {
        const val TAG = "AlbumArtistsRepository"
    }

}