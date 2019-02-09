package com.simplecity.amp_library.ui.screens.songs.list

import com.simplecity.amp_library.data.SongsRepository
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.playback.MediaManager
import com.simplecity.amp_library.ui.common.Presenter
import com.simplecity.amp_library.ui.screens.songs.list.SongListContract.View
import com.simplecity.amp_library.ui.screens.songs.menu.SongMenuContract
import com.simplecity.amp_library.ui.screens.songs.menu.SongMenuPresenter
import com.simplecity.amp_library.utils.LogUtils
import com.simplecity.amp_library.utils.SettingsManager
import com.simplecity.amp_library.utils.sorting.SortManager
import javax.inject.Inject

class SongListPresenter @Inject constructor(
    private val songsRepository: SongsRepository,
    private val mediaManager: MediaManager,
    private val sortManager: SortManager,
    private val settingsManager: SettingsManager,
    private val songMenuPresenter: SongMenuPresenter
) : Presenter<View>(),
    SongListContract.Presenter,
    SongMenuContract.Presenter by songMenuPresenter {

    private var songs = mutableListOf<Song>()

    override fun bindView(view: View) {
        super.bindView(view)
        songMenuPresenter.bindView(view)
    }

    override fun unbindView(view: View) {
        super.unbindView(view)
        songMenuPresenter.unbindView(view)
    }

    override fun loadSongs(scrollToTop: Boolean) {
        addDisposable(songsRepository.getSongs()
            .map { songs ->
                val songs = songs.toMutableList()

                sortManager.sortSongs(songs)

                if (!sortManager.songsAscending) {
                    songs.reverse()
                }
                songs
            }
            .subscribe({ songs ->
                this.songs = songs
                view?.setData(songs, scrollToTop)
            }, { error ->
                LogUtils.logException(TAG, "Failed to load songs", error)
            })
        )
    }

    override fun setSongsSortOrder(order: Int) {
        sortManager.songsSortOrder = order
        loadSongs(true)
        view?.invalidateOptionsMenu()
    }

    override fun setSongsAscending(ascending: Boolean) {
        sortManager.songsAscending = ascending
        loadSongs(true)
        view?.invalidateOptionsMenu()
    }

    override fun setShowArtwork(show: Boolean) {
        settingsManager.setShowArtworkInSongList(show)
        loadSongs(false)
        view?.invalidateOptionsMenu()
    }

    override fun play(song: Song) {
        mediaManager.playAll(songs, songs.indexOf(song), true) {
            view?.showPlaybackError()
        }
    }

    override fun shuffleAll() {
        mediaManager.shuffleAll(songsRepository.getSongs().firstOrError()) {
            view?.showPlaybackError()
        }
    }

    companion object {
        const val TAG = "SongListPresenter"
    }

}

