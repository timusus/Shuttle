package com.simplecity.amp_library.ui.screens.playlist.list

import com.simplecity.amp_library.data.Repository.PlaylistsRepository
import com.simplecity.amp_library.data.Repository.SongsRepository
import com.simplecity.amp_library.ui.common.Presenter
import com.simplecity.amp_library.ui.screens.playlist.list.PlaylistListContract.View
import com.simplecity.amp_library.ui.screens.playlist.menu.PlaylistMenuPresenter
import com.simplecity.amp_library.utils.LogUtils
import com.simplecity.amp_library.utils.menu.playlist.PlaylistMenuCallbacks
import io.reactivex.android.schedulers.AndroidSchedulers
import javax.inject.Inject

class PlaylistListPresenter @Inject constructor(
    private val playlistsRepository: PlaylistsRepository,
    private val songsRepository: SongsRepository,
    private val playlistMenuPresenter: PlaylistMenuPresenter
) :
    Presenter<PlaylistListContract.View>(),
    PlaylistListContract.Presenter,
    PlaylistMenuCallbacks by playlistMenuPresenter {

    override fun bindView(view: View) {
        super.bindView(view)

        playlistMenuPresenter.bindView(view)
    }

    override fun unbindView(view: View) {
        super.unbindView(view)

        playlistMenuPresenter.unbindView(view)
    }

    override fun loadData() {
        addDisposable(playlistsRepository.getAllPlaylists(songsRepository)
            .map { playlists ->
                playlists.apply {
                    sortBy { playlist -> playlist.name }
                    sortBy { playlist -> playlist.type }
                }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { playlists -> view?.setData(playlists) },
                { error -> LogUtils.logException(TAG, "Failed to load data", error) }
            )
        )
    }

    companion object {
        private const val TAG = "PlaylistListPresenter"
    }
}