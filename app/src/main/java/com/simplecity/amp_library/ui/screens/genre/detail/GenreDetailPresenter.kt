package com.simplecity.amp_library.ui.screens.genre.detail

import android.content.Context
import android.support.v4.util.Pair
import com.simplecity.amp_library.model.Album
import com.simplecity.amp_library.model.Genre
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.playback.MediaManager
import com.simplecity.amp_library.ui.common.Presenter
import com.simplecity.amp_library.ui.screens.album.menu.AlbumMenuContract
import com.simplecity.amp_library.ui.screens.album.menu.AlbumMenuPresenter
import com.simplecity.amp_library.ui.screens.genre.menu.GenreMenuContract
import com.simplecity.amp_library.ui.screens.genre.menu.GenreMenuPresenter
import com.simplecity.amp_library.ui.screens.songs.menu.SongMenuContract
import com.simplecity.amp_library.ui.screens.songs.menu.SongMenuPresenter
import com.simplecity.amp_library.utils.LogUtils
import com.simplecity.amp_library.utils.Operators
import com.simplecity.amp_library.utils.PermissionUtils
import com.simplecity.amp_library.utils.extensions.getSongsObservable
import com.simplecity.amp_library.utils.sorting.SortManager
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import java.util.Random
import java.util.concurrent.TimeUnit

class GenreDetailPresenter @AssistedInject constructor(
    private val context: Context,
    private val mediaManager: MediaManager,
    private val sortManager: SortManager,
    private val genreMenuPresenter: GenreMenuPresenter,
    private val albumMenuPresenter: AlbumMenuPresenter,
    private val songsMenuPresenter: SongMenuPresenter,
    @Assisted private val genre: Genre

) : Presenter<GenreDetailView>(),
    GenreMenuContract.Presenter by genreMenuPresenter,
    AlbumMenuContract.Presenter by albumMenuPresenter,
    SongMenuContract.Presenter by songsMenuPresenter {

    @AssistedInject.Factory
    interface Factory {
        fun create(genre: Genre): GenreDetailPresenter
    }

    private var songs: MutableList<Song> = mutableListOf()

    private var currentSlideShowAlbum: Album? = null

    override fun bindView(view: GenreDetailView) {
        super.bindView(view)

        genreMenuPresenter.bindView(view)
        albumMenuPresenter.bindView(view)
        songsMenuPresenter.bindView(view)

        startSlideShow()
    }

    override fun unbindView(view: GenreDetailView) {
        super.unbindView(view)

        genreMenuPresenter.unbindView(view)
        albumMenuPresenter.unbindView(view)
        songsMenuPresenter.unbindView(view)
    }

    private fun sortSongs(songs: MutableList<Song>) {
        @SortManager.SongSort val songSort = sortManager.genreDetailSongsSortOrder

        val songsAscending = sortManager.genreDetailSongsAscending

        sortManager.sortSongs(songs, songSort)
        if (!songsAscending) {
            songs.reverse()
        }
    }

    private fun sortAlbums(albums: MutableList<Album>) {
        @SortManager.AlbumSort val albumSort = sortManager.genreDetailAlbumsSortOrder

        val albumsAscending = sortManager.genreDetailAlbumsAscending

        sortManager.sortAlbums(albums, albumSort)
        if (!albumsAscending) {
            albums.reverse()
        }
    }

    fun loadData() {
        PermissionUtils.RequestStoragePermissions {
            addDisposable(
                genre.getSongsObservable(context)
                    .zipWith<MutableList<Album>, Pair<MutableList<Album>, MutableList<Song>>>(
                        genre.getSongsObservable(context).map { songs -> Operators.songsToAlbums(songs) },
                        BiFunction { songs, albums -> Pair(albums, songs.toMutableList()) }).subscribeOn(Schedulers.io())
                    .doOnSuccess { pair ->
                        sortAlbums(pair.first!!)
                        sortSongs(pair.second!!)
                    }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { pair ->
                        this.songs = pair.second!!

                        view?.setData(pair.first!!, pair.second!!)
                    }
            )
        }
    }

    private fun startSlideShow() {
        val albumsObservable: Observable<List<Album>> = genre.getSongsObservable(context).toObservable()
            .map { songs -> Operators.songsToAlbums(songs) }

        val timer: Observable<Long> = io.reactivex.Observable.interval(8, TimeUnit.SECONDS)
            // Load an image straight away
            .startWith(0L)
            // If we have a 'current slideshowAlbum' then we're coming back from onResume. Don't load a new one immediately.
            .delay(if (currentSlideShowAlbum == null) 0L else 8L, TimeUnit.SECONDS)

        addDisposable(Observable
            .combineLatest(albumsObservable, timer, BiFunction { albums: List<Album>, aLong: Long -> albums })
            .map { albums ->
                if (albums.isEmpty()) {
                    currentSlideShowAlbum
                } else {
                    albums[(Random().nextInt(albums.size))]
                }
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ newAlbum ->
                newAlbum?.let {
                    view?.fadeInSlideShowAlbum(currentSlideShowAlbum, newAlbum)
                    currentSlideShowAlbum = newAlbum
                }
            }, { error ->
                LogUtils.logException(TAG, "startSlideShow threw error", error)
            })
        )
    }

    fun closeContextualToolbar() {
        view?.closeContextualToolbar()
    }

    fun shuffleAll() {
        mediaManager.shuffleAll(songs) {
            view?.onPlaybackFailed()
        }
    }

    fun songClicked(song: Song) {
        mediaManager.playAll(songs, songs.indexOf(song), true) {
            view?.onPlaybackFailed()
        }
    }

    override fun <T> transform(src: Single<List<T>>, dst: (List<T>) -> Unit) {
        addDisposable(
            src
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(
                    { items -> dst(items) },
                    { error -> LogUtils.logException(SongMenuPresenter.TAG, "Failed to transform src single", error) }
                )
        )
    }

    companion object {
        const val TAG = "GenreDetailPresenter"
    }
}
