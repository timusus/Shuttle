package com.simplecity.amp_library.ui.screens.artist.list

import android.annotation.SuppressLint
import com.simplecity.amp_library.data.Repository.AlbumArtistsRepository
import com.simplecity.amp_library.model.AlbumArtist
import com.simplecity.amp_library.ui.common.Presenter
import com.simplecity.amp_library.ui.screens.album.menu.AlbumArtistMenuContract
import com.simplecity.amp_library.ui.screens.album.menu.AlbumArtistMenuPresenter
import com.simplecity.amp_library.ui.screens.artist.list.AlbumArtistListContract.View
import com.simplecity.amp_library.utils.LogUtils
import com.simplecity.amp_library.utils.sorting.SortManager
import io.reactivex.android.schedulers.AndroidSchedulers
import javax.inject.Inject

class AlbumArtistListPresenter @Inject constructor(
    private val artistsRepository: AlbumArtistsRepository,
    private val sortManager: SortManager,
    private val albumArtistsMenuPresenter: AlbumArtistMenuPresenter
) :
    Presenter<View>(),
    AlbumArtistListContract.Presenter,
    AlbumArtistMenuContract.Presenter by albumArtistsMenuPresenter {

    private var albumArtists = mutableListOf<AlbumArtist>()

    override fun bindView(view: View) {
        super.bindView(view)
        albumArtistsMenuPresenter.bindView(view)
    }

    override fun unbindView(view: View) {
        super.unbindView(view)
        albumArtistsMenuPresenter.unbindView(view)
    }

    @SuppressLint("CheckResult")
    override fun loadAlbumArtists(scrollToTop: Boolean) {
        addDisposable(artistsRepository.getAlbumArtists()
            .map { albumArtists ->
                val albumArtists = albumArtists.toMutableList()

                sortManager.sortAlbumArtists(albumArtists)

                if (!sortManager.artistsAscending) {
                    albumArtists.reverse()
                }
                albumArtists
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { albumArtists ->
                    this.albumArtists - albumArtists
                    view?.setData(albumArtists)
                },
                { error -> LogUtils.logException(TAG, "refreshAdapterItems error", error) }
            ))
    }

    override fun setAlbumArtistsSortOrder(order: Int) {
        sortManager.artistsSortOrder = order
        loadAlbumArtists(true)
        view?.invalidateOptionsMenu()
    }

    override fun setAlbumArtistsAscending(ascending: Boolean) {
        sortManager.artistsAscending = ascending
        loadAlbumArtists(true)
        view?.invalidateOptionsMenu()
    }

    companion object {
        const val TAG = "AlbumArtistListPresenter"
    }
}