package com.simplecity.amp_library.ui.detail.genre

import android.content.Context
import android.support.v4.util.Pair
import android.view.MenuItem
import com.simplecity.amp_library.model.Album
import com.simplecity.amp_library.model.Genre
import com.simplecity.amp_library.model.Playlist
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.playback.MediaManager
import com.simplecity.amp_library.rx.UnsafeAction
import com.simplecity.amp_library.ui.presenters.Presenter
import com.simplecity.amp_library.utils.LogUtils
import com.simplecity.amp_library.utils.Operators
import com.simplecity.amp_library.utils.PermissionUtils
import com.simplecity.amp_library.utils.PlaylistUtils
import com.simplecity.amp_library.utils.sorting.SortManager
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import java.util.Random
import java.util.concurrent.TimeUnit

class GenreDetailPresenter constructor(private val mediaManager: MediaManager, private val genre: Genre) : Presenter<GenreDetailView>() {

    private var songs: MutableList<Song> = mutableListOf()

    private var currentSlideShowAlbum: Album? = null

    override fun bindView(view: GenreDetailView) {
        super.bindView(view)

        startSlideShow()
    }

    private fun sortSongs(songs: MutableList<Song>) {
        @SortManager.SongSort val songSort = SortManager.getInstance().genreDetailSongsSortOrder

        val songsAscending = SortManager.getInstance().genreDetailSongsAscending

        SortManager.getInstance().sortSongs(songs, songSort)
        if (!songsAscending) {
            songs.reverse()
        }
    }

    private fun sortAlbums(albums: MutableList<Album>) {
        @SortManager.AlbumSort val albumSort = SortManager.getInstance().genreDetailAlbumsSortOrder

        val albumsAscending = SortManager.getInstance().genreDetailAlbumsAscending

        SortManager.getInstance().sortAlbums(albums, albumSort)
        if (!albumsAscending) {
            albums.reverse()
        }
    }

    fun loadData() {
        PermissionUtils.RequestStoragePermissions {
            addDisposable(
                genre.songsObservable
                    .zipWith<MutableList<Album>, Pair<MutableList<Album>, MutableList<Song>>>(
                        genre.songsObservable.map { songs -> Operators.songsToAlbums(songs) },
                        BiFunction { songs, albums -> Pair(albums, songs) }).subscribeOn(Schedulers.io())
                    .doOnSuccess { pair ->
                        sortAlbums(pair.first!!)
                        sortSongs(pair.second!!)
                    }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { pair ->
                        this.songs = pair.second!!

                        view?.setData(pair)
                    }
            )
        }
    }

    private fun startSlideShow() {
        val albumsObservable: Observable<List<Album>> = genre.songsObservable.toObservable()
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

    fun fabClicked() {
        mediaManager.shuffleAll(songs) { message ->
            view?.showToast(message)
        }
    }

    fun playAll() {
        mediaManager.playAll(songs, 0, true) { message ->
            view?.showToast(message)
        }
    }

    fun playNext() {
        mediaManager.playNext(songs) { message ->
            view?.showToast(message)
        }
    }

    fun addToQueue() {
        mediaManager.addToQueue(songs) { message ->
            view?.showToast(message)
        }
    }

    fun playlistSelected(context: Context, item: MenuItem, insertCallback: UnsafeAction) {
        val playlist = item.intent.getSerializableExtra(PlaylistUtils.ARG_PLAYLIST) as Playlist
        PlaylistUtils.addToPlaylist(context, playlist, songs, insertCallback)
    }

    fun newPlaylist() {
        view?.showCreatePlaylistDialog(songs.toList())
    }

    fun songClicked(song: Song) {
        mediaManager.playAll(songs, songs.indexOf(song), true) { message ->
            view?.showToast(message)
        }
    }

    companion object {
        const val TAG = "GenreDetailPresenter"
    }
}
