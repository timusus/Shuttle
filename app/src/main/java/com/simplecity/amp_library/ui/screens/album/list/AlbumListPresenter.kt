package com.simplecity.amp_library.ui.screens.album.list

import android.annotation.SuppressLint
import com.simplecity.amp_library.data.AlbumsRepository
import com.simplecity.amp_library.model.AlbumArtist
import com.simplecity.amp_library.ui.common.Presenter
import com.simplecity.amp_library.ui.screens.album.list.AlbumListContract.View
import com.simplecity.amp_library.ui.screens.album.menu.AlbumMenuContract
import com.simplecity.amp_library.ui.screens.album.menu.AlbumMenuPresenter
import com.simplecity.amp_library.utils.LogUtils
import com.simplecity.amp_library.utils.sorting.SortManager
import io.reactivex.android.schedulers.AndroidSchedulers
import javax.inject.Inject

class AlbumsPresenter @Inject constructor(
    private val albumsRepository: AlbumsRepository,
    private val sortManager: SortManager,
    private val albumsMenuPresenter: AlbumMenuPresenter
) : Presenter<View>(), AlbumListContract.Presenter, AlbumMenuContract.Presenter by albumsMenuPresenter {

    private var albums = mutableListOf<AlbumArtist>()

    override fun bindView(view: View) {
        super.bindView(view)
        albumsMenuPresenter.bindView(view)
    }

    override fun unbindView(view: View) {
        super.unbindView(view)
        albumsMenuPresenter.unbindView(view)
    }

    @SuppressLint("CheckResult")
    override fun loadAlbums(scrollToTop: Boolean) {
        addDisposable(albumsRepository.getAlbums()
            .map { albumArtists ->
                val albumArtists = albumArtists.toMutableList()

                sortManager.sortAlbums(albumArtists)

                if (!sortManager.artistsAscending) {
                    albumArtists.reverse()
                }
                albumArtists
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { albumArtists ->
                    this.albums - albumArtists
                    view?.setData(albumArtists)
                },
                { error -> LogUtils.logException(TAG, "refreshAdapterItems error", error) }
            ))
    }

    override fun setAlbumsSortOrder(order: Int) {
        sortManager.artistsSortOrder = order
        loadAlbums(true)
        view?.invalidateOptionsMenu()
    }

    override fun setAlbumsAscending(ascending: Boolean) {
        sortManager.artistsAscending = ascending
        loadAlbums(true)
        view?.invalidateOptionsMenu()
    }

    companion object {
        const val TAG = "AlbumPresenter"
    }
}