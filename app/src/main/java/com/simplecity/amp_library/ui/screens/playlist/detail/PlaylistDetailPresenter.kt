package com.simplecity.amp_library.ui.screens.playlist.detail

import com.simplecity.amp_library.data.Repository.SongsRepository
import com.simplecity.amp_library.model.Album
import com.simplecity.amp_library.model.Playlist
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.playback.MediaManager
import com.simplecity.amp_library.ui.common.Presenter
import com.simplecity.amp_library.ui.screens.playlist.menu.PlaylistMenuContract
import com.simplecity.amp_library.ui.screens.playlist.menu.PlaylistMenuPresenter
import com.simplecity.amp_library.ui.screens.songs.menu.SongMenuContract
import com.simplecity.amp_library.ui.screens.songs.menu.SongMenuPresenter
import com.simplecity.amp_library.utils.ComparisonUtils
import com.simplecity.amp_library.utils.LogUtils
import com.simplecity.amp_library.utils.Operators
import com.simplecity.amp_library.utils.PermissionUtils
import com.simplecity.amp_library.utils.sorting.SortManager
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import java.util.Random
import java.util.concurrent.TimeUnit

class PlaylistDetailPresenter @AssistedInject constructor(
    private val mediaManager: MediaManager,
    private val songsRepository: SongsRepository,
    private val sortManager: SortManager,
    private val playlistMenuPresenter: PlaylistMenuPresenter,
    private val songsMenuPresenter: SongMenuPresenter,
    @Assisted private val playlist: Playlist
) : Presenter<PlaylistDetailView>(),
    PlaylistMenuContract.Presenter by playlistMenuPresenter,
    SongMenuContract.Presenter by songsMenuPresenter {

    @AssistedInject.Factory
    interface Factory {
        fun create(playlist: Playlist): PlaylistDetailPresenter
    }

    private var songs: MutableList<Song> = mutableListOf()

    private var currentSlideShowAlbum: Album? = null

    override fun bindView(view: PlaylistDetailView) {
        super.bindView(view)

        playlistMenuPresenter.bindView(view)
        songsMenuPresenter.bindView(view)

        startSlideShow()
    }

    override fun unbindView(view: PlaylistDetailView) {
        super.unbindView(view)

        playlistMenuPresenter.unbindView(view)
        songsMenuPresenter.unbindView(view)
    }

    private fun sortSongs(songs: MutableList<Song>) {
        @SortManager.SongSort val songSort = sortManager.getPlaylistDetailSongsSortOrder(playlist)

        val songsAscending = sortManager.getPlaylistDetailSongsAscending(playlist)

        sortManager.sortSongs(songs, songSort)
        if (!songsAscending) {
            songs.reverse()
        }

        if (songSort == SortManager.SongSort.DETAIL_DEFAULT) {
            when {
                playlist.type == Playlist.Type.MOST_PLAYED -> songs.sortWith(kotlin.Comparator { a, b -> ComparisonUtils.compareInt(b.playCount, a.playCount) })
                playlist.type == Playlist.Type.RECENTLY_ADDED -> songs.sortWith(kotlin.Comparator { a, b -> ComparisonUtils.compareInt(b.dateAdded, a.dateAdded) })
                playlist.type == Playlist.Type.RECENTLY_PLAYED -> songs.sortWith(kotlin.Comparator { a, b -> ComparisonUtils.compareLong(b.lastPlayed, a.lastPlayed) })
            }
            if (playlist.canEdit) {
                songs.sortWith(kotlin.Comparator { a, b -> ComparisonUtils.compareLong(a.playlistSongPlayOrder, b.playlistSongPlayOrder) })
            }
        }
    }

    fun loadData() {
        PermissionUtils.RequestStoragePermissions {
            addDisposable(
                songsRepository.getSongs(playlist)
                    .map { it.toMutableList() }
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnNext { songs -> sortSongs(songs) }
                    .subscribe({ songs ->
                        this.songs = songs
                        view?.setData(songs)
                    }, { error ->
                        LogUtils.logException(TAG, "loadData error", error);
                    })
            )
        }
    }

    private fun startSlideShow() {
        val albumsObservable: Observable<List<Album>> = songsRepository.getSongs(playlist)
            .map { songs -> Operators.songsToAlbums(songs) }

        val timer: Observable<Long> = io.reactivex.Observable.interval(8, TimeUnit.SECONDS)
            // Load an image straight away
            .startWith(0L)
            // If we have a 'current slideshowAlbum' then we're coming back from onResume. Don't load a new one immediately.
            .delay(if (currentSlideShowAlbum == null) 0L else 8L, TimeUnit.SECONDS)

        addDisposable(Observable
            .combineLatest(albumsObservable, timer, BiFunction { albums: List<Album>, _: Long -> albums })
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

    fun play(song: Song) {
        mediaManager.playAll(songs, songs.indexOf(song), true) {
            view?.onPlaybackFailed()
        }
    }

    companion object {
        const val TAG = "PlaylistDetailPresenter"
    }
}
