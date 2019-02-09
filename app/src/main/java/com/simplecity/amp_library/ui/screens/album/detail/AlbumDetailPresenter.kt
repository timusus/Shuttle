package com.simplecity.amp_library.ui.screens.album.detail

import com.simplecity.amp_library.data.Repository.SongsRepository
import com.simplecity.amp_library.model.Album
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.playback.MediaManager
import com.simplecity.amp_library.ui.common.Presenter
import com.simplecity.amp_library.ui.screens.album.menu.AlbumMenuContract
import com.simplecity.amp_library.ui.screens.album.menu.AlbumMenuPresenter
import com.simplecity.amp_library.ui.screens.songs.menu.SongMenuContract
import com.simplecity.amp_library.ui.screens.songs.menu.SongMenuPresenter
import com.simplecity.amp_library.utils.LogUtils
import com.simplecity.amp_library.utils.extensions.getSongsSingle
import com.simplecity.amp_library.utils.sorting.SortManager
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class AlbumDetailPresenter @AssistedInject constructor(
    private val mediaManager: MediaManager,
    private val songsRepository: SongsRepository,
    private val sortManager: SortManager,
    private val albumsMenuPresenter: AlbumMenuPresenter,
    private val songsMenuPresenter: SongMenuPresenter,
    @Assisted private val album: Album
) : Presenter<AlbumDetailView>(),
    AlbumMenuContract.Presenter by albumsMenuPresenter,
    SongMenuContract.Presenter by songsMenuPresenter {

    @AssistedInject.Factory
    interface Factory {
        fun create(album: Album): AlbumDetailPresenter
    }

    private var songs: MutableList<Song> = mutableListOf()

    override fun bindView(view: AlbumDetailView) {
        super.bindView(view)

        songsMenuPresenter.bindView(view)
        albumsMenuPresenter.bindView(view)
    }

    override fun unbindView(view: AlbumDetailView) {
        super.unbindView(view)

        songsMenuPresenter.unbindView(view)
        albumsMenuPresenter.unbindView(view)
    }

    fun loadData() {
        addDisposable(
            album.getSongsSingle(songsRepository)
                .map { it.toMutableList() }
                .doOnSuccess { sortSongs(it) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { songs ->
                    this.songs = songs
                    view?.setData(songs)
                }
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

    private fun sortSongs(songs: MutableList<Song>) {
        @SortManager.SongSort val songSort = sortManager.albumDetailSongsSortOrder

        val songsAscending = sortManager.albumDetailSongsAscending

        sortManager.sortSongs(songs, songSort)
        if (!songsAscending) {
            songs.reverse()
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
}