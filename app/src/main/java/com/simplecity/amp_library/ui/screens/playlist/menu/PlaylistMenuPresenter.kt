package com.simplecity.amp_library.ui.screens.playlist.menu

import com.simplecity.amp_library.data.Repository.SongsRepository
import com.simplecity.amp_library.model.Playlist
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.playback.MediaManager
import com.simplecity.amp_library.ui.common.Presenter
import com.simplecity.amp_library.ui.screens.album.menu.AlbumMenuPresenter
import com.simplecity.amp_library.utils.LogUtils
import com.simplecity.amp_library.utils.playlists.FavoritesPlaylistManager
import com.simplecity.amp_library.utils.playlists.PlaylistManager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class PlaylistMenuPresenter @Inject constructor(
    private val songsRepository: SongsRepository,
    private val mediaManager: MediaManager,
    private val playlistManager: PlaylistManager,
    private val favoritesPlaylistManager: FavoritesPlaylistManager
) :
    Presenter<PlaylistMenuContract.View>(),
    PlaylistMenuContract.Presenter {

    override fun playNext(playlist: Playlist) {
        getSongs(playlist) { songs ->
            mediaManager.playNext(songs) { numSongs ->
                view?.onSongsAddedToQueue(numSongs)
            }
        }
    }

    override fun play(playlist: Playlist) {
        getSongs(playlist) { songs ->
            mediaManager.playAll(songsRepository.getSongs(playlist).first(emptyList())) { view?.onPlaybackFailed() }
        }
    }

    override fun delete(playlist: Playlist) {
        getSongs(playlist) { songs ->
            view?.presentDeletePlaylistDialog(playlist)
        }
    }

    override fun edit(playlist: Playlist) {
        view?.presentEditDialog(playlist)
    }

    override fun rename(playlist: Playlist) {
        view?.presentRenameDialog(playlist)
    }

    override fun createM3uPlaylist(playlist: Playlist) {
        view?.presentM3uDialog(playlist)
    }

    override fun clear(playlist: Playlist) {
        playlist.clear(playlistManager, favoritesPlaylistManager)
    }

    private fun getSongs(playlist: Playlist, onSuccess: (songs: List<Song>) -> Unit) {
        addDisposable(
            songsRepository.getSongs(playlist).first(emptyList())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    onSuccess,
                    { error -> LogUtils.logException(AlbumMenuPresenter.TAG, "Failed to retrieve songs", error) }
                )
        )
    }
}