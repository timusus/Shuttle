package com.simplecity.amp_library.ui.screens.artist.detail

import android.support.v4.util.Pair
import com.simplecity.amp_library.data.Repository.SongsRepository
import com.simplecity.amp_library.model.Album
import com.simplecity.amp_library.model.AlbumArtist
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.playback.MediaManager
import com.simplecity.amp_library.ui.common.Presenter
import com.simplecity.amp_library.ui.screens.album.menu.AlbumArtistMenuContract
import com.simplecity.amp_library.ui.screens.album.menu.AlbumArtistMenuPresenter
import com.simplecity.amp_library.ui.screens.album.menu.AlbumMenuContract
import com.simplecity.amp_library.ui.screens.album.menu.AlbumMenuPresenter
import com.simplecity.amp_library.ui.screens.songs.menu.SongMenuContract
import com.simplecity.amp_library.ui.screens.songs.menu.SongMenuPresenter
import com.simplecity.amp_library.utils.LogUtils
import com.simplecity.amp_library.utils.Operators
import com.simplecity.amp_library.utils.sorting.SortManager
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers

class ArtistDetailPresenter @AssistedInject constructor(
    private val mediaManager: MediaManager,
    private val songsRepository: SongsRepository,
    private val sortManager: SortManager,
    private val artistsMenuPresenter: AlbumArtistMenuPresenter,
    private val albumsMenuPresenter: AlbumMenuPresenter,
    private val songsMenuPresenter: SongMenuPresenter,
    @Assisted private val albumArtist: AlbumArtist
) : Presenter<ArtistDetailView>(),
    AlbumArtistMenuContract.Presenter by artistsMenuPresenter,
    AlbumMenuContract.Presenter by albumsMenuPresenter,
    SongMenuContract.Presenter by songsMenuPresenter {

    @AssistedInject.Factory
    interface Factory {
        fun create(albumArtist: AlbumArtist): ArtistDetailPresenter
    }

    private var songs: MutableList<Song> = mutableListOf()

    override fun bindView(view: ArtistDetailView) {
        super.bindView(view)

        artistsMenuPresenter.bindView(view)
        albumsMenuPresenter.bindView(view)
        songsMenuPresenter.bindView(view)
    }

    override fun unbindView(view: ArtistDetailView) {
        super.unbindView(view)

        artistsMenuPresenter.unbindView(view)
        albumsMenuPresenter.unbindView(view)
        songsMenuPresenter.unbindView(view)
    }

    fun loadData() {
        addDisposable(
            albumArtist.getSongsSingle(songsRepository)
                .zipWith<MutableList<Album>, Pair<MutableList<Album>, MutableList<Song>>>(
                    albumArtist
                        .getSongsSingle(songsRepository)
                        .map { songs -> Operators.songsToAlbums(songs) },
                    BiFunction { songs, albums -> Pair(albums, songs) })
                .subscribeOn(Schedulers.io())
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

    private fun sortSongs(songs: MutableList<Song>) {
        @SortManager.SongSort val songSort = sortManager.artistDetailSongsSortOrder

        val songsAscending = sortManager.artistDetailSongsAscending

        sortManager.sortSongs(songs, songSort)
        if (!songsAscending) {
            songs.reverse()
        }
    }

    private fun sortAlbums(albums: MutableList<Album>) {
        @SortManager.AlbumSort val albumSort = sortManager.artistDetailAlbumsSortOrder

        val albumsAscending = sortManager.artistDetailAlbumsAscending

        sortManager.sortAlbums(albums, albumSort)
        if (!albumsAscending) {
            albums.reverse()
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
